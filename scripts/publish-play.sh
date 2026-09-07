#!/usr/bin/env bash
# Upload an already-signed AAB to an existing Google Play Console app.
# Default target is internal testing. Production explicitly needs approval.
set -Eeuo pipefail

fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
require_env() { [[ -n "${!1:-}" ]] || fail "Missing required environment variable: $1"; }

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export AAB_PATH="${AAB_PATH:-$project_root/app/build/outputs/bundle/release/app-release.aab}"
export PLAY_TRACK="${PLAY_TRACK:-internal}"
export PLAY_RELEASE_STATUS="${PLAY_RELEASE_STATUS:-completed}"
export PLAY_VALIDATE_ONLY="${PLAY_VALIDATE_ONLY:-false}"

require_env GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64
[[ -f "$AAB_PATH" ]] || fail "AAB not found: $AAB_PATH"
[[ "$PLAY_TRACK" =~ ^[a-z][a-z0-9_-]*$ ]] || fail "PLAY_TRACK must contain lowercase letters, digits, _ or -"
case "$PLAY_RELEASE_STATUS" in completed|draft|inProgress|halted) ;; *) fail "Invalid PLAY_RELEASE_STATUS" ;; esac
case "$PLAY_VALIDATE_ONLY" in true|false) ;; *) fail "PLAY_VALIDATE_ONLY must be true or false" ;; esac

if [[ "$PLAY_TRACK" == "production" && "${PLAY_PRODUCTION_APPROVED:-}" != "yes" ]]; then
  fail "Production publishing needs PLAY_PRODUCTION_APPROVED=yes"
fi
if [[ -n "${PLAY_USER_FRACTION:-}" ]]; then
  [[ "$PLAY_RELEASE_STATUS" == "inProgress" ]] || fail "PLAY_USER_FRACTION requires PLAY_RELEASE_STATUS=inProgress"
  [[ "$PLAY_USER_FRACTION" =~ ^(0|0\.[0-9]+|1)(\.0+)?$ ]] || fail "PLAY_USER_FRACTION must be between 0 and 1"
fi

command -v base64 >/dev/null || fail "base64 is required (install coreutils)"
command -v bundle >/dev/null || fail "bundle is required; run bundle install first"

credential_file="$(mktemp "${TMPDIR:-/tmp}/shufa-play-service-account.XXXXXX.json")"
chmod 600 "$credential_file"
cleanup() { rm -f "$credential_file"; unset GOOGLE_PLAY_SERVICE_ACCOUNT_JSON; }
trap cleanup EXIT
printf '%s' "$GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64" | base64 --decode > "$credential_file" \
  || fail "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64 is not valid Base64"

# Avoid writing credential data into CI logs while catching malformed secrets.
grep -q '"client_email"' "$credential_file" || fail "Service-account JSON has no client_email"
export GOOGLE_PLAY_SERVICE_ACCOUNT_JSON="$credential_file"
export FASTLANE_SKIP_UPDATE_CHECK=true

cd "$project_root"
bundle exec fastlane android publish
