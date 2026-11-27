package com.bookeat;
import com.bookeat.common.security.*;
import com.bookeat.common.validation.PasswordRules;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SecurityPrimitivesTest {
    private static final String KEY=Base64.getEncoder().encodeToString(new byte[32]);
    @Test void opaqueTokensAreIndependentAndHashesAreStable() {
        var tokens=new HashSet<String>();
        for(int i=0; i<100; i++) assertThat(tokens.add(SecretTokens.randomToken())).isTrue();
        assertThat(tokens).allMatch(t -> t.matches("[A-Za-z0-9_-]{43}"));
        assertThat(SecretTokens.sha256("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
    @Test void otpDigestIsBoundToTheAccountAndPurpose() {
        byte[] key=SecretTokens.decodeKey(KEY);
        assertThat(SecretTokens.hmac(key,"challenge:EMAIL_VERIFICATION:user-a:123456"))
                .isNotEqualTo(SecretTokens.hmac(key,"challenge:EMAIL_VERIFICATION:user-b:123456"));
        assertThat(SecretTokens.hmac(key,"challenge:EMAIL_VERIFICATION:user-a:123456"))
                .isNotEqualTo(SecretTokens.hmac(key,"challenge:PASSWORD_RESET:user-a:123456"));
        assertThat(SecretTokens.otp()).matches("[0-9]{6}");
    }
    @Test void encryptedOutboxUsesRandomNoncesAndRejectsTampering() {
        var encryption=new EncryptedText(KEY);
        String first=encryption.encrypt("Private reset token");
        String second=encryption.encrypt("Private reset token");
        assertThat(first).isNotEqualTo(second);
        assertThat(encryption.decrypt(first)).isEqualTo("Private reset token");
        byte[] broken=Base64.getDecoder().decode(first); broken[broken.length-1]^=1;
        assertThatThrownBy(() -> encryption.decrypt(Base64.getEncoder().encodeToString(broken))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void malformedKeysAndWrongDecryptionKeysAreRejected() {
        assertThatThrownBy(() -> SecretTokens.decodeKey("not base64")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SecretTokens.decodeKey(Base64.getEncoder().encodeToString(new byte[16]))).isInstanceOf(IllegalArgumentException.class);
        byte[] alternate=new byte[32]; Arrays.fill(alternate,(byte)7);
        String ciphertext=new EncryptedText(KEY).encrypt("private");
        assertThatThrownBy(() -> new EncryptedText(Base64.getEncoder().encodeToString(alternate)).decrypt(ciphertext)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void bcryptInputLimitCountsUtf8Bytes() {
        assertThat(PasswordRules.valid("a".repeat(12))).isTrue();
        assertThat(PasswordRules.valid("a".repeat(72))).isTrue();
        assertThat(PasswordRules.valid("a".repeat(73))).isFalse();
        assertThat(PasswordRules.valid("😀".repeat(18))).isTrue();
        assertThat(PasswordRules.valid("😀".repeat(19))).isFalse();
        assertThat(PasswordRules.valid("short")).isFalse();
        assertThat(PasswordRules.valid(" ".repeat(20))).isFalse();
    }
}
