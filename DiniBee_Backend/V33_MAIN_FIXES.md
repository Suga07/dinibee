# v33 Main Fixes

## Review submit
- POST `/api/v1/customer/reviews` is now registered on the core customer controller.
- Added compatibility alias POST `/api/v1/customer/reviews/submit`.
- Existing GET/PUT/DELETE review APIs remain under `/api/v1/customer/reviews`.

## Restaurant Partner live DiniTicket
- Added GET `/api/v1/owner/restaurants/{restaurantId}/dinitickets/live`.
- Backend is the source of truth for the current/now-serving token.
- Priority: CALLED/ARRIVED/SEATED token; if none exists, first WAITING token is exposed immediately.
- Returns next waiting tokens and live queue counts.
- Restaurant manager role is supported when it has DINITICKET_MANAGE permission.
