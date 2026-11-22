package com.escapa.backend.application.usecase;

import java.util.Locale;

import com.escapa.backend.application.port.ContactValidatorPort;
import com.escapa.backend.domain.company.JobTitleNormalizer;

/**
 * Validação dos dados de um colaborador, a mesma no cadastro (US-31) e na edição (US-32). Devolve os
 * valores já normalizados: sem espaços nas pontas e, no e-mail, em minúsculas.
 */
class EmployeeDataValidator {

    private final ContactValidatorPort contactValidatorPort;

    EmployeeDataValidator(ContactValidatorPort contactValidatorPort) {
        this.contactValidatorPort = contactValidatorPort;
    }

    String requireName(String name) {
        return requireText(name, "Name", "name");
    }

    String requireJobTitle(String jobTitle) {
        return JobTitleNormalizer.display(requireText(jobTitle, "Job title", "jobTitle"));
    }

    String requireValidEmail(String email) {
        final String clean = requireText(email, "Email", "email").toLowerCase(Locale.ROOT);
        if (!contactValidatorPort.isValidEmail(clean)) {
            throw new IllegalArgumentException("Invalid email");
        }
        return clean;
    }

    private static String requireText(String value, String label, String field) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        final String clean = value.trim();
        if (clean.length() > RegisterStudentUseCase.MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException(
                    field + " must have at most " + RegisterStudentUseCase.MAX_FIELD_LENGTH + " characters");
        }
        return clean;
    }
}
