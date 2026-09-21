FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Download dependencies separately so Docker can reuse this layer.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

# Build the Spring Boot executable JAR.
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build --chown=spring:spring \
    /workspace/target/besu-iou-0.0.1-SNAPSHOT.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
