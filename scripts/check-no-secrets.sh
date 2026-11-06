#!/usr/bin/env bash
# Fails when signing material or credential-looking files are tracked by git.
set -euo pipefail

cd "$(dirname "$0")/.."

tracked=$(git ls-files)
forbidden=$(echo "$tracked" | grep -Ei '\.(jks|keystore|p12|pem)$|(^|/)(keystore|signing)\.properties$|google-services\.json$|play-service-account.*\.json$|(^|/)\.env$' || true)
leaks=$(git grep -nIE '(AIza[0-9A-Za-z_-]{35}|-----BEGIN (RSA |EC )?PRIVATE KEY-----|storePassword\s*=\s*"[^"]+")' -- ':!scripts/check-no-secrets.sh' || true)

if [[ -n "$forbidden" || -n "$leaks" ]]; then
  echo "Possible secrets committed:"
  echo "$forbidden"
  echo "$leaks"
  exit 1
fi
echo "No secrets found in tracked files."
