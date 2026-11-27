package com.bookeat.admin.service;
import com.bookeat.admin.dto.AdminRequests;import com.bookeat.common.audit.AuditService;import com.bookeat.exception.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import org.springframework.jdbc.core.JdbcTemplate;import lombok.RequiredArgsConstructor;import java.util.*;
@Service @RequiredArgsConstructor @Transactional public class PlatformSettingsService {
 private final JdbcTemplate jdbc;private final AuditService audit;private static final Set<String> ALLOWED=Set.of("support.email","support.phone","platform.display-name","terms.url","privacy.url");
 @Transactional(readOnly=true) public List<Map<String,Object>> list(){return jdbc.queryForList("select setting_key,setting_value,updated_at from platform_settings order by setting_key");}
 public void update(UUID actor,String key,AdminRequests.Setting q){if(!ALLOWED.contains(key)||key.endsWith(".url")&&!q.value().matches("https://[^\\s]+"))throw new ApiException(ErrorCode.VALIDATION_FAILED);jdbc.update("insert into platform_settings(setting_key,setting_value,updated_by) values(?,?,?) on conflict(setting_key) do update set setting_value=excluded.setting_value,updated_by=excluded.updated_by,updated_at=CURRENT_TIMESTAMP",key,q.value(),actor);audit.recordReason(actor,"PLATFORM_SETTING_UPDATED","PlatformSetting",null,q.reason());}
}
