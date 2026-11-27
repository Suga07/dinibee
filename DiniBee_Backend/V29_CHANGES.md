# v29 fixes
- DiniTicket token numbering is now sequential per restaurant from all persisted tickets.
- PostgreSQL transaction advisory lock prevents duplicate token numbers under concurrent bookings.
- Token numbering no longer depends on active queue contents or a global sequence.
- Existing customer review REST controller remains at `/api/v1/customer/reviews`.
