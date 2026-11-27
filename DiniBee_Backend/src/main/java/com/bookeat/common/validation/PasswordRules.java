package com.bookeat.common.validation;
import java.nio.charset.StandardCharsets;

public final class PasswordRules {
    private PasswordRules() {}
    public static boolean valid(String value) {
        return value != null && !value.isBlank() && value.codePointCount(0, value.length()) >= 8
                && value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
