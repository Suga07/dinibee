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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/customer/corporate-bookings") @PreAuthorize("hasRole('CUSTOMER')") @SecurityRequirement(name="bearerAuth") @Tag(name="Corporate bookings")
public class CorporateBookingController {
    private final CorporateBookingService service;
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Operation(summary="Create corporate booking or atomic recurring series",description="Idempotency-Key required. Supply account OR owned corporateAccountId. Bounded recurrence: NONE/DAILY/WEEKLY/MONTHLY, occurrences and interval 1–12. Any slot conflict rolls back the complete series. Budget is per occurrence; invoiceRequired records intent only.")
    public ApiResponse<List<BookingResponses.Corporate>> create(@AuthenticationPrincipal AppPrincipal p,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody BookingRequests.Corporate q){return ApiResponse.ok("Corporate bookings created",service.create(p.userId(),key,q));}
    @GetMapping @Operation(summary="List own corporate booking occurrences")
    public ApiResponse<PageResponse<BookingResponses.Corporate>> list(@AuthenticationPrincipal AppPrincipal p,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Corporate bookings loaded",service.list(p.userId(),page,size));}
    @GetMapping("/{id}") @Operation(summary="Get organizer's corporate booking and guests")
    public ApiResponse<BookingResponses.Corporate> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Corporate booking loaded",service.get(p.userId(),id));}
    @PostMapping("/{id}/guests") @Operation(summary="Invite guest to one occurrence",description="Requires corporate booking version. Returns a private invitation; no email is sent. Organizer counts as one seat.")
    public ApiResponse<BookingResponses.GuestInvite> guest(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@RequestParam @Min(0) long version,@Valid @RequestBody BookingRequests.Guest q){return ApiResponse.ok("Guest invited",service.addGuest(p.userId(),id,version,q));}
    @PostMapping("/join/{inviteCode}") @Operation(summary="Accept corporate invitation",description="Requires authenticated customer with verified matching email; guest can then place individual orders within the occurrence budget.")
    public ApiResponse<BookingResponses.Guest> join(@AuthenticationPrincipal AppPrincipal p,@PathVariable @Size(max=100) String inviteCode){return ApiResponse.ok("Invitation accepted",service.join(p.userId(),inviteCode));}
    @PutMapping("/{id}") @Operation(summary="Edit one corporate occurrence",description="Nested reservation.version is required. Does not silently change other recurring occurrences.")
    public ApiResponse<BookingResponses.Corporate> change(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody BookingRequests.CorporateChange q){return ApiResponse.ok("Corporate booking updated",service.change(p.userId(),id,q));}
    @PutMapping("/{id}/cancel") @Operation(summary="Cancel one corporate occurrence",description="Uses the reservation version, cancellation deadline and policy.")
    public ApiResponse<BookingResponses.Corporate> cancel(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody BookingRequests.Action q){return ApiResponse.ok("Corporate booking cancelled",service.cancel(p.userId(),id,q));}
}
