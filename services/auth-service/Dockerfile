# auth-service Dockerfile — Multi-stage build

# Stage 1 — Build
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

# Copier uniquement le pom en premier pour profiter du cache Docker
COPY pom.xml .
COPY ../../pom.xml parent-pom.xml

COPY src ./src

RUN ./mvnw -B package -DskipTests --no-transfer-progress

# Stage 2 — Runtime (image minimale)
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Utilisateur non-root (sécurité)
RUN addgroup -S fintrack && adduser -S fintrack -G fintrack
USER fintrack

# Copier le JAR depuis le stage builder
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
