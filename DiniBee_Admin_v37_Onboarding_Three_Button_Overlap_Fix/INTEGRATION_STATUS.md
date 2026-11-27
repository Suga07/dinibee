# DiniBee Pay Bill v2 — source integration status

Base: Customer v71, Backend v64, Restaurant Partner v69, Admin v32. New Flyway migration V65.

## Added in this package
- Database-backed equal-share split allocation (host provides registered customer UUIDs), individual Razorpay orders, server-side capture verification and full-settlement aggregation. Bill can no longer use full-bill order once split allocation exists.
- Public-to-customer non-premium offer listing and admin-side offer create/list APIs. Premium offers intentionally excluded until existing membership entitlement model is wired and tested.
- Backend receipt data endpoint for PAID bills; customer bill screen has receipt summary viewer and offers listing.
- Admin Pay Bill Offers source component mounted in the existing Payments Management page.
- Refund/settlement adjustment records schema + admin read-only listing endpoint for future gateway reconciliation.

## NOT COMPLETE / DO NOT USE IN PRODUCTION
- Admin offer UI can create/list offers; no edit/deactivate endpoints.
- Offers are *display only*: not discounted from Razorpay amount and cashback is not awarded. No stacking, bank BIN validation or membership eligibility.
- Guest invites, QR joining, split participants selection UX and per-guest split checkout screen are not wired into Flutter yet; backend uses pre-existing registered customer UUIDs.
- Refund initiation and settlement payout/reconciliation NOT implemented. No raw status toggles were added; posting a refund without the gateway's confirmation is unsafe.
- Receipt endpoint provides JSON for a paid bill, not a signed PDF/download or tax invoice.
- Existing payment paths and migration were not tested with PostgreSQL / Maven / Flutter / live Razorpay.
- Do NOT deploy this directly to production; verify V63+V65 migrations on a staging DB, run Maven build and integration tests with test keys.
