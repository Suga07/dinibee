package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantSettlementProfile;
public interface RestaurantSettlementProfileRepository extends JpaRepository<RestaurantSettlementProfile, UUID> {
    Optional<RestaurantSettlementProfile> findByRestaurantId(UUID restaurantId);
}
