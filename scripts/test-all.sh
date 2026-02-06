#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/_common.sh"

export SMOKE="${SMOKE:-1}"

TESTS=(
  test-auth-service.sh
  test-user-service.sh
  test-persona-service.sh
  test-ai-service.sh
  test-chat-service.sh
  test-feed-service.sh
  test-presence-service.sh
  test-notification-service.sh
  test-relationship-service.sh
  test-scheduler-service.sh
  test-orchestrator.sh
)

for test_script in "${TESTS[@]}"; do
  log "running ${test_script}"
  bash "${SCRIPT_DIR}/${test_script}"
  log "${test_script} done"
  echo ""
done

log "all tests passed"
