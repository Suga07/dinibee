# V56 DiniTicket Read-only GET Fix

Fixed owner DiniTicket GET requests returning 409 during polling.

Root cause:
- owner ticket list/detail GET requests called expireLocked().
- That could mutate an expired ticket while reading.
- Linked food-order commitments could reject the implicit expiry with HTTP 409 ORDER_IN_PROGRESS.
- Restaurant Partner aborted its snapshot and showed empty Menu, Pre-orders, Live Queue and History.

Fix:
- Owner list/detail GET endpoints are read-only and no longer perform expiry transitions.
- Existing DiniTicket lifecycle transitions remain handled by explicit actions/lifecycle flows.
