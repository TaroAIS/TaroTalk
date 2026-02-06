#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== ai-service tests =="
cd "${ROOT}"
mvn -pl services/ai-service -am test

if [[ "${SMOKE:-0}" != "1" ]]; then
  exit 0
fi

PORT=8090
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting ai-service"
  start_spring_service "${ROOT}/services/ai-service" "/tmp/tarotalk-ai.log" "/tmp/tarotalk-ai.pid"
fi

if wait_for_health "http://localhost:${PORT}/actuator/health" 60; then
  log "ai-service health ok"
else
  log "ai-service health check failed"
  exit 1
fi
