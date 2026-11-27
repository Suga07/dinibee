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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin/restaurants")
@SecurityRequirement(name="bearerAuth") @Tag(name="Admin restaurant review")
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
public class AdminRestaurantController {
    private final AdminRestaurantService review;
    private final RestaurantDocumentService documents;
    @GetMapping @Operation(summary="Filter restaurants",description="Admin + RESTAURANT_VIEW. Optional approvalStatus, status, city and name search. Page starts at 0; size 1-100.")
    public ApiResponse<PageResponse<Summary>> list(@RequestParam(required=false) ApprovalStatus approvalStatus,@RequestParam(required=false) RestaurantStatus status,@RequestParam(required=false) @Size(max=100) String city,@RequestParam(required=false) @Size(max=160) String search,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Restaurants loaded",review.list(approvalStatus,status,city,search,false,page,size));}
    @GetMapping("/pending") @Operation(summary="List submitted and under-review applications",description="Admin + RESTAURANT_VIEW. Page starts at 0; size 1-100.")
    public ApiResponse<PageResponse<Summary>> pending(@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Pending restaurants loaded",review.list(null,null,null,null,true,page,size));}
    @GetMapping("/{id}") @Operation(summary="Get restaurant review details",description="Admin + RESTAURANT_VIEW. Returns complete configuration, progress and masked settlement/document metadata.")
    public ApiResponse<Detail> get(@PathVariable UUID id){return ApiResponse.ok("Restaurant loaded",review.get(id));}
    @GetMapping("/{id}/approval-history") @Operation(summary="List restaurant decision history",description="Admin + RESTAURANT_VIEW. Paginated audit records, newest first.")
    public ApiResponse<PageResponse<ApprovalHistoryView>> history(@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Approval history loaded",review.history(id,page,size));}
    @PutMapping("/{id}/under-review") @Operation(summary="under-review",description="Admin + RESTAURANT_APPROVE. Requires current restaurant version. Self-review is forbidden. SUBMITTED to UNDER_REVIEW.")
    public ApiResponse<Detail> underReview(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody VersionRequest q) {return ApiResponse.ok("Restaurant review updated",review.underReview(p.userId(),id,q));}
    @PutMapping("/{id}/approve") @Operation(summary="approve",description="Admin + RESTAURANT_APPROVE. Requires current restaurant version. Self-review is forbidden. UNDER_REVIEW only; all required documents and settlement must be verified. Explicit nonempty subset of requested services is activated.")
    public ApiResponse<Detail> approve(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Approve q) {return ApiResponse.ok("Restaurant review updated",review.approve(p.userId(),id,q));}
    @PutMapping("/{id}/reject") @Operation(summary="reject",description="Admin + RESTAURANT_REJECT. Requires current restaurant version. Self-review is forbidden. SUBMITTED or UNDER_REVIEW to REJECTED. Reason required.")
    public ApiResponse<Detail> reject(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Decision q) {return ApiResponse.ok("Restaurant review updated",review.reject(p.userId(),id,q));}
    @PutMapping("/{id}/request-changes") @Operation(summary="request-changes",description="Admin + RESTAURANT_REJECT. Requires current restaurant version. Self-review is forbidden. Reopens a submitted, under-review or approved application. Disables services. Reason required.")
    public ApiResponse<Detail> requestChanges(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Decision q) {return ApiResponse.ok("Restaurant review updated",review.requestChanges(p.userId(),id,q));}
    @PutMapping("/{id}/suspend") @Operation(summary="suspend",description="Admin + RESTAURANT_SUSPEND. Requires current restaurant version. Self-review is forbidden. APPROVED to SUSPENDED. Effective services become empty; authorization choices are retained. Reason required.")
    public ApiResponse<Detail> suspend(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Decision q) {return ApiResponse.ok("Restaurant review updated",review.suspend(p.userId(),id,q));}
    @PutMapping("/{id}/reactivate") @Operation(summary="reactivate",description="Admin + RESTAURANT_SUSPEND. Requires current restaurant version. Self-review is forbidden. SUSPENDED to APPROVED after readiness checks. Previously disabled service grants remain disabled. Reason required.")
    public ApiResponse<Detail> reactivate(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Decision q) {return ApiResponse.ok("Restaurant review updated",review.reactivate(p.userId(),id,q));}
    @PutMapping("/{id}/settlement-profile/verification") @Operation(summary="settlement-profile/verification",description="Admin + RESTAURANT_SETTLEMENT_VERIFY. Requires current restaurant version. Self-review is forbidden. UNDER_REVIEW only. Record verified=true after external account validation. Rejections require a reason; the API never returns full account numbers.")
    public ApiResponse<Detail> verifySettlement(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Verification q) {return ApiResponse.ok("Restaurant review updated",review.verifySettlement(p.userId(),id,q));}
    @PatchMapping("/{id}/services/{serviceType}") @Operation(summary="Enable or disable a requested service",description="Admin + RESTAURANT_SERVICE_MANAGE. Requires current version and reason. Enable requires APPROVED + ACTIVE, verified readiness and matching configuration.")
    public ApiResponse<Detail> service(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@PathVariable ServiceType serviceType,@Valid @RequestBody ServiceDecision q){return ApiResponse.ok("Service activation updated",review.service(p.userId(),id,serviceType,q));}
    @GetMapping("/{id}/documents") @Operation(summary="List private review documents",description="Admin + RESTAURANT_DOCUMENT_VERIFY. Lists current metadata and authenticated download URLs.")
    public ApiResponse<List<DocumentView>> documents(@PathVariable UUID id){return ApiResponse.ok("Documents loaded",documents.listAdmin(id));}
    @GetMapping("/{id}/documents/{documentId}/download") @Operation(summary="Download a private document for review",description="Admin + RESTAURANT_DOCUMENT_VERIFY. Returns a binary attachment; private no-store response.")
    public ResponseEntity<byte[]> download(@PathVariable UUID id,@PathVariable UUID documentId){return DocumentDownloadResponse.of(documents.downloadAdmin(id,documentId));}
    @PutMapping("/{id}/documents/{documentId}/verification") @Operation(summary="Verify or reject a document",description="Admin + RESTAURANT_DOCUMENT_VERIFY. UNDER_REVIEW only; current restaurant version required. Rejection requires reason. Self-review forbidden.")
    public ApiResponse<Detail> verify(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@PathVariable UUID documentId,@Valid @RequestBody Verification q){return ApiResponse.ok("Document verification recorded",documents.verify(p.userId(),id,documentId,q));}
}
