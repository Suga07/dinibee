# V32 DiniTicket tracking fix

- Added authenticated `GET /api/v1/customer/dinitickets/all` pagination for persisted active and historical customer DiniTickets.
- Customer order tracking can now use a real DiniTicket even when no food preorder row exists.
- No mock DiniTicket orders are created.
