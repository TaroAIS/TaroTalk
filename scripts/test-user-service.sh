#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== user-service tests =="
cd "${ROOT}"
mvn -pl services/user-service -am test

if [[ "${SMOKE:-0}" != "1" ]]; then
  exit 0
fi

PORT=8082
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting user-service"
  start_spring_service "${ROOT}/services/user-service" "/tmp/tarotalk-user.log" "/tmp/tarotalk-user.pid"
fi

if wait_for_health "http://localhost:${PORT}/actuator/health" 60; then
  log "user-service health ok"
else
  log "user-service health check failed"
  exit 1
fi
