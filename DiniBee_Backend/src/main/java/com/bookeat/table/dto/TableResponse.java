package com.bookeat.table.dto;
import com.bookeat.table.enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record TableResponse(UUID id,long version,UUID restaurantId,String tableNumber,String tableName,int capacity,int minimumGuests,int maximumGuests,int floor,String section,TableShape shape,TableStatus status,BigDecimal xPosition,BigDecimal yPosition,BigDecimal width,BigDecimal height,BigDecimal rotation,boolean active,Instant createdAt,Instant updatedAt) {}
