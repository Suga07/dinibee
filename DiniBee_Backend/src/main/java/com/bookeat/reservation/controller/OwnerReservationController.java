package com.bookeat.reservation.controller;
import com.bookeat.reservation.service.*;
import com.bookeat.reservation.dto.*;
import com.bookeat.reservation.enums.*;
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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/owner/restaurants/{restaurantId}/reservations") @PreAuthorize("hasRole('OWNER') and hasAuthority('RESERVATION_MANAGE')") @SecurityRequirement(name="bearerAuth") @Tag(name="Owner reservations")
public class OwnerReservationController {
    private final ReservationService service;
    @GetMapping @Operation(summary="List reservations of owned restaurant")
    public ApiResponse<PageResponse<BookingResponses.Booking>> list(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Reservations loaded",service.ownerList(p.userId(),restaurantId,page,size));}
    @GetMapping("/{id}") @Operation(summary="Get reservation of owned restaurant")
    public ApiResponse<BookingResponses.Booking> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id){return ApiResponse.ok("Reservation loaded",service.ownerGet(p.userId(),restaurantId,id));}
    @PutMapping("/{id}") @Operation(summary="Edit/reschedule reservation as restaurant owner",description="Uses live table inventory and revalidates allocation overlap before saving.")
    public ApiResponse<BookingResponses.Booking> change(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody BookingRequests.Change q){return ApiResponse.ok("Reservation updated",service.ownerChange(p.userId(),restaurantId,id,q));}
    @PutMapping("/{id}/dining") @Operation(summary="Check in an arrived reservation and start dining")
    public ApiResponse<BookingResponses.Booking> dining(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody BookingRequests.Action q){return ApiResponse.ok("Dining started",service.ownerStartDining(p.userId(),restaurantId,id,q));}
    @PutMapping("/{id}/complete") @Operation(summary="Complete an arrived/dining reservation")
    public ApiResponse<BookingResponses.Booking> complete(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody BookingRequests.Action q){return ApiResponse.ok("Reservation completed",service.ownerComplete(p.userId(),restaurantId,id,q));}
    @PutMapping("/{id}/arrived") @Operation(summary="Mark reservation customer arrived")
    public ApiResponse<BookingResponses.Booking> arrived(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id){return ApiResponse.ok("Customer marked arrived",service.ownerSetArrival(p.userId(),restaurantId,id,true));}
    @PutMapping("/{id}/not-arrived") @Operation(summary="Reset reservation customer arrival")
    public ApiResponse<BookingResponses.Booking> notArrived(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id){return ApiResponse.ok("Customer arrival reset",service.ownerSetArrival(p.userId(),restaurantId,id,false));}
    @PutMapping("/{id}/confirm") @Operation(summary="Confirm reservation",description="Requires reservation version and reason; state and deadline checked by server.")
    public ApiResponse<BookingResponses.Booking> confirm(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody BookingRequests.Action q){return ApiResponse.ok("Reservation updated",service.ownerAction(p.userId(),restaurantId,id,q,ReservationStatus.CONFIRMED));}
    @PutMapping("/{id}/reject") @Operation(summary="Reject reservation",description="Requires reservation version and reason; state and deadline checked by server.")
    public ApiResponse<BookingResponses.Booking> reject(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody BookingRequests.Action q){return ApiResponse.ok("Reservation updated",service.ownerAction(p.userId(),restaurantId,id,q,ReservationStatus.REJECTED));}
    @PutMapping("/{id}/no-show") @Operation(summary="No-show reservation",description="Requires reservation version and reason; state and deadline checked by server.")
    public ApiResponse<BookingResponses.Booking> noshow(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody BookingRequests.Action q){return ApiResponse.ok("Reservation updated",service.ownerAction(p.userId(),restaurantId,id,q,ReservationStatus.NO_SHOW));}
}
