package com.escapa.backend.infrastructure.pdf;

import com.escapa.backend.domain.certificate.CertificateGenerationException;
import com.escapa.backend.domain.certificate.CertificateGenerationUnavailableException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Imprime HTML em PDF com o Chromium do Playwright.
 *
 * O Playwright nao e thread-safe: o Playwright, o Browser e tudo o que sai deles
 * so podem ser usados na thread que os criou. Por isso toda chamada passa por um
 * executor de uma unica thread ("playwright-pdf"), com fila limitada. Um unico
 * Browser e reaproveitado, com um BrowserContext e uma Page novos por PDF.
 *
 * A primeira subida do Chromium no container leva dezenas de segundos, mais que
 * o prazo de uma requisicao. Com certificate.pdf.warm-up=true, o navegador sobe
 * em segundo plano assim que a aplicacao fica pronta; senao, so na primeira geracao.
 */
@Component
public class CertificatePdfRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificatePdfRenderer.class);
    private static final String THREAD_NAME = "playwright-pdf";
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final long MILLIS_PER_SECOND = 1000;
    // A pagina desiste antes do prazo da requisicao, para a tarefa terminar sozinha
    // e liberar a thread mesmo quando quem pediu o PDF ja recebeu o timeout.
    private static final double PAGE_TIMEOUT_SHARE = 0.8;
    // Qualquer requisicao de rede: o template e autossuficiente (data: URIs).
    private static final Pattern NETWORK_REQUEST = Pattern.compile("^(https?|wss?|ftp|file)://.*");
    // Pagina minima impressa no aquecimento: a primeira impressao do Chromium tambem
    // e lenta (fontes e pipeline de PDF), nao so a subida do navegador.
    private static final String WARM_UP_HTML = "<!DOCTYPE html><html><body><p>escapa!</p></body></html>";

    private final ThreadPoolExecutor executor;
    private final long timeoutSeconds;
    private final double pageTimeoutMillis;
    private final boolean warmUp;

    // Usados somente na thread THREAD_NAME.
    private Playwright playwright;
    private Browser browser;

    public CertificatePdfRenderer(
            @Value("${certificate.pdf.queue-capacity}") int queueCapacity,
            @Value("${certificate.pdf.timeout-seconds}") long timeoutSeconds,
            @Value("${certificate.pdf.warm-up}") boolean warmUp
    ) {
        this.timeoutSeconds = timeoutSeconds;
        this.warmUp = warmUp;
        this.pageTimeoutMillis = timeoutSeconds * MILLIS_PER_SECOND * PAGE_TIMEOUT_SHARE;
        this.executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                runnable -> {
                    final Thread thread = new Thread(runnable, THREAD_NAME);
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** Lanca o Chromium e imprime uma pagina em segundo plano, na thread dedicada, sem segurar o startup. */
    @EventListener(ApplicationReadyEvent.class)
    void warmUpOnStartup() {
        if (!warmUp) {
            return;
        }
        executor.execute(() -> {
            try {
                renderOnOwnerThread(WARM_UP_HTML);
            } catch (PlaywrightException e) {
                // A aplicacao sobe mesmo assim: a proxima geracao tenta lancar de novo.
                LOGGER.warn("Could not warm up Chromium for certificate PDFs", e);
            }
        });
    }

    /**
     * @throws CertificateGenerationUnavailableException fila cheia ou prazo esgotado (503)
     * @throws CertificateGenerationException falha do navegador ao gerar o PDF (500)
     */
    public byte[] render(String html) {
        final Future<byte[]> future;
        try {
            future = executor.submit(() -> renderOnOwnerThread(html));
        } catch (RejectedExecutionException e) {
            throw new CertificateGenerationUnavailableException("Certificate generation is busy, try again shortly");
        }

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // Sem interromper a thread: o Playwright nao tolera interrupcao no meio de
            // uma chamada. A pagina tem timeout proprio; se a tarefa ainda estiver na
            // fila, o cancel a tira de la.
            future.cancel(false);
            throw new CertificateGenerationUnavailableException("Certificate generation timed out, try again shortly");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(false);
            throw new CertificateGenerationException("Interrupted while generating the certificate PDF", e);
        } catch (ExecutionException e) {
            throw new CertificateGenerationException("Failed to generate the certificate PDF", e.getCause());
        }
    }

    private byte[] renderOnOwnerThread(String html) {
        ensureBrowser();
        try (BrowserContext context = browser.newContext();
             Page page = context.newPage()) {
            context.route(NETWORK_REQUEST, Route::abort);
            page.setDefaultTimeout(pageTimeoutMillis);
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.LOAD));
            // Sem esperar as fontes, o PDF pode sair com a fonte padrao.
            page.evaluate("() => document.fonts.ready.then(() => true)");
            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setLandscape(true)
                    .setPrintBackground(true)
                    .setPreferCSSPageSize(true)
                    .setMargin(new Margin().setTop("0").setRight("0").setBottom("0").setLeft("0")));
        } catch (PlaywrightException e) {
            if (browser == null || !browser.isConnected()) {
                // Navegador caiu: descarta tudo para relancar na proxima geracao.
                closeBrowser();
            }
            throw e;
        }
    }

    private void ensureBrowser() {
        if (browser != null && browser.isConnected()) {
            return;
        }
        closeBrowser();
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
        LOGGER.info("Chromium launched for certificate PDF generation (version {})", browser.version());
    }

    private void closeBrowser() {
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (PlaywrightException e) {
            LOGGER.warn("Failed to close the Chromium browser", e);
        }
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (PlaywrightException e) {
            LOGGER.warn("Failed to close Playwright", e);
        }
        browser = null;
        playwright = null;
    }

    /** Fecha o Browser e o Playwright na thread que os criou, e so depois encerra o executor. */
    @PreDestroy
    void shutdown() {
        try {
            executor.submit(this::closeBrowser).get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (RejectedExecutionException | ExecutionException | TimeoutException e) {
            LOGGER.warn("Could not close Chromium cleanly on shutdown", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
    }
}
