#!/usr/bin/env bash
#
# Post-deployment smoke test. Every pipeline (Azure, GitHub Actions, Jenkins) runs this
# right after deploying, so a broken release is caught in minutes, not by users.
#
# Usage:  scripts/smoke-test.sh [BASE_URL] [EXPECTED_ENV]
#   scripts/smoke-test.sh                                  # http://localhost:8080
#   scripts/smoke-test.sh https://my-app.azurewebsites.net dev
#
# Environment:
#   SMOKE_RETRIES   how many times to wait for /health (default 30)
#   SMOKE_DELAY     seconds between retries (default 5)
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
EXPECTED_ENV="${2:-}"
RETRIES="${SMOKE_RETRIES:-30}"
DELAY="${SMOKE_DELAY:-5}"

pass() { printf '  \033[32mPASS\033[0m %s\n' "$1"; }
fail() { printf '  \033[31mFAIL\033[0m %s\n' "$1"; exit 1; }

echo "Smoke testing ${BASE_URL}"

# 1. Wait until the app is up (containers and App Service need time to start Tomcat).
for attempt in $(seq 1 "${RETRIES}"); do
  if curl -fsS --max-time 5 "${BASE_URL}/health" >/dev/null 2>&1; then
    pass "/health is UP (attempt ${attempt})"
    break
  fi
  if [[ "${attempt}" -eq "${RETRIES}" ]]; then
    fail "/health did not become healthy after $((RETRIES * DELAY))s"
  fi
  echo "  waiting for /health ... (${attempt}/${RETRIES})"
  sleep "${DELAY}"
done

# 2. Check each endpoint returns what we expect.
check() {
  local path="$1" expected="$2" body
  body="$(curl -fsS --max-time 10 "${BASE_URL}${path}")" || fail "${path} did not return 2xx"
  if [[ "${body}" == *"${expected}"* ]]; then
    pass "${path} contains '${expected}'"
  else
    echo "    body: ${body}"
    fail "${path} does not contain '${expected}'"
  fi
}

check "/health/ready"                "\"status\":\"UP\""
check "/api/greet?name=Smoke"        "Hello Smoke, how are you?"
check "/api/greet?name=Smoke&lang=fr" "Bonjour Smoke"
check "/hello?name=Smoke"            "Hello Smoke"
check "/api/info"                    "\"app\":\"devopsappcicd\""
check "/"                            "DevOps App CI/CD"

if [[ -n "${EXPECTED_ENV}" ]]; then
  check "/api/info" "\"environment\":\"${EXPECTED_ENV}\""
fi

# 3. Bad input must be rejected, not crash the app.
status="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "${BASE_URL}/api/greet?lang=xx")"
if [[ "${status}" == "400" ]]; then
  pass "/api/greet?lang=xx returns 400"
else
  fail "/api/greet?lang=xx returned ${status}, expected 400"
fi

echo "All smoke tests passed."
