# V28 Takeaway + DiniTicket live functionality fix

- Functional Takeaway tabs, restaurant/status filters, date range, search, reset and CSV export.
- Live restaurant/logo/cover, food image and customer contact data from backend v61.
- Customer avatar fallback generated from initials because the current database has no customer profile image column.
- Print Bill opens a printable DiniBee bill.
- Refund Order/Ticket opens Refunds workflow with the linked payment/reference.
- Send Notification/Send Ticket uses the existing admin notification API and the customer's user UUID.
- Functional DiniTicket All/Upcoming/Completed/Cancelled tabs, filters, date range and export.
- Ticket download creates a local ticket text receipt.
