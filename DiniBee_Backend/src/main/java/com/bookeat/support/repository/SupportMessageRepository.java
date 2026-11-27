package com.bookeat.support.repository;
import com.bookeat.support.entity.SupportMessage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface SupportMessageRepository extends JpaRepository<SupportMessage,UUID> {
Page<SupportMessage> findByTicketId(UUID id,Pageable p);
}
