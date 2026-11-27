# v51 DiniTicket service compatibility fix

- DiniTicket runtime now accepts an APPROVED + ACTIVE restaurant when its DiniTicket settings are enabled, even if a legacy service-activation row was not promoted correctly.
- Other services keep the strict activation workflow.
- Fixes customer `POST /api/v1/customer/dinitickets` returning 409 `SERVICE_NOT_AVAILABLE` for an owner-enabled DiniTicket service.
