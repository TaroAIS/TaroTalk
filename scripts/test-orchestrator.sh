#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

log "== orchestrator tests =="
cd "${ROOT}"

if ! has_cmd python3; then
  log "python3 not found"
  exit 1
fi

if ! python3 - <<'PY'
import importlib.util
import sys
sys.exit(0 if importlib.util.find_spec('pytest') else 1)
PY
then
  log "pytest not installed; install with: python3 -m pip install -r orchestrator/requirements.txt"
  exit 1
fi

python3 -m pytest orchestrator/tests

if [[ "${SMOKE:-0}" != "1" ]]; then
  exit 0
fi

if ! python3 - <<'PY'
import importlib.util
import sys
missing = [m for m in ('fastapi','uvicorn') if not importlib.util.find_spec(m)]
sys.exit(1 if missing else 0)
PY
then
  if [[ "${STRICT_DEPS:-0}" == "1" ]]; then
    log "fastapi/uvicorn not installed (STRICT_DEPS=1)"
    exit 1
  fi
  log "fastapi/uvicorn not installed; skip smoke"
  exit 0
fi

PORT=8077
if is_port_listening "${PORT}"; then
  log "port ${PORT} already in use; skip start"
else
  log "starting orchestrator"
  start_uvicorn "${ROOT}/orchestrator" "/tmp/tarotalk-orchestrator.log" "/tmp/tarotalk-orchestrator.pid"
fi

if wait_for_health "http://localhost:${PORT}/a2a/tools" 60; then
  log "orchestrator health ok"
else
  log "orchestrator health check failed"
  exit 1
fi
