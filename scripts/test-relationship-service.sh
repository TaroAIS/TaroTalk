#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== relationship-service tests =="
cd "${ROOT}"
mvn -pl services/relationship-service -am test

if [[ "${SMOKE:-0}" != "1" ]]; then
  exit 0
fi

skip_smoke=0
if ! require_port 7687 "neo4j"; then
  skip_smoke=1
fi
if [[ "${skip_smoke}" == "1" ]]; then
  exit 0
fi

PORT=8088
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting relationship-service"
  start_spring_service "${ROOT}/services/relationship-service" "/tmp/tarotalk-relationship.log" "/tmp/tarotalk-relationship.pid"
fi

if wait_for_health "http://localhost:${PORT}/actuator/health" 60; then
  log "relationship-service health ok"
else
  log "relationship-service health check failed"
  exit 1
fi
