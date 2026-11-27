# V19 Onboarding + Restaurant Exact Reference UI Fix

Base: DiniBee Admin Panel v18 reference UI build.

Changes:
- Restaurant Onboarding rebuilt to match the supplied reference more closely:
  - compact status tabs
  - compact filter row
  - 3-column restaurant application cards
  - inline right-side application details panel instead of dark overlay sheet
  - Overview / Documents / Photos / Bank Details / Notes tabs
  - document verification cards
  - document View downloads private file with admin bearer token and opens it in a new tab
  - approve/reject/request-changes action bar
- Restaurant images:
  - every restaurant list row is enriched from GET /admin/restaurants/{id}
  - supports coverImageUrl, logoUrl and additional media shapes
  - converts old http/localhost/VPS media URLs to https://api.dinibee.tech
  - cycles through image candidates before using fallback
- Restaurants page rebuilt to match the supplied reference more closely:
  - compact restaurant card grid on the left
  - inline right-side restaurant details panel
  - hero image and logo
  - Overview / Orders / Menu / Reviews / Gallery / Settings tabs
  - Today / This Week / This Month controls
  - KPI cards, sales overview and recent-orders section

Backend contracts were not changed.
