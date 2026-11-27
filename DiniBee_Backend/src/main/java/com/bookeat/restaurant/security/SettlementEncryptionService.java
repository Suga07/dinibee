package com.bookeat.restaurant.security;
import com.bookeat.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service
public class SettlementEncryptionService {
    private static final String DEFAULT_DEV_KEY = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private final SettlementCipher cipher;
    public SettlementEncryptionService(@Value("${app.settlement.encryption-key:}") String key) {
        String effectiveKey = key.isBlank() ? DEFAULT_DEV_KEY : key;
        cipher = new SettlementCipher(effectiveKey);
    }
    public String encrypt(UUID id,String account) {
        return cipher.encrypt(id,account);
    }
}
