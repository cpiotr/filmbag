FROM gradle:9.7.1-jdk25-alpine AS build

ARG APP_VERSION=0.0.1
WORKDIR /filmbag
COPY build.gradle settings.gradle gradle.properties ./
COPY src src
# Extracted as a plain jar plus lib/, which the AOT cache needs, split into layers
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle bootJar --no-daemon -Pversion=$APP_VERSION \
    && java -Djarmode=tools -jar build/libs/filmbag-$APP_VERSION.jar extract --layers --destination extracted \
    && mv extracted/application/filmbag-$APP_VERSION.jar extracted/application/filmbag.jar


FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S filmbag && adduser -S -G filmbag filmbag
WORKDIR /app

# Layers ordered from least to most frequently changed
COPY --from=build /filmbag/extracted/dependencies/ ./
COPY --from=build /filmbag/extracted/spring-boot-loader/ ./
COPY --from=build /filmbag/extracted/snapshot-dependencies/ ./
COPY --from=build /filmbag/extracted/application/ ./

# Records the classes a startup loads in a JDK AOT cache, which roughly halves later startups. The training
# start needs no database: it skips migrations and schema validation and exits once the context is ready.
RUN java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh \
        -Dspring.flyway.enabled=false \
        -Dspring.jpa.hibernate.ddl-auto=none \
        -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
        -Dspring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect \
        -jar filmbag.jar

USER filmbag

# Configure the application with Spring environment variables, such as SPRING_DATASOURCE_URL,
# and the JVM with JDK_JAVA_OPTIONS
ENV SERVER_PORT=18080 \
    JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 18080

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s \
    CMD wget -q -O /dev/null "http://localhost:${SERVER_PORT}/resources/actuator/health/readiness" || exit 1

ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-jar", "filmbag.jar"]
