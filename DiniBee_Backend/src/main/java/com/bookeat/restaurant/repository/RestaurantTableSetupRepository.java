package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantTableSetup;
public interface RestaurantTableSetupRepository extends JpaRepository<RestaurantTableSetup, UUID> {
    Optional<RestaurantTableSetup> findByRestaurantId(UUID restaurantId);
}
