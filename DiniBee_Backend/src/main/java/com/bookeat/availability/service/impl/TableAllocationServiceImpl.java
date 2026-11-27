package com.bookeat.availability.service.impl;
import com.bookeat.availability.entity.TableAllocation;
import com.bookeat.availability.enums.AllocationType;
import com.bookeat.availability.repository.TableAllocationRepository;
import com.bookeat.availability.service.*;
import com.bookeat.restaurant.repository.RestaurantRepository;
import com.bookeat.table.repository.RestaurantTableRepository;
import com.bookeat.common.audit.AuditService;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
public class TableAllocationServiceImpl implements TableAllocationService {
    private final RestaurantRepository restaurants;
    private final RestaurantTableRepository tables;
    private final TableAllocationRepository allocations;
    private final AvailabilityService availability;
    private final AuditService audit;
    private final Clock clock;
    @Override public Claim claimReservation(UUID actor,UUID restaurantId,UUID tableId,UUID reservationId,OffsetDateTime startsAt,int guests) {
        if(actor==null || reservationId==null || startsAt==null || guests<1 || guests>500)throw new ApiException(ErrorCode.VALIDATION_FAILED);
        // Lock order: restaurant, table, allocation. Owner edits and all future booking writers share this order.
        var r=restaurants.lockById(restaurantId).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        var table=tables.lockOwned(tableId,restaurantId).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        var existing=allocations.findByRestaurantIdAndTableIdAndReferenceTypeAndReferenceIdAndActiveTrue(restaurantId,tableId,AllocationType.RESERVATION,reservationId);
        if(existing.isPresent()) {
            var a=existing.get();if(!a.getStartsAt().equals(startsAt.toInstant()) || a.getGuestCount()!=guests)throw new ApiException(ErrorCode.CONFLICT);
            return claim(a);
        }
        if(r.getTimezone()==null)throw new ApiException(ErrorCode.TABLE_NOT_AVAILABLE);
        var local=startsAt.atZoneSameInstant(ZoneId.of(r.getTimezone()));
        var result=availability.find(restaurantId,local.toLocalDate(),local.toLocalTime(),local.getOffset(),guests);
        if(!result.available() || result.availableTables().stream().noneMatch(t->t.id().equals(tableId)))throw new ApiException(ErrorCode.TABLE_NOT_AVAILABLE);
        var slot=result.availableTimeSlots().stream().filter(s->s.startsAt().toInstant().equals(startsAt.toInstant())).findFirst().orElseThrow(()->new ApiException(ErrorCode.TABLE_NOT_AVAILABLE));
        var a=new TableAllocation();a.setRestaurant(r);a.setTable(table);a.setReferenceId(reservationId);a.setReferenceType(AllocationType.RESERVATION);a.setStartsAt(slot.startsAt().toInstant());a.setDiningEndsAt(slot.diningEndsAt().toInstant());a.setEndsAt(slot.occupiedUntil().toInstant());a.setGuestCount(guests);a.setActive(true);
        allocations.saveAndFlush(a);audit.record(actor,"TABLE_ALLOCATION_CREATED","TableAllocation",a.getId(),null);return claim(a);
    }
    @Override public void release(UUID actor,UUID restaurantId,UUID allocationId,UUID reservationId) {
        if(actor==null || reservationId==null)throw new ApiException(ErrorCode.VALIDATION_FAILED);
        restaurants.lockById(restaurantId).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        var a=allocations.findByIdAndRestaurantIdAndReferenceId(allocationId,restaurantId,reservationId).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(a.getReferenceType()!=AllocationType.RESERVATION)throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        tables.lockOwned(a.getTable().getId(),restaurantId).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(a.isActive()){a.setActive(false);a.setReleasedAt(clock.instant());allocations.flush();audit.record(actor,"TABLE_ALLOCATION_RELEASED","TableAllocation",a.getId(),null);}
    }
    private Claim claim(TableAllocation a){return new Claim(a.getId(),a.getRestaurant().getId(),a.getTable().getId(),a.getReferenceId(),a.getStartsAt(),a.getDiningEndsAt(),a.getEndsAt(),a.getGuestCount());}
}
