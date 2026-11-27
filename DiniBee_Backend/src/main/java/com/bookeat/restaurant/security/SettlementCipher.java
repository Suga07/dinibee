package com.bookeat.restaurant.security;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
/** Separate, versioned AES-GCM domain; restaurant ID prevents ciphertext substitution. */
public final class SettlementCipher {
    private final SecretKeySpec key;
    private final SecureRandom random=new SecureRandom();
    public SettlementCipher(String encodedKey) {
        byte[] bytes=Base64.getDecoder().decode(encodedKey);
        if(bytes.length!=32) throw new IllegalArgumentException("Settlement key must contain 32 bytes");
        key=new SecretKeySpec(bytes,"AES");
        Arrays.fill(bytes,(byte)0);
    }
    public String encrypt(UUID restaurantId,String account) {
        try {
            byte[] iv=new byte[12]; random.nextBytes(iv);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv));
            cipher.updateAAD(aad(restaurantId));
            byte[] encrypted=cipher.doFinal(account.getBytes(StandardCharsets.UTF_8));
            return "v1:"+Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length+encrypted.length).put(iv).put(encrypted).array());
        } catch(GeneralSecurityException ex) {throw new IllegalStateException("Settlement encryption failed");}
    }
    public String decrypt(UUID restaurantId,String encrypted) {
        try {
            if(!encrypted.startsWith("v1:")) throw new IllegalArgumentException("Invalid ciphertext version");
            byte[] bytes=Base64.getDecoder().decode(encrypted.substring(3));
            if(bytes.length<29) throw new IllegalArgumentException("Invalid ciphertext length");
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(bytes,0,12)));
            cipher.updateAAD(aad(restaurantId));
            return new String(cipher.doFinal(Arrays.copyOfRange(bytes,12,bytes.length)),StandardCharsets.UTF_8);
        } catch(GeneralSecurityException | IllegalArgumentException ex) {throw new IllegalStateException("Settlement decryption failed");}
    }
    private byte[] aad(UUID id) {return ("bookeat:settlement:v1:"+Objects.requireNonNull(id)).getBytes(StandardCharsets.UTF_8);}
}
