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

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system --gid 1001 escapa \
    && useradd --system --uid 1001 --gid escapa escapa

COPY --from=build --chown=escapa:escapa /workspace/target/backend.jar /app/backend.jar

USER escapa
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
