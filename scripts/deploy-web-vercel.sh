#!/usr/bin/env bash
# Deploy the verified wasm dist to Vercel.
# Requires VERCEL_TOKEN. Optionally VERCEL_ORG_ID and VERCEL_PROJECT_ID.
#
# Usage:
#   ./scripts/verify-web-deploy.sh --ci
#   VERCEL_TOKEN=... VERCEL_ORG_ID=... VERCEL_PROJECT_ID=... ./scripts/deploy-web-vercel.sh
#   ./scripts/deploy-web-vercel.sh --preview   # non-production deploy
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/webApp/build/dist/wasmJs/productionExecutable"
PREVIEW=false

for arg in "$@"; do
  case "$arg" in
    --preview) PREVIEW=true ;;
    -h|--help)
      echo "Usage: $0 [--preview]"
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 1
      ;;
  esac
done

if [[ ! -f "$DIST/index.html" || ! -f "$DIST/netguard.js" ]]; then
  echo "Dist not ready. Run ./scripts/verify-web-deploy.sh first." >&2
  exit 1
fi

PROJECT_JSON="$ROOT/webApp/.vercel/project.json"
if [[ -z "${VERCEL_ORG_ID:-}" && -f "$PROJECT_JSON" ]]; then
  VERCEL_ORG_ID="$(python3 -c "import json; print(json.load(open('$PROJECT_JSON'))['orgId'])")"
  export VERCEL_ORG_ID
fi
if [[ -z "${VERCEL_PROJECT_ID:-}" && -f "$PROJECT_JSON" ]]; then
  VERCEL_PROJECT_ID="$(python3 -c "import json; print(json.load(open('$PROJECT_JSON'))['projectId'])")"
  export VERCEL_PROJECT_ID
fi

if [[ -z "${VERCEL_TOKEN:-}" && -f "$HOME/Library/Application Support/com.vercel.cli/auth.json" ]]; then
  VERCEL_TOKEN="$(python3 -c "import json; print(json.load(open('$HOME/Library/Application Support/com.vercel.cli/auth.json'))['token'])")"
  export VERCEL_TOKEN
fi

: "${VERCEL_TOKEN:?Set VERCEL_TOKEN (vercel login, or https://vercel.com/account/tokens)}"

export VERCEL_ORG_ID="${VERCEL_ORG_ID:?Set VERCEL_ORG_ID}"
export VERCEL_PROJECT_ID="${VERCEL_PROJECT_ID:?Set VERCEL_PROJECT_ID}"

cd "$DIST"

args=(deploy --yes --token "$VERCEL_TOKEN")
if [[ "$PREVIEW" == false ]]; then
  args+=(--prod)
fi

echo "==> Deploying $(basename "$DIST") to Vercel"
vercel "${args[@]}"