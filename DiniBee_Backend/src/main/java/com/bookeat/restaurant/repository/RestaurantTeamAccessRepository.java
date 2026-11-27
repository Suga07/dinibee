package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantTeamAccess;
public interface RestaurantTeamAccessRepository extends JpaRepository<RestaurantTeamAccess, UUID> {
    Optional<RestaurantTeamAccess> findByRestaurantId(UUID restaurantId);
}
