#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== notification-service tests =="
cd "${ROOT}"
mvn -pl services/notification-service -am test

if [[ "${SMOKE:-0}" != "1" ]]; then
  exit 0
fi

PORT=8087
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting notification-service"
  start_spring_service "${ROOT}/services/notification-service" "/tmp/tarotalk-notification.log" "/tmp/tarotalk-notification.pid"
fi

if wait_for_health "http://localhost:${PORT}/actuator/health" 60; then
  log "notification-service health ok"
else
  log "notification-service health check failed"
  exit 1
fi
