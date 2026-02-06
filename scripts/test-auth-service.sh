#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== auth-service tests =="
cd "${ROOT}"
mvn -pl services/auth-service -am test

if [[ "${SMOKE:-0}" != "1" ]]; then
  exit 0
fi

PORT=8081
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting auth-service"
  start_spring_service "${ROOT}/services/auth-service" "/tmp/tarotalk-auth.log" "/tmp/tarotalk-auth.pid"
fi

if wait_for_health "http://localhost:${PORT}/actuator/health" 60; then
  log "auth-service health ok"
else
  log "auth-service health check failed"
  exit 1
fi
