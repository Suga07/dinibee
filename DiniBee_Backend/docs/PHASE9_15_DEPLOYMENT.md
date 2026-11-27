# Phases 9–15 deployment and acceptance

## Delivery status

Cumulative source and integration guidance are supplied through Phase 15. Full dependency compilation, JUnit and PostgreSQL acceptance are **NOT EXECUTED** in the authoring environment. The exact evidence is in [VERIFICATION.md](VERIFICATION.md). Do not label this package production-verified until the gates below pass in a suitable environment.

## Upgrade an existing Phase 8 database

Retain the database, existing secrets and applied migration history. V1–V14 and `pom.xml` are byte-preserved. New Flyway migrations V15–V21 add dining/QR, payments/finance, wallet/rewards, reviews/support/notifications, promotions/settings, permissions and audit reasons. Take and verify a restorable backup before upgrading; run the upgrade against a restored Phase 8 database in staging first. Do not edit applied migrations, run Flyway repair to conceal checksum changes, or regenerate existing secrets. Hibernate remains `ddl-auto=validate`; Flyway clean is disabled. PostgreSQL still needs the Phase 4 `btree_gist` extension.

Run from the extracted `bookeat-backend` directory with Java 17+, Maven and Docker available:

```bash
mvn clean test
mvn clean package
mvn clean verify -Pintegration
```

The integration profile uses disposable PostgreSQL Testcontainers and must fail when Docker is unavailable. It covers real HTTP/JWT, migrations and database concurrency; only external provider calls in FinalPhaseFlowIT are mocked. These tests do not call live payment services. The included CI workflow runs the integration profile; its results must be reviewed in your repository.

After a successful build, start staging with the existing documented environment/Compose setup. Confirm readiness, migrations V1–V22, schema validation, owner onboarding/approval, discovery/reservation/table contention and the complete meal/payment/refund flows. Test both fresh install and restored Phase 8 upgrade; empty-schema integration tests alone do not prove data migration against your historical records.

## Payment provider setup

The optional Zoho Payments India adapter is disabled by default, with sandbox selected. Follow [ZOHO_PAYMENTS.md](ZOHO_PAYMENTS.md) for the complete environment variables, OAuth scopes, checkout mapping, signed webhook route and reconciliation process. Do not enable it until matching account, widget/signing keys and OAuth credentials are supplied.

V22 adds account/environment snapshots and durable webhook jobs. V1–V21 remain unchanged. Reconcile outstanding Razorpay payments/refunds before switching; historical labels remain readable but the removed adapter cannot process legacy transactions. Test the V22 upgrade against a restored Phase 15 database as well as a fresh install.

Sandbox acceptance must include verified payment success, invalid signature/amount/account, duplicate delivery, create/attachment races, worker retries, lost callbacks and approved refunds. Do not retry uncertain financial POSTs. Monitor unfinished/exhausted webhook jobs and define manual reconciliation for missing or expired sessions. Full build and actual provider acceptance remain pending.

Settlement APIs record liabilities and manual transfer evidence; they do not execute bank payouts or automatic provider transfers. Test real cash receipt/refund procedures, commission configuration and an independently verified external settlement reference. Payments assigned to settlement cannot enter the automatic refund workflow. Establish a funded, auditable external adjustment process for this case before live settlement use. Cash commission receivables and generated corporate tax invoices are not automated. Monetary/tax/provider settings require your actual business configuration; no financial or tax rates are seeded.

## Security and operations

Keep exact HTTPS CORS origins, database-scoped credentials, TLS, private document storage and the existing production secret validators. Access tokens are rechecked against current user/session/role state; block/deactivate and role changes invalidate sessions. Admin actions require explicit permissions; no public admin registration exists. The bootstrap flow creates the first admin only; disable it and remove its password after use. Later admins are created through the authorized API.

Additional database-backed rate limits: payment attempts 20/minute, coupon validation 20, support writes 15, check-in paths 10, reward writes 20, review creation 10, report requests 30 and gateway webhook 300. Limits are per authenticated actor or remote address. Existing auth/availability/commerce limits also apply. Owner manual-check-in does not share the GPS/QR path matcher; operational access still requires ownership and permission. At the reverse proxy enforce overall request/connection limits and configure trusted forwarding deliberately; server.forward-headers-strategy remains none. JSON and `application/*+json` writes are bounded to 1 MiB; webhook bodies to 256 KiB regardless of content type. Multipart document limits retain Phase 3 settings.

Audit records retain sensitive action reasons; wallet ledger retains adjustments and balance/points/debt evidence. Retain and protect business audit evidence according to your actual policy. Logs must not contain provider secrets, passwords, tokens or precise customer location. Dining coordinates are erased on checkout or after 24 hours; on-the-way location expires after one hour, with a ten-minute purge schedule. GPS is self-reported; do not market it as device-attested presence.

Monitor readiness, migration errors, auth failures, rate-limit spikes, stale reconciliation states, refund PROCESSING age, negative/held settlements, mail outbox failures and database lock waits. Alerts/dashboards are deployment work and are not provisioned by this source ZIP. Load-test actual expected arrival/payment/report concurrency, inspect query plans on representative data and set capacity budgets before deployment. Run dependency/security scanning in CI, remediate actual findings, and rehearse backup restore. Do not claim that source parsing substitutes for these gates.

## Required acceptance record

| Gate | Evidence to collect |
| --- | --- |
| Dependency resolution and complete Java build | Maven test/package logs with resolved pinned dependencies |
| PostgreSQL/Flyway/security/concurrency | Passing integration profile logs; Docker/PostgreSQL versions |
| Existing database upgrade | Restored Phase 8 backup → V21 migration and schema validation |
| Payment provider | Test-account capture, invalid proof, signed replay, timeout reconciliation and refund evidence |
| Finance operations | Correct commission/fee/tax, no duplicate settlement assignment, funded post-settlement refund procedure |
| Frontend integration | Customer/owner/admin module UI tests and approved removal of mocks |
| Deployment | TLS/CORS/storage/secret checks, backup restore, load test and scan results |

Live credentials, frontend application edits, bank-transfer integration and production deployment are outside this archive's executed work. The functionality and present business limits are described in [API contracts](PHASE9_15_API.md), not hidden behind a production-ready label.
