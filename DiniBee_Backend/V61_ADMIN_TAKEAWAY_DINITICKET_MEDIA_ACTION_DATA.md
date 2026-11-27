# Backend v61 admin Takeaway/DiniTicket read model enrichment

The existing admin read endpoints now also return the IDs and media/contact fields required by the admin UI:
- customerId / customerUserId / phone / email
- restaurantId / logo / cover / location
- foodOrderId / paymentId / paymentMethod
- first food image and item summary

No customer profile image field exists in the current schema, so the admin UI uses an initials avatar fallback.
