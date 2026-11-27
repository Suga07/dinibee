# Phases 9–15 API contracts

This is cumulative source through Phase 15. Read [verification](VERIFICATION.md) before deployment. Prefix every route below with the shown `/api/v1`; Postman's `baseUrl` already includes that prefix. Existing contracts remain in [API](API.md), [Phase 3](PHASE3_API.md), [Phase 4](PHASE4_API.md) and [Phases 5–8](PHASE5_8_API.md).

## Shared contract

Authenticated requests use the BookEat bearer token. Ownership derives from the authenticated user and database profiles; a request ID does not grant access. Customer IDs, user IDs, restaurant IDs and booking IDs are distinct. Admin wallet routes use the **customer profile UUID**. Support assignment uses an **admin user UUID**.

JSON returns `ApiResponse`; lists return its `data` containing `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`. CSV/XLSX exports return bytes. Unexpected request properties are rejected. Monetary JSON values are INR major units, with at most two decimal places; provider amounts are integer paise. Use decimal-safe handling in clients. Server-generated IDs, totals, commission, rewards and statuses are authoritative.

Versioned actions require the latest **resource** version and a real reason. Inventory/configuration writes retain their earlier parent `restaurantVersion` convention. Check-in/checkout and queue seating change restaurant versions: reload owner restaurant detail before the next inventory write. On a version conflict, reload and ask the user to reassess; do not silently overwrite.

A documented `Idempotency-Key` is generated once per intent and persisted with the exact request. Exact retries reuse it. Reusing it with a different body conflicts. A new key represents a genuinely new intent; it does not recover an unknown provider outcome. Payment status, ledger references, reward claims and webhook IDs have independent durable uniqueness controls.

Common errors: 400 invalid request/proof/radius; 401 expired or revoked auth; 403 missing permission; 404 missing or unowned resource; 409 state/version/payment/reward conflict; 413 body too large; 429 rate limit; 503 unconfigured/unavailable provider. Inspect the stable `errorCode` and message; do not parse human text. Public IDs may return 404 for ownership protection.

## Phase 9: arrival and dining

A confirmed host reservation can check in during `[startsAt, min(endsAt, max(noShowAfter, startsAt + 15 minutes)))`, while its active allocation covers the current instant and its table is AVAILABLE. The restaurant and service must remain enabled. Group members do not independently occupy the same table; the host arrival represents the party.

GPS requires explicit consent, server-side distance checking and enabled venue settings. Coordinates are self-reported and are **not device-attested proof**. The QR path requires a fresh 256-bit token issued by the owner for that reservation; the server stores only its hash. Tokens expire after at most five minutes, are single-use and are not booking IDs. Owner manual arrival uses the same reservation and allocation gates.

Typical flow: check-in → CHECKED_IN → owner start → DINING → request-payment → PAYMENT_PENDING or PAID → verified coverage → owner checkout → CHECKED_OUT. Bill request freezes further meal additions. All bill items must be served/completed or cancelled/rejected before requesting payment. Checkout rejects unpaid coverage and releases the table/allocation only after the dining gates pass. A zero-value bill can check out. The session may remain PAYMENT_PENDING after payment capture until the next lifecycle action; derive receipt status from the payment API, then refresh/checkout the session.

DiniTicket seating creates the corresponding dining session. Direct ticket completion also enforces paid food obligations. Location is omitted from outward session DTOs, erased on checkout and purged after 24 hours; on-the-way latest location expires after one hour. There is no location-history collection.

## Phase 10: payments, refunds and settlement

Create an order with `referenceType`, `referenceId`, `gateway` and optional `couponCode`. Never send amount. Supported references are FOOD_ORDER, TAKEAWAY, RESERVATION, GROUP_BOOKING, CORPORATE_BOOKING, DINITICKET and DINING_BILL. A RESERVATION payment covers linked food orders; it does not invent a reservation fee/deposit. Individual FOOD_ORDER cannot bypass group, takeaway or company billing rules. Corporate payment uses the organizer's booking; invoice requirements remain an intent, not generated tax invoices.

Group HOST_PAYS is host-only; SPLIT_BY_ITEMS covers that customer's items; SPLIT_EQUAL requires a LOCKED roster and deterministic cent allocation across joined members. Group dining bills use GROUP_BOOKING rather than DINING_BILL. Coupons are excluded from SPLIT_EQUAL. Payment lines reserve remaining gross food obligations while the payment amount records the net customer charge. Coupons are rechecked under lock; preview is advisory and does not reserve anything. A code is one-use per customer, obeys a global cap and reserves its use on payment creation, including a later cancelled cash intent. Do not promise automatic coupon restoration. Offers use the same explicit code rules; there is no silent stacking.

CASH creates PENDING; the owner with OWNER_PAYMENT_MANAGE records actual receipt to make it SUCCESS. A customer can cancel only an unconfirmed cash intent. WALLET debits the server ledger atomically and succeeds only with sufficient balance. Takeaway collection now requires verified coverage. Cancel/reject cannot silently discard an outstanding or successful payment; resolve the payment/refund first.

ZOHO is the online provider, disabled by default. Use `gateway: "ZOHO"` and the returned `checkout` object to initialize the matching India sandbox/live SDK. Submit `paymentId`, `gatewayPaymentId`, `signature` for server verification. Only matching captured INR evidence marks success. Fees and tax are stored separately. See [Zoho setup and exact contracts](ZOHO_PAYMENTS.md).

Session creation is dispatched once. Unknown results require the same idempotency key and reconciliation, never a blind replacement. Finance can provide a known `gatewaySessionId` to `/admin/payments/{id}/reconcile`; no receipt-search API is assumed. Customers can POST `/customer/payments/{id}/refresh` after a lost callback when the session is already attached.

Webhook POST `/api/v1/payments/webhooks/zoho` requires `X-Zoho-Webhook-Signature` over timestamp plus raw bytes. Account/environment checks, durable event deduplication and a background job precede provider fetch verification. The supported financial events are `payment.succeeded`, `refund.succeeded` and `refund.failed`. Unsupported valid events only create receipts.

Refunds reserve the requested amount under the payment/restaurant lock. Only successful or partly refunded own payments are eligible; concurrent total requests cannot exceed captured amount. A separate authorized reviewer approves/rejects; the requester cannot approve their own. Processing is dispatched once. Wallet refunds credit immediately, cash refunds require actual owner refund receipt, provider refunds wait for confirmed `processed` evidence. Unknown provider refunds stay PROCESSING; reconcile by the provider refund ID with matching local refund metadata/payment/amount. Refunded obligations are treated as waived, not automatically re-billed.

Settlement creation assigns each eligible captured noncash payment once, excluding pending refunds. A settlement is a ledger record, **not a bank-transfer instruction**. Net = captured gross − refunds − proportional commission − fee − tax. Negative net stays ON_HOLD. Completion requires a real external transfer reference and manual finance status progression. CASH is excluded because owners already received it; cash commission collection remains an external receivable workflow. A payment already assigned to settlement cannot request another automatic refund: finance must arrange a separate funded adjustment outside this API. Do not bypass the guard by editing database amounts. This limitation must be resolved operationally before enabling live settlements.

## Phase 11: wallet and rewards

Wallet balances and append-only transactions are server controlled and denominated in INR. No top-up, withdrawal or bank wallet integration is included. Admin adjustments require WALLET_MANAGE, an idempotency key and an auditable reason. Debits never allow a negative money balance.

No financial/reward rules are seeded. Finance configures effective rules. SPEND rewards require completed linked orders, a successful non-wallet payment, threshold eligibility and no reserved refund. FIRST_BOOKING is once per customer after completion; REVIEW requires that customer's visible review. Duplicate claims cannot mint another reward even with a new key. Rule edits are represented by new effective rules and disabling old rules.

Redemption uses server conversion rules and commits point debit + wallet credit together. SPEND refunds proportionally reverse awarded points. If those points were already used, `rewardDebt` records the shortfall; future awards repay it first. Ledger `points` is the entitlement change; `pointsBefore/After` and `debtBefore/After` explain the actual balance effect. Money balances do not become negative. FIRST_BOOKING/REVIEW grants do not automatically reverse with a payment refund. REFERRAL/BIRTHDAY rules can be configured but automated claims are disabled until authoritative referral/birthday evidence is implemented; approved adjustments remain available.

## Phase 12: reviews, support and notifications

A review refers to exactly one completed own reservation or food order. Rating is 1–5; optional subratings follow the same range. Lifetime uniqueness includes soft-deleted reviews. Owners can reply to visible reviews for their restaurant. Admin hiding/restoring uses a version and reason. Editing a hidden review cannot restore it; discovery ratings aggregate only visible reviews. Deletion removes public text/reply.

Customer and owner support tickets are private to the creator and authorized support admins. Messages are paginated and idempotent. Assignment requires an active admin with SUPPORT_MANAGE. Closed tickets reject new messages; explicit reopening goes to OPEN. A creator response moves to IN_PROGRESS; admin response waits for the appropriate creator. Attachments are not integrated in this delivery (`attachmentUrl` stays absent/null).

Notifications are database-backed in-app items from actual committed business audit events. Customer, owner and authorized admin inbox routes expose only the caller's items. Read/read-all is scoped. Payment, refund, booking, meal-ready, queue-called, reward, settlement and support-reply events are covered. No push/SMS or marketing broadcast is sent by this module.

## Phase 13: admin and reports

Admin safe user views omit credential material. Block/deactivate and role changes invalidate existing sessions through token-version checks. Self-management and the last active super-admin are protected. The role API assigns existing administrator roles; it cannot manufacture customer/owner profiles or arbitrary permission codes. Existing restaurant verification/suspension and owner settings APIs remain in Phase 3.

Business oversight uses the scoped reservation/order/takeaway/DiniTicket reports and existing lifecycle services. There is no unrestricted generic endpoint to overwrite paid booking/order states. Finance, wallet, review and support operations use their dedicated permissions. Promotion management is create/list/disable; history is retained. Settings allow only support.email, support.phone, platform.display-name, terms.url and privacy.url (HTTPS URLs), never secrets.

Admin report types: RESERVATIONS, REVENUE, RESTAURANTS, CUSTOMERS, ORDERS, TAKEAWAY, DINITICKET, PAYMENTS, SETTLEMENTS, REFUNDS, WALLET, REWARDS, REVIEWS, SUPPORT. Owner reports: DAILY_SALES, MONTHLY_SALES, RESERVATIONS, TABLE_OCCUPANCY, MENU_PERFORMANCE, ORDERS, TAKEAWAY, DINITICKET, CUSTOMER_COUNT, RATINGS; REVENUE/REVIEWS also supported. The owner restaurant path is enforced even if a query parameter supplies another restaurant. Sensitive admin reports require their additional finance, wallet, support or user permission.

`fromDate` and `toDate` are inclusive dates, bounded to at most 366 days difference. Admin timezone is UTC; owner dates/buckets use the venue timezone. Optional restaurantId/status/serviceType/city apply only where the report has the corresponding relationship; unsupported filters fail validation. **serviceType filters venues with that enabled service**, not a payment's historical transaction type. Status uses that report's entity status. Reports without a status field reject status filtering. Paging is 0-based, size 1–100. Export is csv or xlsx, at most 10,000 matching rows; narrow the date range on overflow.

Revenue uses captured payments less refunds, including cash, rather than uncollected order totals. Menu performance counts completed food item subtotals; customer count is distinct customers on completed orders. Occupancy sums session minutes for sessions checked in during the date range; it is not a capacity-utilization percentage and is not clipped at the report boundary. Dashboard trends return observed days, so the frontend may explicitly fill missing chart days with zero. All totals derive from SQL, not mocks.

CSV escapes spreadsheet formula prefixes. XLSX uses text cells and contains no formulas; numeric strings preserve exact decimals but spreadsheet users must explicitly convert them for arithmetic. Export formatting is isolated in ReportExporter; controllers only set transport headers.

## Endpoint index

`Key` means Idempotency-Key is mandatory. Request schemas and populated templates are in [PHASE9_15_REQUEST_EXAMPLES.json](PHASE9_15_REQUEST_EXAMPLES.json). The Postman collection includes every operation below, alongside all original 166 request definitions.

| Method | Route | Request DTO | Key | Authority |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/customer/reservations/{id}/check-in/gps` | DiningRequests.Gps | — | `hasRole('CUSTOMER')` |
| POST | `/api/v1/customer/reservations/{id}/check-in/qr` | DiningRequests.Qr | — | `hasRole('CUSTOMER')` |
| POST | `/api/v1/owner/restaurants/{restaurantId}/reservations/{id}/manual-check-in` | DiningRequests.Action | — | `hasRole('OWNER') and hasAuthority('DINING_MANAGE')` |
| POST | `/api/v1/owner/restaurants/{restaurantId}/reservations/{id}/check-in-token` | — | — | `hasRole('OWNER') and hasAuthority('DINING_MANAGE')` |
| GET | `/api/v1/customer/dining-sessions/current` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/dining-sessions/{id}` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/owner/restaurants/{restaurantId}/dining-sessions` | — | — | `hasRole('OWNER') and hasAuthority('DINING_MANAGE')` |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/dining-sessions/{id}/start` | DiningRequests.Action | — | `hasRole('OWNER') and hasAuthority('DINING_MANAGE')` |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/dining-sessions/{id}/request-payment` | DiningRequests.Action | — | `hasRole('OWNER') and hasAuthority('DINING_MANAGE')` |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/dining-sessions/{id}/checkout` | DiningRequests.Action | — | `hasRole('OWNER') and hasAuthority('DINING_MANAGE')` |
| POST | `/api/v1/customer/payments/create-order` | PaymentRequests.Create | yes | `hasRole('CUSTOMER')` |
| POST | `/api/v1/customer/payments/verify` | PaymentRequests.Verify | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/payments` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/payments/{id}` | — | — | `hasRole('CUSTOMER')` |
| PUT | `/api/v1/customer/payments/{id}/cancel-cash` | PaymentRequests.Action | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/owner/restaurants/{restaurantId}/payments` | — | — | `hasRole('OWNER') and hasAuthority('OWNER_PAYMENT_VIEW')` |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/payments/{id}/confirm-cash` | PaymentRequests.Action | — | `hasRole('OWNER') and hasAuthority('OWNER_PAYMENT_MANAGE')` |
| POST | `/api/v1/customer/refunds` | PaymentRequests.RefundRequest | yes | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/payments/{id}/refunds` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/admin/payments` | — | — | `hasAuthority('PAYMENT_VIEW')` |
| GET | `/api/v1/admin/payments/{id}` | — | — | `hasAuthority('PAYMENT_VIEW')` |
| POST | `/api/v1/admin/payments/{id}/reconcile` | — | — | `hasAuthority('SETTLEMENT_MANAGE')` |
| GET | `/api/v1/admin/refunds` | — | — | `hasAuthority('REFUND_APPROVE')` |
| PUT | `/api/v1/admin/refunds/{id}/approve` | PaymentRequests.Action | — | `hasAuthority('REFUND_APPROVE')` |
| PUT | `/api/v1/admin/refunds/{id}/reject` | PaymentRequests.Action | — | `hasAuthority('REFUND_APPROVE')` |
| POST | `/api/v1/admin/refunds/{id}/process` | — | — | `hasAuthority('REFUND_APPROVE')` |
| POST | `/api/v1/admin/refunds/{id}/reconcile` | — | — | `hasAuthority('REFUND_APPROVE')` |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/refunds/{id}/confirm-cash` | PaymentRequests.Action | — | `hasRole('OWNER') and hasAuthority('OWNER_PAYMENT_MANAGE')` |
| GET | `/api/v1/admin/commission-rules` | — | — | `hasAuthority('COMMISSION_MANAGE')` |
| POST | `/api/v1/admin/commission-rules` | PaymentRequests.Commission | — | `hasAuthority('COMMISSION_MANAGE')` |
| PUT | `/api/v1/admin/commission-rules/{id}/disable` | PaymentRequests.Action | — | `hasAuthority('COMMISSION_MANAGE')` |
| POST | `/api/v1/admin/settlements` | PaymentRequests.SettlementCreate | yes | `hasAuthority('SETTLEMENT_MANAGE')` |
| GET | `/api/v1/admin/settlements` | — | — | `hasAuthority('SETTLEMENT_MANAGE')` |
| PUT | `/api/v1/admin/settlements/{id}/status` | PaymentRequests.SettlementAction | — | `hasAuthority('SETTLEMENT_MANAGE')` |
| GET | `/api/v1/owner/restaurants/{restaurantId}/settlements` | — | — | `hasRole('OWNER') and hasAuthority('OWNER_PAYMENT_VIEW')` |
| POST | `/api/v1/payments/webhooks/zoho` | RAW_WEBHOOK | — | `PUBLIC` |
| GET | `/api/v1/customer/wallet` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/rewards` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/wallet/transactions` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/rewards/history` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/rewards/how-to-earn` | — | — | `hasRole('CUSTOMER')` |
| POST | `/api/v1/customer/rewards/calculate-redemption` | WalletRequests.Redemption | — | `hasRole('CUSTOMER')` |
| POST | `/api/v1/customer/rewards/redeem` | WalletRequests.Redemption | yes | `hasRole('CUSTOMER')` |
| POST | `/api/v1/customer/rewards/claim` | WalletRequests.Claim | yes | `hasRole('CUSTOMER')` |
| GET | `/api/v1/admin/wallets` | — | — | `hasAuthority('WALLET_MANAGE')` |
| GET | `/api/v1/admin/wallets/{id}` | — | — | `hasAuthority('WALLET_MANAGE')` |
| POST | `/api/v1/admin/wallets/{id}/adjust` | WalletRequests.Adjustment | yes | `hasAuthority('WALLET_MANAGE')` |
| POST | `/api/v1/admin/reward-rules` | WalletRequests.Rule | — | `hasAuthority('REWARD_MANAGE')` |
| PUT | `/api/v1/admin/reward-rules/{id}/disable` | PaymentRequests.Action | — | `hasAuthority('REWARD_MANAGE')` |
| POST | `/api/v1/customer/reviews` | ReviewRequests.Create | yes | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/reviews` | — | — | `hasRole('CUSTOMER')` |
| PUT | `/api/v1/customer/reviews/{id}` | ReviewRequests.Update | — | `hasRole('CUSTOMER')` |
| DELETE | `/api/v1/customer/reviews/{id}` | ReviewRequests.Action | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/restaurants/{restaurantId}/reviews` | — | — | `PUBLIC` |
| POST | `/api/v1/owner/restaurants/{restaurantId}/reviews/{id}/reply` | ReviewRequests.Reply | — | `hasRole('OWNER') and hasAuthority('RESTAURANT_OWN_MANAGE')` |
| PUT | `/api/v1/admin/reviews/{id}/hide` | ReviewRequests.Action | — | `hasAuthority('REVIEW_MODERATE')` |
| PUT | `/api/v1/admin/reviews/{id}/restore` | ReviewRequests.Action | — | `hasAuthority('REVIEW_MODERATE')` |
| POST | `/api/v1/customer/support/tickets` | SupportRequests.Create | yes | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/support/tickets` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/support/tickets/{id}` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/customer/support/tickets/{id}/messages` | — | — | `hasRole('CUSTOMER')` |
| POST | `/api/v1/customer/support/tickets/{id}/messages` | SupportRequests.Message | yes | `hasRole('CUSTOMER')` |
| PUT | `/api/v1/customer/support/tickets/{id}/close` | SupportRequests.Action | — | `hasRole('CUSTOMER')` |
| POST | `/api/v1/owner/support/tickets` | SupportRequests.Create | yes | `hasRole('OWNER')` |
| GET | `/api/v1/owner/support/tickets` | — | — | `hasRole('OWNER')` |
| GET | `/api/v1/owner/support/tickets/{id}` | — | — | `hasRole('OWNER')` |
| GET | `/api/v1/owner/support/tickets/{id}/messages` | — | — | `hasRole('OWNER')` |
| POST | `/api/v1/owner/support/tickets/{id}/messages` | SupportRequests.Message | yes | `hasRole('OWNER')` |
| PUT | `/api/v1/owner/support/tickets/{id}/close` | SupportRequests.Action | — | `hasRole('OWNER')` |
| GET | `/api/v1/admin/support/tickets` | — | — | `hasAuthority('SUPPORT_MANAGE')` |
| GET | `/api/v1/admin/support/tickets/{id}` | — | — | `hasAuthority('SUPPORT_MANAGE')` |
| GET | `/api/v1/admin/support/tickets/{id}/messages` | — | — | `hasAuthority('SUPPORT_MANAGE')` |
| POST | `/api/v1/admin/support/tickets/{id}/messages` | SupportRequests.Message | yes | `hasAuthority('SUPPORT_MANAGE')` |
| PUT | `/api/v1/admin/support/tickets/{id}/assign` | SupportRequests.Assign | — | `hasAuthority('SUPPORT_MANAGE')` |
| PUT | `/api/v1/admin/support/tickets/{id}/status` | SupportRequests.Status | — | `hasAuthority('SUPPORT_MANAGE')` |
| GET | `/api/v1/customer/notifications` | — | — | `hasRole('CUSTOMER')` |
| PUT | `/api/v1/customer/notifications/{id}/read` | — | — | `hasRole('CUSTOMER')` |
| PUT | `/api/v1/customer/notifications/read-all` | — | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/owner/notifications` | — | — | `hasRole('OWNER')` |
| PUT | `/api/v1/owner/notifications/{id}/read` | — | — | `hasRole('OWNER')` |
| PUT | `/api/v1/owner/notifications/read-all` | — | — | `hasRole('OWNER')` |
| GET | `/api/v1/admin/users` | — | — | `hasAuthority('USER_BLOCK')` |
| GET | `/api/v1/admin/users/{id}` | — | — | `hasAuthority('USER_BLOCK')` |
| PUT | `/api/v1/admin/users/{id}/block` | AdminRequests.Action | — | `hasAuthority('USER_BLOCK')` |
| PUT | `/api/v1/admin/users/{id}/unblock` | AdminRequests.Action | — | `hasAuthority('USER_BLOCK')` |
| PUT | `/api/v1/admin/users/{id}/activate` | AdminRequests.Action | — | `hasAuthority('USER_BLOCK')` |
| PUT | `/api/v1/admin/users/{id}/deactivate` | AdminRequests.Action | — | `hasAuthority('USER_BLOCK')` |
| POST | `/api/v1/admin/users` | AdminRequests.Create | — | `hasAuthority('ADMIN_MANAGE')` |
| PUT | `/api/v1/admin/users/{id}/roles` | AdminRequests.Roles | — | `hasAuthority('ADMIN_ROLE_MANAGE')` |
| GET | `/api/v1/admin/roles` | — | — | `hasAuthority('ADMIN_MANAGE')` |
| GET | `/api/v1/admin/permissions` | — | — | `hasAuthority('ADMIN_MANAGE')` |
| GET | `/api/v1/admin/settings` | — | — | `hasAuthority('PLATFORM_SETTINGS_MANAGE')` |
| PUT | `/api/v1/admin/settings/{settingKey}` | AdminRequests.Setting | — | `hasAuthority('PLATFORM_SETTINGS_MANAGE')` |
| GET | `/api/v1/admin/offers` | — | — | `hasAuthority('COUPON_MANAGE')` |
| POST | `/api/v1/admin/offers` | AdminRequests.Promotion | — | `hasAuthority('COUPON_MANAGE')` |
| GET | `/api/v1/admin/coupons` | — | — | `hasAuthority('COUPON_MANAGE')` |
| POST | `/api/v1/admin/coupons` | AdminRequests.Promotion | — | `hasAuthority('COUPON_MANAGE')` |
| PUT | `/api/v1/admin/promotions/{id}/disable` | AdminRequests.Action | — | `hasAuthority('COUPON_MANAGE')` |
| POST | `/api/v1/customer/coupons/validate` | AdminRequests.Coupon | — | `hasRole('CUSTOMER')` |
| GET | `/api/v1/admin/dashboard` | — | — | `hasAuthority('REPORT_VIEW')` |
| GET | `/api/v1/admin/reports/{reportType}` | — | — | `hasAuthority('REPORT_VIEW')` |
| GET | `/api/v1/admin/reports/{reportType}/export/{format}` | — | — | `hasAuthority('REPORT_VIEW')` |
| GET | `/api/v1/owner/restaurants/{restaurantId}/reports/{reportType}` | — | — | `hasRole('OWNER') and hasAuthority('OWNER_REPORT_VIEW')` |
| GET | `/api/v1/owner/restaurants/{restaurantId}/reports/{reportType}/export/{format}` | — | — | `hasRole('OWNER') and hasAuthority('OWNER_REPORT_VIEW')` |
| GET | `/api/v1/admin/notifications` | — | — | `hasAuthority('NOTIFICATION_MANAGE')` |
| PUT | `/api/v1/admin/notifications/{id}/read` | — | — | `hasAuthority('NOTIFICATION_MANAGE')` |
| PUT | `/api/v1/admin/notifications/read-all` | — | — | `hasAuthority('NOTIFICATION_MANAGE')` |
| POST | `/api/v1/customer/payments/{id}/refresh` | — | — | CUSTOMER |
