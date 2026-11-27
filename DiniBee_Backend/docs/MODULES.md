# Module-by-module implementation guide

> Current cumulative delivery: Phases 1–15. See [current API](PHASE9_15_API.md), [Phase 14 integration](PHASE14_FRONTEND_INTEGRATION.md), [deployment gates](PHASE9_15_DEPLOYMENT.md) and [verification](VERIFICATION.md). Earlier sections retain historical phase context.


The brief's full platform roadmap is preserved as a future sequence. This release includes Phases 1–8. Empty controllers and repositories for later business features have not been created.

## Foundation

Purpose: configuration, a uniform API boundary, persistent identifiers, timestamps and a migration-managed database.

| Concern | Implementation |
| --- | --- |
| Entry point | `com.bookeat.BookeatApplication` |
| Base entity | `common/entity/BaseEntity`: UUID, UTC timestamps, optimistic `version` |
| Response | `common/response/ApiResponse`, `PageResponse` |
| Exceptions | `exception/ErrorCode`, `ApiException`, `GlobalExceptionHandler` |
| Validation | Jakarta constraints; custom `ValidPassword`, UTF-8-aware `PasswordRules` |
| Environment | Common/dev/prod properties; `ProductionConfigurationGuard` |
| Database | V1 identity; V2 sessions, challenges, outbox, limits and audit; V3 role/permission seed |
| Health | `HealthController`, actual PostgreSQL `SELECT 1` |
| Documentation | `OpenApiConfig`, controller annotations, Postman, API.md |
| Tests | `SecurityPrimitivesTest`; `AuthFlowIT.healthUsesDatabaseAndMigrationsRun` |

Foundation has no business repository or mapper of its own: it supplies reusable infrastructure. SQL migrations are the source of schema creation; Hibernate validates mappings and never recreates production tables.

## Identity, roles and self profiles

Purpose: separate credentials from customer and owner profiles, and define least-privilege role/permission grants.

Entities: `User`, `Role`, `Permission`, `Customer`, `RestaurantOwner`; enums: `RoleCode`, `UserStatus`. Each entity has a JPA repository. `UserMapper` converts entities to `UserResponse`; `ProfileResponse` includes the appropriate profile identifier. Profile retrieval uses `ProfileService` and `ProfileServiceImpl`; thin customer/owner controllers and the admin profile controller provide the API.

Relationships:

| Parent | Child | Cardinality / database rule |
| --- | --- | --- |
| users | customers | 1 to 0..1; unique `user_id` |
| users | restaurant_owners | 1 to 0..1; unique `user_id` |
| users | roles | Many-to-many through `user_roles` |
| roles | permissions | Many-to-many through `role_permissions` |
| users | auth_sessions | 1 to many |
| auth_sessions | refresh_tokens | 1 to many; one row per issued refresh token |
| users | auth_challenges | 1 to many historical records; one unconsumed challenge per purpose |
| users | audit_logs | Actor reference; events preserve affected entity UUID |

Phone numbers submitted at registration remain optional contact data. Multi-role membership is modeled, but public users cannot choose arbitrary roles or add owner/admin privileges to their account. An explicit authorized role-assignment workflow belongs to a later module.

CUSTOMER and OWNER have only their own profile permissions. MANAGER and STAFF roles are defined for future restaurant-scoped assignments; the API does not yet assign staff to restaurants or grant them owner profiles. Admin specialties have bounded grants; ADMIN alone only reads its own admin profile. SUPER_ADMIN receives the seeded permission set, and public registration cannot create it.

`AdminBootstrapService` supplies the one-time first-admin path. It has no controller, accepts explicit environment configuration, serializes concurrent creation with a PostgreSQL advisory lock and creates an audit event.

Validation: registration request DTOs, unique normalized email, verified-phone uniqueness, BCrypt input limit. Exceptions: `USER_ALREADY_EXISTS`, `ACCESS_DENIED`, `CONFLICT`. Security: URL role checks plus controller permission checks. Requests/responses and Postman examples: [API.md](API.md) and collection folders 01, 02, 15, 16. Migrations: V1 and V3. Tests: role injection, profile isolation, duplicate email, admin bootstrap and current grants in `AuthFlowIT`.

## Authentication and sessions

Purpose: registration, login, session renewal/revocation, password recovery and contact verification.

Layers: `auth/request/*` validated records; `auth/response/*` safe API DTOs; `AuthService` interface; `auth/service/impl/AuthServiceImpl`; `AuthController`; user/profile repositories; `AuthSessionRepository`, `RefreshTokenRepository` and `AuthChallengeRepository`. `AccessTokenService` issues JWTs. Mapping reuses `UserMapper`; a second trivial auth mapper is unnecessary because token generation and user mapping compose directly into `AuthResponse`.

Entities: `AuthSession`, `RefreshToken`, `AuthChallenge`. Enum: `ChallengePurpose` (EMAIL_VERIFICATION, PASSWORD_RESET). A refresh token row stores its SHA-256 digest; a low-entropy OTP is protected with HMAC and a separate server pepper. All raw token-bearing request/response record `toString()` methods redact secrets.

Auth mutations acquire the user's pessimistic database lock before changing existing credentials/session/challenge state. Refresh and reset first obtain only a scalar owner ID to avoid reading a stale managed entity before locking. Refresh reuse and invalid-OTP exceptions are excluded from rollback so the recorded revocation/attempt survives the error response.

No idempotency claim is made for public registration; duplicate email returns conflict. Refresh has single-use semantics rather than a replayable idempotency key. Booking/order idempotency is now supplied by the commerce module; payment idempotency remains later work.

Validation, endpoints, all request/response examples and errors: [API.md](API.md). Security: only six public authentication POST routes are permitted; `/auth/me`, password change, logout and contact verification require a bearer token. Tests: JWT expiry/signature/audience/purpose, refresh replay/concurrency, logout, password recovery, OTP exhaustion and inactive accounts. Migrations: V1/V2.

## Mail outbox

`MailOutboxEntry`, `MailOutboxRepository`, `MailOutboxService` and `MailOutboxWorker` supply transactional email persistence and bounded retries. Enum `Status` is PENDING, SENT, FAILED or EXPIRED. `EncryptedText` encrypts stored mail bodies with AES-256-GCM and fresh nonces. The worker uses `FOR UPDATE SKIP LOCKED`. There is no public outbox controller because emails are an internal authentication dependency.

Tests: outbox retry, expiry and content erasure; tamper detection; and email challenge lifetime and usage. Migration: V2.

## Security infrastructure

`SecurityConfig`: stateless Spring Security resource-server setup, exact public routes, role and method-permission checks, explicit CORS origins, no cookie authentication, no form/basic login.

`JwtConfig`: HS256 signature verification, mandatory expiry/issued-at/subject/purpose, issuer/audience validation and limited clock skew. `DatabaseJwtAuthenticationConverter`: current user/session/credential/permission checks on every authenticated request. `JsonSecurityErrors`: consistent 401/403/429 responses.

`RateLimitService`: atomic PostgreSQL counters keyed by HMAC of scope and identity. Login/password-reset account limits supplement route/IP limits; counters survive failed authentication rollbacks and are shared across application instances. Known auth routes and the public availability route create bounded buckets. Old rate buckets are removed hourly. Reverse-proxy IP forwarding is not trusted automatically; an edge rate-limit configuration is part of deployment.

`AuditService` records successful login, logout, password/contact changes, token reuse and initial admin creation in the same transaction. Auth failure logs omit user credentials and request bodies. Phase 3 adds restaurant decision history, verification references and audit events through `RestaurantChangeService`.

## Next phases and concrete boundaries

| Phase | Status in this delivery |
| --- | --- |
| 1 Foundation | Code included; full Maven/PostgreSQL verification pending |
| 2 Authentication | Code included; email verification and password recovery are included |
| 3 Owner onboarding / restaurant approval | Included; full build/integration verification pending |
| 4 Tables, menu and availability | Included; full build/integration verification pending |
| 5 Customer search/discovery | Next phase after cumulative verification |
| 6–9 Reservations, groups, corporate, food, takeaway, DiniTicket, dining | Not implemented |
| 10–13 Payments, wallet, engagement and full admin/reporting | Not implemented |
| 14 Finalization | Foundation Docker/docs/tests supplied; full-system testing/deployment waits for business modules |

Restaurant ownership, service activation, local schedules and allocation constraints are now used by reservation entities, authorization, lifecycle and commerce idempotency in Phases 5–8. Manager/staff assignments and payment verification remain future work.


## Phase 3 implementation

The `com.bookeat.restaurant` package contains entities, enums, repositories, explicit request/response DTOs, a mapper, owner/admin/document service interfaces and implementations, storage adapters and controllers. `RestaurantAccessService` enforces ownership and aggregate version checks. `OnboardingProgressService` calculates completion. `RestaurantChangeService` refreshes stored progress and audit rows in the transaction. See [PHASE3_API.md](PHASE3_API.md).

## Phase 4 implementation

`com.bookeat.table` owns physical table configuration, operational state and owner CRUD. `com.bookeat.menu` owns categories, items, variants/addons and decimal pricing. `com.bookeat.availability` owns public advisory calculations and an internal allocation transaction helper; it contains no booking controller. `RestaurantInventoryAccess` reuses the parent ownership lock while allowing approved inventory maintenance. Owner changes call the existing RestaurantChangeService; reservation allocations do not alter the inventory configuration version; Phase 8 seating advances it when changing a table’s operational status.

V6–V8 add six tables and two permissions without modifying applied migrations. Public output is mapped to safe DTOs. See [PHASE4_API.md](PHASE4_API.md) for field contracts and [PHASE4_DEPLOYMENT.md](PHASE4_DEPLOYMENT.md) for allocation/extension requirements.

## Phases 5–8 implementation

| Package | Responsibility |
| --- | --- |
| `discovery` | Public search/detail/menu/hours, cuisine tags and favorites |
| `commerce` | Actor/service checks, idempotency, continuous operating windows, geo rules and route rate limits |
| `reservation` | Regular/group/corporate entities, DTOs/mapping, repositories, locked lifecycle, invitations and recurrence |
| `order` | Server price snapshots, taxes/budgets, combined proposals, kitchen and takeaway lifecycles, consent tracking |
| `diniticket` | Queue state/capacity/expiry, durable events, customer/owner replay and SSE |
| `availability` extension | Optional queue table claim/release using existing exclusion protection |

Detailed contracts and source-delivery boundaries: [PHASE5_8_API.md](PHASE5_8_API.md). Phase 9 and later modules remain future work.
