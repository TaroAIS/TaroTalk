#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== persona-service tests =="
cd "${ROOT}"
mvn -pl services/persona-service -am test

if [[ "${SMOKE:-0}" != "1" ]]; then
  exit 0
fi

skip_smoke=0
if ! require_port 6379 "redis"; then
  skip_smoke=1
fi
if [[ "${skip_smoke}" == "1" ]]; then
  exit 0
fi

PORT=8083
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting persona-service"
  start_spring_service "${ROOT}/services/persona-service" "/tmp/tarotalk-persona.log" "/tmp/tarotalk-persona.pid"
fi

if wait_for_health "http://localhost:${PORT}/actuator/health" 60; then
  log "persona-service health ok"
else
  log "persona-service health check failed"
  exit 1
fi
