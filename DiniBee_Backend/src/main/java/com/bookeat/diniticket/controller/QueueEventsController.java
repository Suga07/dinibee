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
import com.bookeat.diniticket.repository.QueueEventRepository.Event;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.*;
@RestController @RequiredArgsConstructor @SecurityRequirement(name="bearerAuth") @Tag(name="DiniTicket events")
public class QueueEventsController {
    private final QueueEventService service;
    @GetMapping("/api/v1/customer/dinitickets/events") @PreAuthorize("hasRole('CUSTOMER')") @Operation(summary="Replay own durable queue events",description="after is an exclusive event cursor. At most 100 events; no other customer's ticket data.")
    public ApiResponse<List<Event>> customer(@AuthenticationPrincipal AppPrincipal p,@RequestParam(defaultValue="0") @Min(0) long after){return ApiResponse.ok("Queue events loaded",service.customer(p.userId(),after));}
    @GetMapping("/api/v1/owner/restaurants/{restaurantId}/dinitickets/events") @PreAuthorize("hasRole('OWNER') and hasAuthority('DINITICKET_MANAGE')") @Operation(summary="Replay owned restaurant queue events")
    public ApiResponse<List<Event>> owner(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestParam(defaultValue="0") @Min(0) long after){return ApiResponse.ok("Queue events loaded",service.owner(p.userId(),restaurantId,after));}
    @GetMapping(value="/api/v1/customer/dinitickets/stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE) @PreAuthorize("hasRole('CUSTOMER')") @Operation(summary="Replay own events as a finite SSE batch",description="Bearer-authenticated replay with Last-Event-ID; up to 100 events, then reconnect after 3 seconds. Each reconnect revalidates session and permissions. Use JSON events endpoint for polling clients.")
    public ResponseEntity<SseEmitter> customerStream(@AuthenticationPrincipal AppPrincipal p,@RequestHeader(name="Last-Event-ID",defaultValue="0") @Min(0) long after){return stream(service.customer(p.userId(),after));}
    @GetMapping(value="/api/v1/owner/restaurants/{restaurantId}/dinitickets/stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE) @PreAuthorize("hasRole('OWNER') and hasAuthority('DINITICKET_MANAGE')") @Operation(summary="Replay owned restaurant events as a finite SSE batch")
    public ResponseEntity<SseEmitter> ownerStream(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@RequestHeader(name="Last-Event-ID",defaultValue="0") @Min(0) long after){return stream(service.owner(p.userId(),restaurantId,after));}
    private ResponseEntity<SseEmitter> stream(List<Event> events){var emitter=new SseEmitter(5000L);try{emitter.send(SseEmitter.event().comment("queue replay").reconnectTime(3000));for(var e:events)emitter.send(SseEmitter.event().id(Long.toString(e.id())).name(e.type()).data(e));emitter.complete();}catch(java.io.IOException e){emitter.completeWithError(e);}return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(emitter);}
}
