package com.bookeat.support.repository;
import com.bookeat.support.entity.SupportTicket;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface SupportTicketRepository extends JpaRepository<SupportTicket,UUID> {
Page<SupportTicket> findByCreatedByUser(UUID id,Pageable p);
}
