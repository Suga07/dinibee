# V27 Reservation UI / Live Data Fix

- Reservation top tabs (All / Confirmed / Pending / Cancelled) now filter live rows.
- Restaurant, status, party-size, and date filters now work.
- Reset clears every reservation filter.
- Restaurant image/logo is loaded from the reservation payload and falls back to live restaurant detail.
- Reservation detail Customer tab now shows live customer name, phone and email.
- Reservation detail History tab now loads `/admin/reservations/{id}/history` and falls back to current status when unavailable.
- Restaurant location, special request, table number, start/end time and booking source are shown from live data.

Backend v60 enriches `/admin/reservations` with customer contact, effective restaurant media, location, special request and timestamps, and adds the reservation history endpoint.
