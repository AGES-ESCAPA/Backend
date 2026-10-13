package com.escapa.backend.infrastructure.pdf;

import com.escapa.backend.domain.certificate.CertificateTheme;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fontes, logos e CSS do certificado, lidos do classpath uma unica vez no startup.
 *
 * O HTML e carregado pelo Chromium com setContent (origem about:blank) e sem
 * rede, entao caminhos relativos nao funcionam: tudo vai embutido como data: URI.
 */
@Component
public class CertificateAssets {

    private static final String BASE = "certificate/";
    private static final Pattern ASSET_PLACEHOLDER = Pattern.compile("\\{\\{([^}]+)}}");

    private final String stylesheet;
    private final Map<CertificateTheme, String> logos = new EnumMap<>(CertificateTheme.class);

    public CertificateAssets() {
        this.stylesheet = inlineAssets(read("fonts.css"))
                + read("certificate-tokens.css")
                + read("certificate.css");
        logos.put(CertificateTheme.DARK, dataUri("images/escapa-logo.png", "image/png"));
        logos.put(CertificateTheme.LIGHT, dataUri("images/escapa-logo-preto.png", "image/png"));
    }

    /** @font-face, tokens dos temas e layout, nessa ordem. */
    public String stylesheet() {
        return stylesheet;
    }

    /** Logo branco no tema escuro e preto no claro. */
    public String logo(CertificateTheme theme) {
        return logos.get(theme);
    }

    /** Troca cada {{caminho}} do CSS pelo data: URI do arquivo correspondente. */
    private String inlineAssets(String css) {
        final Matcher matcher = ASSET_PLACEHOLDER.matcher(css);
        final StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            final String path = matcher.group(1);
            matcher.appendReplacement(result, Matcher.quoteReplacement(dataUri(path, mediaType(path))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String mediaType(String path) {
        if (path.endsWith(".woff2")) {
            return "font/woff2";
        }
        if (path.endsWith(".png")) {
            return "image/png";
        }
        throw new IllegalArgumentException("Unsupported certificate asset: " + path);
    }

    private static String dataUri(String path, String mediaType) {
        return "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(bytes(path));
    }

    private static String read(String path) {
        return new String(bytes(path), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String path) {
        try (InputStream input = new ClassPathResource(BASE + path).getInputStream()) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Missing certificate asset: " + BASE + path, e);
        }
    }
}
