package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantOnboardingProgress;
public interface RestaurantOnboardingProgressRepository extends JpaRepository<RestaurantOnboardingProgress, UUID> {
    Optional<RestaurantOnboardingProgress> findByRestaurantId(UUID restaurantId);
}
