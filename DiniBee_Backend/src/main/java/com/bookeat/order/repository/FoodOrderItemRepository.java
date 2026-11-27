package com.bookeat.order.repository;
import com.bookeat.order.entity.FoodOrderItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem,UUID> {
    List<FoodOrderItem> findByOrderIdOrderByCreatedAtAscIdAsc(UUID id);
}
