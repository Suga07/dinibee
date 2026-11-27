# v40 Table Booking Live Sync

- Owner reservation edit endpoint now revalidates live database table allocation.
- Owner can move ARRIVED/CHECKED_IN reservations to DINING and then COMPLETED in the backend.
- Completing a reservation writes completedAt, releases the table allocation, and is visible to the customer.
- Dine-in food orders can be placed once the reservation is ARRIVED/CHECKED_IN/DINING.
