package com.bookeat.table.service;
import com.bookeat.table.dto.*;
import com.bookeat.common.response.*;
import java.util.UUID;
public interface RestaurantTableService {
    RestaurantResource<TableResponse> create(UUID actor,UUID restaurantId,TableRequests.Write request);
    PageResponse<TableResponse> list(UUID actor,UUID restaurantId,int page,int size);
    RestaurantResource<TableResponse> get(UUID actor,UUID restaurantId,UUID tableId);
    TableStatusSummary summary(UUID actor,UUID restaurantId);
    RestaurantResource<TableResponse> update(UUID actor,UUID restaurantId,UUID tableId,TableRequests.Write request);
    RestaurantResource<UUID> delete(UUID actor,UUID restaurantId,UUID tableId,long restaurantVersion);
    RestaurantResource<TableResponse> status(UUID actor,UUID restaurantId,UUID tableId,TableRequests.Status request);
}
