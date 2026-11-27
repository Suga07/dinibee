package com.bookeat.reservation.repository;
import com.bookeat.reservation.entity.GroupMember;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface GroupMemberRepository extends JpaRepository<GroupMember,UUID> {
    List<GroupMember> findByGroupBookingIdOrderByJoinedAtAscIdAsc(UUID id);
    Optional<GroupMember> findByGroupBookingIdAndCustomerId(UUID id,UUID customerId);
    long countByGroupBookingIdAndMemberStatus(UUID id,com.bookeat.reservation.enums.MemberStatus status);
}
