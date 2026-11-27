# DiniBee Admin Panel v23

Applied requested Customer and Restaurant fixes:

- Customer premium detection no longer treats any membershipPlan value as premium.
- Customer detail header/type now follows actual premium state.
- Premium / non-premium summary icons replaced with crown/user icons.
- Customer KPI icons updated for orders/spent/rating/saved items.
- Customer Orders, Addresses, Wallet, Support and Activity tabs now load live related data.
- Block/Unblock action calls admin customer status endpoints with fallback.
- Send Notification uses live selected-customer notification endpoint.
- View in App attempts DiniBee deep link and copies customer id as fallback.
- Restaurant onboarding documents are explicitly fetched on detail open.
- Document View uses authenticated blob download with endpoint fallbacks.
- Notes tab renders backend review notes / approval history when available.
- Restaurants search is on its own line; status/type/location filters are on the next line.
- Restaurant Overview period buttons (Today / This Week / This Month) now change live metrics.
- Restaurant KPI icons updated.
- Sales Overview uses a reference-style orange line/area chart generated from live payment data.
- Recent Orders includes Payment, Status, Time and View columns.
- Restaurant Orders, Menu, Reviews, Gallery and Settings tabs now render selected-restaurant live/context data.
