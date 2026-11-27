package com.bookeat.common.security;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/** AES-256-GCM for short-lived mail outbox bodies containing verification secrets. */
public final class EncryptedText {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final byte[] AAD = "bookeat-mail-v1".getBytes(StandardCharsets.UTF_8);
    private final SecretKeySpec key;
    public EncryptedText(String base64Key) { key = new SecretKeySpec(SecretTokens.decodeKey(base64Key), "AES"); }
    public String encrypt(String text) {
        try {
            byte[] iv = new byte[12]; RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(AAD);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException ex) { throw new IllegalStateException("Cannot encrypt mail"); }
    }
    public String decrypt(String encoded) {
        try {
            ByteBuffer input = ByteBuffer.wrap(Base64.getDecoder().decode(encoded));
            if (input.remaining() < 28) throw new IllegalArgumentException("Invalid encrypted mail");
            byte[] iv = new byte[12]; input.get(iv);
            byte[] encrypted = new byte[input.remaining()]; input.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(AAD);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) { throw new IllegalArgumentException("Invalid encrypted mail"); }
    }
}
