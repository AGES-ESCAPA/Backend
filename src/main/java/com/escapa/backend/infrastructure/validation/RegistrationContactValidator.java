package com.escapa.backend.infrastructure.validation;

import org.springframework.stereotype.Component;

import com.escapa.backend.application.port.ContactValidatorPort;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Component
public class RegistrationContactValidator implements ContactValidatorPort {

    private static final int PHONE_MIN_DIGITS = 10;
    private static final int PHONE_MAX_DIGITS = 11;

    private final Validator validator;

    public RegistrationContactValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public boolean isValidEmail(String email) {
        return validator.validate(new EmailValue(email)).isEmpty();
    }

    @Override
    public String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.isBlank()) {
            return null;
        }
        final String normalized = phone.replaceAll("\\D", "");
        if (normalized.length() < PHONE_MIN_DIGITS || normalized.length() > PHONE_MAX_DIGITS) {
            throw new IllegalArgumentException("Invalid phone");
        }
        return normalized;
    }

    private record EmailValue(@NotBlank @Email String value) {
    }
}
