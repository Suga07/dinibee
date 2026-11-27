# Phases 5–8 API and frontend contract

Cumulative source delivery, 2026-09-13. All paths below include `/api/v1`. The existing authentication, response envelopes, exception mapping and Phase 1–4 routes remain. See [verification](VERIFICATION.md) for unexecuted build gates, [request templates](PHASE5_8_REQUEST_EXAMPLES.json) and the cumulative Postman collection.

## Shared contracts

- Public discovery requires no token. Customer routes require a current customer bearer session. Owner routes require ownership plus `RESERVATION_MANAGE`, `ORDER_MANAGE` or `DINITICKET_MANAGE`, as applicable. Cuisine editing uses existing `MENU_MANAGE`. V14 grants the new permissions to OWNER and SUPER_ADMIN roles; owner endpoints still require OWNER and matching ownership.
- Success uses `ApiResponse<T>`; paged data uses `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`. Times representing moments use ISO-8601 offsets/UTC. `startsAt` and `pickupTime` inputs must contain offsets. Reservation responses also expose local date/time for display in the restaurant timezone.
- Creation of reservations, groups, corporate series, food orders, takeaways and queue tickets requires `Idempotency-Key`: 16–100 letters, digits, underscores or hyphens. A UUID is suitable. Keep the same key and exact request payload after a transport failure. The same actor/scope/key and payload return the same resource (its current representation); changed payload returns 409. A new intent needs a new key. The hash and created resource commit in the same transaction. Keys currently have no automatic deletion policy.
- New resource updates require the latest **resource `version`**, not the restaurant's version. Restaurant cuisine/tax configuration uses `restaurantVersion`. Group invite/remove/lock and corporate guest invitation take `?version=`. Corporate edit and cancellation use the nested reservation's version. Reload after 409 and reconcile the requested change.
- Unknown JSON properties are rejected. Clients cannot set customer/owner identity, booking/payment status, calculated totals, queue position, allocation IDs or server timestamps. Validation errors return 400; missing/scoped-out resources generally return 404; invalid states, stale versions and allocation conflicts return 409.
- List page starts at 0, size defaults to 20 with maximum 100. Public search additionally caps page at 10,000. No provider payment or invoice is created by these phases.

## Phase 5: discovery

Search `/restaurants` or its `/restaurants/search` alias. Results require APPROVED, ACTIVE and the selected service's requested + authorized + enabled flags. Default `serviceType=RESTAURANT`; TAKEAWAY and DINITICKET may be selected explicitly. Detail, menu and hours apply the same visibility rule; they do not expose onboarding documents, settlement/bank information, owner identifiers or approval notes.

| Query | Meaning |
| --- | --- |
| restaurantName / city | Case-insensitive literal substring / exact city |
| latitude / longitude | Supply both, degrees in valid ranges |
| radius | Positive distance in kilometres, maximum 500, coordinates required |
| cuisine | Exact case-insensitive owner cuisine tag |
| rating | Minimum average rating, 0–5 |
| foodType | Available item food type from existing menu enum |
| serviceType | RESTAURANT, TAKEAWAY or DINITICKET |
| openNow | Current operating window including overnight and dated overrides |
| offerAvailable | At least one available selected-service item discounted below base price |
| sort | NAME (default), RATING or DISTANCE; distance sort requires coordinates |
| page / size | Paged, deterministic order with ID tie-breaker |

Missing restaurant coordinates yield null distance and are excluded by a radius filter. Distances are great-circle estimates, not road routes. Rating summaries start at 0 reviews/0 rating until a later verified review workflow supplies them. No fabricated ratings or review-write API is included. Offer filtering currently means a menu discount, not a coupon/campaign system.

Public menu includes active categories, available non-deleted items and active variants/addons with selected-service flags. Options are batch-loaded for the requested page. Availability remains the existing Phase 4 endpoint; its response is advisory. Favorites are unique per customer/restaurant, repeated save is idempotent and listings exclude venues that no longer meet the selected visibility rule. Owner cuisine PUT replaces at most 20 unique nonblank tags using `restaurantVersion`.

## Phase 6: reservations, groups and companies

Regular create body: `restaurantId`, `tableId`, offset `startsAt`, `guestCount`, optional `specialRequest`. The backend locks the restaurant/table, rechecks service, hours, lead time, booking horizon, physical capacity and active overlap, snapshots cancellation/no-show timing, and claims the table atomically. Dining duration and grace come from the restaurant policy. Client-supplied duration or payment status is not accepted. Initial status is PENDING or CONFIRMED according to auto-confirm; payment is UNPAID.

Customer edit applies before the start to eligible regular or unlocked group reservations. It releases the old allocation and attempts the replacement in one transaction; failure restores the original allocation. Active linked food orders block rescheduling. Cancellation follows the snapshotted allowed/deadline policy, releases the allocation, cancels unaccepted PLACED orders and blocks when accepted kitchen work is unresolved. Owner confirmation/rejection operates on PENDING; no-show is allowed only at/after the snapshotted no-show deadline. Full arrival/check-in/dining completion is Phase 9.

Group create wraps a regular reservation with `displayName`, `memberLimit` and `paymentMode`. One physical table is allocated, with host included in both guest and member counts. Modes: HOST_PAYS, SPLIT_EQUAL, SPLIT_BY_ITEMS. A group starts ACTIVE with its host JOINED. Invite rotation returns `inviteCode`, `inviteUrl`, `qrCode` and `expiresAt`; only a SHA-256 token hash is stored. `qrCode` is the deep-link payload for a frontend QR renderer, not an image. The token expires at the earlier of two days or the reservation start. Rotation invalidates the old token. No invitation message is sent automatically.

Only an eligible invited customer may join an unexpired, unlocked group with capacity. GET is limited to host or joined member. Members cannot see one another's email addresses. Only the host removes members or locks the group; the host cannot be removed, and unresolved member orders prevent removal. LOCKED freezes membership and new group orders. **GET the group after invite rotation to obtain its new version.** Combined group orders and proposed payment shares are host-only; amounts are proposals, not payment collection. Equal split distributes exact residual cents in a stable member order; item split groups each customer's committed order total.

Corporate create wraps a reservation, an existing owned active `corporateAccountId` **or** a new `account`, `budgetLimit`, `billingType` COMPANY/INDIVIDUAL, `recurrenceType`, `occurrences`, `interval` and `invoiceRequired`. NONE requires one occurrence; DAILY/WEEKLY/MONTHLY allow 1–12 occurrences and intervals 1–12, anchored to the original local wall time in the restaurant timezone. Every occurrence must satisfy the current booking horizon and availability. Any conflict rolls back the entire series and a newly created account. Monthly dates clamp to the target month's valid day. Each occurrence has one table and its own budget; there is no automatic table combining.

Corporate GET/list/combined orders are organizer-only. A guest invitation reserves a seat; organizer counts as one. Joining requires a signed-in customer with a verified email matching the invitation. The response returns a private deep link; no mail is sent. **Reload corporate details after guest invitation to refresh the corporate version.** Guest acceptance enables that guest's own orders. Edits/cancellation affect one occurrence. Corporate edit currently follows reservation rescheduling rules even when changing only budget/billing details, so active food commitments block it. Budget checks include taxes and all non-cancelled/non-rejected orders, including completed orders. Invoice and billing fields record intent only.

## Phase 7: orders and takeaway

Food create: `restaurantId`, `orderType`, an eligible `reservationId` or `diniTicketId`, `items` and optional instructions. Each line has `menuItemId`, optional `variantId`, an explicit `addonIds` array, quantity 1–99 and optional notes. Up to 50 lines and 50 distinct addons per line. IDs must belong to the selected item/restaurant and options must be active. The server derives group/corporate links and ignores no client financial fields: unknown financial fields are rejected.

DINE_IN_PREORDER requires a pending/confirmed future reservation, restaurant preorder policy and item flag. DINE_IN requires CHECKED_IN/DINING; those transitions await Phase 9. Group ordering additionally requires ACTIVE membership; corporate ordering requires organizer or joined guest. DINITICKET_PREORDER requires the customer's eligible queued ticket, service policy and item flag. TAKEAWAY/ON_THE_WAY use the dedicated takeaway creation route.

The server snapshots item/variant/addon names and prices, quantity, gross subtotal, discount, tax percentage, tax and total. Existing Phase 4 variant pricing is retained: selecting a variant uses its price instead of the base/discounted base; addons add their prices. Currency is INR. Net discounted subtotal is taxed at the restaurant's configured percentage and rounded HALF_UP to two decimals. Owner commerce-policy GET/PUT supplies `restaurantVersion` and `taxPercent` (0–100); default tax is 0 until configured. Historical orders keep their snapshots when menu prices or tax change.

Food lifecycle: PLACED → ACCEPTED → PREPARING → READY → SERVED → COMPLETED. Customer cancellation is PLACED-only; owner may reject PLACED/ACCEPTED/PREPARING/READY to resolve work that cannot be fulfilled. Serving requires an eligible checked-in reservation or SEATED ticket. Takeaway orders cannot use generic food lifecycle writes.

Takeaway create adds offset `pickupTime` and required `onTheWay` boolean. The selected venue must enable the requested takeaway mode, accept cash (online payment is a later phase), meet minimum order amount and have a continuous preparation/pickup window. Pickup must be at least average preparation minutes away and within maximum advance minutes. Its food order and takeaway status update together: PLACED → ACCEPTED → PREPARING → READY_FOR_PICKUP → COLLECTED. Owner collection requires the current takeaway version and the customer's exact 12-character pickup code. Owner read responses omit this secret. Collection does not mark payment PAID.

On-the-way location requires the customer to grant device permission and send `consent=true` explicitly. Only the latest point is retained, with straight-line distance and an arrival estimate using an assumed 30 km/h, not road routing or live traffic. Restaurant owners can see tracking only on their own orders. DELETE location revokes consent and erases coordinates/ETA; collected/cancelled/rejected orders erase tracking too. There is no background device tracking or scheduled cleanup for abandoned nonterminal orders; the client must offer a stop-sharing control and display the location timestamp.

## Phase 8: DiniTicket and event replay

Customer ticket creation accepts only restaurant and guest count. The restaurant must be open with DINITICKET enabled and remote join allowed. Owner walk-in creation accepts an existing customer ID even when remote join is disabled. It does not create anonymous/customer accounts. Queue sequence and ticket number are generated by the server; one active ticket per customer/restaurant is also enforced with a database partial unique index. Capacity counts WAITING/CALLED/ARRIVED; SEATED remains active but is outside the waiting queue. Wait estimate is `(position - 1) × configured minutes per party`, not a promised seating time.

Lifecycle: WAITING → CALLED → ARRIVED → SEATED → COMPLETED. Only the first WAITING ticket may be called. Calling starts a fresh expiry deadline. No-show is CALLED-only at/after that deadline. Customer cancel is limited to queued states. Owner seat may omit `tableId`; supplying it locks and allocates an available compatible table for configured dining duration plus grace (90 minutes if reservation policy is absent). It checks a continuous opening window and database overlap protection without requiring a reservation start grid. Table status becomes OCCUPIED until completion releases the claim. Owner inventory version is advanced when seating changes table state; refresh restaurant detail before later inventory writes.

Configured automatic expiry applies to WAITING/CALLED, not ARRIVED/SEATED. A 30-second worker processes bounded restaurant batches and read/issue paths also expire eligible tickets. Owner queue reads should precede call actions so overdue queue heads are refreshed. Accepted/preparing/ready preorder work defers automatic expiry; `expiryDeferredByPreorder` signals this when automatic expiry is enabled. The owner must resolve the food work; automatic expiry cancels only unaccepted PLACED preorders. Ticket completion requires all linked orders terminal.

Queue transitions append durable events in the same database transaction. Events include TICKET_CREATED, QUEUE_POSITION_CHANGED, TICKET_CALLED, TICKET_EXPIRED, TICKET_SEATED and other status transitions. Customers replay only their own events; owners only their restaurant's events. JSON `/events?after=N` returns up to 100 rows after the exclusive numeric cursor. SSE `/stream` uses `Last-Event-ID: N` and bearer authentication, sends a **finite replay batch** with event ID/name/data and retry=3000, then closes. Reconnect after three seconds; if a batch is full, drain further events using the last event ID. Every reconnect revalidates session and permissions. This is durable replay architecture, not a persistent socket or external push-notification delivery. PostgreSQL-backed replay works across application instances without in-memory subscribers.

Flutter/custom HTTP streaming clients can supply Authorization headers. Browser native EventSource cannot set bearer headers; use authenticated fetch streaming or JSON polling instead of putting access tokens in URLs. Maintain a separate event cursor for each account/restaurant scope. Initial/recovery flow is GET current/owner waiting, followed by replay and a refreshed snapshot; never infer authority from client-computed positions. Event retention/pruning is not automated in this phase.

## Postman workflow

The master collection contains 166 requests: the original 85 are retained and 81 examples are added. Import the matching environment; populate actual IDs from responses and `startsAt` from a currently available slot. Set a valid future `pickupTime`. New request tests save IDs and resource versions. Keys initialize once with a UUID and must be cleared only for a new intent.

Folders contain alternate lifecycle outcomes, not one collection-run sequence. Use separate customer sessions for host and invited guests; return `customerToken` to the host before organizer actions. The owner walk-in endpoint requires **customer profile ID**, not user ID. Reload group/corporate details after invites, and reload restaurant detail after seating changes table state. Do not reuse an event cursor after switching customer/restaurant. Secret tokens, pickup codes and invite links belong in private local environments.

## Endpoint index

### Discovery

| Method | Path | Access | Body DTO |
| --- | --- | --- | --- |
| GET | `/api/v1/restaurants` | Public | — |
| GET | `/api/v1/restaurants/search` | Public | — |
| GET | `/api/v1/restaurants/{id}` | Public | — |
| GET | `/api/v1/restaurants/{id}/menu` | Public | — |
| GET | `/api/v1/restaurants/{id}/operating-hours` | Public | — |
| POST | `/api/v1/customer/favorites/{id}` | Customer | — |
| DELETE | `/api/v1/customer/favorites/{id}` | Customer | — |
| GET | `/api/v1/customer/favorites` | Customer | — |
| PUT | `/api/v1/owner/restaurants/{id}/cuisines` | Owner | Cuisines |

### Reservations

| Method | Path | Access | Body DTO |
| --- | --- | --- | --- |
| POST | `/api/v1/customer/reservations` | Customer | BookingRequests.Create |
| GET | `/api/v1/customer/reservations` | Customer | — |
| GET | `/api/v1/customer/reservations/{id}` | Customer | — |
| PUT | `/api/v1/customer/reservations/{id}` | Customer | BookingRequests.Change |
| PUT | `/api/v1/customer/reservations/{id}/cancel` | Customer | BookingRequests.Action |
| GET | `/api/v1/owner/restaurants/{restaurantId}/reservations` | Owner | — |
| GET | `/api/v1/owner/restaurants/{restaurantId}/reservations/{id}` | Owner | — |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/reservations/{id}/confirm` | Owner | BookingRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/reservations/{id}/reject` | Owner | BookingRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/reservations/{id}/no-show` | Owner | BookingRequests.Action |

### Groups

| Method | Path | Access | Body DTO |
| --- | --- | --- | --- |
| POST | `/api/v1/customer/group-bookings` | Customer | BookingRequests.Group |
| GET | `/api/v1/customer/group-bookings/{id}` | Customer | — |
| POST | `/api/v1/customer/group-bookings/{id}/invite` | Customer | — |
| POST | `/api/v1/customer/group-bookings/join/{inviteCode}` | Customer | BookingRequests.Join |
| GET | `/api/v1/customer/group-bookings/{id}/members` | Customer | — |
| DELETE | `/api/v1/customer/group-bookings/{id}/members/{memberId}` | Customer | — |
| POST | `/api/v1/customer/group-bookings/{id}/lock` | Customer | — |

### Corporate

| Method | Path | Access | Body DTO |
| --- | --- | --- | --- |
| POST | `/api/v1/customer/corporate-bookings` | Customer | BookingRequests.Corporate |
| GET | `/api/v1/customer/corporate-bookings` | Customer | — |
| GET | `/api/v1/customer/corporate-bookings/{id}` | Customer | — |
| POST | `/api/v1/customer/corporate-bookings/{id}/guests` | Customer | BookingRequests.Guest |
| POST | `/api/v1/customer/corporate-bookings/join/{inviteCode}` | Customer | — |
| PUT | `/api/v1/customer/corporate-bookings/{id}` | Customer | BookingRequests.CorporateChange |
| PUT | `/api/v1/customer/corporate-bookings/{id}/cancel` | Customer | BookingRequests.Action |

### Food orders

| Method | Path | Access | Body DTO |
| --- | --- | --- | --- |
| POST | `/api/v1/customer/orders` | Customer | OrderRequests.Create |
| GET | `/api/v1/customer/orders` | Customer | — |
| GET | `/api/v1/customer/orders/{id}` | Customer | — |
| PUT | `/api/v1/customer/orders/{id}/cancel` | Customer | OrderRequests.Action |
| GET | `/api/v1/customer/group-bookings/{id}/orders` | Customer | — |
| GET | `/api/v1/customer/corporate-bookings/{id}/orders` | Customer | — |
| GET | `/api/v1/owner/restaurants/{id}/commerce-policy` | Owner | — |
| PUT | `/api/v1/owner/restaurants/{id}/commerce-policy` | Owner | Policy |
| GET | `/api/v1/owner/restaurants/{restaurantId}/orders` | Owner | — |
| GET | `/api/v1/owner/restaurants/{restaurantId}/orders/{id}` | Owner | — |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/orders/{id}/accept` | Owner | OrderRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/orders/{id}/prepare` | Owner | OrderRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/orders/{id}/ready` | Owner | OrderRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/orders/{id}/serve` | Owner | OrderRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/orders/{id}/complete` | Owner | OrderRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/orders/{id}/reject` | Owner | OrderRequests.Action |

### Takeaway

| Method | Path | Access | Body DTO |
| --- | --- | --- | --- |
| POST | `/api/v1/customer/takeaway/orders` | Customer | OrderRequests.Takeaway |
| GET | `/api/v1/customer/takeaway/orders` | Customer | — |
| GET | `/api/v1/customer/takeaway/orders/{id}` | Customer | — |
| PUT | `/api/v1/customer/takeaway/orders/{id}/cancel` | Customer | OrderRequests.Action |
| PUT | `/api/v1/customer/takeaway/orders/{id}/location` | Customer | OrderRequests.Location |
| DELETE | `/api/v1/customer/takeaway/orders/{id}/location` | Customer | — |
| GET | `/api/v1/owner/restaurants/{restaurantId}/takeaway/orders` | Owner | — |
| GET | `/api/v1/owner/restaurants/{restaurantId}/takeaway/orders/{id}` | Owner | — |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/takeaway/orders/{id}/collected` | Owner | OrderRequests.Collect |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/takeaway/orders/{id}/accept` | Owner | OrderRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/takeaway/orders/{id}/preparing` | Owner | OrderRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/takeaway/orders/{id}/ready` | Owner | OrderRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/takeaway/orders/{id}/reject` | Owner | OrderRequests.Action |

### DiniTicket

| Method | Path | Access | Body DTO |
| --- | --- | --- | --- |
| POST | `/api/v1/customer/dinitickets` | Customer | TicketRequests.Create |
| GET | `/api/v1/customer/dinitickets/current` | Customer | — |
| GET | `/api/v1/customer/dinitickets/{id}` | Customer | — |
| PUT | `/api/v1/customer/dinitickets/{id}/cancel` | Customer | TicketRequests.Action |
| POST | `/api/v1/owner/restaurants/{restaurantId}/dinitickets` | Owner | WalkIn |
| GET | `/api/v1/owner/restaurants/{restaurantId}/dinitickets` | Owner | — |
| GET | `/api/v1/owner/restaurants/{restaurantId}/dinitickets/waiting` | Owner | — |
| GET | `/api/v1/owner/restaurants/{restaurantId}/dinitickets/{id}` | Owner | — |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/dinitickets/{id}/seat` | Owner | TicketRequests.Seat |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/dinitickets/{id}/call` | Owner | TicketRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/dinitickets/{id}/arrived` | Owner | TicketRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/dinitickets/{id}/no-show` | Owner | TicketRequests.Action |
| PUT | `/api/v1/owner/restaurants/{restaurantId}/dinitickets/{id}/complete` | Owner | TicketRequests.Action |
| GET | `/api/v1/customer/dinitickets/events` | Customer | — |
| GET | `/api/v1/owner/restaurants/{restaurantId}/dinitickets/events` | Owner | — |
| GET | `/api/v1/customer/dinitickets/stream` | Customer | — |
| GET | `/api/v1/owner/restaurants/{restaurantId}/dinitickets/stream` | Owner | — |
