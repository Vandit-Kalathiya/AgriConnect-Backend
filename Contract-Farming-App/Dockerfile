FROM maven:3-eclipse-temurin-24 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:resolve -B -q

COPY src/ src/
RUN mvn package -DskipTests -B -q

FROM eclipse-temurin:24-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=build /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 2526

HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=40s \
  CMD curl -f http://localhost:2526/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:+ZGenerational", \
  "-Xms256m", \
  "-Xmx512m", \
  "-XX:MaxGCPauseMillis=100", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseStringDeduplication", \
  "-Djdk.tracePinnedThreads=full", \
  "-Dspring.threads.virtual.enabled=true", \
  "-jar", "app.jar"]
