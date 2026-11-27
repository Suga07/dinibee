# Razorpay webhook deployment

## Endpoint

`POST https://api.dinibee.tech/api/v1/payments/razorpay/webhook`

The endpoint is public at the JWT layer but authenticates every request with
`X-Razorpay-Signature` over the unmodified request body. Duplicate deliveries
are deduplicated through the existing `payment_webhook_events` table.

## VPS environment

Keep real credentials only in `/etc/dinibee/dinibee.env`:

```text
RAZORPAY_KEY_ID=<current Razorpay key id>
RAZORPAY_KEY_SECRET=<current Razorpay key secret>
RAZORPAY_WEBHOOK_SECRET=<new random webhook secret>
RAZORPAY_WEBHOOK_ENABLED=true
```

Generate the webhook secret on the VPS with:

```bash
openssl rand -hex 32
```

Use the exact same webhook secret in the Razorpay Dashboard and the VPS
environment. Never put it in Flutter, React, or committed source code.

## Active events

- `payment.captured`
- `payment.failed`
- `order.paid`
- `refund.processed`

## Build

```bash
mvn clean package -DskipTests
```

Deploy the newly generated `target/bookeat-backend-0.1.0.jar`, restart the
`dinibee` systemd service, and confirm:

```bash
curl -s https://api.dinibee.tech/actuator/health
```

Expected status is `UP` before enabling the webhook in Razorpay.

## Covered flows

- Customer Razorpay payments: captured payments reconcile to DiniBee payments.
- Membership checkout: captured membership orders activate the purchased plan.
- Failed attempts: failure evidence is recorded without prematurely making the
  whole Razorpay order unretryable.
- Refunds: processed provider refunds reconcile to the matching local refund.
- Duplicate deliveries: same event id and payload are acknowledged once;
  conflicting reuse of an event id is rejected.

