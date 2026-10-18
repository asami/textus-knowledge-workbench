#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=cncf-common.sh
source "$SCRIPT_DIR/cncf-common.sh"

if [[ ! -s "$CNCF_RUNTIME_CLASSPATH_FILE" ]]; then
  echo "Runtime classpath is not prepared." >&2
  echo "Run: $SCRIPT_DIR/update-runtime-classpath.sh" >&2
  exit 1
fi

if [[ -z "${WORKBENCH_ASSEMBLY_DESCRIPTOR:-}" || ! -f "$WORKBENCH_ASSEMBLY_DESCRIPTOR" || ! -r "$WORKBENCH_ASSEMBLY_DESCRIPTOR" ]]; then
  echo "Workbench assembly descriptor is not configured as a readable regular file." >&2
  echo "Copy $PROJECT_ROOT/src/main/car/assembly-descriptor.yaml to an operator-owned instance and add security.authentication.localSubject.id for your selected stable standalone user." >&2
  echo "Set WORKBENCH_ASSEMBLY_DESCRIPTOR to that file; see $PROJECT_ROOT/README.md." >&2
  exit 1
fi

exec "$CNCF_LAUNCHER" \
  --classpath-file "$CNCF_RUNTIME_CLASSPATH_FILE" \
  --main-class "$CNCF_MAIN_CLASS" \
  --java-opt "-Dcncf.server.port=$CNCF_SERVER_PORT" \
  --java-opt "-Dcncf.http.baseurl=$CNCF_HTTP_BASEURL" \
  -- \
  "${CNCF_COMMON_ARGS[@]}" \
  "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" \
  server
