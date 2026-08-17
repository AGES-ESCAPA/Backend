FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml checkstyle.xml ./
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar /app/backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
