package com.bookeat.order.repository;
import com.bookeat.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory,UUID> {
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAscIdAsc(UUID id);
}
