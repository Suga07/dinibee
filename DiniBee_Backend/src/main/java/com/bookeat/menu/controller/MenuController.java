package com.bookeat.menu.controller;
import com.bookeat.menu.dto.*;
import com.bookeat.menu.enums.*;
import com.bookeat.menu.service.MenuService;
import com.bookeat.common.response.*;
import com.bookeat.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/owner/restaurants/{restaurantId}/menu")
@SecurityRequirement(name="bearerAuth") @Tag(name="Owner menu")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="Invalid fields, capacity, price, option IDs or query parameters"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="Bearer token absent or expired"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="Required OWNER role/permission is missing"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="Restaurant/resource absent or belongs to another owner or restaurant"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="Stale restaurant version, locked review state, duplicate name, resource in use or limit reached")
})
public class MenuController {
    private final MenuService menu;
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED) @Operation(summary="Create menu category",description="OWNER + MENU_MANAGE. Requires restaurantVersion. Category names are unique ignoring case within the restaurant.")
    public ApiResponse<RestaurantResource<MenuResponses.Category>> createCategory(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@Valid @RequestBody MenuRequests.Category q){return ApiResponse.ok("Category created",menu.createCategory(p.userId(),restaurantId,q));}
    @GetMapping("/categories") @Operation(summary="List menu categories",description="OWNER + MENU_MANAGE. Page 0-based, size 1-100; ordered by displayOrder and ID.")
    public ApiResponse<PageResponse<MenuResponses.Category>> categories(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Categories loaded",menu.categories(p.userId(),restaurantId,page,size));}
    @GetMapping("/categories/{categoryId}") @Operation(summary="Get menu category",description="OWNER + MENU_MANAGE. Includes current restaurantVersion.")
    public ApiResponse<RestaurantResource<MenuResponses.Category>> category(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID categoryId){return ApiResponse.ok("Category loaded",menu.getCategory(p.userId(),restaurantId,categoryId));}
    @PutMapping("/categories/{categoryId}") @Operation(summary="Replace menu category",description="OWNER + MENU_MANAGE. Requires restaurantVersion. Inactive categories make their items effectively unavailable.")
    public ApiResponse<RestaurantResource<MenuResponses.Category>> updateCategory(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID categoryId,@Valid @RequestBody MenuRequests.Category q){return ApiResponse.ok("Category updated",menu.updateCategory(p.userId(),restaurantId,categoryId,q));}
    @DeleteMapping("/categories/{categoryId}") @Operation(summary="Archive an empty category",description="OWNER + MENU_MANAGE. Requires restaurantVersion query parameter. Move/archive its items first; returns MENU_CATEGORY_NOT_EMPTY otherwise.")
    public ApiResponse<RestaurantResource<UUID>> deleteCategory(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID categoryId,@RequestParam @Min(0) long restaurantVersion){return ApiResponse.ok("Category archived",menu.deleteCategory(p.userId(),restaurantId,categoryId,restaurantVersion));}
    @PostMapping("/items") @ResponseStatus(HttpStatus.CREATED) @Operation(summary="Create menu item with variants and addons",description="OWNER + MENU_MANAGE. Requires restaurantVersion. Option IDs must be null for new options. Prices use decimals with at most 2 fractional digits; discount cannot exceed base.")
    public ApiResponse<RestaurantResource<MenuResponses.Item>> createItem(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@Valid @RequestBody MenuRequests.Item q){return ApiResponse.ok("Menu item created",menu.createItem(p.userId(),restaurantId,q));}
    @GetMapping("/items") @Operation(summary="Filter owned menu items",description="OWNER + MENU_MANAGE. Optional categoryId, foodType, stored available flag and literal name search. Page 0-based; size 1-100. Response separately reports effectiveAvailable.")
    public ApiResponse<PageResponse<MenuResponses.Item>> items(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestParam(required=false) UUID categoryId,@RequestParam(required=false) FoodType foodType,@RequestParam(required=false) Boolean available,@RequestParam(required=false) @Size(max=160) String search,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Menu items loaded",menu.items(p.userId(),restaurantId,categoryId,foodType,available,search,page,size));}
    @GetMapping("/items/{itemId}") @Operation(summary="Get menu item and options",description="OWNER + MENU_MANAGE. Includes current restaurantVersion. Options retain stable IDs across updates.")
    public ApiResponse<RestaurantResource<MenuResponses.Item>> item(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID itemId){return ApiResponse.ok("Menu item loaded",menu.getItem(p.userId(),restaurantId,itemId));}
    @PutMapping("/items/{itemId}") @Operation(summary="Replace item and its options",description="OWNER + MENU_MANAGE. Requires restaurantVersion. Include existing option IDs to retain them; omit an option to archive it. Cross-item IDs rejected. Category must belong to this restaurant.")
    public ApiResponse<RestaurantResource<MenuResponses.Item>> updateItem(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID itemId,@Valid @RequestBody MenuRequests.Item q){return ApiResponse.ok("Menu item updated",menu.updateItem(p.userId(),restaurantId,itemId,q));}
    @DeleteMapping("/items/{itemId}") @Operation(summary="Archive a menu item and its options",description="OWNER + MENU_MANAGE. restaurantVersion query parameter required. Rows remain for historical references.")
    public ApiResponse<RestaurantResource<UUID>> deleteItem(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID itemId,@RequestParam @Min(0) long restaurantVersion){return ApiResponse.ok("Menu item archived",menu.deleteItem(p.userId(),restaurantId,itemId,restaurantVersion));}
    @PatchMapping("/items/{itemId}/availability") @Operation(summary="Set menu item availability",description="OWNER + MENU_MANAGE. Requires restaurantVersion and available boolean. Does not enable a disabled restaurant service or category.")
    public ApiResponse<RestaurantResource<MenuResponses.Item>> availability(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID itemId,@Valid @RequestBody MenuRequests.Availability q){return ApiResponse.ok("Menu availability updated",menu.availability(p.userId(),restaurantId,itemId,q));}
    @PostMapping(value="/images", consumes=org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary="Upload menu item image", description="OWNER + MENU_MANAGE. Uploads image file and returns accessible URL.")
    public ApiResponse<java.util.Map<String, String>> uploadImage(
        @AuthenticationPrincipal AppPrincipal p,
        @PathVariable UUID restaurantId,
        @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
        jakarta.servlet.http.HttpServletRequest request) {
        String baseUri = request.getScheme() + "://" + request.getHeader("host");
        String url = menu.uploadItemImage(p.userId(), restaurantId, file, baseUri);
        return ApiResponse.ok("Image uploaded", java.util.Map.of("imageUrl", url));
    }
}
