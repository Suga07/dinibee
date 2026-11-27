# DiniBee Admin Panel v18 — Reference UI Applied

Base: DiniBee_Admin_Panel_v16_DiniBee_Branding_Only (built from original v15).

Applied live-data reference UI models for:
- Restaurant Onboarding
- Restaurants
- Customers
- Table Bookings / Reservations
- Take Away Orders
- DiniTicket Orders
- Payments / Transactions

Implementation notes:
- Existing production API services are retained; no mock dataset is introduced.
- Restaurant onboarding and restaurant cards use live restaurant API data.
- Restaurant cards enrich list records with detail calls when the list response does not include logo/cover media.
- Customer cards use the live admin customer API and normalize common backend response envelopes/field names.
- Detail views open in a right-side drawer to match the supplied reference layouts.
- Old duplicate restaurant/reservation/takeaway/DiniTicket routes are handled by the consolidated navigation/routes included in this build.
- DiniBee branding from v16 is retained.
