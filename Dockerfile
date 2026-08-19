FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace

# Dependencies are resolved before the sources are copied, so editing code
# reuses this layer instead of downloading the whole tree again.
COPY pom.xml checkstyle.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system --gid 1001 escapa \
    && useradd --system --uid 1001 --gid escapa escapa

COPY --from=build --chown=escapa:escapa /workspace/target/backend.jar /app/backend.jar

USER escapa
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
