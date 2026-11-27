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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/owner/restaurants/{restaurantId}/takeaway/orders") @PreAuthorize("hasRole('OWNER') and hasAuthority('ORDER_MANAGE')") @SecurityRequirement(name="bearerAuth") @Tag(name="Owner takeaway")
public class OwnerTakeawayController {
    private final TakeawayService service;
    @GetMapping @Operation(summary="List owned restaurant takeaway orders; pickup secrets omitted")
    public ApiResponse<PageResponse<OrderResponses.Takeaway>> list(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Takeaways loaded",service.ownerList(p.userId(),restaurantId,page,size));}
    @GetMapping("/{id}") @Operation(summary="Get owned restaurant takeaway")
    public ApiResponse<OrderResponses.Takeaway> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id){return ApiResponse.ok("Takeaway loaded",service.ownerGet(p.userId(),restaurantId,id));}
    @PutMapping("/{id}/collected") @Operation(summary="Verify customer pickup code and mark collected",description="Requires takeaway version and exact customer pickup code. Requires confirmed full payment; completes linked food order and erases location.")
    public ApiResponse<OrderResponses.Takeaway> collect(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Collect q){return ApiResponse.ok("Takeaway collected",service.collect(p.userId(),restaurantId,id,q));}
    @PutMapping("/{id}/collect-by-order-id") @Operation(summary="Verify entered order ID and mark takeaway collected",description="Owner-only live-data handover. The entered order ID must exactly match the takeaway ID and the order must satisfy normal completion/payment rules.")
    public ApiResponse<OrderResponses.Takeaway> collectByOrderId(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.CollectByOrderId q){return ApiResponse.ok("Order ID verified and takeaway collected successfully",service.collectByOrderId(p.userId(),restaurantId,id,q));}
    @PutMapping("/{id}/accept") @Operation(summary="Accept takeaway",description="Requires takeaway version and reason; keeps food order state synchronized.")
    public ApiResponse<OrderResponses.Takeaway> accept(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Takeaway updated",service.ownerAction(p.userId(),restaurantId,id,q,FoodOrderStatus.ACCEPTED));}
    @PutMapping("/{id}/preparing") @Operation(summary="Preparing takeaway",description="Requires takeaway version and reason; keeps food order state synchronized.")
    public ApiResponse<OrderResponses.Takeaway> preparing(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Takeaway updated",service.ownerAction(p.userId(),restaurantId,id,q,FoodOrderStatus.PREPARING));}
    @PutMapping("/{id}/ready") @Operation(summary="Ready takeaway",description="Requires takeaway version and reason; keeps food order state synchronized.")
    public ApiResponse<OrderResponses.Takeaway> ready(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Takeaway updated",service.ownerAction(p.userId(),restaurantId,id,q,FoodOrderStatus.READY));}
    @PutMapping("/{id}/reject") @Operation(summary="Reject takeaway",description="Requires takeaway version and reason; keeps food order state synchronized.")
    public ApiResponse<OrderResponses.Takeaway> reject(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody OrderRequests.Action q){return ApiResponse.ok("Takeaway updated",service.ownerAction(p.userId(),restaurantId,id,q,FoodOrderStatus.REJECTED));}
}
