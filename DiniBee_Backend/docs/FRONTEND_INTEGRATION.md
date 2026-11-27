# Connecting the three existing clients

> Current cumulative delivery: Phases 1–15. See [current API](PHASE9_15_API.md), [Phase 14 integration](PHASE14_FRONTEND_INTEGRATION.md), [deployment gates](PHASE9_15_DEPLOYMENT.md) and [verification](VERIFICATION.md). Earlier sections retain historical phase context.




This archive supplies the backend contract and guidance. It does not modify or verify the existing Flutter/React source. Integrate authentication first, then move each business page after its backend module exists.

## One base URL per application environment

| Client / environment | Typical API base URL |
| --- | --- |
| Postman / admin on the backend computer | `http://localhost:8080/api/v1` |
| Android Studio emulator, backend on host computer | `http://10.0.2.2:8080/api/v1` |
| iOS simulator, backend on the same Mac | `http://localhost:8080/api/v1` |
| Physical Android/iPhone on the same trusted LAN | `http://<backend-computer-LAN-IP>:8080/api/v1` |
| Deployed clients | `https://<your-api-domain>/api/v1` |

The Android emulator address assumes the standard emulator host mapping. On a physical device, `localhost` points to that phone, not your computer. The development Compose setup binds port 8080 to loopback; set `APP_BIND_ADDRESS=0.0.0.0` only when intentionally testing on a trusted LAN. Configure the local firewall accordingly. HTTP exceptions, where required, belong only in development app configuration; production should use HTTPS.

Each application should centralize the URL in its environment configuration. No change to controller paths is needed when deploying. Configure backend database credentials, CORS origins and service secrets separately from the public frontend API origin.

## Flutter customer and owner apps

Use one API client in each app, for example under `lib/core/network/`, with endpoint constants, JSON/error decoding and an auth interceptor. Store access and refresh credentials using platform secure storage. Never place the server JWT signing key, OTP pepper or mail encryption key in a mobile build.

1. Customer signup calls `/auth/register/customer`; owner signup calls `/auth/register/owner`.
2. Parse the wrapper: `response.data['data']` contains tokens and the user. Store the returned opaque refresh token as-is, without decoding it.
3. Send `Authorization: Bearer <accessToken>` on protected requests.
4. Read `/auth/me` to obtain current roles and verification flags. Owner role means an owner account exists; restaurant approval/service activation is returned by `/owner/dashboard-context`.
5. Customer self data uses `/customer/profile`; owner self data uses `/owner/profile`. Use authenticated identity; do not send a user/customer/owner ID to select whose profile is loaded.
6. When access expires, use a **single shared refresh operation**. Other requests wait for the same result. Persist both replacement credentials together and retry a failed protected request at most once.
7. Do not attach an expired BookEat access token to public login/register/refresh/reset requests. Resource-server authentication rejects malformed/expired bearer headers even on a route that otherwise permits anonymous access.
8. If refresh fails or the previous refresh result was lost after an ambiguous network error, clear tokens and return to sign-in. Never loop endlessly or retry the old refresh token automatically.
9. Logout calls `/auth/logout` while the access token is valid, then clears local credentials. Password reset/change revokes all sessions and returns the UI to sign-in.

Treat 400 as field/proof validation, 401 as authentication failure, 403 as insufficient permission, 409 as a conflicting operation, 429 as throttling/cooldown and 503 as an unavailable configured service. Preserve server `errorCode` for consistent messages, while displaying `message`/field errors without raw exception traces.

Phone numbers are optional registration contact data. SMS and phone authentication are not part of the current client contract.

## React / Next.js admin

Use the same `/auth/login` and `/auth/me`. Check roles/permissions to decide which controls to show, and rely on backend authorization for enforcement. Registration UI must not offer public admin signup.

This version returns tokens in JSON and accepts bearer headers. For a direct browser client, keep tokens in memory and sign in again after reload; do not store privileged tokens in localStorage. A later same-origin BFF can keep refresh credentials server-side with a carefully configured HttpOnly-cookie/CSRF design. The backend does not implement that browser-session/BFF flow in this package.

Configure the admin's exact browser origin in `CORS_ALLOWED_ORIGINS`. Production requires HTTPS origins and rejects wildcards. Mobile native networking does not use browser CORS in the same manner.

## Preserve business-page mock data during migration

Keep mock restaurant, booking, report and payment data until the corresponding endpoints exist, pass Postman tests and have working frontend services. Migrate page by page using existing feature controls. If `NEXT_PUBLIC_USE_DEMO_DATA` is one global switch for the whole app, do not flip it while most modules remain unimplemented. Set it to `false` after all required real endpoints are connected and verified; then remove unused mock code.

## Questions resolved by this phase

Android and iOS share this backend. Customer, owner and admin clients use separate route/permission boundaries within the same backend. Use the Phase 3 persistence, submission and approval endpoints in [PHASE3_API.md](PHASE3_API.md). Successful owner signup alone must not unlock all restaurant services.

## Phase 4 additions

Owner table and menu screens can use the contracts in [PHASE4_API.md](PHASE4_API.md) after the backend build/integration gates pass. Read the existing restaurant detail's version, then use restaurantVersion for all table/category/item mutations. Update it from each response. A 409 stale version requires refetch and edit reconciliation. Variants/addons live inside item POST/PUT; preserve IDs during edits and omit an option only when archiving it.

Availability GET is public, requires restaurant-local date and guests, and takes optional time/offset. Use returned offset timestamps for selection, distinguish date-only slots from time-specific tables, and display availability=false reasons normally. Do not treat a displayed slot as reserved. Reservations and customer discovery/menu browse APIs remain later phases. These notes do not modify or connect any actual Flutter/React code.
