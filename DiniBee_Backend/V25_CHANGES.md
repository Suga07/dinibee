# v25 Backend changes
- Payment creation now recovers the latest CREATED/PENDING payment for the same customer/reference/gateway.
- This makes DiniTicket/Takeaway payment creation idempotent across retries and prevents duplicate Razorpay orders.
- Switching gateway while another payment is active returns PAYMENT_IN_PROGRESS so the old attempt is not silently duplicated.
- Existing CASH support remains server-side and is used by the v25 customer flows.
