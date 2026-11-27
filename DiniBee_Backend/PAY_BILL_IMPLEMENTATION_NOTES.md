# DiniBee Pay Bill - Integration Notes (base-preserving update)

This code was overlaid on Customer v68, Restaurant Partner v67, Backend v62 and Admin v30.
The previous Pay Bill source was reused selectively; existing files otherwise retained.

## Supported source features
- Customer manual restaurant bill creation and reading eligible bills
- Restaurant-side approval/rejection and bill listing
- Backend customer/owner/admin bill endpoints
- Razorpay order creation and verified capture for fully approved bills
- React Admin bill transactions listing
- Customer full bill / split bill selection UI. **Split payment gateway processing is intentionally disabled** pending transactional participant-level settlement APIs; no fraudulent partial payment success is simulated.

## NOT production-ready
- Do not deploy without Maven, Flutter and Vinext builds and PostgreSQL migration review.
- Razorpay webhook, ownership checks, idempotency, bank offers, cashback, refund, settlement and split billing need focused integration tests.
- No cashback amount should be presented or granted until the admin offer engine is implemented.
- Back up PostgreSQL and use Razorpay TEST environment first.
