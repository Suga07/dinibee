package com.bookeat.dining.dto;
import com.bookeat.dining.enums.*;
import java.util.UUID;import java.time.Instant;import java.math.BigDecimal;
public record DiningResponse(UUID id,long version,UUID reservationId,UUID diniTicketId,UUID restaurantId,UUID tableId,DiningStatus status,CheckInMethod checkInMethod,Instant checkedInAt,Instant diningStartedAt,Instant paymentPendingAt,Instant paidAt,Instant checkedOutAt,BigDecimal billTotal){}
