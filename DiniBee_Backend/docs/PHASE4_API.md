# Phase 4 — tables, menu and availability

This extends the existing `com.bookeat` project with 18 HTTP operations: 6 table, 11 menu and 1 public availability operation. Authentication, identity, onboarding and approval APIs remain in place. Variants and addons are managed inside menu item requests. Full Spring/PostgreSQL execution is pending; see [VERIFICATION.md](VERIFICATION.md).

## Common contract

Prefix: `/api/v1`. Owner paths start with `/owner/restaurants/{restaurantId}`. The bearer principal must have OWNER plus TABLE_MANAGE or MENU_MANAGE respectively. Owning a different restaurant, using another restaurant's category/item/table, or supplying another item's option ID returns 404. The future manager/staff assignment model is not enabled by these permissions. SUPER_ADMIN's seeded permission alone does not grant ownership of another owner's restaurant.

Owners can maintain inventory in DRAFT, CHANGES_REQUESTED, REJECTED or APPROVED. SUBMITTED, UNDER_REVIEW and SUSPENDED are locked. Approved status permits inventory updates without reopening evidence review; it does not bypass service activation for public availability or effective menu visibility.

Every owner mutation supplies the **parent `restaurantVersion`**, which is the `version` returned by the existing restaurant detail endpoint. For POST/PUT/PATCH it is a required nonnegative JSON integer. DELETE takes it as a query parameter. Resource-specific `version` values are informational; do not send them in place of the parent version.

Single-resource reads and all writes return this shape inside the existing `ApiResponse.data`:

```json
{
  "restaurantVersion": 12,
  "resource": { "id": "11111111-1111-4111-8111-111111111111", "version": 0 }
}
```

The abbreviated resource above represents a full table/category/item DTO. An archive response instead uses the archived UUID as `resource`. POST returns 201; other successful operations return 200. Page reads use the existing `PageResponse`: content, page, size, totalElements, totalPages, first, last. Page is zero-based; size is 1–100, default 20. Refresh restaurantVersion after every write; on 409 refetch and reconcile the edit before retrying.

Parent locking serializes owner inventory changes and the internal allocation writer. Each inventory mutation increments the restaurant version and writes audit history in the same transaction. Listings exclude soft-deleted resources; rows are retained for future historical references.

## Tables

| Method | Owner path suffix | Body / query | Result |
|---|---|---|---|
| POST | `/tables` | Full table body | New table; status starts AVAILABLE |
| GET | `/tables` | page, size | All current owned tables, including inactive/status-blocked ones |
| GET | `/tables/{tableId}` | None | Full table and parent version |
| PUT | `/tables/{tableId}` | Full table body | Replacement configuration |
| PATCH | `/tables/{tableId}/status` | restaurantVersion, status, reason | Audited operational state |
| DELETE | `/tables/{tableId}` | restaurantVersion query | Archive unused table |

Full table body: restaurantVersion, tableNumber, tableName, capacity, minimumGuests, maximumGuests, floor, section, shape, xPosition, yPosition, width, height, rotation, active. TableNumber is an ASCII letter/digit followed by letters, digits, spaces, `_` or `-`, at most 30 characters; it is trimmed and uppercased and unique among current tables in a restaurant. The same number can be used in another restaurant or after archival. Names/sections are at most 100 characters.

Capacity is 1–500, with `1 <= minimumGuests <= maximumGuests <= capacity`. Floor is −10–200. Coordinates are 0–100000; dimensions must be positive and at most 10000; rotation is `[0,360)`. Geometry has at most two decimal places and uses consistent client canvas units; the API does not assume physical units. Shapes: ROUND, SQUARE, RECTANGLE, OVAL, CUSTOM. Maximum current tables per restaurant: 1000.

Statuses: AVAILABLE, RESERVED, OCCUPIED, BLOCKED, MAINTENANCE. Only **active AVAILABLE** tables can be offered. RESERVED is a conservative operational block until staff restore AVAILABLE; scheduled future reservations are represented by timed allocation rows, not by setting this all-day status. Changing status never cancels an allocation. Reason is required (1–500 nonblank characters); old status and new status/reason are audited.

Configuration changes or archival return TABLE_IN_USE if an active allocation ends after the current instant. Finished historical allocations do not block archival. Capacity is evaluated per table; Phase 4 does not combine multiple tables.

## Menu

| Method | Owner path suffix | Body / query | Result |
|---|---|---|---|
| POST | `/menu/categories` | Category body | Create category |
| GET | `/menu/categories` | page, size | Categories by displayOrder then UUID |
| GET | `/menu/categories/{categoryId}` | None | Category and parent version |
| PUT | `/menu/categories/{categoryId}` | Category body | Replace category |
| DELETE | `/menu/categories/{categoryId}` | restaurantVersion | Archive empty category |
| POST | `/menu/items` | Full item body and nested options | Create item |
| GET | `/menu/items` | categoryId, foodType, available, search, page, size | Filter current items |
| GET | `/menu/items/{itemId}` | None | Full item, options and parent version |
| PUT | `/menu/items/{itemId}` | Full item body and nested options | Replace item/options |
| PATCH | `/menu/items/{itemId}/availability` | restaurantVersion, available | Toggle stored availability |
| DELETE | `/menu/items/{itemId}` | restaurantVersion | Archive item and options |

Category body: restaurantVersion, name, description, displayOrder, active. Names are nonblank, at most 100 characters and unique ignoring case in a restaurant. Description is optional, at most 2000 characters. DisplayOrder is 0–100000. Deletion requires no current items, including unavailable ones; move or archive the items first. Maximum current categories: 100.

Item body: restaurantVersion, categoryId, name, description, basePrice, discountPrice, foodType, spiceLevel, imageUrl, available, preOrderAvailable, takeawayAvailable, diniTicketPreOrderAvailable, preparationTimeMinutes, featured, displayOrder, variants, addons. All booleans and option arrays must be supplied. Description, discountPrice and imageUrl may be null. Names are nonblank, at most 160 characters, and unique ignoring case within their restaurant/category. Description is at most 3000 characters; imageUrl is an optional HTTPS URL of at most 2048 characters. The server stores the URL and does not fetch it. Preparation time is 0–1440 minutes; displayOrder is 0–100000. Maximum current items: 5000.

FoodType: VEG, NON_VEG, VEGAN, EGG. SpiceLevel: NONE, MILD, MEDIUM, HOT, EXTRA_HOT. Prices are nonnegative decimals with at most two fractional digits, up to 9999999999.99. Discount price cannot exceed base price. The response's sellingPrice is discountPrice if present, otherwise basePrice. Currency/tax/payment calculation belongs to later commerce modules; no exchange conversion occurs here.

Each option is `{ "id": null, "name": "Family", "price": 300.00, "active": true }`. Each list supports up to 50 options, names up to 80 characters, unique ignoring case within that list/item. New options have null/omitted IDs. PUT includes existing IDs to preserve them; omitted old options are archived. Empty lists archive all options of that kind. Cross-item IDs, duplicate IDs and duplicate names are rejected before any option is changed. Archived options cannot be resurrected by reusing an old ID. Variant prices replace the item's base/discount price; addon prices add per unit. This defines future order pricing, not an order endpoint.

The response distinguishes stored `available` from `effectiveAvailable`. Effective availability also requires an active current category and an effective restaurant service: RESTAURANT, or TAKEAWAY with the item flag, or DINITICKET with the item flag. Item flags cannot activate a disabled service. The list `available` query filters the stored value. Search matches a literal case-insensitive name substring; `%` and `_` are escaped, not user wildcards. Options are loaded in page batches. Public customer menu browsing belongs to Phase 5.

## Public availability

`GET /api/v1/restaurants/{restaurantId}/availability?date=2026-09-13&time=12:00&guests=2`

No bearer token is required. Supply date in the **restaurant's timezone** and guests 1–500. Time is optional; omit it to list availableTimeSlots for the date. Supply time to obtain availableTables for that exact slot. An unknown, unapproved, inactive or service-disabled restaurant returns 200 with available=false and no private owner/configuration fields. Responses use Cache-Control: no-store. The endpoint allows 60 requests per minute per client address across all restaurant IDs using persistent shared counters. Forwarded client IP headers are not trusted automatically; configure edge limits for reverse-proxy deployments.

Response data fields:

| Field | Meaning |
|---|---|
| restaurantId, date, requestedTime, guests | Requested selection |
| timezone | Restaurant timezone when usable configuration was loaded; otherwise null |
| available | Whether a suitable table exists for the requested slot, or any slot if time omitted |
| availableTimeSlots | startsAt, diningEndsAt, occupiedUntil, availableTableCount |
| availableTables | Matching table IDs, number/name, capacity/min/max, floor/section/shape/layout; empty for date-only requests |
| reservationDuration | Dining duration in minutes; nullable when configuration is unavailable |
| gracePeriodMinutes | Additional protected time after dining; nullable when unavailable |
| reason, message | Machine-readable result plus explanatory message |
| checkedAt | Server instant of the snapshot |

Rules, applied on the server:

1. Approval must be APPROVED and restaurant status ACTIVE; RESTAURANT service must be requested, authorized and enabled. Reservation settings and timezone must exist and reservations must be enabled.
2. Date must be today through today + advanceBookingDays, inclusive. Guest count must also satisfy the restaurant limits.
3. Weekly hours and date exceptions determine service windows. Overnight windows may carry into the next regular day. A special schedule replaces the entire local date, including carry-in. A next-day special schedule clips the previous overnight window at local midnight; a dated closure therefore cannot be crossed by a booking.
4. Slots follow slotDurationMinutes from opening, measured in elapsed minutes. Start must satisfy minimumAdvanceMinutes. Dining duration **plus grace** must fit before closing. Grace is a protected buffer after dining, not permission to start outside hours.
5. Active AVAILABLE tables must satisfy capacity/minimum/maximum guests. Active timed allocations exclude overlaps with the complete `[startsAt, occupiedUntil)` interval. Exact endpoint adjacency is allowed; other tables are unaffected.
6. An optional time must match an offered slot exactly. Non-grid times and nonexistent daylight-saving times are unavailable. Repeated local times require an explicit offset, such as `offset=-04:00`; encode a positive plus sign as `%2B`. Use the offset from a returned slot. Durations remain real elapsed minutes through clock changes.

Returned slot timestamps include offsets. Weekly opening in a clock gap follows Java's forward-gap adjustment; ambiguous opening uses the earlier offset and closing uses the later offset. These choices preserve a continuous elapsed service window. Java exposes whether a local time has zero, one or two valid offsets. [Java 17 ZoneRules](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/zone/ZoneRules.html)

Reasons: AVAILABLE, RESTAURANT_UNAVAILABLE, RESERVATIONS_DISABLED, OUTSIDE_BOOKING_WINDOW, INVALID_GUEST_COUNT, CLOSED, NO_TABLE_CAPACITY, NO_AVAILABLE_SLOT, AMBIGUOUS_LOCAL_TIME. Availability=false is a normal 200 business result; malformed date/time/offset/guest input returns 400. The result is an advisory snapshot and **never creates a hold or reservation**.

## Allocation foundation for Phase 6

There is no HTTP booking/allocation creation endpoint in Phase 4. `TableAllocationService.claimReservation` is an internal helper requiring an existing transaction. The future reservation service must first authenticate and verify booking ownership/eligibility, then create its reservation and allocation within the same transaction. Actor IDs or reference UUIDs alone are not authorization. This helper is not safe as a directly exposed controller.

The helper locks restaurant then table, rechecks server availability, derives duration/grace, inserts the allocation and audits it. Repeating the same active restaurant/table/reference/start/guest tuple returns the same allocation. A differing tuple conflicts. Release requires the matching restaurant, allocation ID and reservation reference, is idempotent and audited once. A failure in the surrounding booking transaction rolls back allocation and audit together. Claims do not increment the inventory configuration version.

PostgreSQL additionally enforces non-overlap using a GiST exclusion constraint on `(table_id, occupied_period)` for active allocations. This covers overlapping concurrent writers even if they do not use the helper. Half-open ranges allow one booking to start exactly when the prior protected interval ends. [PostgreSQL 17 range constraints](https://www.postgresql.org/docs/17/rangetypes.html)

The allocation reference UUID has no reservation FK yet because the reservation entity belongs to Phase 6. The database already rejects table/restaurant mismatches through a composite FK. RESERVATION, DINITICKET and MANUAL_BLOCK are schema types; only the internal reservation claim helper is provided now. Future booking cancellation/no-show/expiry flows must explicitly release their allocations. Reading availability does not expire, release or mutate allocation rows.

## Errors and examples

| HTTP | Error | Action |
|---|---|---|
| 400 | VALIDATION_FAILED | Correct field ranges, enums, payload or option lists |
| 401 | AUTH_TOKEN_INVALID | Sign in/refresh bearer token for owner operations |
| 403 | ACCESS_DENIED | Required role/permission absent |
| 404 | RESOURCE_NOT_FOUND | Verify the resource belongs to the selected owned restaurant/item |
| 409 | STALE_RESTAURANT_VERSION | Refetch parent, reconcile and retry |
| 409 | RESTAURANT_STATE_INVALID | Wait for review or resolve suspension |
| 409 | TABLE_NUMBER_EXISTS / MENU_NAME_EXISTS | Use another current name/number |
| 409 | TABLE_IN_USE | Resolve active current/future bookings before changing table configuration |
| 409 | MENU_CATEGORY_NOT_EMPTY | Move/archive current items first |
| 409 | INVENTORY_LIMIT_REACHED | Archive unused current inventory |
| 409 | TABLE_NOT_AVAILABLE / CONFLICT | Recheck selection; a concurrent allocation can win |
| 429 | RATE_LIMITED | Wait and reduce availability polling |

See [PHASE4_REQUEST_EXAMPLES.json](PHASE4_REQUEST_EXAMPLES.json) for complete bodies and `postman/BookEat_Master.postman_collection.json` folders 04–06. New scripts save restaurantVersion and created table/category/item/option IDs. Set availabilityDate explicitly when testing another timezone; the convenience default is tomorrow in UTC. Run optional archive requests only after testing your flow.
