package com.bookeat.restaurant.controller;
import com.bookeat.restaurant.dto.RestaurantRequests.*;
import com.bookeat.restaurant.dto.RestaurantResponses.*;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.service.*;
import com.bookeat.common.response.ApiResponse;
import com.bookeat.common.response.PageResponse;
import com.bookeat.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/owner/restaurants")
@SecurityRequirement(name="bearerAuth") @Tag(name="Owner onboarding")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="Invalid input; inspect ApiResponse.errors"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="Bearer token absent or expired"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="Required role or permission is missing"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="Restaurant/document absent or not owned"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="Stale version, invalid state, or required verification missing"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="413",description="Document exceeds the upload size limit"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="422",description="Required onboarding steps incomplete"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503",description="Storage or settlement encryption unavailable")
})
public class OwnerRestaurantController {
    private final OwnerOnboardingService onboarding;
    private final RestaurantDocumentService documents;
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Create a draft restaurant",description="OWNER + RESTAURANT_CREATE. No approval or service activation is accepted from the client.")
    public ApiResponse<Detail> create(@AuthenticationPrincipal AppPrincipal p,@Valid @RequestBody CreateRestaurant q) {
        return ApiResponse.ok("Draft restaurant created",onboarding.create(p.userId(),q));
    }
    @GetMapping @Operation(summary="List owned restaurants",description="OWNER + RESTAURANT_OWN_MANAGE. Page starts at 0; size 1-100. Ordered newest first.")
    public ApiResponse<PageResponse<Summary>> list(@AuthenticationPrincipal AppPrincipal p,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok("Restaurants loaded",onboarding.list(p.userId(),page,size));
    }
    @GetMapping("/{id}") @Operation(summary="Load complete restaurant onboarding",description="OWNER + RESTAURANT_OWN_MANAGE. Other owners receive 404. Settlement account is masked.")
    public ApiResponse<Detail> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Restaurant loaded",onboarding.get(p.userId(),id));}
    @GetMapping("/{id}/onboarding-progress") @Operation(summary="Calculate onboarding progress",description="OWNER + RESTAURANT_OWN_MANAGE. Missing and non-applicable steps are calculated by the backend.")
    public ApiResponse<Progress> progress(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Onboarding progress loaded",onboarding.progress(p.userId(),id));}
    @PutMapping("/{id}") @Operation(summary="Save restaurant details",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step.")
    public ApiResponse<Detail> updateDetails(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Details q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateDetails(p.userId(),id,q));}
    @PutMapping("/{id}/location") @Operation(summary="Save location",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step.")
    public ApiResponse<Detail> updateLocation(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Location q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateLocation(p.userId(),id,q));}
    @PutMapping("/{id}/business-info") @Operation(summary="Save business-info",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step.")
    public ApiResponse<Detail> updateBusinessInfo(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody BusinessInfo q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateBusinessInfo(p.userId(),id,q));}
    @PutMapping("/{id}/operating-hours") @Operation(summary="Save operating-hours",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step.")
    public ApiResponse<Detail> updateHours(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Hours q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateHours(p.userId(),id,q));}
    @PutMapping("/{id}/special-schedules") @Operation(summary="Save special-schedules",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step.")
    public ApiResponse<Detail> updateSpecialSchedules(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody SpecialSchedules q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateSpecialSchedules(p.userId(),id,q));}
    @PutMapping("/{id}/services") @Operation(summary="Save services",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step. Selects requested services only; admin approval is required for activation.")
    public ApiResponse<Detail> selectServices(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody ServiceSelection q) {return ApiResponse.ok("Onboarding step saved",onboarding.selectServices(p.userId(),id,q));}
    @PutMapping("/{id}/reservation-settings") @Operation(summary="Save reservation-settings",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step.")
    public ApiResponse<Detail> updateReservationSettings(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody ReservationSettings q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateReservationSettings(p.userId(),id,q));}
    @PutMapping("/{id}/takeaway-settings") @Operation(summary="Save takeaway-settings",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step.")
    public ApiResponse<Detail> updateTakeawaySettings(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody TakeawaySettings q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateTakeawaySettings(p.userId(),id,q));}
    @PutMapping("/{id}/diniticket-settings") @Operation(summary="Save diniticket-settings",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step.")
    public ApiResponse<Detail> updateDiniSettings(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody DiniSettings q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateDiniSettings(p.userId(),id,q));}
    @PutMapping("/{id}/policies") @Operation(summary="Save policies",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step.")
    public ApiResponse<Detail> updatePolicies(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Policies q) {return ApiResponse.ok("Onboarding step saved",onboarding.updatePolicies(p.userId(),id,q));}
    @PutMapping("/{id}/settlement-profile") @Operation(summary="Save settlement-profile",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step.")
    public ApiResponse<Detail> updateSettlementProfile(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody SettlementProfile q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateSettlementProfile(p.userId(),id,q));}
    @PutMapping("/{id}/table-setup") @Operation(summary="Save table-setup",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step. Saves table/seat planning; live tables arrive in Phase 4.")
    public ApiResponse<Detail> updateTableSetup(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody TableSetup q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateTableSetup(p.userId(),id,q));}
    @PutMapping("/{id}/menu-setup") @Operation(summary="Save menu-setup",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step. Acknowledges the menu setup plan; live menu items arrive in Phase 4.")
    public ApiResponse<Detail> updateMenuSetup(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody MenuSetup q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateMenuSetup(p.userId(),id,q));}
    @PutMapping("/{id}/team-access") @Operation(summary="Save team-access",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current restaurant version. Editable in DRAFT, CHANGES_REQUESTED or REJECTED. PUT replaces this step. Saves planned staff access; does not grant roles or send invitations.")
    public ApiResponse<Detail> updateTeamAccess(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody TeamAccess q) {return ApiResponse.ok("Onboarding step saved",onboarding.updateTeamAccess(p.userId(),id,q));}
    @PutMapping("/{id}/onboarding-form")
    public ApiResponse<Detail> updateForm(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Form q) {
        return ApiResponse.ok("Onboarding form saved",onboarding.updateForm(p.userId(),id,q));
    }
    @PostMapping("/{id}/submit") @Operation(summary="Submit restaurant for review",description="OWNER + RESTAURANT_OWN_MANAGE. Requires current version and complete applicable steps. Returns 422 with missing steps. Review documents may be pending.")
    public ApiResponse<Detail> submit(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody VersionRequest q){return ApiResponse.ok("Restaurant submitted",onboarding.submit(p.userId(),id,q));}
    @PostMapping(value="/{id}/documents",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary="Upload or replace a private business document",description="OWNER + RESTAURANT_OWN_MANAGE. Parts: metadata (application/json DocumentUpload), file (PDF/PNG/JPEG <=5 MiB). Requires aggregate version. Generated private key; no URL ingestion.")
    public ApiResponse<Detail> upload(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestPart("metadata") DocumentUpload metadata,@RequestPart("file") MultipartFile file){return ApiResponse.ok("Document uploaded",documents.upload(p.userId(),id,metadata,file));}
    @GetMapping("/{id}/documents") @Operation(summary="List current document metadata",description="OWNER + RESTAURANT_OWN_MANAGE. Document numbers are masked; file URLs require bearer authentication.")
    public ApiResponse<List<DocumentView>> documents(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Documents loaded",documents.listOwner(p.userId(),id));}
    @DeleteMapping("/{id}/documents/{documentId}") @Operation(summary="Remove a document from the application",description="OWNER + RESTAURANT_OWN_MANAGE. Version query parameter required. Soft deletion preserves review history. Editing states only.")
    public ApiResponse<Detail> delete(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@PathVariable UUID documentId,@RequestParam @Min(0) long version){return ApiResponse.ok("Document removed",documents.delete(p.userId(),id,documentId,version));}
    @GetMapping("/{id}/documents/{documentId}/download") @Operation(summary="Download an owned private document",description="OWNER + RESTAURANT_OWN_MANAGE. Binary attachment, private no-store response; document must belong to this restaurant.")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@PathVariable UUID documentId){return DocumentDownloadResponse.of(documents.downloadOwner(p.userId(),id,documentId));}
}
