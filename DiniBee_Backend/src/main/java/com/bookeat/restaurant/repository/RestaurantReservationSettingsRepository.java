package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantReservationSettings;
public interface RestaurantReservationSettingsRepository extends JpaRepository<RestaurantReservationSettings, UUID> {
    Optional<RestaurantReservationSettings> findByRestaurantId(UUID restaurantId);
}
