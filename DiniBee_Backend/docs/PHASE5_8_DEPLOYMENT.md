# Phases 5–8 deployment and acceptance

This is cumulative source, not a built/deployed release. The user explicitly requested continuing through Phase 8 after the missing build-tool limitation had been disclosed. Complete these checks in an environment with Java 17, Maven, Docker and PostgreSQL before connecting production clients. No schema migration, Spring startup or provider transaction was executed here.

## Database changes

Keep V1–V8 unchanged. Flyway applies six new migrations:

| Migration | Change |
| --- | --- |
| V9 | Cuisine/rating summaries, favorites, idempotency and timezone-aware public opening SQL functions |
| V10 | Reservations and history, scoped allocation references |
| V11 | Group members, corporate accounts/occurrences/guests and hashed invites |
| V12 | Food price snapshots, addons/history, tax policy, takeaways and consent tracking |
| V13 | Queue tickets/sequence, active-ticket uniqueness, durable events and food ticket foreign key |
| V14 | Owner reservation/order/queue permissions |

The existing V6 GiST exclusion constraint and `btree_gist` extension remain required. Restaurant-first locks serialize availability claims, menu snapshots, corporate budget changes and queue mutations for the same venue; the database exclusion constraint remains the final overlap guard. This deliberately favours correctness over per-restaurant write throughput. Transaction-scoped advisory locks serialize an actor's identical idempotency key and release on commit/rollback. [PostgreSQL locking documentation](https://www.postgresql.org/docs/17/explicit-locking.html).

Do not enable Hibernate schema creation or run Flyway clean on an existing database. Back up an existing Phase 4 database, rehearse V9–V14 on its restored copy, and verify Flyway history/checksums and `ddl-auto=validate` startup. No down migrations are supplied: recover an unsuccessful deployment using your tested backup/restore procedure and compatible application version. Tests use real PostgreSQL 17 Testcontainers rather than H2 because SQL functions, advisory locks, partial indexes and exclusion constraints are PostgreSQL-specific.

## Required checks

From the project root:

```bash
mvn clean test
mvn clean package
mvn clean verify -Pintegration
```

All three commands were attempted in the authoring environment and returned shell exit 127 (`mvn: command not found`). Docker and psql commands also returned exit 127. They are **NOT EXECUTED**, not passing or failing Java test runs. See [VERIFICATION.md](VERIFICATION.md) for the checks that did run.

The integration suite has 108 test methods, including 33 commerce-flow methods. It covers migrations/roles, public privacy/search/DST schedules, same-key and overlapping allocation concurrency, reservation rollback/cancellation, group/corporate isolation and invites, atomic recurrence, immutable pricing/taxes, kitchen commitment/budget guards, pickup code verification, explicit location consent, queue uniqueness/capacity/positions/expiry, preorder expiry deferral and seating/food completion, table overlap and scoped event replay. The suite has not run here; fix any dependency compilation, mapping, migration or assertion failure before deployment.

After a green build and schema rehearsal, exercise independent Postman scenarios against an approved venue with each service configured: one regular booking, one group with a second customer, one corporate occurrence/series, one takeaway, one consented on-the-way order and a full DiniTicket preorder → seating → food completion → ticket completion flow. Verify expected cancellation/rejection branches with fresh resources. For reservation food serving, implement and validate Phase 9 check-in before expecting CHECKED_IN/DINING prerequisites to succeed.

## Configuration and operation

No new secret or third-party provider is required. Existing auth, encryption, storage and mail configuration remain unchanged. Queue expiry scheduling uses `app.diniticket.expiry-poll-ms` (default 30000 ms); enabled scheduling already exists. Each poll processes up to 100 restaurants with eligible expirations, excluding accepted-preorder deferrals. Read/issue paths also apply expiry. Monitor failures and queue age; accepted food work intentionally keeps a ticket active until resolved.

Commerce discovery/create/join/location/event routes use database-backed rate limiting in addition to existing auth and availability limits. The defaults are implemented in `CommerceRateLimitFilter`; ordinary read and existing inventory limits retain their prior behavior. Keep proxy/forwarded-header trust consistent with the existing security deployment guide. Use TLS for bearer tokens, invitations and pickup codes. Avoid logging location request bodies or invitation URLs.

SSE uses finite, no-store batches and bearer-authenticated reconnects; no sticky sessions or shared in-memory event bus is required. Reverse proxies must permit `text/event-stream` and Authorization/Last-Event-ID headers. Configure CORS explicitly for browser fetch clients. Durable event and idempotency tables currently have no purge job; choose retention/replay policies before adding pruning, so retries and client cursors remain well-defined.

Location retains one latest consented point; cancellation/rejection/collection and explicit revoke erase it. There is no scheduler to close abandoned orders or age out their coordinates. The app must expose stop sharing and treat old ETA timestamps as stale. Estimate accuracy is limited to straight-line distance and an assumed 30 km/h.

Owner tax defaults to 0 and must be configured to the business's intended calculation. All orders remain financial intent/snapshots; no gateway, settlement, split charge, invoice document or tax compliance certification is provided. The Flutter/React clients have not been changed or connected by this package.
