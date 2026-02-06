#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PIDFILES=()

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

has_cmd() {
  command -v "$1" >/dev/null 2>&1
}

is_port_listening() {
  local port="$1"
  if has_cmd lsof; then
    lsof -iTCP:"${port}" -sTCP:LISTEN -n -P >/dev/null 2>&1
  elif has_cmd nc; then
    nc -z 127.0.0.1 "${port}" >/dev/null 2>&1
  else
    return 1
  fi
}

require_port() {
  local port="$1"
  local name="$2"
  if is_port_listening "${port}"; then
    return 0
  fi
  if [[ "${STRICT_DEPS:-0}" == "1" ]]; then
    log "missing dependency ${name} on port ${port} (STRICT_DEPS=1)"
    exit 1
  fi
  log "missing dependency ${name} on port ${port}, skip smoke"
  return 1
}

wait_for_health() {
  local url="$1"
  local retries="${2:-60}"
  for _ in $(seq 1 "${retries}"); do
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" "${url}" || true)
    if [[ "${code}" == "200" ]]; then
      return 0
    fi
    sleep 1
  done
  return 1
}

register_pidfile() {
  PIDFILES+=("$1")
}

start_spring_service() {
  local module_dir="$1"
  local log_file="$2"
  local pid_file="$3"
  (cd "${module_dir}" && nohup mvn spring-boot:run > "${log_file}" 2>&1 & echo $! > "${pid_file}")
  register_pidfile "${pid_file}"
}

start_uvicorn() {
  local module_dir="$1"
  local log_file="$2"
  local pid_file="$3"
  (cd "${module_dir}" && nohup python3 -m uvicorn app.main:app --host 0.0.0.0 --port 8077 > "${log_file}" 2>&1 & echo $! > "${pid_file}")
  register_pidfile "${pid_file}"
}

stop_pidfile() {
  local pid_file="$1"
  if [[ -f "${pid_file}" ]]; then
    local pid
    pid=$(cat "${pid_file}")
    if kill -0 "${pid}" >/dev/null 2>&1; then
      kill "${pid}" || true
    fi
    rm -f "${pid_file}"
  fi
}

cleanup_on_exit() {
  for pid_file in "${PIDFILES[@]:-}"; do
    stop_pidfile "${pid_file}"
  done
}

trap cleanup_on_exit EXIT
