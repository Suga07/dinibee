package com.bookeat.order.repository;
import com.bookeat.order.entity.FoodOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface FoodOrderRepository extends JpaRepository<FoodOrder,UUID> {
    Page<FoodOrder> findByCustomerId(UUID id,Pageable page);
    Page<FoodOrder> findByRestaurantId(UUID id,Pageable page);
    List<FoodOrder> findByReservationId(UUID id);
    List<FoodOrder> findByGroupBookingId(UUID id);
    List<FoodOrder> findByCorporateBookingId(UUID id);
    List<FoodOrder> findByDiniTicketId(UUID id);
    boolean existsByOrderNumber(String orderNumber);
    @Query("select o.restaurantId from FoodOrder o where o.id=:id") Optional<UUID> restaurantId(@Param("id") UUID id);
}
