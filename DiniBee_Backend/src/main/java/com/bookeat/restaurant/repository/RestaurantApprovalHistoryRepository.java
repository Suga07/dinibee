package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantApprovalHistory;
public interface RestaurantApprovalHistoryRepository extends JpaRepository<RestaurantApprovalHistory, UUID> {
    org.springframework.data.domain.Page<RestaurantApprovalHistory> findByRestaurantId(UUID restaurantId,org.springframework.data.domain.Pageable pageable);
    List<RestaurantApprovalHistory> findByRestaurantIdOrderByCreatedAtAscIdAsc(UUID restaurantId);
}
