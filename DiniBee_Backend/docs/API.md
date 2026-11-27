# BookEat API contract — Phases 1–4

> Current cumulative delivery: Phases 1–15. See [current API](PHASE9_15_API.md), [Phase 14 integration](PHASE14_FRONTEND_INTEGRATION.md), [deployment gates](PHASE9_15_DEPLOYMENT.md) and [verification](VERIFICATION.md). Earlier sections retain historical phase context.




Phase 3 owner/admin endpoints: [PHASE3_API.md](PHASE3_API.md). Existing authentication and profile contracts below are preserved.

Base URL: `http://localhost:8080/api/v1`. In a deployment, replace the origin with the configured HTTPS API domain. UUIDs are strings; never cast them to numeric IDs. All examples below are illustrative, not captured server responses.

## Endpoints

| Method | Path | Authorization | Request body | Success | Expected errors |
| --- | --- | --- | --- | --- | --- |
| GET | `/public/health` | Public | None | 200 readiness | 503 database unavailable |
| POST | `/auth/register/customer` | Public | Register | 201 Auth | 400, 409, 429 |
| POST | `/auth/register/owner` | Public | Register | 201 Auth | 400, 409, 429 |
| POST | `/auth/login` | Public | Login | 200 Auth | 400, 401, 429 |
| POST | `/auth/refresh-token` | Refresh token in body | Refresh | 200 Auth | 400, 401, 429 |
| POST | `/auth/logout` | BookEat access token | None | 200 message | 401, 429 |
| POST | `/auth/forgot-password` | Public | Forgot | 202 generic message | 400, 429 |
| POST | `/auth/reset-password` | Emailed reset proof | Reset | 200 message | 400, 401 inactive account, 429 |
| POST | `/auth/change-password` | BookEat access token | Change | 200 message | 400, 401, 429 |
| POST | `/auth/send-email-otp` | BookEat access token | None | 202 Challenge | 401, 409, 429 |
| POST | `/auth/verify-email-otp` | BookEat access token | VerifyEmail | 200 message | 400, 401, 429 |
| GET | `/auth/me` | Any active session | None | 200 User | 401 |
| GET | `/customer/profile` | CUSTOMER + CUSTOMER_SELF_READ | None | 200 Profile | 401, 403, 404 |
| GET | `/owner/profile` | OWNER + OWNER_SELF_READ | None | 200 Profile | 401, 403, 404 |
| GET | `/admin/profile` | Admin role + ADMIN_SELF_READ | None | 200 User | 401, 403 |

Wrong HTTP methods return 405, non-JSON request media can return 415, and unexpected failures return a sanitized 500 envelope. Swagger endpoints and infrastructure Actuator health are outside API versioning; business endpoints remain under `/api/v1`.

## Requests

Register:

```json
{"firstName":"Test","lastName":"User","email":"customer@example.test","password":"Example-passphrase-71","phoneNumber":"+919876543210"}
```

First/last names are required, at most 80 characters each. Email is required, at most 320 characters, and stored lowercase. Phone is optional but must use E.164 format when supplied. Passwords require 12+ Unicode characters and at most 72 UTF-8 bytes. Roles, permissions, IDs and verification flags cannot be supplied; unknown JSON properties produce 400.

Login:

```json
{"email":"customer@example.test","password":"Example-passphrase-71"}
```

Refresh:

```json
{"refreshToken":"<43-character-opaque-token-from-the-server>"}
```

Forgot:

```json
{"email":"customer@example.test"}
```

Reset (both proof values come from the email):

```json
{"challengeId":"<uuid-from-email>","token":"<43-character-reset-token>","newPassword":"New-example-passphrase-92"}
```

Change:

```json
{"currentPassword":"Example-passphrase-71","newPassword":"New-example-passphrase-92"}
```

VerifyEmail:

```json
{"challengeId":"<uuid-from-send-email-otp>","code":"<6-digit-code-from-email>"}
```

## Success envelopes

Auth:

```json
{
  "success":true,
  "message":"Signed in",
  "data":{
    "tokenType":"Bearer",
    "accessToken":"<JWT>",
    "accessTokenExpiresAt":"2026-09-11T10:15:00Z",
    "refreshToken":"<opaque-token>",
    "refreshTokenExpiresAt":"2026-10-11T10:00:00Z",
    "user":{
      "id":"4fd8026f-fb4b-424f-a5e3-3f8cdb7d9334",
      "firstName":"Test",
      "lastName":"User",
      "email":"customer@example.test",
      "status":"ACTIVE",
      "emailVerified":false,
      "roles":["CUSTOMER"],
      "permissions":["CUSTOMER_SELF_READ"],
      "createdAt":"2026-09-11T10:00:00Z"
    }
  },
  "timestamp":"2026-09-11T10:00:00Z"
}
```

`/auth/me` and `/admin/profile` put the same safe User object directly under `data`. Profile endpoints return `data: {"profileId":"<uuid>","user":{...}}`; the profile ID differs from the User ID. Controllers never return JPA entities.

Challenge:

```json
{"success":true,"message":"Email verification queued","data":{"challengeId":"<uuid>","expiresAt":"2026-09-11T10:10:00Z"},"timestamp":"2026-09-11T10:00:00Z"}
```

Message-only results omit `data`, for example logout:

```json
{"success":true,"message":"Signed out","timestamp":"2026-09-11T10:00:00Z"}
```

## Error envelope

```json
{"success":false,"message":"Request validation failed","errorCode":"VALIDATION_FAILED","errors":[{"field":"email","message":"must be a well-formed email address"}],"timestamp":"2026-09-11T10:00:00Z"}
```

An error without field violations has `errors: []`. No SQL exception, password, token, rejected password value or stack trace is returned. JWT failures intentionally use one `AUTH_TOKEN_INVALID` message for malformed/expired/invalid-session tokens; refresh may return the more specific codes below.

| Code | Meaning |
| --- | --- |
| AUTH_INVALID_CREDENTIALS | Password/email incorrect, or login account inactive |
| AUTH_TOKEN_INVALID | Access or refresh authentication invalid/expired |
| AUTH_SESSION_REVOKED | Session or account is no longer active |
| AUTH_REFRESH_REUSED | Already consumed refresh token; entire session revoked |
| AUTH_CHALLENGE_INVALID | Unknown, expired, consumed, wrong-owner or incorrect proof |
| AUTH_ALREADY_VERIFIED | Email already verified |
| USER_ALREADY_EXISTS | Duplicate normalized email |
| ACCESS_DENIED | Role or permission missing |
| RATE_LIMITED | Attempt limit or resend cooldown reached |
| CONFLICT | Database uniqueness or optimistic locking conflict |

Pagination has a reusable `PageResponse<T>` with `content`, `page`, `size`, `totalElements`, `totalPages`, `first` and `last`. No collection/search business endpoint is implemented in these phases, so no fake paginated lists are served.

## Session behavior

Access JWTs last up to 15 minutes. Each session has a 30-day absolute expiry that refresh rotation does not extend. JWT claims include userId, email, roles, permissions, issuer, audience, expiry, token purpose, session ID and credential version. No secret credential is included.

The server validates JWT signatures and checks current user status, credential version, session status and database permissions. Logout affects the current session. Password changes/resets affect all sessions. In-flight requests that were already authenticated may finish while logout commits; subsequent authentication is rejected.

Refresh is deliberately single-use. If the same refresh token is sent concurrently, one request can succeed but the reuse revokes that session, including the new tokens. Clients must use a single refresh operation shared by concurrent requests. If a refresh response is lost after the server commits, do not automatically replay the old token; sign in again. This is an explicit security/UX tradeoff in this version.

## Phase 4 operations

Table/menu ownership, request bodies, availability parameters, error codes and allocation boundaries are documented in [PHASE4_API.md](PHASE4_API.md). Existing authentication/profile response contracts remain unchanged.
