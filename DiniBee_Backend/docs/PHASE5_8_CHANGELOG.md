# Phases 5–8 source changes

Delivered 2026-09-13 as one cumulative `com.bookeat` project.

- Phase 5: paged public discovery, safe detail/menu/hours, cuisine tags, selected-service search and customer favorites; no invented reviews.
- Phase 6: transactionally allocated regular/group/corporate reservations, status history, cancellation snapshots, secure private invitations, bounded atomic recurrence and organizer/guest scope.
- Phase 7: immutable server price/tax snapshots, corporate budgets, proposed exact group splits, kitchen transitions, regular/on-the-way takeaway, private collection code and consent/revocation for latest location.
- Phase 8: unique active queue tickets, FIFO positions, expiry and accepted-preorder guards, optional protected table seating, durable customer/owner event replay and finite SSE batches.
- V9–V14 added; V1–V8 preserved byte-for-byte. POM and dependency pins retained. Original 85 Postman requests retained; 81 examples added. Existing auth/storage/encryption behavior retained, with narrow public-discovery and commerce-rate-filter integration plus new error handling and permissions.
- Phase 4 availability gains a reusable continuous-window check for takeaway and live queue seating. Booking allocation protection remains half-open and includes grace; table seating advances owner inventory version when operational state changes.
- Added 6 JUnit wrappers, 33 PostgreSQL integration test methods and a dependency-free runner with 4,093 executed assertions. The existing Phase 4 runner still passes 5,074 assertions. Full Maven/JUnit/PostgreSQL execution remains unavailable; see [VERIFICATION.md](VERIFICATION.md).

## Scope boundaries

Phase 9 remains next: reservation arrival/check-in/dining-session completion. DINE_IN order states are modelled but require those future legitimate transitions. Queue seating and queue preorder serving have their own Phase 8 transitions. Payment providers, actual payment splitting, wallets, invoices, rewards, reviews and notification delivery remain later phases. Invitations return links/payloads only. The frontend applications are unchanged.
