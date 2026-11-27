package com.bookeat.availability.service;
import java.time.*;
import java.util.UUID;
/** Internal Phase 6 transaction boundary. Deliberately has no HTTP creation endpoint in Phase 4. */
public interface TableAllocationService {
    Claim claimReservation(UUID actor,UUID restaurantId,UUID tableId,UUID reservationId,OffsetDateTime startsAt,int guests);
    void release(UUID actor,UUID restaurantId,UUID allocationId,UUID reservationId);
    record Claim(UUID id,UUID restaurantId,UUID tableId,UUID referenceId,Instant startsAt,Instant diningEndsAt,Instant occupiedUntil,int guestCount) {}
}
