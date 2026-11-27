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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/owner/restaurants/{restaurantId}/orders") @PreAuthorize("hasRole('OWNER') and hasAuthority('ORDER_MANAGE')") @SecurityRequirement(name="bearerAuth") @Tag(name="Owner food orders")
public class OwnerOrderController {
    private final OrderService service;
    @GetMapping @Operation(summary="List orders of owned restaurant")
    public ApiResponse<PageResponse<OrderResponses.Order>> list(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Orders loaded",service.ownerList(p.userId(),restaurantId,page,size));}
    @GetMapping("/{id}") @Operation(summary="Get order of owned restaurant")
    public ApiResponse<OrderResponses.Order> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id){return ApiResponse.ok("Order loaded",service.ownerGet(p.userId(),restaurantId,id));}
    @PutMapping("/{id}/accept") @Operation(summary="Accept food order",description="Owner permission, order version and reason required; transition and dining context checked. Takeaway orders use their dedicated routes.")
    public ApiResponse<OrderResponses.Order> accept(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Order updated",service.ownerAction(p.userId(),restaurantId,id,q,FoodOrderStatus.ACCEPTED));}
    @PutMapping("/{id}/prepare") @Operation(summary="Prepare food order",description="Owner permission, order version and reason required; transition and dining context checked. Takeaway orders use their dedicated routes.")
    public ApiResponse<OrderResponses.Order> prepare(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Order updated",service.ownerAction(p.userId(),restaurantId,id,q,FoodOrderStatus.PREPARING));}
    @PutMapping("/{id}/ready") @Operation(summary="Ready food order",description="Owner permission, order version and reason required; transition and dining context checked. Takeaway orders use their dedicated routes.")
    public ApiResponse<OrderResponses.Order> ready(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Order updated",service.ownerAction(p.userId(),restaurantId,id,q,FoodOrderStatus.READY));}
    @PutMapping("/{id}/serve") @Operation(summary="Serve food order",description="Owner permission, order version and reason required; transition and dining context checked. Takeaway orders use their dedicated routes.")
    public ApiResponse<OrderResponses.Order> serve(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Order updated",service.ownerAction(p.userId(),restaurantId,id,q,FoodOrderStatus.SERVED));}
    @PutMapping("/{id}/complete") @Operation(summary="Complete food order",description="Owner permission, order version and reason required; transition and dining context checked. Takeaway orders use their dedicated routes.")
    public ApiResponse<OrderResponses.Order> complete(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Order updated",service.ownerAction(p.userId(),restaurantId,id,q,FoodOrderStatus.COMPLETED));}
    @PutMapping("/{id}/reject") @Operation(summary="Reject food order",description="Owner permission, order version and reason required; transition and dining context checked. Takeaway orders use their dedicated routes.")
    public ApiResponse<OrderResponses.Order> reject(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Order updated",service.ownerAction(p.userId(),restaurantId,id,q,FoodOrderStatus.REJECTED));}
}
