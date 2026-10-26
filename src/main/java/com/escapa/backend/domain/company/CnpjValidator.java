package com.escapa.backend.domain.company;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Valida CNPJ numerico ou alfanumerico (formato em vigor desde julho/2026) pelos
 * digitos verificadores. As 12 primeiras posicoes aceitam letras maiusculas e
 * numeros; as 2 ultimas (digitos verificadores) sao sempre numeros.
 */
public final class CnpjValidator {

    private static final int BASE_LENGTH = 12;
    private static final int MODULUS = 11;
    private static final int MIN_REMAINDER_FOR_DIGIT = 2;
    private static final int[] FIRST_DIGIT_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_DIGIT_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final Pattern CNPJ_PATTERN = Pattern.compile("[0-9A-Z]{12}[0-9]{2}");
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^0-9A-Za-z]");

    private CnpjValidator() {
    }

    /** Remove a pontuacao e converte as letras para maiusculas (nao valida nem trunca). */
    public static String normalize(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        return NOT_ALPHANUMERIC.matcher(cnpj).replaceAll("").toUpperCase(Locale.ROOT);
    }

    /** Valida um CNPJ ja normalizado (sem pontuacao e com letras maiusculas). */
    public static boolean isValid(String cnpj) {
        if (cnpj == null || !CNPJ_PATTERN.matcher(cnpj).matches()) {
            return false;
        }
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }
        final int firstDigit = calculateDigit(cnpj, FIRST_DIGIT_WEIGHTS);
        final int secondDigit = calculateDigit(cnpj, SECOND_DIGIT_WEIGHTS);
        return firstDigit == charValue(cnpj.charAt(BASE_LENGTH))
                && secondDigit == charValue(cnpj.charAt(BASE_LENGTH + 1));
    }

    private static int calculateDigit(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += charValue(cnpj.charAt(i)) * weights[i];
        }
        final int remainder = sum % MODULUS;
        return remainder < MIN_REMAINDER_FOR_DIGIT ? 0 : MODULUS - remainder;
    }

    /**
     * Numero vale ele mesmo; letra vale o codigo ASCII menos 48 (A = 17, B = 18...).
     * Nao usar Character.getNumericValue: ele devolve 10 para 'A'.
     */
    private static int charValue(char character) {
        return character - '0';
    }
}
