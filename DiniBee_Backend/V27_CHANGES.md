# v27 fixes
- Repairs `dini_ticket_sequence` from persisted tickets so restored/imported databases do not reuse DT numbers.
- DiniTicket numbers continue from the highest persisted sequence and remain unique.
- Existing reservation ARRIVED transition does not allocate another table or create another reservation; availability remains derived from the existing reservation/table/date.
- Customer review GET/POST routes are present at `/api/v1/customer/reviews` and backed by persisted reviews.
- DiniTicket food pre-orders continue to be regular persisted `food_orders` linked by `dini_ticket_id`, so customer Orders and owner pre-orders use the same records.
