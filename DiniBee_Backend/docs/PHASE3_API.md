# Phase 3 — Owner onboarding and admin approval

Base path: `/api/v1`. All routes use the existing Bearer JWT and `ApiResponse` envelope. The implementation is cumulative; authentication URLs and V1–V3 migrations are unchanged. Full build and database verification are **NOT EXECUTED** here; see [VERIFICATION.md](VERIFICATION.md).

## Run the owner-to-admin flow

1. Register/login an OWNER with the existing auth endpoints. Create a restaurant with `POST /owner/restaurants`.
2. Keep `data.id` and **the latest `data.version`** from every detail response. Every edit, upload and review decision requires that restaurant version. For a 409 `STALE_RESTAURANT_VERSION`, reload the detail and let the user review newer data before retrying.
3. Save details, location, business registration, seven operating days, services, applicable service settings, table plan, menu plan, settlement profile, policies and team plan.
4. Upload the required documents. The default policy requires FSSAI, BUSINESS_REGISTRATION, OWNER_ID and PAN; GST is additionally required when a GST number is supplied. `requiredDocuments` in progress is authoritative. This is configurable product policy, not a statement of regulatory requirements.
5. Read onboarding progress. `complete=true` means every applicable onboarding step is ready for **submission**. It does not mean admin approval or live table/menu inventory exists.
6. Submit. The application becomes `SUBMITTED`; owner edits are locked during review.
7. An authorized admin marks it `UNDER_REVIEW`, inspects/downloads evidence, verifies each required document, and records settlement verification after external account checks. This release does not call a banking verification provider.
8. Approve an explicit nonempty subset of requested services. This changes approval to `APPROVED`, status to `ACTIVE`, and enables only that subset.
9. Owner Flutter reads `GET /owner/dashboard-context?restaurantId=...` and uses `enabledServices` to show available modules. Live tables/menu and transaction APIs belong to later phases.

Use `postman/BookEat_Master.postman_collection.json` with the local environment. Run requests individually in this sequence: deletion, rejection, change requests and suspension are alternative flows, not steps to run immediately after every successful approval. Document uploads require selecting a local file. The collection has four required-document examples; for GST/OTHER use the same endpoint and change `documentType`/`documentNumber`.

## Version and ownership rules

An owner’s restaurant ID is always resolved against the authenticated user. The same 404 is returned for an absent restaurant and another owner’s restaurant. Client input never supplies owner ID, approval state, verifier, completion flags or activation grants. Unknown JSON fields are rejected.

Each mutation locks the restaurant row, checks the supplied version, changes children and audit/progress rows transactionally, then increments the parent version. This protects edits across different onboarding screens as well as concurrent admin decisions. Child row versions are internal; send the parent restaurant version. PUT replaces the complete named step, including nullable fields; hours/schedules/team lists replace their previous lists.

OWNER plus the indicated permission is required. Manager/staff assignment is not implemented in this phase, so manager-only accounts are denied onboarding access. Admin routes additionally require an admin role in the existing security filter. An administrator cannot review their own restaurant.

## Owner endpoint reference

`{id}` is the restaurant UUID. All routes below start with `/owner/restaurants` unless stated otherwise. Requests are typed records in `RestaurantRequests.java`; complete sample payloads for each form are in [PHASE3_REQUEST_EXAMPLES.json](PHASE3_REQUEST_EXAMPLES.json). The sample `version: 0` is illustrative; always replace it with the latest version.

| Method | Path | Request / result |
|---|---|---|
| POST | `/owner/restaurants` | `CreateRestaurant` → Detail, 201; requires RESTAURANT_CREATE |
| GET | `/owner/restaurants?page=0&size=20` | PageResponse of Summary |
| GET | `/{id}` | Complete Detail with current version |
| PUT | `/{id}` | Details |
| PUT | `/{id}/location` | Location |
| PUT | `/{id}/business-info` | BusinessInfo; changed registrations invalidate verified evidence/settlement |
| PUT | `/{id}/operating-hours` | Hours: exactly seven distinct days |
| PUT | `/{id}/special-schedules` | SpecialSchedules: replaces up to 365 dated exceptions |
| PUT | `/{id}/services` | ServiceSelection: nonempty services set |
| PUT | `/{id}/table-setup` | TableSetup: planned table/seat counts |
| PUT | `/{id}/reservation-settings` | ReservationSettings |
| PUT | `/{id}/takeaway-settings` | TakeawaySettings |
| PUT | `/{id}/diniticket-settings` | DiniSettings |
| PUT | `/{id}/menu-setup` | MenuSetup |
| PUT | `/{id}/settlement-profile` | SettlementProfile: bank number is write-only in the API contract |
| PUT | `/{id}/policies` | Policies |
| PUT | `/{id}/team-access` | TeamAccess: planned access, not invitations or live roles |
| POST | `/{id}/documents` | Multipart: JSON `metadata` and binary `file`; returns updated Detail |
| GET | `/{id}/documents` | Current safe document metadata |
| GET | `/{id}/documents/{documentId}/download` | Authorized binary attachment |
| DELETE | `/{id}/documents/{documentId}?version=...` | Soft-delete current evidence; returns updated Detail |
| GET | `/{id}/onboarding-progress` | Calculated Progress; no caller-supplied completion flags |
| POST | `/{id}/submit` | `{"version": ...}`; returns submitted Detail |
| GET | `/owner/dashboard-context?restaurantId=...` | Owner, selected restaurant, approval/status, permissions, enabledServices, progress |

All owner operations except creation require `RESTAURANT_OWN_MANAGE`. Creation requires `RESTAURANT_CREATE`. Omit dashboard `restaurantId` to select the oldest owned restaurant, with UUID as a stable tie-breaker. An owner with no restaurants receives empty `enabledServices`, `onboardingComplete=false`, and no selected restaurant/progress. There are no fabricated analytics.

## Validation and completion

| Step | Rules |
|---|---|
| Details | Name 1–160 characters, description needed for completion; valid email, E.164 phone; optional HTTPS logo/cover URLs. URLs are stored only, never fetched by the backend. |
| Location | Address, city, state, postal code, uppercase two-letter country, latitude −90…90, longitude −180…180 and valid IANA timezone required. |
| Business | Business registration required; FSSAI exactly 14 digits. Optional GST uses a structured 15-character format. Required evidence numbers must match saved FSSAI/GST/business registration. |
| Hours | Exactly seven unique days; at least one open day for completion. Closed rows have null times and `closesNextDay=false`. Same-day closing must follow opening. Overnight closing must be at or before opening with `closesNextDay=true`; equal times then represent 24 hours. |
| Tables | Required when RESTAURANT or DINITICKET is selected. 1–1000 planned tables; seats between table count and 30 × table count. Actual table inventory is Phase 4. |
| Reservations | Required/enabled when RESTAURANT is selected. Guests 1–500, minimum ≤ maximum; slot 5–240 minutes; duration 5–720 and ≥ slot; booking horizon 1–365 days; notice/cancellation windows within horizon; grace 0–120; GPS radius 10–1000 metres. |
| Takeaway | Required/enabled when selected. At least one takeaway mode and one payment method; nonnegative amount with two decimals; preparation 1–1440 minutes; advance window 1–10080 and ≥ preparation. Payment flags configure future order APIs, not actual gateway processing. |
| DiniTicket | Required/enabled when selected. Queue 1–1000; estimate 1–240 minutes per party; expiry 5–1440 minutes. Queue operations are Phase 8. |
| Menu | `acknowledged=true`; `CREATE_IN_MENU_MODULE` or `EXTERNAL_MENU`. The latter needs an HTTPS URL. This stores a plan/reference, not menu items. |
| Settlement | Holder, bank, account 8–34 digits, IFSC, tax name required. GST must match restaurant GST. Updates clear verification. Responses return `maskedAccountNumber` such as `****9012`; full account/ciphertext are never returned. |
| Policies | Cancellation/refund text required, each ≤3000 characters. Child/pet/dress/additional policies optional. |
| Team | `acknowledged=true`; OWNER_ONLY requires no members; DRAFT_TEAM needs 1–50 members, unique case-insensitive emails, roles limited to RESTAURANT_MANAGER/RESTAURANT_STAFF. No accounts or grants are created. |

Progress exposes all 13 completion flags, `currentStep`, `missingSteps`, `notApplicableSteps`, `requiredDocuments`, `submitted` and `complete`. Service-specific flags count as satisfied when not applicable; the separate `notApplicableSteps` list explains why. `submitted` describes the current review/live state; rejected or reopened drafts have `submitted=false` while `submittedAt` and audit history preserve prior submission.

## Documents

Upload with content type `multipart/form-data`:

- `metadata`: `application/json`, e.g. `{"version":7,"documentType":"OWNER_ID","documentNumber":"ID-1234"}`.
- `file`: PDF, PNG or JPEG, 1 byte to 5 MiB; the server checks the byte signature, not just the declared MIME type. Maximum request size is 6 MB.

Each non-OTHER type has at most one current document. Re-uploading replaces it and resets verification to PENDING; the prior metadata/file remain retained. OTHER allows multiple files, with at most 20 current documents overall. Identifiers are masked in responses. Owner/admin `fileUrl` values point to their respective authenticated download endpoints. No public bucket URLs, file keys or arbitrary remote URL uploads are exposed.

A download verifies the stored SHA-256 and returns `application/octet-stream`, attachment disposition, `nosniff` and `Cache-Control: no-store`. Deleted/superseded documents are not downloadable by these endpoints. Old evidence is retained for audit; see retention notes in [PHASE3_DEPLOYMENT.md](PHASE3_DEPLOYMENT.md).

## Admin endpoint reference

All paths below start with `/admin/restaurants`. Mutations require current restaurant `version`. `Decision` is `{"version":...,"reason":"..."}`. `Verification` is `{"version":...,"verified":true|false,"reason":null|"..."}`; false requires a nonblank reason.

| Method | Path | Permission / contract |
|---|---|---|
| GET | `/admin/restaurants` | RESTAURANT_VIEW; optional approvalStatus/status/city/search and page/size |
| GET | `/pending` | RESTAURANT_VIEW; submitted and under-review applications |
| GET | `/{id}` | RESTAURANT_VIEW; full safe Detail |
| GET | `/{id}/approval-history` | RESTAURANT_VIEW; paginated decisions with actor, timestamp, statuses, reason, service and optional reference UUID |
| PUT | `/{id}/under-review` | RESTAURANT_APPROVE; VersionRequest |
| PUT | `/{id}/approve` | RESTAURANT_APPROVE; version, nonempty services set, optional reason |
| PUT | `/{id}/reject` | RESTAURANT_REJECT; Decision |
| PUT | `/{id}/request-changes` | RESTAURANT_REJECT; Decision |
| PUT | `/{id}/suspend` | RESTAURANT_SUSPEND; Decision |
| PUT | `/{id}/reactivate` | RESTAURANT_SUSPEND; Decision |
| PATCH | `/{id}/services/{serviceType}` | RESTAURANT_SERVICE_MANAGE; version, enabled, nonblank reason |
| GET | `/{id}/documents` | RESTAURANT_DOCUMENT_VERIFY |
| GET | `/{id}/documents/{documentId}/download` | RESTAURANT_DOCUMENT_VERIFY; binary |
| PUT | `/{id}/documents/{documentId}/verification` | RESTAURANT_DOCUMENT_VERIFY; Verification, under review only |
| PUT | `/{id}/settlement-profile/verification` | RESTAURANT_SETTLEMENT_VERIFY; Verification, under review only |

Pagination is zero-based, size 1–100. List ordering is newest creation first with UUID tie-breaker; history is newest event first. Search is literal case-insensitive restaurant-name matching, with SQL wildcard characters escaped.

### State transitions

| Action | Allowed current state | Result |
|---|---|---|
| Owner edits | DRAFT, CHANGES_REQUESTED, REJECTED | Same state, new version |
| Submit | DRAFT, CHANGES_REQUESTED, REJECTED | SUBMITTED / INACTIVE |
| Under review | SUBMITTED | UNDER_REVIEW / INACTIVE |
| Verify evidence/profile | UNDER_REVIEW | Same state, verification updated |
| Approve | UNDER_REVIEW | APPROVED / ACTIVE; verified readiness required |
| Reject | SUBMITTED, UNDER_REVIEW | REJECTED / INACTIVE; clears service grants |
| Request changes | SUBMITTED, UNDER_REVIEW, APPROVED | CHANGES_REQUESTED / INACTIVE; clears service grants |
| Suspend | APPROVED | SUSPENDED / SUSPENDED; effective services empty |
| Reactivate | SUSPENDED | APPROVED / ACTIVE; restores only still-requested, previously authorized services |
| Service enable/disable | APPROVED | Explicit grant updated; enable additionally needs ACTIVE and verified configuration |

Approval requires completed applicable onboarding, VERIFIED matching required documents, and a verified settlement profile. Rejected evidence blocks completion; request changes to let the owner replace it. Suspension preserves grants; explicit admin service disable clears the grant, so reactivation cannot silently undo that choice.

### Seeded role grants

| Role | Phase 3 grants |
|---|---|
| OWNER | Existing create/manage own restaurant permissions |
| SUPER_ADMIN | All restaurant review, document and settlement permissions |
| RESTAURANT_VERIFICATION_ADMIN | View, approve/under-review, reject/request-changes, document verification, service manage |
| OPERATIONS_ADMIN | View, suspend/reactivate, service manage |
| FINANCE_ADMIN | View and settlement verification; no document download or restaurant approval |
| ADMIN / SUPPORT_ADMIN / CONTENT_ADMIN | No new restaurant grants |

The first bootstrap SUPER_ADMIN can exercise the whole review flow. Subsequent admin account/role management is Phase 13; no new public role-selection endpoint is added.

## Errors

| HTTP | errorCode | Action |
|---|---|---|
| 400 | VALIDATION_FAILED / DOCUMENT_INVALID | Correct request fields or file |
| 401 | AUTH_TOKEN_INVALID | Refresh/sign in using existing auth flow |
| 403 | ACCESS_DENIED | Required role/permission absent or attempted self-review |
| 404 | RESOURCE_NOT_FOUND | Restaurant/document absent or not owned |
| 409 | STALE_RESTAURANT_VERSION | Reload detail and reconcile edits |
| 409 | RESTAURANT_STATE_INVALID | Use an allowed workflow transition |
| 409 | VERIFICATION_REQUIRED | Complete admin evidence and settlement verification |
| 409 | SERVICE_NOT_AVAILABLE | Requested/configured/active checks failed |
| 413 | DOCUMENT_TOO_LARGE | Upload within size limits |
| 422 | ONBOARDING_INCOMPLETE | Render `errors[].field` as missing onboarding step names |
| 503 | SETTLEMENT_NOT_CONFIGURED / STORAGE_UNAVAILABLE | Fix backend configuration/provider availability |

Other pre-existing ApiResponse errors remain unchanged. Request bodies, bank numbers and provider exception text are not included in error responses.
