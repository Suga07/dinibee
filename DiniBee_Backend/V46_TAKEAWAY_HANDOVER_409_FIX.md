# V46 Takeaway Handover 409 Fix

- `collect-by-order-id` is idempotent for already COLLECTED / linked COMPLETED orders.
- Cash/pay-at-restaurant is settled before the food-order payment guard.
- Stale/double handover requests return the current completed order instead of 409.
- Public four-character order number remains the verification value; UUID remains internal.
