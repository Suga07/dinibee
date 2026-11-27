package com.bookeat.order.controller;
import com.bookeat.order.service.OrderSettingsService;
import com.bookeat.common.response.*;
import com.bookeat.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.math.BigDecimal;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/owner/restaurants/{id}/commerce-policy") @PreAuthorize("hasRole('OWNER') and hasAuthority('ORDER_MANAGE')") @SecurityRequirement(name="bearerAuth")
public class OrderSettingsController {
    private final OrderSettingsService service;
    public record Policy(@NotNull @PositiveOrZero Long restaurantVersion,@NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal taxPercent) {}
    @GetMapping @Operation(summary="Get owner's order tax policy",description="INR prices exclude this percentage tax. Default 0 until configured. Existing orders retain their snapshots.")
    public ApiResponse<RestaurantResource<BigDecimal>> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Order tax policy loaded",service.get(p.userId(),id));}
    @PutMapping @Operation(summary="Set order tax percentage",description="Requires restaurantVersion. Net discounted subtotal is taxed, rounded HALF_UP to two decimals; no tax-on-tax calculation.")
    public ApiResponse<RestaurantResource<BigDecimal>> put(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Policy q){return ApiResponse.ok("Order tax policy updated",service.update(p.userId(),id,q.restaurantVersion(),q.taxPercent()));}
}
