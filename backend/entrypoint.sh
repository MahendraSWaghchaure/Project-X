#!/bin/sh
# Only used when INFISICAL_CLIENT_ID is set (the free-tier test deploy on Render). Fetches every
# other secret (DB_URL, DB_PASSWORD, REDIS_*, JWT_*_PEM, TRACKING_INGEST_SECRET,
# PLATFORM_ADMIN_*, CORS_ALLOWED_ORIGINS, ...) from Infisical at container start instead of them
# living in Render's dashboard or any committed file. docker-compose.prod.yml's VM deploy never
# sets INFISICAL_CLIENT_ID, so it skips this block and falls through to plain `java -jar`
# unchanged, reading the env vars it already sets directly.
set -e

if [ -n "$INFISICAL_CLIENT_ID" ]; then
    INFISICAL_TOKEN=$(infisical login --method=universal-auth \
        --client-id="$INFISICAL_CLIENT_ID" \
        --client-secret="$INFISICAL_CLIENT_SECRET" \
        --silent --plain)
    export INFISICAL_TOKEN

    # --format=dotenv so a multi-line secret value (the JWT PEM contents) survives as one
    # properly-quoted line instead of being split across several.
    infisical export \
        --projectId="$INFISICAL_PROJECT_ID" \
        --env="$INFISICAL_ENV" \
        --format=dotenv > /tmp/secrets.env

    set -a
    # shellcheck disable=SC1091
    . /tmp/secrets.env
    set +a
    rm -f /tmp/secrets.env

    # PemKeyLoader reads these via Spring's ResourceLoader, which understands a plain "file:"
    # location - the PEM text itself only ever exists in Infisical and in this container's
    # memory/tmpfs, never in git or Render's env var dashboard.
    if [ -n "$JWT_PRIVATE_KEY_PEM" ] && [ -n "$JWT_PUBLIC_KEY_PEM" ]; then
        printf '%s\n' "$JWT_PRIVATE_KEY_PEM" > /tmp/jwt-private.pem
        printf '%s\n' "$JWT_PUBLIC_KEY_PEM" > /tmp/jwt-public.pem
        export JWT_PRIVATE_KEY_LOCATION=file:/tmp/jwt-private.pem
        export JWT_PUBLIC_KEY_LOCATION=file:/tmp/jwt-public.pem
    fi
fi

exec java -jar /app/app.jar
