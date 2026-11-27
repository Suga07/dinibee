package com.bookeat.notification.mapper;
import com.bookeat.notification.entity.Notification;import com.bookeat.notification.dto.NotificationResponse;import org.springframework.stereotype.Component;
@Component public class NotificationMapper {public NotificationResponse view(Notification n){return new NotificationResponse(n.getId(),n.getTitle(),n.getMessage(),n.getNotificationType(),n.getReferenceType(),n.getReferenceId(),n.isReadFlag(),n.getCreatedAt());}}
