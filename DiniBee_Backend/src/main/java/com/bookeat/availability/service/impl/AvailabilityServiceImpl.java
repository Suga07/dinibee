package com.bookeat.availability.service.impl;
import com.bookeat.availability.dto.AvailabilityResponse;
import com.bookeat.availability.dto.AvailabilityResponse.*;
import com.bookeat.availability.enums.AvailabilityReason;
import com.bookeat.availability.repository.TableAllocationRepository;
import com.bookeat.availability.service.*;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.repository.*;
import com.bookeat.restaurant.service.WorkflowRules;
import com.bookeat.table.repository.RestaurantTableRepository;
import com.bookeat.table.entity.RestaurantTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {
    private final RestaurantRepository restaurants;
    private final RestaurantServiceActivationRepository services;
    private final RestaurantReservationSettingsRepository settings;
    private final RestaurantOperatingHourRepository hours;
    private final RestaurantSpecialScheduleRepository special;
    private final RestaurantTableRepository tables;
    private final TableAllocationRepository allocations;
    private final Clock clock;
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public AvailabilityResponse find(UUID restaurantId,LocalDate date,LocalTime time,ZoneOffset offset,int guests) {
        Instant now=clock.instant();var r=restaurants.findById(restaurantId).orElse(null);
        if(r==null || r.getApprovalStatus()!=ApprovalStatus.APPROVED || r.getStatus()!=RestaurantStatus.ACTIVE)
            return unavailable(restaurantId,date,time,guests,AvailabilityReason.RESTAURANT_UNAVAILABLE,now);
        var candidates=tables.findByRestaurantIdAndDeletedAtIsNullOrderByCapacityAscTableNumberAsc(restaurantId);
        boolean enabled=services.findByRestaurantIdOrderByCreatedAtAscIdAsc(restaurantId).stream().anyMatch(s->s.getServiceType()==ServiceType.DINE_IN && WorkflowRules.effectiveService(r.getApprovalStatus(),r.getStatus(),s.isRequested(),s.isAuthorized(),s.isEnabled()));
        var config=settings.findByRestaurantId(restaurantId).orElse(null);
        // Restaurant detail calls this endpoint without an exact time. Return the
        // restaurant's own live table inventory even when reservation/service setup
        // is incomplete; exact booking requests (time != null) remain strict.
        if(time==null && (!enabled || config==null || !config.isReservationEnabled() || r.getTimezone()==null)) {
            var live=candidates.stream().filter(RestaurantTable::isActive)
                    .filter(t->t.getStatus()==com.bookeat.table.enums.TableStatus.AVAILABLE).map(this::table).toList();
            var reason=!enabled?AvailabilityReason.RESTAURANT_UNAVAILABLE:AvailabilityReason.RESERVATIONS_DISABLED;
            return new AvailabilityResponse(restaurantId,date,null,r.getTimezone(),guests,false,List.of(),live,null,null,reason,
                    live.isEmpty()?message(reason):"Live table inventory loaded. Select a date and time to validate booking availability.",now);
        }
        if(!enabled)return unavailable(restaurantId,date,time,guests,AvailabilityReason.RESTAURANT_UNAVAILABLE,now);
        if(config==null || !config.isReservationEnabled() || r.getTimezone()==null)return unavailable(restaurantId,date,time,guests,AvailabilityReason.RESERVATIONS_DISABLED,now);
        var zone=ZoneId.of(r.getTimezone());var today=now.atZone(zone).toLocalDate();
        if(date.isBefore(today) || date.isAfter(today.plusDays(config.getAdvanceBookingDays())))return unavailable(restaurantId,date,time,guests,AvailabilityReason.OUTSIDE_BOOKING_WINDOW,now);
        if(guests<config.getMinimumGuests() || guests>config.getMaximumGuestsPerBooking())return unavailable(restaurantId,date,time,guests,AvailabilityReason.INVALID_GUEST_COUNT,now);
        var weekly=hours.findByRestaurantIdOrderByCreatedAtAscIdAsc(restaurantId).stream().collect(Collectors.toMap(h->h.getDayOfWeek(),h->h));
        var exceptions=special.findByRestaurantIdOrderByCreatedAtAscIdAsc(restaurantId).stream().filter(s->!s.getDate().isBefore(date.minusDays(1)) && !s.getDate().isAfter(date.plusDays(1))).collect(Collectors.toMap(s->s.getDate(),s->s));
        var schedules=new ArrayList<AvailabilityEngine.Schedule>();
        for(var d:List.of(date.minusDays(1),date,date.plusDays(1))) {
            var override=exceptions.get(d);var normal=weekly.get(d.getDayOfWeek());
            if(override!=null)schedules.add(new AvailabilityEngine.Schedule(d,override.isClosed(),override.getOpeningTime(),override.getClosingTime(),override.isClosesNextDay(),true));
            else if(normal!=null)schedules.add(new AvailabilityEngine.Schedule(d,normal.isClosed(),normal.getOpeningTime(),normal.getClosingTime(),normal.isClosesNextDay(),false));
        }
        var occupancy=allocations.overlapping(restaurantId,date.atStartOfDay(zone).toInstant(),date.plusDays(2).atStartOfDay(zone).toInstant());
        var cfg=new AvailabilityEngine.Settings(config.getMinimumGuests(),config.getMaximumGuestsPerBooking(),config.getSlotDurationMinutes(),config.getReservationDurationMinutes(),config.getGracePeriodMinutes(),config.getAdvanceBookingDays(),config.getMinimumAdvanceMinutes());
        var result=AvailabilityEngine.calculate(zone,date,time,offset,guests,now,cfg,schedules,candidates.stream().map(t->new AvailabilityEngine.Table(t.getId(),t.getCapacity(),t.getMinimumGuests(),t.getMaximumGuests(),t.isActive(),t.getStatus())).toList(),occupancy.stream().map(a->new AvailabilityEngine.Occupancy(a.getTable().getId(),a.getStartsAt(),a.getEndsAt())).toList());
        var availableIds=new HashSet<>(result.requestedTableIds());
        // When the caller asks for the day (no exact time), expose every table that is
        // genuinely free in at least one returned slot. This lets customer clients show
        // owner-created live tables without fabricating inventory.
        if(time==null) {
            result.slots().forEach(slot -> availableIds.addAll(slot.tableIds()));
            // Restaurant detail is an inventory view. Include every live AVAILABLE
            // table owned by this restaurant even when it does not fit the default
            // guest count; exact booking validation still happens for date/time/guests.
            candidates.stream().filter(RestaurantTable::isActive)
                    .filter(t -> t.getStatus()==com.bookeat.table.enums.TableStatus.AVAILABLE)
                    .forEach(t -> availableIds.add(t.getId()));
        }
        return new AvailabilityResponse(restaurantId,date,time,zone.getId(),guests,result.available(),result.slots().stream().map(s->new TimeSlot(s.startsAt(),s.diningEndsAt(),s.occupiedUntil(),s.tableIds().size())).toList(),candidates.stream().filter(t->availableIds.contains(t.getId())).map(this::table).toList(),cfg.durationMinutes(),cfg.graceMinutes(),result.reason(),message(result.reason()),now);
    }
    private AvailableTable table(RestaurantTable t){return new AvailableTable(t.getId(),t.getTableNumber(),t.getTableName(),t.getCapacity(),t.getMinimumGuests(),t.getMaximumGuests(),t.getFloor(),t.getSection(),t.getShape(),t.getXPosition(),t.getYPosition(),t.getWidth(),t.getHeight(),t.getRotation());}
    private AvailabilityResponse unavailable(UUID id,LocalDate date,LocalTime time,int guests,AvailabilityReason reason,Instant now){return new AvailabilityResponse(id,date,time,null,guests,false,List.of(),List.of(),null,null,reason,message(reason),now);}
    private String message(AvailabilityReason reason){return switch(reason){
        case AVAILABLE -> "Availability found. Recheck when creating the reservation; this response does not hold a table.";
        case RESTAURANT_UNAVAILABLE -> "Restaurant is not available for reservations.";
        case RESERVATIONS_DISABLED -> "Reservations are not configured or enabled.";
        case OUTSIDE_BOOKING_WINDOW -> "Date is outside the restaurant's advance booking window.";
        case INVALID_GUEST_COUNT -> "Guest count is outside the restaurant's reservation limits.";
        case CLOSED -> "Restaurant is closed for the selected date.";
        case NO_TABLE_CAPACITY -> "No active bookable table fits this guest count.";
        case NO_AVAILABLE_SLOT -> "No table is available for the requested time or interval.";
        case AMBIGUOUS_LOCAL_TIME -> "This local time occurs twice; supply the offset from an availableTimeSlots entry.";
    };}
}
