# V53 DiniTicket runtime service fix

- Customer DiniTicket creation no longer rejects an enabled DiniTicket service solely because a legacy `allowRemoteJoin=false` flag is still stored.
- Runtime access still requires the restaurant/DiniTicket configuration checks in `config()` and normal opening-hours/capacity checks.
- Fixes customer POST `/api/v1/customer/dinitickets` returning `409 SERVICE_NOT_AVAILABLE` while Restaurant Partner shows DiniTicket enabled.
