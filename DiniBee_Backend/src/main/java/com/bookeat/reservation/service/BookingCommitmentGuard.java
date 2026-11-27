package com.bookeat.reservation.service;
import com.bookeat.reservation.entity.Reservation;
import java.util.UUID;
import java.math.BigDecimal;
/** Extensions participate in the same restaurant-locked booking transaction. */
public interface BookingCommitmentGuard {
    void beforeReschedule(Reservation reservation);
    void beforeCancel(Reservation reservation,UUID actor);
    void beforeMemberRemoval(UUID groupId,UUID customerId);
    void checkCorporateBudget(UUID corporateId,BigDecimal budget);
}
