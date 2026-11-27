# Zoho Payments replacement — Phase 1–15 backend

Updated 14 September 2026. New online payment intents use `gateway: "ZOHO"` with Zoho Payments India. The package includes source, a V22 migration, protocol checks, JUnit fixtures and Postman requests. Live account acceptance and full Maven/PostgreSQL execution remain pending; see [verification](VERIFICATION.md).

## Configure the server

Use `.env.example` and your existing environment loader. Set `ZOHO_PAYMENTS_ENABLED=true` only after supplying the matching account and credentials. The default is disabled, with `ZOHO_PAYMENTS_SANDBOX=true`.

| Environment variable | Purpose |
| --- | --- |
| `ZOHO_PAYMENTS_ACCOUNT_ID` | India account ID, kept as a numeric string |
| `ZOHO_PAYMENTS_API_KEY` | Public widget/SDK API key |
| `ZOHO_PAYMENTS_SIGNING_KEY` | Private checkout signature verification key |
| `ZOHO_PAYMENTS_WEBHOOK_SIGNING_KEY` | Private webhook signature key |
| `ZOHO_PAYMENTS_CLIENT_ID` | OAuth client ID |
| `ZOHO_PAYMENTS_CLIENT_SECRET` | Private OAuth client secret |
| `ZOHO_PAYMENTS_REFRESH_TOKEN` | Private offline OAuth refresh token |
| `ZOHO_PAYMENTS_REDIRECT_URI` | OAuth redirect URI, if required by your registered client |
| `ZOHO_PAYMENTS_SANDBOX` | `true` for sandbox; `false` for live, using matching credentials |

Keep private keys and OAuth tokens on the server. The public API key is distinct from both signing keys. Obtain an organization-scoped OAuth grant using the account's environment: sandbox scopes `ZohoPaySandbox.payments.CREATE`, `ZohoPaySandbox.payments.READ`, `ZohoPaySandbox.refunds.CREATE`, `ZohoPaySandbox.refunds.READ`; live scopes use `ZohoPay` instead. The organization identifier uses `zohopaysandbox.<account_id>` or `zohopay.<account_id>`. Register and validate OAuth redirect/state handling when obtaining the grant. Follow [Zoho authentication](https://www.zoho.com/in/payments/api/v1/authentication/).

The adapter uses `https://paymentssandbox.zoho.in/api/v1` or `https://payments.zoho.in/api/v1`, with OAuth refresh at `https://accounts.zoho.in/oauth/v2/token`. Hosts are fixed; redirects are disabled. Access tokens are cached until shortly before expiry. GET may refresh once on 401; financial POST operations are never automatically repeated. The [official Java SDK](https://github.com/zoho/zoho-payments-java-sdk) documents the India environment domains. No new Maven dependency was added.

## Customer checkout contract

1. POST `/api/v1/customer/payments/create-order`, bearer customer token and a saved `Idempotency-Key`. Example body: `{"referenceType":"RESERVATION","referenceId":"<local UUID>","gateway":"ZOHO"}`. The server calculates the amount in INR.
2. Read `data.id` (local payment UUID), `data.amount`, `data.currency` and `data.checkout`. Checkout contains `provider`, `paymentSessionId`, `accountId`, `apiKey`, `domain: "IN"`, and `sandbox`. Launch checkout only with an attached session. Existing response fields `gatewayOrderId` and `publicKey` remain aliases for the Zoho session ID and public widget key. Keep every provider ID as a string.
3. Initialize the matching Zoho SDK/widget with the returned account, public key and environment. Pass the session ID and the returned amount where the SDK requests it. The backend submits major decimal INR units to Zoho; it does not submit paise as rupees.
4. On SDK success, POST `/api/v1/customer/payments/verify` with `{"paymentId":"<local UUID>","gatewayPaymentId":"<Zoho payment ID>","signature":"<SDK signature>"}`. The server verifies HMAC-SHA256 over **payment ID + `|` + session ID**, using the private signing key, then fetches payment evidence. Only matching account/environment, session, amount, currency and captured success can mark payment SUCCESS.
5. If the callback is lost, POST `/api/v1/customer/payments/{id}/refresh`, then read the returned state. If the session was never attached, finance reconciliation is required. A checkout dialog's success alone is not paid evidence.

Use [Zoho's widget integration](https://www.zoho.com/in/payments/api/v1/widget/) and [signature verification](https://www.zoho.com/in/payments/developerdocs/signature-verification/). For Flutter, [Zoho's integration guide](https://www.zoho.com/in/payments/developerdocs/flutter/) documents `zoho_payments_flutter_sdk`, `ZpaymentsFlutterSdk.initialize`, `ZohoPaymentsCheckoutOptions`, and `showCheckout`. Its documented platform minimums are Android 26 and iOS 15.6; validate compatibility with your existing app before adoption. The frontend applications themselves are not included or modified in this backend package.

## Webhooks and recovery

Register HTTPS `/api/v1/payments/webhooks/zoho`. Configure `payment.succeeded`, `refund.succeeded`, and `refund.failed`. The header is `X-Zoho-Webhook-Signature: t=<Unix milliseconds>,v=<hex HMAC>`. The signed message is the exact timestamp, a dot, and the original raw request bytes. The event body includes `event_id`, `event_type`, `account_id`, `live_mode`, and `event_object.payment.payment_id` or `event_object.refund.refund_id`.

Verification checks the separate webhook key, a five-minute timestamp window, account/environment and a 256 KiB limit. Keep server clocks synchronized. A valid supported event is stored transactionally as a receipt plus a durable job before returning success; provider HTTP calls run later in a worker. Duplicate event/body pairs are acknowledged; reuse of an event ID with different bytes is rejected. Unsupported valid events are recorded without changing balances. A failed payment attempt does not release bill claims because another attempt can still succeed.

The worker leases one due job per poll (default five seconds), fetches authoritative provider evidence, and applies the same payment/refund checks. A payment event received before local session attachment can recover it through server-written session metadata. Retries use increasing delays, capped at one hour, with 12 attempts and two-minute leases. Monitor unfinished jobs, `attempts`, `next_attempt_at`, `lease_until` and `last_error` in `zoho_webhook_jobs`; exhausted jobs require finance review. Tune and load-test polling for the expected traffic. Account/environment changes prevent processing old-context jobs.

See [webhook verification](https://www.zoho.com/in/payments/developerdocs/webhooks/verification/) and [canonical event payloads](https://www.zoho.com/in/payments/api/v1/webhooks-events/). Test real delivery/retry signatures and processing latency with your sandbox account.

## Uncertain outcomes and refunds

The local payment and a one-time dispatch marker commit before session creation. The session contains `reference_number=<local payment UUID>` plus `meta_data` key `bookeat_payment_id`. After a timeout, reuse the same local idempotency key. Never create a second intent to bypass an uncertain result.

Finance POST `/api/v1/admin/payments/{id}/reconcile?gatewaySessionId=<known Zoho session ID>` fetches that session and validates metadata, reference, amount and currency before attachment. Omit the query parameter when the local session ID exists. The adapter does not invent a session search-by-receipt API. If no session ID or successful webhook is available, investigate provider records; this version conservatively retains the pending obligation. Expired/failed sessions do not automatically release claims or create replacements. Establish a reviewed operational resolution before live use. See the [session API](https://www.zoho.com/in/payments/api/v1/online-payment-session/).

Approved provider refunds POST to `/payments/{payment_id}/refunds` once, with major decimal amount and metadata `bookeat_refund_id=<local refund UUID>`. Zoho's own refund reference number is not the local correlation ID. Signed refund events or finance POST `/api/v1/admin/refunds/{id}/reconcile?gatewayRefundId=<known numeric Zoho refund ID>` fetch evidence and match payment, metadata and amount. Provider `succeeded` normalizes to internal `processed`; unknown outcomes remain PROCESSING until reconciled. There is no assumed provider idempotency guarantee for blind refund redispatch. See [payments](https://www.zoho.com/in/payments/api/v1/payments/) and [refunds](https://www.zoho.com/in/payments/api/v1/refunds/).

## Upgrade and acceptance

V1–V21 and the POM are byte-preserved. V22 adds account/environment snapshots and the webhook job table. Test both a fresh database and an upgrade from a restored existing database. Existing Razorpay labels and historical IDs remain readable; new Razorpay intents are rejected. The old adapter, configuration and webhook route are removed. Resolve outstanding Razorpay payments/refunds using the old integration/provider records before cutover; this Zoho-only adapter cannot reconcile them. Do not relabel old transactions as ZOHO.

In sandbox, verify checkout success/failure, tampered signature, wrong session/amount/account, repeated callbacks, duplicate/out-of-order webhooks, a webhook preceding local attachment, worker retry/restart, uncertain creation, refund approval/success/failure, and finance reconciliation. Validate the actual fee/tax fields with your account. Complete Maven/PostgreSQL gates in [deployment](PHASE9_15_DEPLOYMENT.md) before enabling live payments. CASH/WALLET remain supported; settlement records still require separately completed external transfers.
