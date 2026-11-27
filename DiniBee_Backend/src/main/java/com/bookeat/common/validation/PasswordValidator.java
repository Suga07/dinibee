package com.bookeat.common.validation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    @Override public boolean isValid(String value, ConstraintValidatorContext context) { return PasswordRules.valid(value); }
}
