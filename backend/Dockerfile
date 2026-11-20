# Multi-stage build: compile with a full JDK, run on a slim JRE. Built and tested against the
# same JDK 25 / Maven 3.9 combination this project has been developed against locally (see
# backend/README.md) - not assumed compatible with other versions.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Maven isn't preinstalled on the temurin base images - pull the same binary distribution used
# for local development instead of relying on whatever version a package manager happens to carry.
# archive.apache.org (not dlcdn.apache.org, which only mirrors the last couple of releases and
# 404s on anything older - caught in CI when 3.9.9 aged out of it) keeps every release forever.
ARG MAVEN_VERSION=3.9.16
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
       -o /tmp/maven.tar.gz \
    && tar -xzf /tmp/maven.tar.gz -C /opt \
    && ln -s "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn \
    && rm -rf /tmp/maven.tar.gz /var/lib/apt/lists/*

# Copy every module's pom.xml first (source unchanged) so `mvn -q dependency:go-offline` is cached
# as its own Docker layer - only re-runs when a pom.xml actually changes, not on every source edit.
COPY pom.xml ./
COPY common/pom.xml common/
COPY module-identity/pom.xml module-identity/
COPY module-school/pom.xml module-school/
COPY module-people/pom.xml module-people/
COPY module-tracking/pom.xml module-tracking/
COPY module-notification/pom.xml module-notification/
COPY module-insurance/pom.xml module-insurance/
COPY module-payment/pom.xml module-payment/
COPY module-grievance/pom.xml module-grievance/
COPY module-audit/pom.xml module-audit/
COPY module-geo/pom.xml module-geo/
COPY module-attendance/pom.xml module-attendance/
COPY module-hardware/pom.xml module-hardware/
COPY app/pom.xml app/
RUN mvn -q -B dependency:go-offline || true

COPY common common
COPY module-identity module-identity
COPY module-school module-school
COPY module-people module-people
COPY module-tracking module-tracking
COPY module-notification module-notification
COPY module-insurance module-insurance
COPY module-payment module-payment
COPY module-grievance module-grievance
COPY module-audit module-audit
COPY module-geo module-geo
COPY module-attendance module-attendance
COPY module-hardware module-hardware
COPY app app

RUN mvn -q -B -pl app -am -DskipTests package \
    && cp app/target/projectx-app.jar /workspace/app.jar

# --- Runtime image ---
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# curl is for the HEALTHCHECK below. The Infisical CLI is only exercised by entrypoint.sh when
# INFISICAL_CLIENT_ID is set (the Render free-tier test deploy, which fetches every other secret
# from Infisical instead of Render's env var dashboard) - harmless but unused on the Oracle VM
# deploy, which never sets that var.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && curl -1sLf 'https://artifacts-cli.infisical.com/setup.deb.sh' | bash \
    && apt-get update && apt-get install -y infisical \
    && rm -rf /var/lib/apt/lists/*

# No fixed --uid: eclipse-temurin's base image already claims 1000 for its own default user,
# and useradd fails outright on a collision rather than picking the next free one itself.
RUN useradd --system --create-home projectx
COPY --from=build /workspace/app.jar app.jar
COPY entrypoint.sh /app/entrypoint.sh
RUN chown projectx:projectx app.jar entrypoint.sh && chmod +x entrypoint.sh
USER projectx

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/app/entrypoint.sh"]
