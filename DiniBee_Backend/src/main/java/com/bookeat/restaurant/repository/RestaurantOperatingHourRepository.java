package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantOperatingHour;
public interface RestaurantOperatingHourRepository extends JpaRepository<RestaurantOperatingHour, UUID> {
    List<RestaurantOperatingHour> findByRestaurantIdOrderByCreatedAtAscIdAsc(UUID restaurantId);
    void deleteByRestaurantId(UUID restaurantId);
}
