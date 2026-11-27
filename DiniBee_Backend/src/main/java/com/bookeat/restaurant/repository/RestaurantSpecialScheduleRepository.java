package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantSpecialSchedule;
public interface RestaurantSpecialScheduleRepository extends JpaRepository<RestaurantSpecialSchedule, UUID> {
    List<RestaurantSpecialSchedule> findByRestaurantIdOrderByCreatedAtAscIdAsc(UUID restaurantId);
    void deleteByRestaurantId(UUID restaurantId);
}
