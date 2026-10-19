# Versao do Playwright: precisa ser a mesma do pom.xml (playwright.version), senao
# a biblioteca nao encontra o Chromium da imagem. O CI confere as duas.
ARG PLAYWRIGHT_VERSION=1.63.0

FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace

ENV MAVEN_OPTS="-Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120"

# Dependencies are resolved before the sources are copied, so editing code
# reuses this layer instead of downloading the whole tree again. O cache mount
# preserva o ~/.m2 mesmo quando um download falha no meio.
COPY pom.xml checkstyle.xml ./
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B dependency:resolve dependency:resolve-plugins

COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -DskipTests clean package

# Imagem oficial do Playwright para Java: ja traz o Chromium na versao da
# biblioteca e as dependencias de sistema dele, para amd64 e arm64. O PDF do
# certificado (US-19) e impresso por esse Chromium.
FROM mcr.microsoft.com/playwright/java:v${PLAYWRIGHT_VERSION}-noble
WORKDIR /app

# A imagem do Playwright vem com Java 25; o projeto roda em Java 21 (pom.xml e CI),
# entao a aplicacao usa o mesmo JRE Temurin 21 da imagem anterior.
COPY --from=eclipse-temurin:21-jre /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="/opt/java/openjdk/bin:${PATH}"

# O navegador ja esta na imagem (PLAYWRIGHT_BROWSERS_PATH=/ms-playwright): a
# aplicacao nunca baixa nada no startup.
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

COPY --from=build --chown=pwuser:pwuser /workspace/target/backend.jar /app/backend.jar

# Usuario sem root que a imagem ja cria. Assim o Chromium roda com sandbox, sem
# precisar de --no-sandbox. O Playwright extrai o driver no /tmp, gravavel por ele.
USER pwuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
