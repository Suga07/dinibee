package com.bookeat.discovery.controller;

import com.bookeat.common.response.ApiResponse;
import com.bookeat.commerce.service.RestaurantWindowService;
import com.bookeat.exception.ApiException;
import com.bookeat.exception.ErrorCode;
import com.bookeat.restaurant.repository.RestaurantRepository;
import com.bookeat.restaurant.repository.RestaurantTakeawaySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/restaurants")
public class TakeawaySlotController {
    private final RestaurantRepository restaurants;
    private final RestaurantTakeawaySettingsRepository settings;
    private final RestaurantWindowService windows;
    private final Clock clock;

    public record Slot(String startsAt, String label) {}

    @GetMapping("/{restaurantId}/takeaway-slots")
    public ApiResponse<List<Slot>> slots(@PathVariable UUID restaurantId, @RequestParam LocalDate date) {
        var r = restaurants.findById(restaurantId).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        var cfg = settings.findByRestaurantId(restaurantId).orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE));
        if (!cfg.isEnabled() || !cfg.isRegularTakeawayEnabled()) {
            return ApiResponse.ok("Takeaway slots loaded", List.of());
        }
        ZoneId zone = ZoneId.of(r.getTimezone() == null || r.getTimezone().isBlank() ? "Asia/Kolkata" : r.getTimezone());
        Instant now = clock.instant();
        Instant earliest = now.plusSeconds(cfg.getAveragePreparationMinutes() * 60L);
        Instant latest = now.plusSeconds(cfg.getMaximumAdvanceOrderMinutes() * 60L);
        ZonedDateTime cursor = date.atStartOfDay(zone);
        int minute = cursor.getMinute();
        int add = (15 - (minute % 15)) % 15;
        cursor = cursor.plusMinutes(add);
        var out = new ArrayList<Slot>();
        var labelFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
        for (int i = 0; i < 96; i++, cursor = cursor.plusMinutes(15)) {
            Instant pickup = cursor.toInstant();
            if (pickup.isBefore(earliest) || pickup.isAfter(latest)) continue;
            Instant prepStart = pickup.minusSeconds(cfg.getAveragePreparationMinutes() * 60L);
            if (!windows.contains(r, prepStart, pickup.plusNanos(1))) continue;
            out.add(new Slot(cursor.toOffsetDateTime().toString(), cursor.format(labelFmt)));
        }
        return ApiResponse.ok("Takeaway slots loaded", out);
    }
}
