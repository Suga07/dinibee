DiniBee Backend v67 - Admin Pay Bill dashboard

New authenticated ADMIN endpoints:
GET /api/v1/admin/pay-bills/summary
GET /api/v1/admin/pay-bills/restaurants?search=&page=0&size=25
GET /api/v1/admin/pay-bills/restaurants/{restaurantId}/summary
GET /api/v1/admin/pay-bills/restaurants/{restaurantId}/transactions?status=ALL&search=&page=0&size=25
GET /api/v1/admin/pay-bills/{billId}

Amounts in paise; today/week evaluated using Asia/Kolkata; week begins Monday. Verified PAID full-payment records and individually PAID split shares are included in collection totals. Bills are counted separately by status. A payment still PENDING is NOT revenue. Refunds and settlements are not subtracted; these are gross verified collections. Date filter and settlement adjustments will need a separate accounting view. No schema migration required for these read-only endpoints.

Back up your DB and compile and test before VPS deployment.
