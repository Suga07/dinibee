package com.bookeat.restaurant.service;
import com.bookeat.restaurant.entity.Restaurant;
import com.bookeat.restaurant.repository.RestaurantRepository;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class RestaurantAccessService {
    private final RestaurantRepository restaurants;
    @Transactional(readOnly=true)
    public Restaurant getOwnedRestaurant(UUID userId,UUID restaurantId) {
        return restaurants.findByIdAndOwnerUserId(restaurantId,userId).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    }
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Restaurant lockOwnedRestaurant(UUID userId,UUID restaurantId,long version) {
        Restaurant r=restaurants.lockOwned(restaurantId,userId).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        checkVersion(r,version); return r;
    }
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Restaurant lockForReview(UUID restaurantId,long version) {
        Restaurant r=restaurants.lockById(restaurantId).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        checkVersion(r,version); return r;
    }
    private void checkVersion(Restaurant r,long expected) {
        // if(r.getVersion()!=expected) throw new ApiException(ErrorCode.STALE_RESTAURANT_VERSION);
    }
}
