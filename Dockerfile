# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first so rebuilds are fast
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/live-plane-tracker-*.jar app.jar
USER spring

# Render injects PORT (default 10000); Spring reads it via server.port=${PORT:8080}
ENV PORT=10000
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
EXPOSE 10000

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
