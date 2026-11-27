package com.bookeat.reservation.service;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class InviteTokens {
    private static final SecureRandom RANDOM=new SecureRandom();private InviteTokens() {}
    public static String issue(){byte[] b=new byte[32];RANDOM.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    public static String hash(String token){if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw new com.bookeat.exception.ApiException(com.bookeat.exception.ErrorCode.INVITE_INVALID);try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
