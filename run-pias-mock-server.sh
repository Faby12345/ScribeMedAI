#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_DIR="${SCRIPT_DIR}/pias-mock-server"
UVICORN="${SERVICE_DIR}/.venv/bin/uvicorn"

if [[ ! -x "${UVICORN}" ]]; then
  echo "PIAS mock virtual environment is missing or incomplete." >&2
  echo "Create it in ${SERVICE_DIR}/.venv and install FastAPI and Uvicorn." >&2
  exit 1
fi

exec "${UVICORN}" main:app \
  --app-dir "${SERVICE_DIR}" \
  --host "${PIAS_MOCK_HOST:-127.0.0.1}" \
  --port "${PIAS_MOCK_PORT:-8090}" \
  --reload
