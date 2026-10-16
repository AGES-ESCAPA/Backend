package com.escapa.backend.domain.user;

public final class CpfValidator {

    private static final int CPF_LENGTH = 11;
    private static final int FIRST_CHECK_DIGIT_INDEX = 9;
    private static final int CHECK_DIGIT_BASE = 11;

    private CpfValidator() {
    }

    public static boolean isValid(String value) {
        if (value == null || !value.matches("[0-9.\\-\\s]+")) {
            return false;
        }
        final String cpf = digitsOnly(value);
        if (cpf.length() != CPF_LENGTH || allDigitsAreEqual(cpf)) {
            return false;
        }
        return digitAt(cpf, FIRST_CHECK_DIGIT_INDEX) == calculateDigit(cpf, FIRST_CHECK_DIGIT_INDEX)
                && digitAt(cpf, 10) == calculateDigit(cpf, 10);
    }

    public static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static boolean allDigitsAreEqual(String cpf) {
        return cpf.chars().allMatch(character -> character == cpf.charAt(0));
    }

    private static int calculateDigit(String cpf, int digitIndex) {
        int sum = 0;
        for (int index = 0; index < digitIndex; index++) {
            final int weight = digitIndex + 1 - index;
            sum += digitAt(cpf, index) * weight;
        }
        final int result = CHECK_DIGIT_BASE - sum % CHECK_DIGIT_BASE;
        return result >= 10 ? 0 : result;
    }

    private static int digitAt(String cpf, int index) {
        return Character.digit(cpf.charAt(index), 10);
    }
}
