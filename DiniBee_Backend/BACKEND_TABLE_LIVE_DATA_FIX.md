# Backend live table-state update

Changes in this build:

- `restaurant_tables` remains the canonical source for the owner table screen. Do not generate T01/T02/etc. in the client.
- Added `CLEANING` to `TableStatus` and Flyway migration V46 so Cleaning is persisted in PostgreSQL.
- Added `GET /api/v1/owner/restaurants/{restaurantId}/tables/summary` for database-backed dashboard counts.
- Existing `GET /api/v1/owner/restaurants/{restaurantId}/tables` returns the actual restaurant tables created/stored in PostgreSQL.
- Existing `PATCH /api/v1/owner/restaurants/{restaurantId}/tables/{tableId}/status` persists AVAILABLE/RESERVED/OCCUPIED/CLEANING/BLOCKED/MAINTENANCE.

Important: a Flutter `Cannot hit test a render box with no size` error is a client layout/rendering error. The backend cannot repair that widget error. The owner Flutter app should navigate to a normal table list and consume these endpoints instead of rendering hardcoded floor-plan data.
