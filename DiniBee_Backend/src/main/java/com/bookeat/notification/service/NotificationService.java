package com.bookeat.notification.service;
import com.bookeat.notification.dto.NotificationResponse;import com.bookeat.notification.repository.NotificationRepository;import com.bookeat.notification.mapper.NotificationMapper;import com.bookeat.common.response.PageResponse;import com.bookeat.exception.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import org.springframework.data.domain.*;import org.springframework.jdbc.core.JdbcTemplate;import lombok.RequiredArgsConstructor;import java.util.*;
@Service @RequiredArgsConstructor @Transactional public class NotificationService {
 private final NotificationRepository notifications;private final NotificationMapper mapper;private final JdbcTemplate jdbc;
 @Transactional(readOnly=true) public PageResponse<NotificationResponse> list(UUID actor,int page,int size){return PageResponse.from(notifications.findByUserId(actor,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"))).map(mapper::view));}
 public void read(UUID actor,UUID id){if(jdbc.update("update notifications set read_flag=true,version=version+1,updated_at=CURRENT_TIMESTAMP where id=? and user_id=?",id,actor)!=1)throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);}
 public void readAll(UUID actor){jdbc.update("update notifications set read_flag=true,version=version+1,updated_at=CURRENT_TIMESTAMP where user_id=? and not read_flag",actor);}
 @Transactional public int broadcast(UUID actor,String title,String message,String type,String audience,List<UUID> recipientIds){
  List<UUID> users;
  if(recipientIds!=null&&!recipientIds.isEmpty()) users=recipientIds;
  else if("ALL_CUSTOMERS".equals(audience)) users=jdbc.query("select distinct u.id from users u join user_roles ur on ur.user_id=u.id join roles r on r.id=ur.role_id where r.code='CUSTOMER' and u.status='ACTIVE'",(r,n)->r.getObject(1,UUID.class));
  else if("ALL_RESTAURANTS".equals(audience)) users=jdbc.query("select distinct u.id from users u join user_roles ur on ur.user_id=u.id join roles r on r.id=ur.role_id where r.code='OWNER' and u.status='ACTIVE'",(r,n)->r.getObject(1,UUID.class));
  else users=List.of();
  int count=0; UUID ref=UUID.randomUUID();
  for(UUID user:users){String dedup="ADMIN:"+ref+":"+user;if(jdbc.update("insert into notifications(id,user_id,title,message,notification_type,reference_type,reference_id,read_flag,deduplication_key) values(?,?,?,?,?,'ADMIN_BROADCAST',?,false,?) on conflict(user_id,deduplication_key) do nothing",UUID.randomUUID(),user,title,message,type,ref,dedup)>0)count++;}
  return count;
 }
}
