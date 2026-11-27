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
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/owner/restaurants/{restaurantId}/dinitickets") @PreAuthorize("hasAnyRole('OWNER','RESTAURANT_MANAGER') and hasAuthority('DINITICKET_MANAGE')") @SecurityRequirement(name="bearerAuth") @Tag(name="Owner DiniTicket")
public class OwnerDiniTicketController {
    private final DiniTicketService service;
    private final DiniTicketSessionMenuService sessionMenu;
    public record WalkIn(@NotNull UUID customerId,@Min(1) @Max(500) int guestCount) {}

    @GetMapping("/session-menu") @Operation(summary="Get active DiniTicket session food")
    public ApiResponse<java.util.List<DiniTicketSessionMenuService.SessionMenuItem>> sessionMenu(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId){return ApiResponse.ok("DiniTicket session menu loaded",sessionMenu.listOwner(p.userId(),restaurantId));}

    @PutMapping("/session-menu") @Operation(summary="Replace active DiniTicket session food",description="Creates a DiniTicket-only temporary food menu. Old session food is soft-deleted so order history remains intact.")
    public ApiResponse<java.util.List<DiniTicketSessionMenuService.SessionMenuItem>> saveSessionMenu(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestBody java.util.List<DiniTicketSessionMenuService.SaveItem> items){return ApiResponse.ok("DiniTicket session menu saved",sessionMenu.replace(p.userId(),restaurantId,items));}

    @PostMapping(value="/session-menu/image",consumes=org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE) @Operation(summary="Upload DiniTicket session food photo")
    public ApiResponse<String> uploadSessionImage(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestPart("file") org.springframework.web.multipart.MultipartFile file,jakarta.servlet.http.HttpServletRequest request){String base=request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort();return ApiResponse.ok("Image uploaded",sessionMenu.uploadImage(p.userId(),restaurantId,file,base));}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Operation(summary="Issue walk-in ticket for an existing customer",description="Owner only; Idempotency-Key required. Supports venues with remote join disabled. Does not create customer accounts.")
    public ApiResponse<TicketResponse> create(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody WalkIn q){return ApiResponse.ok("Ticket issued",service.walkIn(p.userId(),restaurantId,key,q.customerId(),q.guestCount()));}
    @GetMapping @Operation(summary="List owned restaurant tickets")
    public ApiResponse<PageResponse<TicketResponse>> list(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Tickets loaded",service.ownerList(p.userId(),restaurantId,false,page,size));}
    @GetMapping("/waiting") @Operation(summary="List active queue in server FIFO order")
    public ApiResponse<PageResponse<TicketResponse>> waiting(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok("Queue loaded",service.ownerList(p.userId(),restaurantId,true,page,size));}
    @GetMapping("/live") @Operation(summary="Get server-authoritative live DiniTicket token",description="Returns the persisted now-serving token and next waiting tokens. A newly generated WAITING token is visible immediately even before Call Token is pressed.")
    public ApiResponse<DiniTicketLiveResponse> live(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId){return ApiResponse.ok("Live queue loaded",service.live(p.userId(),restaurantId));}
    @GetMapping("/{id}") @Operation(summary="Get ticket of owned restaurant")
    public ApiResponse<TicketResponse> get(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id){return ApiResponse.ok("Ticket loaded",service.ownerGet(p.userId(),restaurantId,id));}
    @PutMapping("/{id}/seat") @Operation(summary="Seat an arrived ticket with optional table allocation",description="Requires ticket version. If tableId is supplied, locks table and claims dining duration plus grace without overlap; sets operational status OCCUPIED until completion. Missing reservation settings use a 90-minute seating interval.")
    public ApiResponse<TicketResponse> seat(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody TicketRequests.Seat q){return ApiResponse.ok("Ticket seated",service.seat(p.userId(),restaurantId,id,q));}
    @PutMapping("/{id}/call") @Operation(summary="Call queue ticket",description="Requires current ticket version and reason. Calls respect FIFO; no-show waits until call deadline; completion releases table and requires food orders resolved.")
    public ApiResponse<TicketResponse> call(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody TicketRequests.Action q){return ApiResponse.ok("Ticket updated",service.action(p.userId(),restaurantId,id,q,TicketStatus.CALLED));}
    @PutMapping("/{id}/arrived") @Operation(summary="Arrived queue ticket",description="Requires current ticket version and reason. Calls respect FIFO; no-show waits until call deadline; completion releases table and requires food orders resolved.")
    public ApiResponse<TicketResponse> arrived(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody TicketRequests.Action q){return ApiResponse.ok("Ticket updated",service.action(p.userId(),restaurantId,id,q,TicketStatus.ARRIVED));}
    @PutMapping("/{id}/no-show") @Operation(summary="No-show queue ticket",description="Requires current ticket version and reason. Calls respect FIFO; no-show waits until call deadline; completion releases table and requires food orders resolved.")
    public ApiResponse<TicketResponse> noshow(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody TicketRequests.Action q){return ApiResponse.ok("Ticket updated",service.action(p.userId(),restaurantId,id,q,TicketStatus.NO_SHOW));}
    @PutMapping("/{id}/cancel") @Operation(summary="Cancel queue ticket",description="Owner/manager cancellation for the latest ticket version. Linked food work must still be cancellable.")
    public ApiResponse<TicketResponse> cancel(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody TicketRequests.Action q){return ApiResponse.ok("Ticket cancelled",service.action(p.userId(),restaurantId,id,q,TicketStatus.CANCELLED));}
    @PutMapping("/{id}/handover") @Operation(summary="Verify DiniTicket handover",description="Verifies the token, settles pending CASH, persists the handed-over stage, and leaves final completion as a separate owner action. The submitted token number must match the ticket.")
    public ApiResponse<TicketResponse> handover(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody TicketRequests.Handover q){return ApiResponse.ok("DiniTicket handed over",service.handover(p.userId(),restaurantId,id,q));}
    @PutMapping("/{id}/complete") @Operation(summary="Complete handed-over DiniTicket",description="Completes the linked DiniTicket food order and then completes the handed-over ticket. Repeated completion is idempotent.")
    public ApiResponse<TicketResponse> complete(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody TicketRequests.Action q){return ApiResponse.ok("DiniTicket completed",service.completeHandover(p.userId(),restaurantId,id,q));}
}
