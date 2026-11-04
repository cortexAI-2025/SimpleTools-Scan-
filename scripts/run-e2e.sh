#!/usr/bin/env bash
# End-to-end tests of SimpleTools Scan on a connected emulator/device, in airplane mode.
# Order matters: the camera refusal needs a fresh install, the resume test kills the app.
# Usage: scripts/run-e2e.sh [output-dir]   (APKs must be built: :app:assembleDebug :app:assembleDebugAndroidTest)
set -uo pipefail
cd "$(dirname "$0")/.."

OUT="${1:-build/e2e}"
PKG=com.simpletools.scan.debug
RUNNER="$PKG.test/androidx.test.runner.AndroidJUnitRunner"
APK=app/build/outputs/apk/debug/app-debug.apk
TEST_APK=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
mkdir -p "$OUT"
SUMMARY="$OUT/summary.md"
: > "$SUMMARY"
failures=0

adb uninstall "$PKG" >/dev/null 2>&1 || true
adb uninstall "$PKG.test" >/dev/null 2>&1 || true
adb install -r "$APK" && adb install -r "$TEST_APK" || { echo "install failed"; exit 1; }

# Airplane mode: no network at all during the tests (the app has no INTERNET permission anyway).
adb shell cmd connectivity airplane-mode enable
sleep 3
echo "airplane_mode_on=$(adb shell settings get global airplane_mode_on | tr -d '\r')" | tee -a "$OUT/device.txt"
adb shell getprop ro.build.version.release | sed 's/^/android=/' >> "$OUT/device.txt"
adb shell getprop ro.product.model | sed 's/^/model=/' >> "$OUT/device.txt"

run() { # $1 = label, $2 = test class#method
  echo "::group::$1"
  adb shell am instrument -w -r -e requireAirplane true -e class "$2" "$RUNNER" | tee "$OUT/$1.log"
  echo "::endgroup::"
  if grep -q "^OK (" "$OUT/$1.log"; then
    echo "- ✅ $1 (\`$2\`)" >> "$SUMMARY"
  else
    echo "- ❌ $1 (\`$2\`)" >> "$SUMMARY"
    failures=$((failures + 1))
  fi
}

E2E=com.simpletools.app.e2e
run 1-camera-refused "$E2E.CameraPermissionDeniedTest"

adb shell pm grant "$PKG" android.permission.CAMERA
run 2-three-pages-to-pdf "$E2E.ScanScenarioTest"

run 3-resume-recreate "$E2E.ResumeTest#recreatingTheScreenKeepsTheCrop"
run 4-resume-before-kill "$E2E.ResumeTest#leaveWorkInProgress"
adb shell am force-stop "$PKG"
echo "process after force-stop: $(adb shell pidof $PKG | tr -d '\r')" | tee -a "$OUT/device.txt"
run 5-resume-after-kill "$E2E.ResumeTest#resumeAfterForceStop"

run 6-export-cancel-and-failure "$E2E.ExportCancelAndFailureTest"

# PDF, rendered pages and screenshots written by the tests in the app's private storage.
adb exec-out run-as "$PKG" tar -cf - -C files e2e-output > "$OUT/e2e-output.tar" 2>/dev/null \
  && tar -xf "$OUT/e2e-output.tar" -C "$OUT" && rm "$OUT/e2e-output.tar"

adb shell cmd connectivity airplane-mode disable || true
cat "$SUMMARY"
exit "$failures"
