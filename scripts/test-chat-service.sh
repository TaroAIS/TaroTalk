#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== chat-service tests =="
cd "${ROOT}"
mvn -pl services/chat-service -am test

if [[ "${SMOKE:-0}" != "1" ]]; then
  exit 0
fi

skip_smoke=0
if ! require_port 27017 "mongodb"; then
  skip_smoke=1
fi
if ! require_port 6379 "redis"; then
  skip_smoke=1
fi
if [[ "${skip_smoke}" == "1" ]]; then
  exit 0
fi

PORT=8084
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting chat-service"
  start_spring_service "${ROOT}/services/chat-service" "/tmp/tarotalk-chat.log" "/tmp/tarotalk-chat.pid"
fi

if wait_for_health "http://localhost:${PORT}/actuator/health" 60; then
  log "chat-service health ok"
else
  log "chat-service health check failed"
  exit 1
fi
