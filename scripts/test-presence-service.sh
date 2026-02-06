#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== presence-service tests =="
cd "${ROOT}"
mvn -pl services/presence-service -am test

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

PORT=8086
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting presence-service"
  start_spring_service "${ROOT}/services/presence-service" "/tmp/tarotalk-presence.log" "/tmp/tarotalk-presence.pid"
fi

if wait_for_health "http://localhost:${PORT}/actuator/health" 60; then
  log "presence-service health ok"
else
  log "presence-service health check failed"
  exit 1
fi
