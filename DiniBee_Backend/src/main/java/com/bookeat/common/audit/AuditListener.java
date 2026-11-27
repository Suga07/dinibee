package com.bookeat.common.audit;
import java.util.UUID;
public interface AuditListener {void recorded(UUID actor,String action,String entityType,UUID entityId,UUID auditId);}
