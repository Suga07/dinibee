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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/customer") @PreAuthorize("hasRole('CUSTOMER')") @SecurityRequirement(name="bearerAuth") @Tag(name="Customer food orders")
public class CustomerOrderController {
    private final OrderService service;
    @PostMapping("/orders") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Operation(summary="Place food order with server-calculated price snapshots",description="Idempotency-Key required. Dine-in requires an eligible own/joined reservation; DiniTicket preorder requires an eligible own ticket. Use takeaway routes for pickup orders. Client totals/payment states are rejected.")
    public ApiResponse<OrderResponses.Order> create(@AuthenticationPrincipal AppPrincipal p,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody OrderRequests.Create q){return ApiResponse.ok("Order placed",service.create(p.userId(),key,q));}
    @GetMapping("/orders") @Operation(summary="List own food orders")
    public ApiResponse<PageResponse<OrderResponses.Order>> list(@AuthenticationPrincipal AppPrincipal p,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Orders loaded",service.list(p.userId(),page,size));}
    @GetMapping("/orders/{id}") @Operation(summary="Get own food order and immutable price snapshots")
    public ApiResponse<OrderResponses.Order> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Order loaded",service.get(p.userId(),id));}
    @PutMapping("/orders/{id}/cancel") @Operation(summary="Cancel a PLACED food order",description="Requires order version and reason; accepted kitchen work cannot be cancelled by the customer.")
    public ApiResponse<OrderResponses.Order> cancel(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Order cancelled",service.cancel(p.userId(),id,q));}
    @GetMapping("/group-bookings/{id}/orders") @Operation(summary="Host combined group order summary and proposed split",description="Amounts are exact decimal proposals, not payment collection. Group membership controls access.")
    public ApiResponse<OrderResponses.Combined> group(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Group orders loaded",service.groupSummary(p.userId(),id));}
    @GetMapping("/corporate-bookings/{id}/orders") @Operation(summary="Organizer combined corporate orders and committed total")
    public ApiResponse<OrderResponses.Combined> corporate(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Corporate orders loaded",service.corporateSummary(p.userId(),id));}
}
