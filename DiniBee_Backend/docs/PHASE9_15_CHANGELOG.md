# Phases 9–15 cumulative change log

## Added

- Phase 9: GPS/QR/manual arrival, hashed single-use QR tokens, dining sessions, bill freeze, checkout coverage and table release, DiniTicket dining bridge.
- Phase 10: server-derived payment reservations, CASH/WALLET paths, disabled-by-default online adapter (now Zoho), signature/provider verification, durable webhook deduplication, dispatch/reconciliation, commission rules, approved refunds and manual settlement ledger.
- Phase 11: customer wallet, immutable transaction history, audited admin adjustment, configured rewards/redemption, duplicate-claim prevention and proportional refunded-spend reward debt.
- Phase 12: completed-service reviews, visible aggregates and moderation, scoped support threads/messages, transactional in-app notifications and own inbox/read controls.
- Phase 13: safe admin account/role management, immediate session invalidation, protected platform settings, offers/coupons, real dashboard and scoped admin/owner reports with separate CSV/XLSX exporters.
- Phase 14: customer/owner/admin integration maps, centralized network/token/refresh guidance, staged mock migration and updated cumulative Postman examples.
- Phase 15: body/rate limits, location retention, audit reason evidence, pure-rule/export checks, JUnit wrappers and PostgreSQL/MockMvc/Mockito integration test source, deployment and acceptance documentation.

## Existing behavior updated

Takeaway collection and DiniTicket meal completion now require verified payment coverage. Dining and paid orders cannot bypass cancellation/payment guards. CORS permits Last-Event-ID for authenticated event reconnection. Historical Phase 8 integration scenarios now record cash payment before collection/completion. Wallet spend rewards reverse on refund even if points were already redeemed; debt offsets later awards without making money balance negative.

V1–V14 and dependency pins are preserved. V15–V22 are additive. All 166 prior Postman request definitions remain; 107 operations are added for 273 total. No customer, restaurant, money, reward or production credentials are seeded.

## Execution limits

Source parsing and dependency-free rule/export execution are recorded in VERIFICATION.md. Full Maven compilation, JUnit, PostgreSQL/Flyway runtime and provider integration are not executed in this environment. The package does not modify the three actual frontend apps or deploy a service. Financial/provider and business limitations are explicit in PHASE9_15_API.md and PHASE9_15_DEPLOYMENT.md.

## Zoho replacement — 14 September 2026

Replaced the active Razorpay adapter with Zoho Payments India OAuth, session checkout, server verification and approved refunds. Added timestamped webhook verification and a durable worker queue; bound new intents to account/environment. Added customer refresh and explicit-session finance recovery. V22 is additive; V1–V21 and the POM remain byte-identical. Legacy provider history remains readable. Updated Postman (274 requests), configuration, frontend handoff and protocol/JUnit fixtures. See [verification](VERIFICATION.md) for executed checks and pending runtime gates.
