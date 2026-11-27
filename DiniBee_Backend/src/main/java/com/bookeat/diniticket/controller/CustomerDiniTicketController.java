package com.bookeat.diniticket.controller;
import com.bookeat.diniticket.service.*;
import com.bookeat.diniticket.dto.*;
import com.bookeat.diniticket.enums.*;
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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/customer/dinitickets") @PreAuthorize("hasRole('CUSTOMER')") @SecurityRequirement(name="bearerAuth") @Tag(name="Customer DiniTicket")
public class CustomerDiniTicketController {
    private final DiniTicketService service;
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Operation(summary="Request queue ticket",description="Idempotency-Key required. Restaurant must be open with DINITICKET enabled and remote join allowed. One active ticket per customer/restaurant; queue capacity and position enforced on server.")
    public ApiResponse<TicketResponse> create(@AuthenticationPrincipal AppPrincipal p,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody TicketRequests.Create q){return ApiResponse.ok("Ticket issued",service.create(p.userId(),key,q));}
    @GetMapping("/current") @Operation(summary="List customer's current tickets across restaurants",description="Configured expirations are processed transactionally before returning the snapshot.")
    public ApiResponse<List<TicketResponse>> current(@AuthenticationPrincipal AppPrincipal p){return ApiResponse.ok("Current tickets loaded",service.current(p.userId()));}
    @GetMapping("/all") @Operation(summary="List all customer DiniTickets",description="Returns persisted active and historical DiniTickets for Orders & Tracking. This endpoint is read-only and does not synthesize mock orders.")
    public ApiResponse<PageResponse<TicketResponse>> all(@AuthenticationPrincipal AppPrincipal p,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("DiniTickets loaded",service.customerList(p.userId(),page,size));}
    @GetMapping("/{id}") @Operation(summary="Get own ticket and server queue position")
    public ApiResponse<TicketResponse> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){return ApiResponse.ok("Ticket loaded",service.get(p.userId(),id));}
    @PutMapping("/{id}/cancel") @Operation(summary="Cancel own waiting/called/arrived ticket",description="Requires ticket version and reason. Accepted preorder work must be resolved by owner first.")
    public ApiResponse<TicketResponse> cancel(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody TicketRequests.Action q){return ApiResponse.ok("Ticket cancelled",service.cancel(p.userId(),id,q));}
}
