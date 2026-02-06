#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== feed-service tests =="
cd "${ROOT}"
mvn -pl services/feed-service -am test

if [[ "${SMOKE:-0}" != "1" ]]; then
  exit 0
fi

PORT=8085
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting feed-service"
  start_spring_service "${ROOT}/services/feed-service" "/tmp/tarotalk-feed.log" "/tmp/tarotalk-feed.pid"
fi

if wait_for_health "http://localhost:${PORT}/actuator/health" 60; then
  log "feed-service health ok"
else
  log "feed-service health check failed"
  exit 1
fi
