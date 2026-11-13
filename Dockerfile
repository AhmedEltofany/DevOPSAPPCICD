# syntax=docker/dockerfile:1
#
# Multi-stage build: compiles the code INSIDE Docker, so the only thing you need
# installed is Docker itself.
#
#   docker build -t devopsappcicd .
#   docker run --rm -p 8080:8080 devopsappcicd
#   open http://localhost:8080/
#
# Pipelines that already ran `mvn package` use AzureDockerfile instead (no second compile).

############################
# Stage 1: build the WAR
############################
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

# Copy the POMs first: Docker caches this layer, so dependencies are only
# re-downloaded when a pom.xml changes, not on every code change.
COPY pom.xml .
COPY server/pom.xml server/
COPY webapp/pom.xml webapp/

COPY server/src server/src
COPY webapp/src webapp/src

ARG GIT_COMMIT=docker
ARG SKIP_TESTS=false
# The cache mount keeps ~/.m2 between builds (BuildKit), which makes rebuilds much faster.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp package -DskipTests=${SKIP_TESTS} -Dbuild.commit=${GIT_COMMIT}

############################
# Stage 2: runtime image
############################
FROM tomcat:10.1-jre17-temurin

LABEL org.opencontainers.image.title="devopsappcicd" \
      org.opencontainers.image.description="Sample Java web app for learning CI/CD" \
      org.opencontainers.image.source="https://github.com/AhmedEltofany/DevOPSAPPCICD"

# curl is only used by HEALTHCHECK.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Run as an unprivileged user with a fixed numeric UID (Kubernetes runAsNonRoot needs a number).
RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home app \
 && rm -rf /usr/local/tomcat/webapps/* \
 && chown -R app:app /usr/local/tomcat

# Deploy as ROOT.war so the app is served at "/" instead of "/webapp".
COPY --from=build --chown=app:app /src/webapp/target/webapp.war /usr/local/tomcat/webapps/ROOT.war

# Let the JVM size its heap from the container memory limit.
ENV CATALINA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    APP_ENV=docker

USER 10001
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/health/live || exit 1

CMD ["catalina.sh", "run"]
