# V59 DiniTicket Cancel / No Show / Token / Notification Fix

- Owner Cancel and No Show now release unresolved CASH/Pay-at-Restaurant payment intents before ending linked DiniTicket food work.
- Linked PLACED food orders are cancelled; ACCEPTED/PREPARING/READY work is rejected when the ticket is explicitly cancelled/no-show.
- Captured online payments remain protected and still require refund handling.
- Manual owner No Show is allowed for WAITING/CALLED tickets without waiting for auto-expiry time.
- New token numbers are persisted with a minimum four-digit display format (`DT-0001`, `DT-0002`, ...).
- Handover verification accepts both padded and legacy token input.
- Customer in-app notifications added for ARRIVED, CANCELLED, NO_SHOW and EXPIRED in addition to existing Preparing/Ready/Called/Handover/Completed events.
