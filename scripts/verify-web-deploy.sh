#!/usr/bin/env bash
# Mirrors the GitHub Actions build/verify steps for the Vercel web deploy.
# Usage:
#   ./scripts/verify-web-deploy.sh          # build + verify artifacts
#   ./scripts/verify-web-deploy.sh --ci     # same, non-interactive (for CI/act)
#   ./scripts/verify-web-deploy.sh --serve  # build + verify, then preview locally
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/webApp/build/dist/wasmJs/productionExecutable"
CI_MODE=false
SERVE_MODE=false

for arg in "$@"; do
  case "$arg" in
    --ci) CI_MODE=true ;;
    --serve) SERVE_MODE=true ;;
    -h|--help)
      echo "Usage: $0 [--ci] [--serve]"
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 1
      ;;
  esac
done

cd "$ROOT"

echo "==> Building wasm web distribution"
./gradlew :webApp:wasmJsBrowserDistribution --no-daemon

echo "==> Staging Vercel config in dist"
cp "$ROOT/webApp/vercel.json" "$DIST/vercel.json"

echo "==> Verifying production artifacts"
required_files=(index.html netguard.js)
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

echo "==> Web deploy bundle is ready"
echo "    dist:       $DIST"
echo "    netguard.js: $(du -h "$DIST/netguard.js" | awk '{print $1}')"
echo "    wasm:       $wasm_count file(s)"

if [[ "$SERVE_MODE" == true ]]; then
  if command -v vercel >/dev/null 2>&1; then
    echo "==> Starting local Vercel preview (Ctrl+C to stop)"
    cd "$DIST"
    vercel dev --listen 4173
  else
    echo "vercel CLI not found; install with: npm i -g vercel" >&2
    exit 1
  fi
fi