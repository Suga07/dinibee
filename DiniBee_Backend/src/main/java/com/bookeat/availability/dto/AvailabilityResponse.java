package com.bookeat.availability.dto;
import com.bookeat.availability.enums.AvailabilityReason;
import com.bookeat.table.enums.TableShape;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public record AvailabilityResponse(UUID restaurantId,LocalDate date,LocalTime requestedTime,String timezone,int guests,boolean available,List<TimeSlot> availableTimeSlots,List<AvailableTable> availableTables,Integer reservationDuration,Integer gracePeriodMinutes,AvailabilityReason reason,String message,Instant checkedAt) {
    public record TimeSlot(OffsetDateTime startsAt,OffsetDateTime diningEndsAt,OffsetDateTime occupiedUntil,int availableTableCount) {}
    public record AvailableTable(UUID id,String tableNumber,String tableName,int capacity,int minimumGuests,int maximumGuests,int floor,String section,TableShape shape,BigDecimal xPosition,BigDecimal yPosition,BigDecimal width,BigDecimal height,BigDecimal rotation) {}
}
