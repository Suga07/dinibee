# Remove Mock Data and Connect the Backend

Use two phases. First disable mock data and prove every backend endpoint. Delete the mock files only after the live integration passes. This avoids changing the UI and the data contracts at the same time.

## Phase A — switch from demo data to the backend

### Step 1: prepare Spring Boot

Run the backend on a stable API root, for example `http://localhost:8080/api/v1`. Enable CORS for the frontend development origin and allow the `Authorization` and `Content-Type` headers.

Return JSON field names and status strings exactly as defined in `lib/bookeat/types.ts`. A list endpoint can return either:

```json
[{ "id": "example" }]
```

or:

```json
{
  "content": [{ "id": "example" }],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Step 2: create the local environment file

Copy `.env.example` to `.env.local` and change it to:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_USE_DEMO_DATA=false
```

Restart `npm run dev` after changing environment variables. With the flag set to `false`, `data-source.ts` does not read from `demo-data.ts` at runtime.

### Step 3: connect authentication

Call `adminApi.auth.login(email, password)` from the login page. After a successful response, store the token:

```ts
const result = await adminApi.auth.login(email, password);
setStoredToken(result.token);
```

`apiClient` then sends `Authorization: Bearer <token>` automatically. HTTP 401 clears that token and dispatches `bookeat:unauthorized`; connect that event to the login redirect in the final authentication shell.

### Step 4: implement the endpoints

The service files already call these paths:

| Module | Method and path |
| --- | --- |
| Dashboard | `GET /admin/dashboard` |
| Restaurants | `GET /admin/restaurants?approvalStatus=...` |
| Restaurant approval | `PATCH /admin/restaurants/{id}/approval` |
| Restaurant services | `PUT /admin/restaurants/{id}/services` |
| Restaurant suspension | `PATCH /admin/restaurants/{id}/suspension` |
| Reservations | `GET /admin/reservations?bookingType=...` |
| Food orders | `GET /admin/food-orders` |
| Takeaway | `GET /admin/takeaway-orders?type=...` |
| DiniTicket | `GET /admin/diniticket/orders?statuses=...` |
| Payments | `GET /admin/finance/payments` |
| Settlements | `GET /admin/finance/settlements` |
| Refund list | `GET /admin/finance/refunds` |
| Submit refund | `POST /admin/finance/refunds/{refundId}/submit` |
| Corporate invoices | `GET /admin/corporate-invoices` |
| Wallet overview | `GET /admin/wallet/overview` |
| Customer wallets | `GET /admin/wallet/customers` |
| Wallet transactions | `GET /admin/wallet/transactions` |
| Reward rules | `GET` and `PUT /admin/wallet/reward-rules` |
| Wallet adjustment | `POST /admin/wallet/adjustments` |
| Reviews | `GET /admin/reviews` |
| Review moderation | `PATCH /admin/reviews/{id}/moderation` |
| Support tickets | `GET /admin/support/tickets?userType=...&status=...` |
| Support reply | `POST /admin/support/tickets/{id}/messages` |
| Support status | `PATCH /admin/support/tickets/{id}/status` |
| Notifications | `GET` and `POST /admin/notifications` |
| Analytics | `GET /admin/reports/overview?...` |

### Step 5: test one module at a time

Start with the dashboard, then restaurants, reservations, operations, finance, wallet, and finally support/notifications. For each page verify:

1. The request URL, query parameters, and bearer token are correct.
2. Loading changes to populated or empty state.
3. A backend error shows the retry state and retry sends a new request.
4. A mutation returns the complete updated record expected by its service type.
5. Refreshing a deep URL keeps the same route.

Run after every group of endpoints:

```bash
npm run lint
npx tsc --noEmit
npm test
```

### Step 6: keep backend-owned truth on the backend

- A refund must remain `PROCESSING` until a verified payment-gateway callback marks it `COMPLETED` or `FAILED`.
- Notification delivery status and recipient count must come from the backend worker/provider.
- Payment totals, commission, settlement totals, and wallet balances must be calculated and validated server-side.
- Permissions must be enforced by Spring Security; hiding a frontend button is only a usability feature.

## Phase B — delete mock data permanently

Do this only when every live page has passed Phase A.

### Step 7: remove demo fallbacks from services

In every file under `lib/bookeat/services/`:

1. Remove the `demoStore` or demo factory import.
2. Replace `readCollection(...)`, `readResource(...)`, and `runMutation(...)` with direct `apiClient` calls.
3. For list endpoints that return `PageResponse<T>`, keep `unwrapCollection` or normalize the response in the service.

Example before:

```ts
list: () => readCollection(
  () => apiClient.get<RestaurantRecord[]>("/admin/restaurants"),
  () => demoStore.restaurants,
)
```

Example after, when the backend returns an array:

```ts
list: () => apiClient.get<RestaurantRecord[]>("/admin/restaurants")
```

### Step 8: replace the source indicator

`components/bookeat/ecosystem/shared.tsx` imports `isDemoDataEnabled` for the “Backend-ready demo source” badge. After demo mode is permanently removed, delete that import and make `DataSourceNotice` always render “Connected to live API”, or remove the component from page headers.

### Step 9: delete the demo files and flag

When no import remains:

1. Delete `lib/bookeat/demo-data.ts`.
2. Delete demo-only functions from `lib/bookeat/services/data-source.ts`, or delete the file if `unwrapCollection` is no longer used.
3. Remove `NEXT_PUBLIC_USE_DEMO_DATA` from `.env.local` and `.env.example`.
4. Confirm there are no remaining references:

```bash
rg "demoStore|demo-data|isDemoDataEnabled|readCollection|readResource|runMutation" . \
  -g '!node_modules' -g '!dist'
```

### Step 10: perform the final live check

Run lint, TypeScript, tests, and the production build. Then visit every sidebar page with the backend running and check list, empty, error, detail, update, and refresh behavior. Do not remove the mock repository until this check is clean.
