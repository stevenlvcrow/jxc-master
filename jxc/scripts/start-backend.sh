#!/usr/bin/env bash
set -euo pipefail

PORT=8080
PIDS=""
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${PROJECT_DIR}"

if command -v lsof >/dev/null 2>&1; then
  PIDS="$(lsof -ti tcp:${PORT} -sTCP:LISTEN || true)"
elif command -v ss >/dev/null 2>&1; then
  PIDS="$(ss -ltnp "sport = :${PORT}" 2>/dev/null | awk -F 'pid=' 'NR>1 && NF>1 {split($2,a,","); print a[1]}' | sort -u)"
fi

if [[ -n "${PIDS}" ]]; then
  for pid in ${PIDS}; do
    if [[ "${pid}" == "$$" ]]; then
      continue
    fi
    echo "Stopping process on port ${PORT}: PID=${pid}"
    kill "${pid}" 2>/dev/null || true
    sleep 1
    if kill -0 "${pid}" 2>/dev/null; then
      kill -9 "${pid}" 2>/dev/null || true
    fi
  done
fi

echo "Starting backend with Maven..."
exec mvn spring-boot:run "$@"
