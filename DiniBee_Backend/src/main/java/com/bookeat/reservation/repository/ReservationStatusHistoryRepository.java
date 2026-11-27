package com.bookeat.reservation.repository;
import com.bookeat.reservation.entity.ReservationStatusHistory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface ReservationStatusHistoryRepository extends JpaRepository<ReservationStatusHistory,UUID> {
    List<ReservationStatusHistory> findByReservationIdOrderByChangedAtAscIdAsc(UUID id);
}
