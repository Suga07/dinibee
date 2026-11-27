package com.bookeat.notification.dto;
import java.util.UUID;import java.time.Instant;
public record NotificationResponse(UUID id,String title,String message,String notificationType,String referenceType,UUID referenceId,boolean read,Instant createdAt){}
