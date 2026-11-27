# Phase 3 change record

Historical Phase 3 scope. The subsequent additions are described in [PHASE4_CHANGELOG.md](PHASE4_CHANGELOG.md).

This continues the original `com.bookeat` project. Existing auth and profile endpoints are preserved. Applied migrations V1–V3 are unchanged; only V4 and V5 are added.

- Added 16 restaurant entities with repositories: restaurant aggregate, service activation, hours, special schedules, documents, reservation/takeaway/DiniTicket settings, policies, settlement profile, table/menu/team plans, team drafts, onboarding progress and approval history.
- Added owner/admin/document service interfaces and implementations, typed request/response DTOs, explicit safe mapper, thin REST controllers, validation, access checks and permission grants.
- Added 39 owner/admin REST operations, including private document review/download and owner dashboard context.
- Added aggregate locking/version checks, backend-derived progress and transactional audit history. Approval, rejection, requested changes, service activation and suspension are explicit transitions.
- Added independent settlement encryption and local/S3 private evidence storage. Production configuration requires the additional independent key and S3 provider; existing non-production authentication startup remains possible without settlement configuration.
- Added 9 unit and 27 PostgreSQL integration methods. Existing AuthFlowIT now counts successful original migrations 1–3 specifically so additive migrations do not break its original assertion.
- Reorganized the original Postman collection into one master collection with 66 requests; all 19 pre-existing auth/profile requests are retained. Added example payloads, upgrade instructions and CI phase gates.

Deferred by the required phase gate: live table/menu CRUD and availability (Phase 4), customer discovery, reservations, orders, queue operations, check-in, payments, wallets, rewards and later modules. Onboarding table/menu/team forms store configuration/plans only. No existing Flutter or React source project was edited.

## Unchanged applied migration hashes

| Migration | SHA-256 |
|---|---|
| V1__create_identity.sql | a0bb0bd40f4546f61a819157af27e779c75a36a502c130a497476b5768954a5a |
| V2__create_authentication.sql | 207630932612d0d092e82bb121b7915d318aec5e70762e9afa163cb6fe8f175d |
| V3__seed_roles_and_permissions.sql | 7804076d3466db3d369f7fe94eb75d1c0e51f4d7171b254fbc6def9b08dafaa8 |
