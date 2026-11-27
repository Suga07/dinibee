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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/customer/reservations") @PreAuthorize("hasRole('CUSTOMER')") @SecurityRequirement(name="bearerAuth") @Tag(name="Customer reservations")
public class CustomerReservationController {
    private final ReservationService service;
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Operation(summary="Create regular reservation",description="Idempotency-Key required. Server revalidates approval, service, hours, capacity and allocation overlap in one transaction. Payment remains UNPAID.")
    public ApiResponse<BookingResponses.Booking> create(@AuthenticationPrincipal AppPrincipal p,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody BookingRequests.Create q){return ApiResponse.ok("Reservation created",service.create(p.userId(),key,q));}
    @GetMapping @Operation(summary="List own reservations")
    public ApiResponse<PageResponse<BookingResponses.Booking>> list(@AuthenticationPrincipal AppPrincipal p,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Reservations loaded",service.list(p.userId(),page,size));}
    @GetMapping("/{id}") @Operation(summary="Get own reservation and status history")
    public ApiResponse<BookingResponses.Booking> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Reservation loaded",service.get(p.userId(),id));}
    @PutMapping("/{id}") @Operation(summary="Reschedule regular or unlocked group reservation",description="Requires reservation version; original allocation is restored if the new slot cannot be claimed. Corporate edits use the corporate route.")
    public ApiResponse<BookingResponses.Booking> change(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody BookingRequests.Change q){return ApiResponse.ok("Reservation updated",service.change(p.userId(),id,q));}
    @PutMapping("/{id}/cancel") @Operation(summary="Cancel own reservation under snapshotted cancellation policy")
    public ApiResponse<BookingResponses.Booking> cancel(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody BookingRequests.Action q){return ApiResponse.ok("Reservation cancelled",service.cancel(p.userId(),id,q));}
}
