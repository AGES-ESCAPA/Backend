package com.escapa.backend.infrastructure.validation;

import java.text.Normalizer;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.escapa.backend.application.port.ContactValidatorPort;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Component
public class RegistrationContactValidator implements ContactValidatorPort {

    private static final String DEFAULT_REGION = "BR";
    private static final Pattern PHONE_FORMAT = Pattern.compile(
            "[+\\p{Nd}()./\\s-]+(?:(?:ext\\.?|x|ramal|;ext=)\\s*\\p{Nd}+)?", Pattern.CASE_INSENSITIVE);

    private final Validator validator;
    private final PhoneNumberUtil phoneNumbers = PhoneNumberUtil.getInstance();

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
        final String input = Normalizer.normalize(phone, Normalizer.Form.NFKC)
                .replaceAll("\\p{Z}", " ").replaceAll("\\p{Pd}", "-").trim();
        if (input.isBlank()) {
            return null;
        }
        if (!PHONE_FORMAT.matcher(input).matches()) {
            throw new IllegalArgumentException("Invalid phone");
        }
        final String withExtension = input.replaceFirst("(?i)ramal\\s*", "ext ");
        final String internationalInput = withExtension.startsWith("00")
                ? "+" + withExtension.substring(2) : withExtension;
        try {
            final PhoneNumber number = phoneNumbers.parse(internationalInput, DEFAULT_REGION);
            if (!phoneNumbers.isValidNumber(number)) {
                throw new IllegalArgumentException("Invalid phone");
            }
            final String normalized = phoneNumbers.format(number, PhoneNumberFormat.E164);
            return number.hasExtension() ? normalized + ";ext=" + number.getExtension() : normalized;
        } catch (NumberParseException exception) {
            throw new IllegalArgumentException("Invalid phone", exception);
        }
    }

    private record EmailValue(@NotBlank @Email String value) {
    }
}
