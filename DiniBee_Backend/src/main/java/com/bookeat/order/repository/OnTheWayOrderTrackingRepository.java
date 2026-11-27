package com.bookeat.order.repository;
import com.bookeat.order.entity.OnTheWayOrderTracking;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface OnTheWayOrderTrackingRepository extends JpaRepository<OnTheWayOrderTracking,UUID> {
    Optional<OnTheWayOrderTracking> findByTakeawayOrderId(UUID id);
}
