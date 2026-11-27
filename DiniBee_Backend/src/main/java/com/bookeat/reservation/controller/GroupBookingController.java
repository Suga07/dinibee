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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/customer/group-bookings") @PreAuthorize("hasRole('CUSTOMER')") @SecurityRequirement(name="bearerAuth") @Tag(name="Group bookings")
public class GroupBookingController {
    private final GroupBookingService service;
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Operation(summary="Create group reservation including its host member",description="Idempotency-Key required. One table per group; memberLimit cannot exceed guestCount.")
    public ApiResponse<BookingResponses.Group> create(@AuthenticationPrincipal AppPrincipal p,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody BookingRequests.Group q){return ApiResponse.ok("Group created",service.create(p.userId(),key,q));}
    @GetMapping("/{id}") @Operation(summary="Get group as host or joined member")
    public ApiResponse<BookingResponses.Group> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Group loaded",service.get(p.userId(),id));}
    @PostMapping("/{id}/invite") @Operation(summary="Rotate host invitation",description="Requires group version. Returns token and deep-link QR payload; does not send a message. Previous token becomes invalid.")
    public ApiResponse<BookingResponses.Invitation> invite(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@RequestParam @Min(0) long version){return ApiResponse.ok("Invitation generated",service.invite(p.userId(),id,version));}
    @PostMapping("/join/{inviteCode}") @Operation(summary="Join unexpired unlocked group invitation")
    public ApiResponse<BookingResponses.Group> join(@AuthenticationPrincipal AppPrincipal p,@PathVariable @Size(max=100) String inviteCode,@Valid @RequestBody BookingRequests.Join q){return ApiResponse.ok("Group joined",service.join(p.userId(),inviteCode,q));}
    @GetMapping("/{id}/members") @Operation(summary="List members as host or joined member")
    public ApiResponse<List<BookingResponses.Member>> members(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Members loaded",service.get(p.userId(),id).members());}
    @DeleteMapping("/{id}/members/{memberId}") @Operation(summary="Host removes a member",description="Requires group version; host cannot be removed and active food orders must be resolved first.")
    public ApiResponse<BookingResponses.Group> remove(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@PathVariable UUID memberId,@RequestParam @Min(0) long version){return ApiResponse.ok("Member removed",service.remove(p.userId(),id,memberId,version));}
    @PostMapping("/{id}/lock") @Operation(summary="Host locks membership and new group orders")
    public ApiResponse<BookingResponses.Group> lock(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@RequestParam @Min(0) long version){return ApiResponse.ok("Group locked",service.lockGroup(p.userId(),id,version));}
    @PostMapping("/{id}/remind") @Operation(summary="Host sends reminder notification to all joined group guests")
    public ApiResponse<Map<String, Object>> remind(@AuthenticationPrincipal AppPrincipal p, @PathVariable UUID id) {
        return ApiResponse.ok("Reminder sent to joined guests", service.remindGuests(p.userId(), id));
    }
}
