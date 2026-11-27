# V58 DiniTicket Cancel / No Show Backend Fix

Backend fixes:
- Added owner endpoint `PUT /api/v1/owner/restaurants/{restaurantId}/dinitickets/{id}/cancel`.
- Owner cancellation uses the existing ticket transition and linked-order commitment guards.
- NO_SHOW transition now supports overdue WAITING pre-book tokens in addition to CALLED tokens.
- NO_SHOW still rejects attempts before the configured ticket expiry/deadline.
