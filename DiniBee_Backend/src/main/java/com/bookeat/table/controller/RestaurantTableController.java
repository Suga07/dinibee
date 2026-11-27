package com.bookeat.table.controller;
import com.bookeat.table.dto.*;
import com.bookeat.table.service.RestaurantTableService;
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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/owner/restaurants/{restaurantId}/tables")
@SecurityRequirement(name="bearerAuth") @Tag(name="Owner tables")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="Invalid fields, capacity, price, option IDs or query parameters"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="Bearer token absent or expired"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="Required OWNER role/permission is missing"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="Restaurant/resource absent or belongs to another owner or restaurant"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="Stale restaurant version, locked review state, duplicate name, resource in use or limit reached")
})
public class RestaurantTableController {
    private final RestaurantTableService tables;
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Operation(summary="Create a restaurant table",description="OWNER + TABLE_MANAGE. Requires restaurantVersion. Capacity and geometry validated; initial status AVAILABLE. Editable draft/reopened/approved restaurants only.")
    public ApiResponse<RestaurantResource<TableResponse>> create(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@Valid @RequestBody TableRequests.Write q){return ApiResponse.ok("Table created",tables.create(p.userId(),restaurantId,q));}
    @GetMapping @Operation(summary="List owned restaurant tables",description="OWNER + TABLE_MANAGE. Page starts at 0, size 1-100. Archived tables excluded; inactive/status-blocked tables remain visible to owner.")
    public ApiResponse<PageResponse<TableResponse>> list(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Tables loaded",tables.list(p.userId(),restaurantId,page,size));}
    @GetMapping("/summary") @Operation(summary="Get live table status counts",description="Returns database-backed table counts for the owned restaurant. Use this for dashboard Active seating / Cleaning tables; no UI mock counts are required.")
    public ApiResponse<TableStatusSummary> summary(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId){return ApiResponse.ok("Table status summary loaded",tables.summary(p.userId(),restaurantId));}
    @GetMapping("/{tableId}") @Operation(summary="Get a table and latest restaurant version",description="OWNER + TABLE_MANAGE. Resource is scoped to the owned restaurant.")
    public ApiResponse<RestaurantResource<TableResponse>> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID tableId){return ApiResponse.ok("Table loaded",tables.get(p.userId(),restaurantId,tableId));}
    @PutMapping("/{tableId}") @Operation(summary="Replace table configuration",description="OWNER + TABLE_MANAGE. Requires current restaurantVersion. Current/future active allocations prevent configuration edits. Status changes use the status endpoint.")
    public ApiResponse<RestaurantResource<TableResponse>> update(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID tableId,@Valid @RequestBody TableRequests.Write q){return ApiResponse.ok("Table updated",tables.update(p.userId(),restaurantId,tableId,q));}
    @DeleteMapping("/{tableId}") @Operation(summary="Archive an unused table",description="OWNER + TABLE_MANAGE. restaurantVersion query parameter required. Current/future allocations prevent archival. Historical references are retained.")
    public ApiResponse<RestaurantResource<UUID>> delete(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID tableId,@RequestParam @Min(0) long restaurantVersion){return ApiResponse.ok("Table archived",tables.delete(p.userId(),restaurantId,tableId,restaurantVersion));}
    @PatchMapping("/{tableId}/status") @Operation(summary="Change operational table status",description="OWNER + TABLE_MANAGE. Requires restaurantVersion, status and reason. Status never releases timed allocations. Only active AVAILABLE tables are advertised as bookable.")
    public ApiResponse<RestaurantResource<TableResponse>> status(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID tableId,@Valid @RequestBody TableRequests.Status q){return ApiResponse.ok("Table status updated",tables.status(p.userId(),restaurantId,tableId,q));}
}
