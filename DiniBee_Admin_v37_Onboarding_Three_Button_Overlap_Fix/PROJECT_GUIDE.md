# Project Guide

## 1. What this application does

This repository is the frontend for the BookEat / DiniBee super-admin panel. One admin workspace monitors the three customer services:

- Restaurant: table reservations, arrival, check-in, dining, food orders, checkout, and payment.
- Takeaway: regular pre-orders and On-the-Way orders.
- DiniTicket: restaurant token queues, preparation, ready, called, collected, completed, cancelled, and no-show states.

It also handles the platform functions shared by those services: restaurant onboarding, users, finance, wallet points, reviews, support, notifications, reports, roles, and settings.

## 2. How the frontend is organized

The application has four layers:

1. Page components render tables, summaries, drawers, forms, and confirmations.
2. Service files describe the data operation needed by each page.
3. `data-source.ts` chooses either the centralized demo repository or the REST API.
4. `api.ts` sends HTTP requests, adds the JWT bearer token, parses errors, and handles HTTP 401.

Page components do not import business demo records directly. This keeps the later backend migration small and predictable.

## 3. Main modules

| Area | Pages and behavior |
| --- | --- |
| Dashboard | Ecosystem KPIs, service volume, revenue trend, finance, support, and activity |
| Restaurants | All, pending, approved, rejected, services, suspension, and verification |
| Reservations | All, Regular, Group, Corporate, arrival, dining, checkout, payment, and table state |
| Orders | Food orders, Takeaway, On-the-Way, and DiniTicket queue monitoring |
| Finance | Payments, settlements, refunds, commission, and corporate invoices |
| Wallet | Overview, reward rules, transactions, redemptions, and audited adjustments |
| Community | Reviews, moderation, customer support, and restaurant-owner support |
| Communication | Audience-based and scheduled notifications |
| Analytics | Cross-service metrics, filters, and export requests |
| Platform | Users, offers, coupons, admin users, roles, and settings |

## 4. Important files

| File or folder | Responsibility |
| --- | --- |
| `components/bookeat/admin-panel.tsx` | Responsive shell, sidebar, top bar, and route registration |
| `components/bookeat/ecosystem/core-pages.tsx` | Dashboard, restaurants, services, and reservations |
| `components/bookeat/ecosystem/operations-pages.tsx` | Food orders, Takeaway, and DiniTicket |
| `components/bookeat/ecosystem/finance-pages.tsx` | Payments, settlements, wallet, and invoices |
| `components/bookeat/ecosystem/community-pages.tsx` | Reviews, support, and analytics |
| `components/bookeat/ecosystem/platform-pages.tsx` | Refunds and notifications |
| `components/bookeat/shared.tsx` | Common data table, status badge, page header, and summary cards |
| `lib/bookeat/types.ts` | TypeScript contracts expected from the backend |
| `lib/bookeat/services/` | REST paths and demo fallbacks |
| `lib/bookeat/demo-data.ts` | All new ecosystem demo records in one repository |
| `lib/bookeat/api.ts` | Base URL, HTTP methods, JWT header, and API errors |

## 5. Data and state flow

1. A route renders its page component.
2. `useApiResource` starts the request and exposes `loading`, `error`, `data`, and `reload`.
3. The module service calls `readCollection`, `readResource`, or `runMutation`.
4. Demo mode returns a safe clone of centralized demo data. Live mode calls `apiClient`.
5. The page renders success, empty, error, and retry states from the same contract.

## 6. Safety behavior

- Approvals, rejections, suspensions, moderation, refunds, and similar sensitive changes use confirmation dialogs.
- Demo refund submission becomes `PROCESSING`; the browser never invents a successful gateway result.
- Demo notification submission becomes `QUEUED`; the backend remains responsible for delivery and recipient counts.
- Wallet adjustments require a reason and remain auditable.
- Support chats keep customer and restaurant-owner conversations in one workflow.
- A 401 response removes the stored token and broadcasts `bookeat:unauthorized` for an auth shell to handle.

## 7. Responsive behavior

The navigation collapses on smaller screens, tables keep horizontal scrolling inside their cards, cards reflow into fewer columns, and dialogs/drawers use constrained widths. The desktop layout preserves the supplied navy-and-gold BookEat design.

## 8. Backend contract

The types in `lib/bookeat/types.ts` are the source of truth for JSON field names and statuses. Collection endpoints may return either a JSON array or the `PageResponse<T>` shape from `lib/bookeat/api.ts`.

Follow [MOCK_DATA_TO_BACKEND_GUIDE.md](./MOCK_DATA_TO_BACKEND_GUIDE.md) to activate the real API and safely remove the demo repository.
