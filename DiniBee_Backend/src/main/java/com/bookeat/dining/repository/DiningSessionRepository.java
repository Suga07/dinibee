package com.bookeat.dining.repository;
import com.bookeat.dining.entity.DiningSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface DiningSessionRepository extends JpaRepository<DiningSession,UUID> {
Optional<DiningSession> findByReservationId(UUID id);
Optional<DiningSession> findByDiniTicketId(UUID id);
List<DiningSession> findByCustomerIdAndStatusNotIn(UUID id,Collection<com.bookeat.dining.enums.DiningStatus> statuses);
Page<DiningSession> findByRestaurantId(UUID id,Pageable p);
@Query("select d.restaurantId from DiningSession d where d.id=:id") Optional<UUID> restaurantId(@Param("id") UUID id);
}
