package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantTakeawaySettings;
public interface RestaurantTakeawaySettingsRepository extends JpaRepository<RestaurantTakeawaySettings, UUID> {
    Optional<RestaurantTakeawaySettings> findByRestaurantId(UUID restaurantId);
}
