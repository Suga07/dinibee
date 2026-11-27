package com.bookeat.reservation.repository;
import com.bookeat.reservation.entity.CorporateBooking;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface CorporateBookingRepository extends JpaRepository<CorporateBooking,UUID> {
    Page<CorporateBooking> findByOrganizerCustomerId(UUID id,Pageable page);
    List<CorporateBooking> findBySeriesIdOrderByCreatedAtAscIdAsc(UUID id);
    Optional<CorporateBooking> findByReservationId(UUID id);
    @Query("select c.restaurantId from CorporateBooking c where c.id=:id") Optional<UUID> restaurantId(@Param("id") UUID id);
}
