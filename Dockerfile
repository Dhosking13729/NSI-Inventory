# Image used from STAGE onward (Docker on AWS EC2). CI builds it on every run to prove it still builds.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.title="nsi-inventory" org.opencontainers.image.revision="${GIT_SHA}"
RUN addgroup -S nsi && adduser -S nsi -G nsi
WORKDIR /app
COPY --from=build /src/target/nsi-inventory.jar app.jar
ENV GIT_SHA=${GIT_SHA} JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
USER nsi
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
