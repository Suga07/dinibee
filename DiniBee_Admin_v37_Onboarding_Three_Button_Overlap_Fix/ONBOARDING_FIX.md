# Restaurant onboarding persistence fix — 21 September 2026

## Finding
The supplied CSV contains four restaurants with basic details but no address, coordinates, business registration, GST or FSSAI. All four have no submitted_at timestamp, including records moved to review/approval. This is consistent with partially saved drafts, not a successful complete onboarding submission.

The owner model constructor leaves latitude/longitude null, and the location screen had no inputs for them. saveAllAndSubmit saved basic details first, then toLocationRequest threw before the location HTTP call. Later requests also used unsupported enums: MANUAL instead of CREATE_IN_MENU_MODULE, TEAM instead of DRAFT_TEAM, and MANAGER/WAITER rather than the backend team roles. Compliance document buttons assigned hard-coded filenames without selecting or uploading a file. Selected logo/cover files were never uploaded and their request URLs were always null. Several additional fields were omitted from all request DTOs.

The admin UI independently read a nested location/businessInfo object and operatingHours.hours although Spring returns flat address/business fields and an operatingHours array. It also displayed DRAFT as SUBMITTED. Backend readiness and verification checks had been bypassed, allowing incomplete records to advance.

## Changes
- Validate before restaurant creation; collect actual map coordinates and time zone. Retain location inputs when returning to that step.
- Correct menu, team mode and team role enum mappings; support overnight hours; remove invented registration/policy/description fallbacks.
- Save normalized sections sequentially with each returned version. Check response identity/version/status and require SUBMITTED before showing success. Failed sections preserve the draft and show field-level errors.
- Reuse the owner's single editable restaurant when restarting a submission, instead of creating a duplicate. Multiple restaurants require explicitly selecting a restaurant ID in the integrating app; this patch deliberately does not guess between them.
- Add PUT /api/v1/owner/restaurants/{id}/onboarding-form and Flyway V25. Typed, validated JSON in restaurants.onboarding_form retains website, cuisine, legal business name, PAN, landmark, extra preferences, complete table/menu plans and staff details/permissions. It is returned in authenticated owner/admin Detail responses.
- Select real files and upload bytes as multipart metadata/file parts, with a 5 MB limit. LOGO and COVER_IMAGE use existing protected document storage and populate logo_url/cover_image_url. Registration and FSSAI files use their existing document types. Empty details updates no longer erase uploaded branding; document deletion clears its corresponding URL.
- Normalize the actual Detail response in admin; show requested services and uploaded documents, preserve DRAFT status, and show detail-load errors. Require review and readiness before approval; restore configured document/settlement verification checks. Approve only the requested services.

## Database field locations
| Information | Persistence |
|---|---|
| Restaurant name, description, phone, email, type | restaurants existing columns |
| Address, city/state, postal code, country, coordinates, time zone | restaurants existing location columns |
| Business registration, GST, FSSAI | restaurants existing business columns |
| Website, cuisine, legal name, PAN, landmark and extra settings | restaurants.onboarding_form JSONB |
| Table identities/capacities, full menu items/prices, staff names/contact/permissions | restaurants.onboarding_form typed onboarding plans |
| Hours, requested services, reservation/takeaway/Dini settings, policies | existing related restaurant configuration tables |
| Bank details | existing settlement profile; account number stays encrypted and is returned masked |
| Uploaded files | existing restaurant_documents metadata plus protected file storage |
| Logo/cover links | restaurants.logo_url / cover_image_url, referencing protected downloads |

Table/menu/staff entries are preserved as onboarding plans; this patch does not publish live menu inventory or grant staff permissions from that JSON. Branding downloads remain protected owner/admin assets, not public image URLs. Passwords/tokens are not stored in the restaurant form. Blank optional fields and workflow fields such as approved_at on a draft legitimately remain null.

## Apply and verify
1. Keep your existing environment configuration. Apply the source changes from the three corrected projects (each includes a focused patch for review).
2. Deploy the backend first and let Flyway apply V25. Do not edit old migrations or fill missing columns with placeholder strings. Keep settlement encryption/storage configuration intact. The new Flutter client requires this updated backend.
3. Rebuild the owner app and admin panel; do not reuse stale build output. Run flutter pub get, flutter analyze, flutter test test/onboarding_persistence_test.dart; run mvn test and mvn -Pintegration verify with Docker/PostgreSQL; run the admin project's normal build/test commands.
4. Submit a test restaurant with distinctive values in every form field, non-default coordinates, overnight hours, at least one table/menu/staff entry, and real PNG/PDF uploads. A successful result must return approvalStatus SUBMITTED and submittedAt. Reload GET /api/v1/owner/restaurants/{id} and the admin detail view and compare the saved data, not just the submit response.
5. Test a failed location request and retry. No submission success or approval should occur while required steps are incomplete. Verify that retry reuses the same restaurant and does not duplicate acknowledged document uploads.

## Existing data recovery
No live database was connected or modified. Already missing values cannot be inferred from this CSV. Owners must re-enter those values from their original information and resubmit the same record. DRAFT, REJECTED and CHANGES_REQUESTED records are editable. For incomplete SUBMITTED, UNDER_REVIEW or APPROVED records, an administrator can use PUT /api/v1/admin/restaurants/{id}/request-changes with the latest version and a reason, then the owner resubmits. This preserves the review history and disables services through the existing workflow. Never manually set approval/submitted timestamps to disguise missing onboarding data.

For this upload, the latest restaurant is still DRAFT despite the old admin screen showing it as submitted. The other three rows include REJECTED, UNDER_REVIEW and APPROVED statuses; review those records before making them active.

The app retains draft form state while running. Reopening a previously partial form can reuse the backend restaurant ID during submission, but this change does not add full automatic form hydration after process termination. Re-enter incomplete sections and reselect unsent files. With multiple restaurants, supply an explicitly selected restaurant ID/version to the onboarding provider.

## Verification performed here
- 3 executable Node tests passed for flat DTO mapping, requested services/documents, legitimate missing values and failed detail responses.
- 316 executable assertions passed against the real Java workflow, hours, settlement cipher and document-content helper classes.
- Java 17 syntax parsing passed for all 425 application/test source files; dependency type checking was not performed.
- The modified admin TypeScript service passed Node's TypeScript syntax check.
- All 21 supplemental form fields have corresponding outgoing owner mappings.
- Added six Flutter submission regression tests and three PostgreSQL/Spring integration tests for reload persistence, draft gating and branding uploads.

Flutter/Dart, Maven, Docker/PostgreSQL and frontend build dependencies are unavailable in this workspace, so the new Flutter tests, Spring integration suite and complete frontend builds were NOT executed. The full application/database flow still needs the deployment checks above.
