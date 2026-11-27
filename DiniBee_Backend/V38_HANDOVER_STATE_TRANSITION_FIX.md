# v38 Handover State Transition Fix

- DiniTicket token verification now persists a distinct handed-over stage instead of immediately completing the ticket.
- Existing database states are reused safely: `dini_tickets.SEATED` represents handed-over and linked `food_orders.SERVED` represents handed-over.
- `PUT /owner/restaurants/{restaurantId}/dinitickets/{id}/handover` stops at handed-over.
- `PUT /owner/restaurants/{restaurantId}/dinitickets/{id}/complete` completes the linked food order first, then completes the DiniTicket.
- Final completion is idempotent.
