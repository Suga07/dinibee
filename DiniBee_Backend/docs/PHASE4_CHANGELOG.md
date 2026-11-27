# Phase 4 change record

Continued the existing backend with table and menu inventory plus public availability. Existing authentication code, JWT conversion, entity identities and applied migrations V1–V5 are preserved.

- Added six entities, six repositories, request/response DTOs, explicit mappers, service interfaces/implementations and three controllers. Tables/categories/items/options support archival and owner/restaurant isolation.
- Added 18 HTTP operations, parent version checks, inventory limits, decimal price rules, audited table statuses and stable nested option IDs.
- Added a pure availability engine, interval index, timezone/overnight/special-schedule handling, lead/horizon/capacity rules and dining-plus-grace protection.
- Added an internal transaction-required reservation allocation helper, reference-scoped release and PostgreSQL exclusion constraints. No reservation API or customer order workflow was added.
- Added V6–V8 only. SecurityConfig permits the exact public availability GET and installs its rate-limit filter; existing authentication routes and roles remain intact.
- Added seven JUnit rule methods and 25 PostgreSQL integration methods, plus an executable dependency-free rules harness. Original RestaurantOnboardingIT now counts V1–V5 explicitly so new migrations do not invalidate its original migration assertion.
- Added 19 Postman examples (18 operations plus date-only availability) to the existing 66 requests, complete bodies, API/upgrade documentation and reproducible rule-check scripts.

Java rule checks ran successfully; dependency-resolved compilation and Spring/PostgreSQL tests remain NOT EXECUTED. See [VERIFICATION.md](VERIFICATION.md) for exact evidence. No existing Flutter/React project was modified. Phase 5 customer discovery remains next; Phases 6+ retain the original roadmap.
