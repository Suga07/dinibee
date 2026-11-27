package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantServiceActivation;
public interface RestaurantServiceActivationRepository extends JpaRepository<RestaurantServiceActivation, UUID> {
    List<RestaurantServiceActivation> findByRestaurantIdOrderByCreatedAtAscIdAsc(UUID restaurantId);
}
