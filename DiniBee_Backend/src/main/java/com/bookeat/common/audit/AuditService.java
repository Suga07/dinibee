package com.bookeat.common.audit;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import java.util.UUID;
@Service @RequiredArgsConstructor public class AuditService {
 private final AuditLogRepository logs;private final java.util.List<AuditListener> listeners;
 @Transactional(propagation=Propagation.MANDATORY) public void record(UUID actor,String action,String type,UUID entity,String ip){save(actor,action,type,entity,ip,null);}
 @Transactional(propagation=Propagation.MANDATORY) public void recordReason(UUID actor,String action,String type,UUID entity,String reason){save(actor,action,type,entity,null,reason);}
 private void save(UUID actor,String action,String type,UUID entity,String ip,String reason){AuditLog log=new AuditLog();log.setActorId(actor);log.setAction(action);log.setEntityType(type);log.setEntityId(entity);log.setIpAddress(ip);log.setReason(reason);logs.saveAndFlush(log);listeners.forEach(l->l.recorded(actor,action,type,entity,log.getId()));}
}
