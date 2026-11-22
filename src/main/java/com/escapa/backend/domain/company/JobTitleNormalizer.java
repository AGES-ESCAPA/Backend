package com.escapa.backend.domain.company;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normaliza o cargo ({@code users_company.role}) usado como departamento no dashboard.
 * A chave de comparação ignora acentos, caixa e espaços extras; o texto de exibição
 * fica em Title Case e sem espaços duplicados.
 */
public final class JobTitleNormalizer {

    private JobTitleNormalizer() {
    }

    /** Forma canônica para comparar e agrupar (ex.: {@code "Recepção"} e {@code "RECEPCAO"} → mesma chave). */
    public static String key(String raw) {
        if (raw == null) {
            return "";
        }
        return stripAccents(collapseSpaces(raw)).toUpperCase(Locale.ROOT);
    }

    /** Texto gravado/exibido: trim, espaços colapsados e Title Case preservando acentos. */
    public static String display(String raw) {
        if (raw == null) {
            return "";
        }
        final String collapsed = collapseSpaces(raw);
        if (collapsed.isEmpty()) {
            return collapsed;
        }
        final StringBuilder result = new StringBuilder(collapsed.length());
        for (final String word : collapsed.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            final int first = word.offsetByCodePoints(0, 1);
            result.appendCodePoint(Character.toUpperCase(word.codePointAt(0)));
            if (first < word.length()) {
                result.append(word.substring(first).toLowerCase(Locale.ROOT));
            }
        }
        return result.toString();
    }

    /**
     * Se a empresa já tem um cargo com a mesma chave, reutiliza o rótulo existente
     * (evita duplicar "RECEPCAO" e "Recepção" no gráfico).
     */
    public static String resolveForCompany(String requested, Iterable<String> existingTitles) {
        final String normalized = display(requested);
        final String requestedKey = key(normalized);
        if (requestedKey.isEmpty()) {
            return normalized;
        }
        String preferred = null;
        for (final String existing : existingTitles) {
            if (!requestedKey.equals(key(existing))) {
                continue;
            }
            preferred = preferred == null ? existing : preferLabel(preferred, existing);
        }
        return preferred != null ? preferred : normalized;
    }

    public static String preferLabel(String left, String right) {
        final boolean leftAccent = hasAccent(left);
        final boolean rightAccent = hasAccent(right);
        if (leftAccent != rightAccent) {
            return leftAccent ? left : right;
        }
        final boolean leftAllCaps = left.equals(left.toUpperCase(Locale.ROOT));
        final boolean rightAllCaps = right.equals(right.toUpperCase(Locale.ROOT));
        if (leftAllCaps != rightAllCaps) {
            return leftAllCaps ? right : left;
        }
        return left.compareToIgnoreCase(right) <= 0 ? left : right;
    }

    private static boolean hasAccent(String value) {
        return !stripAccents(value).equals(value);
    }

    private static String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private static String collapseSpaces(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }
}
