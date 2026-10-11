# syntax=docker/dockerfile:1

# ---- Stage 1: build the fat jar and split it into layers ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q clean package -DskipTests \
    && java -Djarmode=tools -jar target/ecommerce-api-*.jar extract --destination /workspace/extracted

# ---- Stage 2: JDK 25 AOT cache (JEP 483/514/515) trained on a context-refresh run ----
# Must use the same JRE and the same /app layout as the runtime stage, or the JVM rejects the cache.
FROM eclipse-temurin:25-jre AS aot
WORKDIR /app
COPY --from=build /workspace/extracted/ ./

# Training run: boot the context and exit, without needing a database
# (Hikari lazy-fails, Hibernate gets its dialect from config instead of JDBC metadata).
RUN java -XX:AOTCacheOutput=/app/app.aot \
    -Dspring.context.exit=onRefresh \
    -Dspring.datasource.hikari.initialization-fail-timeout=-1 \
    -Dspring.jpa.hibernate.ddl-auto=none \
    -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
    -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
    -jar ecommerce-api-0.0.1-SNAPSHOT.jar

# ---- Stage 3: runtime ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

COPY --from=aot --chown=app:app /app/ ./

USER app
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:AOTCache=/app/app.aot -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "ecommerce-api-0.0.1-SNAPSHOT.jar"]
