#!/usr/bin/env bash
# Build a signed Android App Bundle suitable for Google Play.
# Required CI secrets:
#   ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD,
#   ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD
set -Eeuo pipefail

fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
require_env() { [[ -n "${!1:-}" ]] || fail "Missing required environment variable: $1"; }

for variable in ANDROID_KEYSTORE_BASE64 ANDROID_KEYSTORE_PASSWORD ANDROID_KEY_ALIAS ANDROID_KEY_PASSWORD; do
  require_env "$variable"
done

if [[ -n "${RELEASE_VERSION_CODE:-}" ]]; then
  [[ "$RELEASE_VERSION_CODE" =~ ^[1-9][0-9]*$ ]] || fail "RELEASE_VERSION_CODE must be a positive integer"
fi
if [[ -n "${RELEASE_VERSION_NAME:-}" ]]; then
  [[ "$RELEASE_VERSION_NAME" != *$'\n'* && "$RELEASE_VERSION_NAME" != *$'\r'* ]] || fail "RELEASE_VERSION_NAME cannot contain a newline"
fi

command -v base64 >/dev/null || fail "base64 is required (install coreutils)"
command -v keytool >/dev/null || fail "keytool is required (install a JDK, not only a JRE)"

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
output_dir="${OUTPUT_DIR:-$project_root/dist}"
mkdir -p "$output_dir"

keystore_file="$(mktemp "${TMPDIR:-/tmp}/shufa-release-keystore.XXXXXX.jks")"
chmod 600 "$keystore_file"
cleanup() {
  rm -f "$keystore_file"
  unset RELEASE_STORE_FILE RELEASE_STORE_PASSWORD RELEASE_KEY_ALIAS RELEASE_KEY_PASSWORD
}
trap cleanup EXIT

# GitHub Actions secrets are normally encoded without line wrapping. Accepting
# wrapped input makes this script usable with other CI systems too.
printf '%s' "$ANDROID_KEYSTORE_BASE64" | base64 --decode > "$keystore_file" \
  || fail "ANDROID_KEYSTORE_BASE64 is not valid Base64"

export RELEASE_STORE_FILE="$keystore_file"
export RELEASE_STORE_PASSWORD="$ANDROID_KEYSTORE_PASSWORD"
export RELEASE_KEY_ALIAS="$ANDROID_KEY_ALIAS"
export RELEASE_KEY_PASSWORD="$ANDROID_KEY_PASSWORD"

keytool -list -keystore "$keystore_file" -storepass "$RELEASE_STORE_PASSWORD" \
  -alias "$RELEASE_KEY_ALIAS" >/dev/null \
  || fail "Cannot open the release keystore with the supplied alias/password"

gradle_args=(--no-daemon clean bundleRelease)
[[ -n "${RELEASE_VERSION_CODE:-}" ]] && gradle_args+=("-PRELEASE_VERSION_CODE=$RELEASE_VERSION_CODE")
[[ -n "${RELEASE_VERSION_NAME:-}" ]] && gradle_args+=("-PRELEASE_VERSION_NAME=$RELEASE_VERSION_NAME")

cd "$project_root"
./gradlew "${gradle_args[@]}"

aab="$project_root/app/build/outputs/bundle/release/app-release.aab"
[[ -f "$aab" ]] || fail "Expected bundle was not generated: $aab"
jarsigner -verify -strict "$aab" >/dev/null \
  || fail "Generated AAB signature verification failed"

version_label="${RELEASE_VERSION_NAME:-1.0.1}-${RELEASE_VERSION_CODE:-11}"
published_aab="$output_dir/shufa-${version_label}.aab"
cp "$aab" "$published_aab"
printf 'Signed AAB created: %s\n' "$published_aab"
