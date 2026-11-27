package com.bookeat.order.controller;
import com.bookeat.order.service.*;
import com.bookeat.order.dto.*;
import com.bookeat.order.enums.*;
import com.bookeat.common.response.*;
import com.bookeat.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/customer/takeaway/orders") @PreAuthorize("hasRole('CUSTOMER')") @SecurityRequirement(name="bearerAuth") @Tag(name="Customer takeaway")
public class CustomerTakeawayController {
    private final TakeawayService service;
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Operation(summary="Create regular or on-the-way takeaway",description="Idempotency-Key required. Enabled TAKEAWAY service, pickup window, an eligible payment mode and menu flags required. Complete server-verified payment before collection.")
    public ApiResponse<OrderResponses.Takeaway> create(@AuthenticationPrincipal AppPrincipal p,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody OrderRequests.Takeaway q){return ApiResponse.ok("Takeaway placed",service.create(p.userId(),key,q));}
    @GetMapping @Operation(summary="List own takeaway orders")
    public ApiResponse<PageResponse<OrderResponses.Takeaway>> list(@AuthenticationPrincipal AppPrincipal p,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Takeaway orders loaded",service.list(p.userId(),page,size));}
    @GetMapping("/{id}") @Operation(summary="Get own takeaway and private pickup code")
    public ApiResponse<OrderResponses.Takeaway> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Takeaway loaded",service.get(p.userId(),id));}
    @PutMapping("/{id}/cancel") @Operation(summary="Cancel own PLACED takeaway")
    public ApiResponse<OrderResponses.Takeaway> cancel(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Takeaway cancelled",service.cancel(p.userId(),id,q));}
    @PutMapping("/{id}/location") @Operation(summary="Share on-the-way location with explicit consent",description="consent=true required. Latest point only; straight-line distance with a disclosed 30 km/h estimate, no routing or traffic provider. Customer must grant device permission before sending coordinates.")
    public ApiResponse<OrderResponses.Tracking> location(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Location q){return ApiResponse.ok("Location updated",service.location(p.userId(),id,q));}
    @DeleteMapping("/{id}/location") @Operation(summary="Revoke consent and erase latest location")
    public ApiResponse<Void> stop(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){service.stopLocation(p.userId(),id);return ApiResponse.ok("Location erased",null);}
}
