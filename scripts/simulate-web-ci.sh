#!/usr/bin/env bash
# Local dry-run of the Deploy Web to Vercel GitHub Action.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Simulating GitHub Actions job: build-and-deploy (build step)"
./scripts/verify-web-deploy.sh --ci

if command -v act >/dev/null 2>&1; then
  echo ""
  echo "==> act is installed — running workflow in Docker"
  act push \
    -W .github/workflows/deploy-web-vercel.yml \
    -j build-and-deploy \
    --container-architecture linux/amd64
else
  echo ""
  echo "Tip: install act to run the workflow in Docker:"
  echo "  brew install act"
  echo "  act push -W .github/workflows/deploy-web-vercel.yml -j build-and-deploy"
fi

echo ""
echo "To deploy locally after linking a Vercel project:"
echo "  VERCEL_TOKEN=... ./scripts/deploy-web-vercel.sh"
echo "To preview locally:"
echo "  ./scripts/verify-web-deploy.sh --serve"