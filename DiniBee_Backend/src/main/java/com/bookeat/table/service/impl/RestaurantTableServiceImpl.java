package com.bookeat.table.service.impl;
import com.bookeat.table.dto.*;
import com.bookeat.table.entity.RestaurantTable;
import com.bookeat.table.enums.TableStatus;
import com.bookeat.table.mapper.TableMapper;
import com.bookeat.table.repository.RestaurantTableRepository;
import com.bookeat.table.service.*;
import com.bookeat.availability.repository.TableAllocationRepository;
import com.bookeat.restaurant.entity.Restaurant;
import com.bookeat.restaurant.service.*;
import com.bookeat.common.response.*;
import com.bookeat.common.audit.*;
import com.bookeat.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.time.Clock;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
@PreAuthorize("hasRole('OWNER') and hasAuthority('TABLE_MANAGE')")
public class RestaurantTableServiceImpl implements RestaurantTableService {
    private final RestaurantAccessService access;
    private final RestaurantInventoryAccess inventory;
    private final RestaurantChangeService changes;
    private final RestaurantTableRepository tables;
    private final TableAllocationRepository allocations;
    private final TableMapper mapper;
    private final AuditLogRepository audit;
    private final ObjectMapper json;
    private final Clock clock;
    @Override public RestaurantResource<TableResponse> create(UUID actor,UUID restaurantId,TableRequests.Write q) {
        var r=inventory.lockEditable(actor,restaurantId,q.restaurantVersion());
        if(tables.countByRestaurantIdAndDeletedAtIsNull(restaurantId)>=1000)throw new ApiException(ErrorCode.INVENTORY_LIMIT_REACHED);
        var t=new RestaurantTable();t.setRestaurant(r);t.setStatus(TableStatus.AVAILABLE);assign(t,q);tables.save(t);
        return finish(r,actor,t,"TABLE_CREATED");
    }
    @Override @Transactional(readOnly=true)
    public PageResponse<TableResponse> list(UUID actor,UUID restaurantId,int page,int size) {
        access.getOwnedRestaurant(actor,restaurantId);
        return PageResponse.from(tables.findByRestaurantIdAndDeletedAtIsNull(restaurantId,PageRequest.of(page,size,Sort.by("tableNumber").and(Sort.by("id")))).map(mapper::map));
    }
    @Override @Transactional(readOnly=true)
    public TableStatusSummary summary(UUID actor,UUID restaurantId) {
        access.getOwnedRestaurant(actor,restaurantId);
        return new TableStatusSummary(
            tables.countByRestaurantIdAndDeletedAtIsNull(restaurantId),
            tables.countByRestaurantIdAndDeletedAtIsNullAndStatus(restaurantId,TableStatus.AVAILABLE),
            tables.countByRestaurantIdAndDeletedAtIsNullAndStatus(restaurantId,TableStatus.RESERVED),
            tables.countByRestaurantIdAndDeletedAtIsNullAndStatus(restaurantId,TableStatus.OCCUPIED),
            tables.countByRestaurantIdAndDeletedAtIsNullAndStatus(restaurantId,TableStatus.CLEANING),
            tables.countByRestaurantIdAndDeletedAtIsNullAndStatus(restaurantId,TableStatus.BLOCKED),
            tables.countByRestaurantIdAndDeletedAtIsNullAndStatus(restaurantId,TableStatus.MAINTENANCE));
    }
    @Override @Transactional(readOnly=true)
    public RestaurantResource<TableResponse> get(UUID actor,UUID restaurantId,UUID tableId) {
        var r=access.getOwnedRestaurant(actor,restaurantId);return new RestaurantResource<>(r.getVersion(),mapper.map(table(restaurantId,tableId)));
    }
    @Override public RestaurantResource<TableResponse> update(UUID actor,UUID restaurantId,UUID tableId,TableRequests.Write q) {
        var r=inventory.lockEditable(actor,restaurantId,q.restaurantVersion());var t=table(restaurantId,tableId);requireUnused(t);assign(t,q);return finish(r,actor,t,"TABLE_UPDATED");
    }
    @Override public RestaurantResource<UUID> delete(UUID actor,UUID restaurantId,UUID tableId,long restaurantVersion) {
        var r=inventory.lockEditable(actor,restaurantId,restaurantVersion);var t=table(restaurantId,tableId);requireUnused(t);
        t.setActive(false);t.setDeletedAt(clock.instant());changes.finish(r,actor,"TABLE_ARCHIVED");return new RestaurantResource<>(r.getVersion(),t.getId());
    }
    @Override public RestaurantResource<TableResponse> status(UUID actor,UUID restaurantId,UUID tableId,TableRequests.Status q) {
        var r=inventory.lockEditable(actor,restaurantId,q.restaurantVersion());var t=table(restaurantId,tableId);
        if(q.status()==TableStatus.AVAILABLE && !t.isActive())throw new ApiException(ErrorCode.VALIDATION_FAILED);
        // Status changes never cancel timed allocations. Existing reservations stay protected by their intervals.
        var log=new AuditLog();log.setActorId(actor);log.setAction("TABLE_STATUS_CHANGED");log.setEntityType("RestaurantTable");log.setEntityId(t.getId());log.setOldValue(t.getStatus().name());
        try{log.setNewValue(json.writeValueAsString(Map.of("status",q.status().name(),"reason",q.reason())));}catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException("Cannot encode table status audit");}
        audit.save(log);t.setStatus(q.status());return finish(r,actor,t,"TABLE_STATUS_UPDATED");
    }
    private RestaurantTable table(UUID restaurantId,UUID tableId){return tables.findByIdAndRestaurantIdAndDeletedAtIsNull(tableId,restaurantId).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    private void requireUnused(RestaurantTable t){if(allocations.existsByTableIdAndActiveTrueAndEndsAtAfter(t.getId(),clock.instant()))throw new ApiException(ErrorCode.TABLE_IN_USE);}
    private void assign(RestaurantTable t,TableRequests.Write q) {
        if(!TableRules.validCapacity(q.capacity(),q.minimumGuests(),q.maximumGuests()))throw new ApiException(ErrorCode.VALIDATION_FAILED);
        var number=TableRules.normalizeNumber(q.tableNumber());
        if(tables.existsByRestaurantIdAndTableNumberAndDeletedAtIsNullAndIdNot(t.getRestaurant().getId(),number,t.getId()==null?new UUID(0,0):t.getId()))throw new ApiException(ErrorCode.TABLE_NUMBER_EXISTS);
        t.setTableNumber(number);t.setTableName(q.tableName());t.setCapacity(q.capacity());t.setMinimumGuests(q.minimumGuests());t.setMaximumGuests(q.maximumGuests());t.setFloor(q.floor());t.setSection(q.section());t.setShape(q.shape());t.setXPosition(q.xPosition());t.setYPosition(q.yPosition());t.setWidth(q.width());t.setHeight(q.height());t.setRotation(q.rotation());t.setActive(q.active());
    }
    private RestaurantResource<TableResponse> finish(Restaurant r,UUID actor,RestaurantTable t,String action){changes.finish(r,actor,action);return new RestaurantResource<>(r.getVersion(),mapper.map(t));}
}
