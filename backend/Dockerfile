# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S scribemed && adduser -S scribemed -G scribemed

WORKDIR /app

ENV SPRING_DOCKER_COMPOSE_ENABLED=false \
    SCRIBEMED_AUDIO_LOCAL_STORAGE_DIR=/app/var/audio

RUN mkdir -p /app/var/audio && chown -R scribemed:scribemed /app

COPY --from=build --chown=scribemed:scribemed /workspace/target/backend-*.jar /app/app.jar

USER scribemed

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
