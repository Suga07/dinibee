# Phase 14 — customer, owner and admin integration

This package documents and implements the backend contracts. Existing Flutter/admin source was not supplied for editing, and no actual client has been connected or UI-tested. Run the [backend acceptance gates](PHASE9_15_DEPLOYMENT.md) first, then migrate one module at a time. All routes below are relative to `/api/v1`; dynamic identifiers must come from API responses.

## Shared network layer

Both Flutter apps should centralize these responsibilities under `lib/core/network/` and `lib/core/auth/`:

| Component | Responsibility |
| --- | --- |
| ApiClient | One configured base URL, connect/read timeouts, request IDs, ApiResponse decoding and byte exports |
| ApiEndpoints | Relative endpoint builders with validated/encoded IDs and query parameters |
| ApiException | HTTP status, stable errorCode, safe message, field validation and retry metadata |
| SecureTokenStorage | `flutter_secure_storage`; one serialized access/refresh pair updated together, cleared on logout/revocation |
| AuthInterceptor | Attach the current BookEat bearer only to protected API requests; never to provider URLs or public auth calls |
| RefreshTokenInterceptor | Single-flight refresh, atomic token-pair replacement, at most one replay of the failed request |

Choose Flutter/Dio/secure-storage versions compatible with the actual app when integrating; no frontend dependency manifest or untested version pin is added here. Do not log tokens, passwords, raw QR tokens, precise location or payment signatures. Use decimal handling for amounts; do not recompute billed totals with binary floating point.

Refresh algorithm (implementation-neutral):

1. Capture the token generation attached to each outgoing protected request.
2. On 401, fail immediately if this is already a replay or a public auth endpoint.
3. If another request has already installed a newer token generation, replay once with that generation without refreshing again.
4. Otherwise all concurrent 401 requests await the same refresh future. POST `/auth/refresh` with the current opaque `refreshToken`, without an Authorization header.
5. Persist the returned access/refresh token pair as one secure-store value before releasing waiters. Guard the write with the session generation, so a refresh completing after logout cannot restore a logged-out session.
6. Replay the original request at most once, preserving its body and Idempotency-Key. Do not replay non-repeatable uploads automatically.
7. On invalid refresh or uncertain refresh network outcome, clear credentials and require sign-in. A used rotating refresh token cannot safely be retried after the replacement response is lost.

Use separate interceptors/clients for public auth and protected calls to prevent refresh recursion. A 403 is an authorization failure, not an instruction to refresh. A 409 requires current state/version reconciliation. Persist an idempotency key **before** submitting a financial/booking intent and retain it until the result is known. A generic network retry policy must not invent a new payment or refund intent.

## Environment configuration

| Client | Development base URL |
| --- | --- |
| Admin/Postman on backend host | `http://localhost:8080/api/v1` |
| Android Studio emulator | `http://10.0.2.2:8080/api/v1` |
| iOS simulator on backend Mac | `http://localhost:8080/api/v1` |
| Physical phone | `http://<trusted-LAN-backend-address>:8080/api/v1` |
| Production | `https://<your-api-domain>/api/v1` |

Physical-device localhost means the device. Compose binds loopback by default; intentionally set APP_BIND_ADDRESS for trusted-LAN testing. Use development-only HTTP exceptions; deploy HTTPS. Base URL and the Zoho public widget API key may be in frontend configuration. JWT signing keys, provider secret/webhook keys, settlement encryption keys and mail credentials stay on the server.

## Customer Flutter API map

| Screen/module | API family and flow | Integration note |
| --- | --- | --- |
| Auth/profile | `/auth/*`, `/customer/profile` | Register/login → secure token pair → `/auth/me`; email verification is supported |
| Restaurant discovery | `/restaurants`, `/restaurants/{id}` | Search/filter/paginate; approved enabled venues only |
| Menu | `/restaurants/{id}/menu` | Use returned active item/variant/addon IDs; order total is server-priced |
| Availability | `/restaurants/{id}/availability` | Venue-local date/guests; returned offset timestamps are authoritative; no hold until successful booking |
| Reservations | `/customer/reservations` and resource actions | Save idempotency key; refetch after conflict, never assume displayed availability guarantees a seat |
| Group booking | `/customer/group-bookings` and invitation/member routes | Use joined roster, refresh versions after membership changes; lock equal-split membership before payment |
| Corporate booking | `/customer/corporate-bookings` and occurrence/guest routes | Organizer scope and budget; company invoice request is an intent, not a generated invoice |
| Food orders | `/customer/orders` | Associate the correct reservation/ticket and order type; show immutable item/price snapshots |
| Takeaway | `/customer/takeaway/orders` | Keep pickup code private; collection requires payment; latest-location sharing is optional and consented |
| DiniTicket | `/customer/dinitickets` | Poll status or use existing snapshot/events contract; display server queue number and estimate |
| Dining | `/customer/reservations/{id}/check-in/gps` or `/check-in/qr`; `/customer/dining-sessions/current` | Ask location consent only for GPS; display session and bill status; owner controls service/checkout |
| Payments | `/customer/payments/create-order`, `/verify`, `/customer/payments/{id}` | Create from server reference; launch Zoho checkout only when a session exists; verify and refetch before showing paid |
| Wallet | `/customer/wallet`, `/customer/wallet/transactions` | Show money, points and reward debt; no client balance updates |
| Rewards | `/customer/rewards/rules`, `/calculate-redemption`, `/claim`, `/redeem`, `/history` | Follow actual operation methods in Postman; server conversion and eligibility; no automatic birthday/referral grant |
| Reviews | `/customer/reviews`; public restaurant reviews | Show form only after eligible completion; one review per eligible reference; edit preserves moderation status |
| Support | `/customer/support/tickets` and messages | Private creator scope; page messages; disable composition when closed |
| Notifications | `/customer/notifications`, `/{id}/read`, `/read-all` | In-app list; unread state is per user; no push subscription endpoint |

The detailed Phase 5–8 invitation, order and event paths are in [PHASE5_8_API.md](PHASE5_8_API.md). Exact Phase 9–15 methods/bodies are in [the endpoint index](PHASE9_15_API.md) and the cumulative Postman collection. API families in this table are navigation guidance, not substitutes for exact HTTP methods.

## Owner Flutter API map

Restaurant-scoped paths use `/owner/restaurants/{restaurantId}`; the backend always checks ownership.

| Screen/module | API family | Integration note |
| --- | --- | --- |
| Onboarding | `/owner/restaurants`, scoped profile/location/hours/documents/services/settlement | Preserve evidence and independent versions; document upload uses existing private storage flow |
| Approval status | Scoped restaurant detail and submission | Approval is distinct from account signup and enabled services |
| Dashboard context | `/owner/dashboard-context` | Render modules from effective enabledServices; handle suspension/revocation on refresh |
| Tables | Scoped `/tables` | Use parent restaurantVersion; reload after seating/check-in/checkout |
| Menu | Scoped `/menu/categories`, `/menu/items` | Preserve variant/addon IDs during edits; archive rather than breaking order history |
| Reservations | Scoped `/reservations` and confirm/reject/cancel actions | Verify current resource version and arrival window |
| Food orders | Scoped `/orders` and lifecycle actions | Use documented transitions; paid commitments cannot be silently rejected |
| Takeaway | Scoped `/takeaway/orders` | Ready/collected flow; verify pickup code and full payment |
| DiniTicket | Scoped `/dinitickets` and queue operations | Seat an eligible table; refresh queue and inventory context |
| Dining | Scoped `/dining-sessions`; reservation manual check-in/token routes | CHECKED_IN → DINING → bill request → paid coverage → checkout |
| Payments | Scoped `/payments`, `/{id}/confirm-cash`, `/refunds/{id}/confirm-cash`, `/settlements` | Confirm actual cash receipt/refund only; owner cannot approve own finance settlement |
| Analytics | Scoped `/reports/{reportType}` and `/export/{format}` | Dates use venue timezone, export is byte response; show metric definitions |
| Notifications | `/owner/notifications` | Scope is owner user, not arbitrary restaurant/member identity |
| Support | `/owner/support/tickets` | Creator-private thread with support team |
| Settings | Existing restaurant reservation/takeaway/DiniTicket settings | Service authorization gates still apply; do not enable modules locally |

On-the-way coordinates are latest-only and expire. The existing event API uses Last-Event-ID for reconnection where supported; CORS permits that header. Native EventSource libraries differ in bearer-header support: choose a client that can send Authorization, or use authenticated polling. Never put bearer tokens in query strings. After reconnect, fetch a fresh snapshot; event cursors are not database version numbers.

## Admin gradual migration

Keep the existing UI and migrate each module in this order:

1. Run the backend endpoint's happy path and permission/ownership failure in Postman.
2. Add a typed admin service for that module using the centralized client.
3. Map the real DTO to the screen, including loading, empty, forbidden, conflict and provider-unavailable states.
4. Disable demo data for that module only after its real flow works.
5. Test the UI with the least-privileged appropriate admin role.
6. Remove that module's obsolete mocks after the real path is accepted.

Authentication and `/admin/users` come first, followed by existing restaurant verification, booking/order reports, finance, wallet/reviews/support, promotions/settings and analytics. `/admin/dashboard` supplies actual totals/trends; missing days can be filled with zero explicitly in chart presentation. Use separate finance permissions for detail reports. Admin roles and permissions can hide controls, while the backend remains the enforcement point.

```dotenv
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_USE_DEMO_DATA=false
```

The global false setting is the **final** state once all required modules are migrated. Use per-module flags during the transition. Do not silently fall back to fake balances/payments when a real API fails.

The current backend returns bearer tokens in JSON. Keep privileged browser tokens in memory and sign in again after reload. Persistent browser sessions require a separately implemented same-origin BFF with server-side refresh storage and a reviewed HttpOnly cookie/CSRF design; no BFF is included here. Do not place admin tokens in localStorage. Configure exact HTTPS CORS origins; wildcard origins are rejected in production.

## Frontend acceptance per module

Exercise success, empty list, 400 fields, expired/revoked token, forbidden role, another user's ID, stale version, repeated tap with same key, network uncertainty and server outage. For payment UI, distinguish CREATED/reconciliation, PENDING and verified SUCCESS; never display paid based solely on the checkout dialog. For exports, download bytes with the response filename; never pass them through the JSON-envelope decoder. Record UI evidence before deleting the final mocks.

## Zoho payment handoff

The online gateway value is `ZOHO`. Use the server `checkout` object for account, public API key, session and sandbox flag; keep provider IDs as strings. Map SDK `paymentId` and `signature` to backend `gatewayPaymentId` and `signature`, retaining the local payment UUID separately. POST the customer payment refresh route after a lost callback. The exact contracts and Flutter platform requirements are in [ZOHO_PAYMENTS.md](ZOHO_PAYMENTS.md).
