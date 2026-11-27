# Customer Profile Edit v55

Added authenticated endpoint:

PUT /api/v1/customer/profile

Request:
```json
{
  "firstName": "Vazhmnui",
  "lastName": "P",
  "email": "user@example.com",
  "phoneNumber": "+911234567890"
}
```

Notes:
- Requires CUSTOMER_SELF_READ authority (same authenticated customer profile scope as GET profile).
- Duplicate email is rejected.
- Email change resets emailVerified=false.
