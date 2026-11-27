package com.bookeat.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.HexFormat;

/** Pure Java cryptographic helpers; no raw secrets are persisted. */
public final class SecretTokens {
    private static final SecureRandom RANDOM = new SecureRandom();
    private SecretTokens() {}
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public static String otp() { return String.format(java.util.Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000)); }
    public static String otp4() { return String.format(java.util.Locale.ROOT, "%04d", RANDOM.nextInt(10_000)); }
    public static byte[] decodeKey(String base64) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(base64); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Secret must be Base64 encoded"); }
        if (bytes.length != 32) throw new IllegalArgumentException("Secret must contain exactly 32 random bytes");
        return bytes;
    }
    public static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 is unavailable"); }
    }
    public static String hmac(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) { throw new IllegalStateException("HMAC is unavailable"); }
    }
    public static boolean equal(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
