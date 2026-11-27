package com.bookeat.commerce.service;
import com.bookeat.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.*;
import org.springframework.transaction.annotation.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
public class IdempotencyService {
    private final JdbcTemplate jdbc;private final ObjectMapper json;
    public UUID execute(UUID actor,String scope,String key,Object payload,Supplier<UUID> create) {
        if(key==null || !key.matches("[A-Za-z0-9_-]{16,100}"))throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        try {
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(payload)));
            long lock=ByteBuffer.wrap(MessageDigest.getInstance("SHA-256").digest((actor+":"+scope+":"+key).getBytes(StandardCharsets.UTF_8))).getLong();
            jdbc.execute((ConnectionCallback<Void>)con->{try(var s=con.prepareStatement("select pg_advisory_xact_lock(?)")){s.setLong(1,lock);s.execute();}return null;});
            var rows=jdbc.query("select request_hash, resource_id from request_idempotency where actor_id=? and scope=? and request_key=?",(rs,n)->new Stored(rs.getString(1),rs.getObject(2,UUID.class)),actor,scope,key);
            if(!rows.isEmpty()){if(!rows.get(0).hash().equals(hash))throw new ApiException(ErrorCode.IDEMPOTENCY_CONFLICT);return rows.get(0).id();}
            UUID id=create.get();jdbc.update("insert into request_idempotency(actor_id,scope,request_key,request_hash,resource_id) values (?,?,?,?,?)",actor,scope,key,hash,id);return id;
        }catch(ApiException ex){throw ex;}catch(com.fasterxml.jackson.core.JsonProcessingException|java.security.NoSuchAlgorithmException ex){throw new IllegalStateException("Cannot fingerprint request",ex);}
    }
    private record Stored(String hash,UUID id) {}
}
