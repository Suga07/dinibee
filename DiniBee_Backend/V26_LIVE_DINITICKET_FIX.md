# v26 live DiniTicket / takeaway fix
- Customer DiniTicket current endpoint is recovery/read-only and no longer expires linked work while checkout is reopening.
- Reuses an existing pending payment when the same gateway is selected.
- Allows explicit gateway switch (for example Razorpay -> CASH) by failing the superseded unpaid intent first, releasing its payment-line claim and preventing duplicate charge coverage.
- Takeaway Pay-at-Restaurant cash intent is confirmed at owner handover before the paid-order completion guard runs.
- Takeaway handover remains idempotent for already-collected orders.
