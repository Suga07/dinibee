package com.bookeat.reservation.repository;
import com.bookeat.reservation.entity.GroupBooking;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface GroupBookingRepository extends JpaRepository<GroupBooking,UUID> {
    Optional<GroupBooking> findByReservationId(UUID id);
    Optional<GroupBooking> findByInviteHash(String hash);
    @Query("select g.restaurantId from GroupBooking g where g.id=:id") Optional<UUID> restaurantId(@Param("id") UUID id);
    @Query("select g.restaurantId from GroupBooking g where g.inviteHash=:hash") Optional<UUID> restaurantIdByInvite(@Param("hash") String hash);
}
