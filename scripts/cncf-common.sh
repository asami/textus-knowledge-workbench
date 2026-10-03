#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

CNCF_MAIN_CLASS="${CNCF_MAIN_CLASS:-org.goldenport.cncf.CncfMain}"
CNCF_SERVER_PORT="${CNCF_SERVER_PORT:-19532}"
CNCF_HTTP_BASEURL="${CNCF_HTTP_BASEURL:-http://127.0.0.1:$CNCF_SERVER_PORT}"
CNCF_LAUNCHER="${CNCF_LAUNCHER:-$PROJECT_ROOT/bin/launcher}"
CNCF_RUNTIME_CLASSPATH_FILE="${CNCF_RUNTIME_CLASSPATH_FILE:-$PROJECT_ROOT/target/cncf.d/runtime-classpath.txt}"

CNCF_COMMON_ARGS=(--discover=classes)
