# v28 fixes
- DiniTicket token sequence is sourced from PostgreSQL and repaired against persisted queue_sequence and ticket_number values before issuing each token.
- Existing active ticket recovery remains idempotent; no duplicate token is created for repeated recovery calls.
- Customer review GET/POST controller is available at /api/v1/customer/reviews.
- Owner/customer order APIs remain persisted-data sources; no mock order count is introduced.
