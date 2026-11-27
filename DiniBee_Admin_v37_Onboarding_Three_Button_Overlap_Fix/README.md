# BookEat / DiniBee Admin Frontend

A frontend-only administration panel for the complete DiniBee restaurant ecosystem. It covers restaurant onboarding, reservations, food orders, Takeaway, DiniTicket queues, payments, settlements, wallet points, reviews, support, notifications, and analytics.

The UI uses BookEat's navy-and-gold visual language, is responsive, and is designed to connect to a Spring Boot REST API without changing page components.

## What is included

- 48 working application routes, including filtered and compatibility routes
- Restaurant approval, rejection, service-access, and suspension workflows
- Regular, Group, and Corporate reservation monitoring
- Arrival, dining, checkout, table, and payment state visibility
- Food-order, Takeaway, On-the-Way, and DiniTicket operations
- Payments, settlements, refunds, commissions, and corporate invoices
- Wallet balances, reward rules, transactions, redemptions, and adjustments
- Review moderation and combined customer/restaurant-owner support chat
- Targeted and scheduled notification composer
- Ecosystem analytics and export request controls
- Centralized demo repository with a one-variable switch to real APIs
- Loading, error, retry, empty, detail, confirmation, and toast states

## Technology

- React 19 and TypeScript
- Vinext / Vite
- Tailwind CSS 4
- Shadcn/Radix UI primitives
- Recharts
- Lucide icons

## Run locally

Use Node.js 22.13 or newer.

```bash
npm install
npm run dev
```

Then open the URL printed by the development server.

## Validate the project

```bash
npm run lint
npx tsc --noEmit
npm test
```

`npm test` runs the production build before the project tests.

## Connect the backend

Copy `.env.example` to `.env.local`, point it at the Spring Boot API, and disable demo mode:

```env
NEXT_PUBLIC_API_BASE_URL=https://api.dinibee.tech/api/v1
NEXT_PUBLIC_USE_DEMO_DATA=false
```

The frontend will then call the REST endpoints in `lib/bookeat/services/`. Read [MOCK_DATA_TO_BACKEND_GUIDE.md](./MOCK_DATA_TO_BACKEND_GUIDE.md) before deleting the demo repository.

## Project map

- `components/bookeat/admin-panel.tsx` — shell, navigation, and route map
- `components/bookeat/ecosystem/` — full ecosystem pages and shared UI
- `components/bookeat/shared.tsx` — reusable tables, badges, headers, and cards
- `lib/bookeat/types.ts` — shared frontend/backend contracts
- `lib/bookeat/services/` — API boundary for every business module
- `lib/bookeat/demo-data.ts` — centralized development-only dataset
- `lib/bookeat/api.ts` — REST client, JWT storage, and 401 handling
- `hooks/use-api-resource.ts` — loading/error/retry data hook
- `app/globals.css` — theme and responsive styles

For a module-by-module explanation, see [PROJECT_GUIDE.md](./PROJECT_GUIDE.md).
