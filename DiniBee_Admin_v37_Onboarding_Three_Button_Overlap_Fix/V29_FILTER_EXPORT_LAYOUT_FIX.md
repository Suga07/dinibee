# DiniBee Admin Panel v29

Applied on top of v28.

Fixes:
- DiniTicket/Takeaway filter-row Reset overlap at 100% zoom.
- DiniTicket status/date/search/reset filtering made robust.
- CSV export download made reliable and shows success feedback.
- Customers: functional type/status/location/sort filters + CSV export.
- Restaurants: functional status/type/location filters.
- Restaurant Onboarding: functional type/location/document/sort filters.
- Removed orange grid/list view buttons from Customers, Restaurants, and Restaurant Onboarding headers.
- Customer block/unblock now first calls the backend action endpoint without a JSON body, with query fallback, avoiding request-body validation errors on compatible backends.

No mock data added.
