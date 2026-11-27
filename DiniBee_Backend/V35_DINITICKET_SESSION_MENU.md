# v35 DiniTicket Session Menu

- Adds owner DiniTicket session-food endpoints under `/api/v1/owner/restaurants/{restaurantId}/dinitickets/session-menu`.
- Session food is stored as normal menu items in a dedicated `DiniTicket Session` category so the existing food-order pipeline can use real menu item IDs.
- Public DiniTicket menu now exposes only the active session category; Dine-in/Takeaway exclude it.
- When tickets issued since the session menu was created reach `maximumQueueSize`, session food is soft-deleted automatically. Historical orders remain intact.
- Adds image upload endpoint for DiniTicket session food.
