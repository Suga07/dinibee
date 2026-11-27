package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantPolicy;
public interface RestaurantPolicyRepository extends JpaRepository<RestaurantPolicy, UUID> {
    Optional<RestaurantPolicy> findByRestaurantId(UUID restaurantId);
}
