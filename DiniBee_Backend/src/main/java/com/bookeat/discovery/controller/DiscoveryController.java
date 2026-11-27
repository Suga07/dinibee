package com.bookeat.discovery.controller;
import com.bookeat.discovery.service.DiscoveryService;
import com.bookeat.discovery.dto.DiscoveryDtos.*;
import com.bookeat.common.response.*;
import com.bookeat.menu.dto.MenuResponses;
import com.bookeat.restaurant.enums.ServiceType;
import com.bookeat.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1") @Tag(name="Customer restaurant discovery")
public class DiscoveryController {
    private final DiscoveryService service;
    @GetMapping({"/restaurants","/restaurants/search"}) @Operation(summary="Search approved active restaurants",description="Public. Filters: restaurantName, city, latitude/longitude, radius km, cuisine, minimum rating, foodType, serviceType, openNow, offerAvailable, sort NAME/RATING/DISTANCE, page and size. Default service DINE_IN. Unrated venues have rating 0.")
    public ApiResponse<PageResponse<Card>> search(@Valid @ModelAttribute Search q){return ApiResponse.ok("Restaurants loaded",service.search(q));}
    @GetMapping("/restaurants/{id}") @Operation(summary="Get safe public restaurant details")
    public ApiResponse<PublicDetail> detail(@PathVariable UUID id,@RequestParam(required=false) ServiceType serviceType){return ApiResponse.ok("Restaurant loaded",service.detail(id,serviceType));}
    @GetMapping("/restaurants/{id}/menu") @Operation(summary="Get available public menu for an enabled service")
    public ApiResponse<PageResponse<MenuResponses.Item>> menu(@PathVariable UUID id,@RequestParam(required=false) ServiceType serviceType,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Menu loaded",service.menu(id,serviceType,page,size));}
    @GetMapping("/restaurants/{id}/operating-hours") @Operation(summary="Get weekly hours and future special schedules")
    public ApiResponse<Map<String,Object>> hours(@PathVariable UUID id,@RequestParam(required=false) ServiceType serviceType){return ApiResponse.ok("Hours loaded",service.hours(id,serviceType));}
    @PostMapping("/customer/favorites/{id}") @PreAuthorize("hasRole('CUSTOMER')") @Operation(summary="Save a restaurant as favorite; idempotent")
    public ApiResponse<UUID> favorite(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Favorite saved",service.favorite(p.userId(),id));}
    @DeleteMapping("/customer/favorites/{id}") @PreAuthorize("hasRole('CUSTOMER')") @Operation(summary="Remove own favorite")
    public ApiResponse<Void> unfavorite(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){service.unfavorite(p.userId(),id);return ApiResponse.ok("Favorite removed",null);}
    @GetMapping("/customer/favorites") @PreAuthorize("hasRole('CUSTOMER')") @Operation(summary="List own favorites that remain publicly discoverable")
    public ApiResponse<PageResponse<Card>> favorites(@AuthenticationPrincipal AppPrincipal p,@Valid @ModelAttribute Search q){return ApiResponse.ok("Favorites loaded",service.favorites(p.userId(),q));}
    @PutMapping("/owner/restaurants/{id}/cuisines") @PreAuthorize("hasRole('OWNER') and hasAuthority('MENU_MANAGE')") @Operation(summary="Replace owned restaurant cuisine tags",description="Requires current restaurantVersion and at most 20 unique cuisine names.")
    public ApiResponse<RestaurantResource<List<String>>> cuisines(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody Cuisines q){return ApiResponse.ok("Cuisines updated",service.cuisines(p.userId(),id,q));}
}
