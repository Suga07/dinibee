package com.bookeat.availability.service;
import com.bookeat.availability.entity.TableAllocation;
import com.bookeat.availability.enums.AllocationType;
import com.bookeat.availability.repository.TableAllocationRepository;
import com.bookeat.table.repository.RestaurantTableRepository;
import com.bookeat.table.service.TableRules;
import com.bookeat.table.enums.TableStatus;
import com.bookeat.restaurant.entity.Restaurant;
import com.bookeat.restaurant.repository.RestaurantReservationSettingsRepository;
import com.bookeat.commerce.service.RestaurantWindowService;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
public class DiniTicketAllocationService {
    private final RestaurantTableRepository tables;private final TableAllocationRepository allocations;private final RestaurantReservationSettingsRepository settings;private final RestaurantWindowService windows;private final Clock clock;private final com.bookeat.restaurant.repository.RestaurantRepository restaurants;
    /** Caller must hold restaurant lock. Real-time seating does not require the reservation slot grid. */
    public UUID claim(Restaurant restaurant,UUID tableId,UUID ticketId,int guests){var t=tables.lockOwned(tableId,restaurant.getId()).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));if(!TableRules.bookable(t.isActive(),t.getStatus(),t.getMinimumGuests(),t.getMaximumGuests(),t.getCapacity(),guests))throw new ApiException(ErrorCode.TABLE_NOT_AVAILABLE);var cfg=settings.findByRestaurantId(restaurant.getId());int duration=cfg.map(c->c.getReservationDurationMinutes()).orElse(90),grace=cfg.map(c->c.getGracePeriodMinutes()).orElse(0);Instant start=clock.instant(),diningEnd=start.plusSeconds(duration*60L),end=diningEnd.plusSeconds(grace*60L);if(!windows.contains(restaurant,start,end)||allocations.overlapping(restaurant.getId(),start,end).stream().anyMatch(a->a.getTable().getId().equals(tableId)))throw new ApiException(ErrorCode.TABLE_NOT_AVAILABLE);
        var a=new TableAllocation();a.setRestaurant(restaurant);a.setTable(t);a.setReferenceType(AllocationType.DINITICKET);a.setReferenceId(ticketId);a.setStartsAt(start);a.setDiningEndsAt(diningEnd);a.setEndsAt(end);a.setGuestCount(guests);a.setActive(true);allocations.saveAndFlush(a);t.setStatus(TableStatus.OCCUPIED);restaurant.setChangeSequence(restaurant.getChangeSequence()+1);tables.flush();return a.getId();}
    public void release(UUID rid,UUID allocationId,UUID ticketId){if(allocationId==null)return;var a=allocations.findByIdAndRestaurantIdAndReferenceId(allocationId,rid,ticketId).filter(x->x.getReferenceType()==AllocationType.DINITICKET).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));var t=tables.lockOwned(a.getTable().getId(),rid).orElseThrow();if(a.isActive()){a.setActive(false);a.setReleasedAt(clock.instant());if(t.getStatus()==TableStatus.OCCUPIED){t.setStatus(TableStatus.AVAILABLE);var r=restaurants.findById(rid).orElseThrow();r.setChangeSequence(r.getChangeSequence()+1);}allocations.flush();}}
}
