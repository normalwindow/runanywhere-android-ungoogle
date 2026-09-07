#!/usr/bin/env bash
# Clean-clone verification for the native Android sample.
#
# The RunAnywhere SDK and its backend engines are resolved from Maven Central,
# so this gate needs nothing but the Android SDK and network access.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

log() {
    printf '\n==> %s\n' "$*"
}

cd "${APP_ROOT}"

if [ -f "./gradlew.bat" ] && command -v cmd.exe >/dev/null 2>&1; then
    run_gradle() {
        cmd.exe /c gradlew.bat "$@"
    }
else
    run_gradle() {
        ./gradlew "$@"
    }
fi

if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ] && [ ! -f local.properties ]; then
    echo "warning: Android SDK variables are unset; Gradle will resolve or report the SDK requirement" >&2
fi

log "Building Android debug APK"
run_gradle --dependency-verification strict :app:assembleDebug

log "Checking release APK for Google mobile-service dependencies"
run_gradle --dependency-verification strict :app:verifyNoGoogleRuntime

log "Android verification complete"
