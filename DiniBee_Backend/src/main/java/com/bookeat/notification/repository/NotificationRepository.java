package com.bookeat.notification.repository;
import com.bookeat.notification.entity.Notification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface NotificationRepository extends JpaRepository<Notification,UUID> {
Page<Notification> findByUserId(UUID id,Pageable p);
}
