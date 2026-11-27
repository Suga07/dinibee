package com.bookeat.availability.controller;
import com.bookeat.availability.dto.AvailabilityResponse;
import com.bookeat.availability.service.AvailabilityService;
import com.bookeat.availability.service.SeatAvailabilitySummaryService;
import com.bookeat.availability.dto.SeatAvailabilitySummary;
import com.bookeat.common.response.ApiResponse;
import com.bookeat.exception.*;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor @Tag(name="Restaurant availability")
public class AvailabilityController {
    private final AvailabilityService availability;
    private final SeatAvailabilitySummaryService seatSummary;
    @GetMapping("/api/v1/restaurants/{restaurantId}/availability")
    @Operation(summary="Check restaurant table availability",description="Public read-only. Required date (restaurant-local YYYY-MM-DD) and guests (1-500). Optional time (HH:mm) selects matching tables; omit to list slots. Optional offset (+05:30, URL-encoded) disambiguates repeated DST times. Requires approved/active restaurant, enabled RESTAURANT service, valid settings, hours, capacity and free allocation interval. Result is advisory and never creates a booking or hold.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Availability with reason and slots; closed/private/nonexistent restaurant returns available=false"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="Invalid date/time/offset/guest parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="429",description="More than 60 availability requests per minute from this client address")
    })
    public ResponseEntity<ApiResponse<AvailabilityResponse>> find(@PathVariable UUID restaurantId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime time,@RequestParam(required=false) @Size(max=9) String offset,@RequestParam @Min(1) @Max(500) int guests) {
        ZoneOffset zoneOffset=null;
        if(offset!=null){try{zoneOffset=ZoneOffset.of(offset);}catch(DateTimeException ex){throw new ApiException(ErrorCode.VALIDATION_FAILED);}if(time==null)throw new ApiException(ErrorCode.VALIDATION_FAILED);}
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok("Availability calculated",availability.find(restaurantId,date,time,zoneOffset,guests)));
    }
    @GetMapping("/api/v1/restaurants/{restaurantId}/availability/seat-summary")
    @Operation(summary="Live seat summary by date",description="Returns total, booked and available seat counts grouped by table capacity for the requested restaurant-local date. Reservations on other dates never reduce this date's counts.")
    public ResponseEntity<ApiResponse<SeatAvailabilitySummary>> seatSummary(@PathVariable UUID restaurantId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok("Seat availability calculated",seatSummary.find(restaurantId,date)));
    }

}
