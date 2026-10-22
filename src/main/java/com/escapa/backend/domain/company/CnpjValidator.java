package com.escapa.backend.domain.company;

/** Valida CNPJ (somente dígitos) pelos dígitos verificadores. */
public final class CnpjValidator {

    private static final int CNPJ_LENGTH = 14;
    private static final int BASE_LENGTH = 12;
    private static final int MODULUS = 11;
    private static final int MIN_REMAINDER_FOR_DIGIT = 2;
    private static final int[] FIRST_DIGIT_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_DIGIT_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
    }

    public static boolean isValid(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{" + CNPJ_LENGTH + "}")) {
            return false;
        }
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }
        final int firstDigit = calculateDigit(cnpj, FIRST_DIGIT_WEIGHTS);
        final int secondDigit = calculateDigit(cnpj, SECOND_DIGIT_WEIGHTS);
        return firstDigit == Character.getNumericValue(cnpj.charAt(BASE_LENGTH))
                && secondDigit == Character.getNumericValue(cnpj.charAt(BASE_LENGTH + 1));
    }

    private static int calculateDigit(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.getNumericValue(cnpj.charAt(i)) * weights[i];
        }
        final int remainder = sum % MODULUS;
        return remainder < MIN_REMAINDER_FOR_DIGIT ? 0 : MODULUS - remainder;
    }
}