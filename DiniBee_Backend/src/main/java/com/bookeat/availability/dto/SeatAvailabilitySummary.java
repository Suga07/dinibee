package com.bookeat.availability.dto;
import java.time.*;import java.util.*;
public record SeatAvailabilitySummary(UUID restaurantId, LocalDate date, int totalSeats, int bookedSeats, int availableSeats, List<Category> categories, Instant checkedAt) {
 public record Category(int capacity, int totalTables, int bookedTables, int availableTables, int totalSeats, int bookedSeats, int availableSeats) {}
}
