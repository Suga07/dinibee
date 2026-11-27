package com.bookeat.availability.service;
import com.bookeat.availability.dto.AvailabilityResponse;
import java.time.*;
import java.util.UUID;
public interface AvailabilityService {
    AvailabilityResponse find(UUID restaurantId,LocalDate date,LocalTime time,ZoneOffset offset,int guests);
}
