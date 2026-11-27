package com.bookeat.reservation.repository;
import com.bookeat.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface ReservationRepository extends JpaRepository<Reservation,UUID> {
    Page<Reservation> findByCustomerId(UUID id,Pageable p);
    Page<Reservation> findByRestaurantId(UUID id,Pageable p);
    Optional<Reservation> findByIdAndCustomerId(UUID id,UUID customerId);
    @Query("select r.restaurantId from Reservation r where r.id=:id") Optional<UUID> restaurantId(@Param("id") UUID id);
}
