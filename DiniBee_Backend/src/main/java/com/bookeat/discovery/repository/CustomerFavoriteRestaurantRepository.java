package com.bookeat.discovery.repository;
import com.bookeat.discovery.entity.CustomerFavoriteRestaurant;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface CustomerFavoriteRestaurantRepository extends JpaRepository<CustomerFavoriteRestaurant,UUID> {
    Optional<CustomerFavoriteRestaurant> findByCustomerIdAndRestaurantId(UUID customerId,UUID restaurantId);
    Page<CustomerFavoriteRestaurant> findByCustomerId(UUID customerId,Pageable pageable);
}
