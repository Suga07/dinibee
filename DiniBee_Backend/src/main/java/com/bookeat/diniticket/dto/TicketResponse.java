package com.bookeat.diniticket.dto;
import com.bookeat.diniticket.enums.TicketStatus;
import java.time.Instant;
import java.util.UUID;
public record TicketResponse(UUID id,long version,String ticketNumber,UUID restaurantId,int guestCount,int queuePosition,int estimatedWaitMinutes,TicketStatus status,Instant issuedAt,Instant calledAt,Instant arrivedAt,Instant seatedAt,Instant completedAt,Instant expiresAt,UUID tableId,Instant tableProtectedUntil,boolean expiryDeferredByPreorder) {}
