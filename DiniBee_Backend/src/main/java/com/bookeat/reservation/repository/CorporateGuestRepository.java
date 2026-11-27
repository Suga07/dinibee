package com.bookeat.reservation.repository;
import com.bookeat.reservation.entity.CorporateGuest;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface CorporateGuestRepository extends JpaRepository<CorporateGuest,UUID> {
    @Query("select g.corporateBookingId from CorporateGuest g where g.inviteHash=:hash") Optional<UUID> bookingIdByInvite(@Param("hash") String hash);
    List<CorporateGuest> findByCorporateBookingIdOrderByCreatedAtAscIdAsc(UUID id);
    Optional<CorporateGuest> findByInviteHash(String hash);
    Optional<CorporateGuest> findByCorporateBookingIdAndCustomerId(UUID id,UUID customerId);
    long countByCorporateBookingIdAndStatusNot(UUID id,com.bookeat.reservation.enums.GuestStatus status);
}
