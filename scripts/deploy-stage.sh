#!/usr/bin/env bash
# Runs ON the STAGE EC2 instance (started by the "Deploy to STAGE" workflow through AWS Systems Manager).
#   deploy-stage.sh <bucket> <build-id>
# 1. reads STAGE settings from SSM Parameter Store (/nsi/stage/*)   2. downloads the passing JAR from S3
# 3. restarts the Docker container with it   4. smoke-tests it   5. rolls back to the previous build if anything fails
set -euo pipefail

BUCKET=${1:?bucket}; BUILD=${2:?build id}
HOME_DIR=/opt/nsi; REL=$HOME_DIR/releases; ENV_FILE=$HOME_DIR/stage.env
CURRENT=$HOME_DIR/current_build; PREVIOUS=$HOME_DIR/previous_build
APP=nsi-app; IMAGE=eclipse-temurin:21-jre
TOKEN=$(curl -s -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')
REGION=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/placement/region)
mkdir -p "$REL"

log() { echo "[$(date -u +%H:%M:%S)] $*"; }

write_env() {
  umask 077
  aws ssm get-parameters-by-path --region "$REGION" --path /nsi/stage/ --with-decryption \
    --query 'Parameters[].[Name,Value]' --output text \
    | awk -F'\t' '{ n=$1; sub(".*/", "", n); print n "=" $2 }' > "$ENV_FILE"
  for k in DB_HOST DB_USERNAME DB_PASSWORD; do
    grep -q "^$k=" "$ENV_FILE" || { log "Missing SSM parameter /nsi/stage/$k"; exit 1; }
  done
  printf 'SPRING_PROFILES_ACTIVE=stage\nAPP_ENV=stage\n' >> "$ENV_FILE"
}

start() {   # start <build>
  local b=$1 sha
  sha=$(grep '^commit=' "$REL/$b/build-info.txt" 2>/dev/null | cut -d= -f2 | cut -c1-7 || echo unknown)
  docker rm -f "$APP" >/dev/null 2>&1 || true
  docker run -d --name "$APP" --restart unless-stopped -p 80:8080 \
    --env-file "$ENV_FILE" -e GIT_SHA="$sha" \
    -v "$REL/$b/nsi-inventory.jar:/app/app.jar:ro" \
    "$IMAGE" java -Xmx512m -XX:+ExitOnOutOfMemoryError -jar /app/app.jar >/dev/null
}

smoke() {   # smoke <expected-version>
  local want=$1 i
  for i in $(seq 1 40); do
    curl -fs http://localhost/actuator/health 2>/dev/null | grep -q '"UP"' && break
    sleep 3
  done
  curl -fs http://localhost/actuator/health | grep -q '"UP"'                  || { log "FAIL health not UP"; return 1; }
  log "PASS health is UP"
  curl -fs http://localhost/actuator/info | grep -q "\"version\":\"$want\""      || { log "FAIL version is not $want"; return 1; }
  log "PASS version $want, environment $(curl -fs http://localhost/actuator/info | sed -E 's/.*"environment":"([^"]*)".*/\1/')"
  [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost/login)" = 200 ] || { log "FAIL login page"; return 1; }
  log "PASS login page loads"
  [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost/alerts)" = 302 ] || { log "FAIL /alerts not protected"; return 1; }
  log "PASS alerts page requires log in"
  docker logs "$APP" 2>&1 | grep -E "Successfully applied|Schema .* is up to date|Current version of schema" | tail -2 | sed 's/^.*o.f.core.internal.command./  flyway: /'
  return 0
}

log "Deploying $BUILD to STAGE"
write_env
mkdir -p "$REL/$BUILD"
aws s3 cp "s3://$BUCKET/builds/$BUILD/nsi-inventory.jar" "$REL/$BUILD/nsi-inventory.jar" --only-show-errors --region "$REGION"
aws s3 cp "s3://$BUCKET/builds/$BUILD/build-info.txt" "$REL/$BUILD/build-info.txt" --only-show-errors --region "$REGION" || true
docker pull -q "$IMAGE" >/dev/null
VERSION=${BUILD%%-*}

[ -f "$CURRENT" ] && [ "$(cat "$CURRENT")" != "$BUILD" ] && cp "$CURRENT" "$PREVIOUS"
start "$BUILD"
if smoke "$VERSION"; then
  echo "$BUILD" > "$CURRENT"
  log "STAGE is running $BUILD"
  exit 0
fi

log "Smoke test failed - last 40 log lines:"
docker logs --tail 40 "$APP" 2>&1 || true
if [ -f "$PREVIOUS" ]; then
  OLD=$(cat "$PREVIOUS")
  log "Rolling back to $OLD"
  start "$OLD"
  smoke "${OLD%%-*}" && log "Rolled back to $OLD" || log "Rollback also failed"
fi
exit 1
