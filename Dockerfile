# Stage 1 — build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy POMs first for dependency caching
COPY pom.xml .
COPY rate-limiter-core/pom.xml ./rate-limiter-core/pom.xml
COPY rate-limiter-service/pom.xml ./rate-limiter-service/pom.xml
COPY rate-limiter-spring-boot-starter/pom.xml ./rate-limiter-spring-boot-starter/pom.xml

# Download dependencies as a separate cached layer
RUN mvn dependency:go-offline -pl rate-limiter-service -am

# Now copy source code
COPY rate-limiter-core ./rate-limiter-core
COPY rate-limiter-service ./rate-limiter-service
COPY rate-limiter-spring-boot-starter ./rate-limiter-spring-boot-starter

# Build
RUN mvn clean package -DskipTests -pl rate-limiter-service -am

# Stage 2 — run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/rate-limiter-service/target/rate-limiter-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]