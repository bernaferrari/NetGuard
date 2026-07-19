#!/usr/bin/env bash
# Mirrors the GitHub Actions build/verify steps for the Vercel web deploy.
# Usage:
#   ./scripts/verify-web-deploy.sh          # build + verify artifacts
#   ./scripts/verify-web-deploy.sh --ci     # same, non-interactive (for CI/act)
#   ./scripts/verify-web-deploy.sh --web-only # skip unrelated Android project configuration
#   ./scripts/verify-web-deploy.sh --serve  # build + verify, then preview locally (COOP/COEP)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/webApp/build/dist/wasmJs/productionExecutable"
CI_MODE=false
SERVE_MODE=false
SKIP_BUILD=false
WEB_ONLY=false

for arg in "$@"; do
  case "$arg" in
    --ci) CI_MODE=true ;;
    --web-only) WEB_ONLY=true ;;
    --skip-build) SKIP_BUILD=true ;;
    --serve) SERVE_MODE=true ;;
    -h|--help)
      echo "Usage: $0 [--ci] [--web-only] [--skip-build] [--serve]"
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 1
      ;;
  esac
done

cd "$ROOT"

echo "==> Building wasm web distribution (includes Room + sqlite-web worker)"
if [[ "$SKIP_BUILD" == false ]]; then
  gradle_args=(
    :webApp:wasmJsBrowserDistribution
    --no-daemon
    --parallel
    --build-cache
    --configuration-cache
  )
  ./gradlew "${gradle_args[@]}"
fi

echo "==> Staging Vercel config in dist"
cp "$ROOT/webApp/vercel.json" "$DIST/vercel.json"
cp "$ROOT/webApp/src/wasmJsMain/resources/social.png" "$DIST/social.png"

echo "==> Verifying production artifacts"
required_files=(index.html netguard.js social.png vercel.json sqlite-wasm-worker/worker.js)
for file in "${required_files[@]}"; do
  if [[ ! -f "$DIST/$file" ]]; then
    echo "Missing required file: $DIST/$file" >&2
    exit 1
  fi
done

wasm_count="$(find "$DIST" -maxdepth 1 -name '*.wasm' | wc -l | tr -d ' ')"
if [[ "$wasm_count" -lt 1 ]]; then
  echo "Expected at least one .wasm file in $DIST" >&2
  exit 1
fi

if ! grep -q 'base href="/"' "$DIST/index.html"; then
  echo 'index.html must use base href="/" for Vercel root deployment' >&2
  exit 1
fi

# COOP/COEP required for OPFS / SharedArrayBuffer used by WebWorkerSQLiteDriver.
if ! grep -q 'Cross-Origin-Embedder-Policy' "$DIST/vercel.json" || \
   ! grep -q 'Cross-Origin-Opener-Policy' "$DIST/vercel.json"; then
  echo "vercel.json must declare COOP/COEP headers for Room sqlite-web OPFS" >&2
  exit 1
fi

# Room + worker must be linked into the production bundle (not a stale pre-Room build).
# Search netguard.js, source maps, and .wasm binaries for known markers.
bundle_has_marker() {
  local marker="$1"
  if grep -aFq "$marker" "$DIST/netguard.js" 2>/dev/null; then
    return 0
  fi
  if [[ -f "$DIST/netguard.js.map" ]] && grep -aFq "$marker" "$DIST/netguard.js.map" 2>/dev/null; then
    return 0
  fi
  local wasm_file
  while IFS= read -r -d '' wasm_file; do
    if grep -aFq "$marker" "$wasm_file" 2>/dev/null; then
      return 0
    fi
  done < <(find "$DIST" -maxdepth 1 -name '*.wasm' -print0)
  return 1
}

echo "==> Verifying Room / sqlite-web linkage in bundle"
room_markers=(
  "netguard.db"
  "sqlite-wasm-worker"
  "WebWorkerSQLiteDriver"
  "createSQLiteWasmWorker"
  "bernaferrari.quietguard.data.db"
  "bernaferrari.quietguard.worker"
)
room_hits=0
for marker in "${room_markers[@]}"; do
  if bundle_has_marker "$marker"; then
    echo "    ok: found '$marker'"
    room_hits=$((room_hits + 1))
  else
    echo "    miss: '$marker'"
  fi
done

# Require at least a few strong signals so minor renames don't false-fail, but a fully
# pre-Room stale build (zero markers) always fails.
if [[ "$room_hits" -lt 2 ]]; then
  echo "Production bundle does not appear to include Room/sqlite-web (found $room_hits/${#room_markers[@]} markers)." >&2
  echo "Rebuild with a clean :webApp:wasmJsBrowserDistribution after Room/wasm sources changed." >&2
  exit 1
fi

# Worker entry is loaded via import.meta.url at runtime; also accept packaged worker path in node_modules/build.
worker_source="$ROOT/sqliteWasmWorker/worker/worker.js"
if [[ ! -f "$worker_source" ]]; then
  echo "Missing sqlite wasm worker source: $worker_source" >&2
  exit 1
fi
if ! grep -q 'OpfsDb' "$worker_source"; then
  echo "sqlite wasm worker must use OpfsDb for OPFS-backed Room storage" >&2
  exit 1
fi
if ! grep -q '@sqlite.org/sqlite-wasm' "$worker_source"; then
  echo "sqlite wasm worker must import @sqlite.org/sqlite-wasm" >&2
  exit 1
fi

echo "==> Web deploy bundle is ready"
echo "    dist:       $DIST"
echo "    netguard.js: $(du -h "$DIST/netguard.js" | awk '{print $1}')"
echo "    wasm:       $wasm_count file(s)"
echo "    room/sqlite markers: $room_hits/${#room_markers[@]}"
echo "    note: serve with COOP/COEP (vercel dev, or Gradle wasmJsBrowserDevelopmentRun with webpack.config.d)"

if [[ "$SERVE_MODE" == true ]]; then
  if command -v vercel >/dev/null 2>&1; then
    echo "==> Starting local Vercel preview with COOP/COEP (Ctrl+C to stop)"
    echo "    http://127.0.0.1:4173"
    cd "$DIST"
    vercel dev --listen 4173
  else
    echo "vercel CLI not found; install with: npm i -g vercel" >&2
    echo "Alternatively run: ./gradlew :webApp:wasmJsBrowserDevelopmentRun" >&2
    exit 1
  fi
fi
