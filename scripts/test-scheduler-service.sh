#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== scheduler-service tests =="
cd "${ROOT}"
mvn -pl services/scheduler-service -am test

if [[ "${SMOKE:-0}" != "1" ]]; then
  exit 0
fi

PORT=8089
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting scheduler-service"
  start_spring_service "${ROOT}/services/scheduler-service" "/tmp/tarotalk-scheduler.log" "/tmp/tarotalk-scheduler.pid"
fi

if wait_for_health "http://localhost:${PORT}/actuator/health" 60; then
  log "scheduler-service health ok"
else
  log "scheduler-service health check failed"
  exit 1
fi
