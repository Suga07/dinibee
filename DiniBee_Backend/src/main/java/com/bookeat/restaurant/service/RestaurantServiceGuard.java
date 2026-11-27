package com.bookeat.restaurant.service;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.repository.*;
import com.bookeat.restaurant.mapper.RestaurantMapper;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
/** Future booking/order/queue services must call this guard inside their transaction. */
@Service @RequiredArgsConstructor
public class RestaurantServiceGuard {
    private final RestaurantRepository restaurants;
    private final RestaurantMapper mapper;
    @Transactional(readOnly=true)
    public void requireEnabled(UUID id,ServiceType type) {
        var r=restaurants.findById(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(!mapper.enabledServices(r).contains(type)) throw new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE);
    }
}
