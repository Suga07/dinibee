# v34 synchronization fixes

- Customer reservation cancellation is idempotent when the restaurant already cancelled the booking; stale client versions no longer cause a 409 in that case.
- Preserves server-authoritative DiniTicket live/current token and completion state.
