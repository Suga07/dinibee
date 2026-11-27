package com.bookeat.support.dto;
import java.util.UUID;import java.time.Instant;import com.bookeat.support.enums.*;
public final class SupportResponses {
 private SupportResponses(){}
 public record Ticket(UUID id,long version,String ticketNumber,CreatorType creatorType,String category,String subject,String description,SupportPriority priority,SupportStatus status,UUID assignedAdmin,Instant createdAt,Instant resolvedAt){}
 public record Message(UUID id,boolean fromSupport,String message,String attachmentUrl,Instant createdAt){}
}
