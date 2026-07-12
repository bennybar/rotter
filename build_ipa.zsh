#!/bin/zsh
# One-shot: build a signed Ad Hoc IPA and drop it in ~/Downloads so it can be
# sideloaded via Apple Configurator. (An App Store export cannot be installed via
# Configurator — that's the "integrity could not be verified" error.)
#
# Usage:   ./build_ipa.zsh           # normal build
#          ./build_ipa.zsh --clean   # flutter clean first (slower, fully fresh)
#
# Ad Hoc signing for team 57L2ULXB7Z is already working (Kenes Apple Distribution
# cert + 35 registered devices). The export step occasionally fails with a
# transient Apple-portal error ("exportArchive: The request expected results but
# none were found"); this script retries the export (which is fast — it reuses
# the archive) before giving up, and NEVER copies a stale IPA.

set -e

PROJECT="/Users/bennybarak/StudioProjects/rotter_scoops"
FLUTTER_BIN="/Users/bennybarak/Downloads/flutter/bin"
EXPORT_PLIST="$PROJECT/ios/ExportOptions-adhoc.plist"
ARCHIVE="$PROJECT/build/ios/archive/Runner.xcarchive"
IPA_DIR="$PROJECT/build/ios/ipa"
OUT="$HOME/Downloads/RotterScoops-adhoc.ipa"

export PATH="$PATH:$FLUTTER_BIN"
cd "$PROJECT"

# Never copy a previous run's artifact: clear it first.
rm -rf "$IPA_DIR"
rm -f "$OUT"

if [[ "$1" == "--clean" ]]; then
  echo "==> flutter clean"
  flutter clean
fi

echo "==> flutter pub get"
flutter pub get

echo "==> flutter build ipa (Ad Hoc, team 57L2ULXB7Z)"
# Don't let a transient export failure abort the whole script — we retry below.
set +e
flutter build ipa --release --export-options-plist "$EXPORT_PLIST"
set -e

# Retry just the export from the (already-built) archive — this is the step that
# flakes, and re-exporting takes seconds instead of re-archiving.
have_ipa() { [[ -n "$(ls "$IPA_DIR"/*.ipa(N) 2>/dev/null | head -1)" ]]; }

attempt=1
while ! have_ipa && (( attempt <= 4 )); do
  if [[ ! -d "$ARCHIVE" ]]; then
    echo "✗ No archive at $ARCHIVE — the build (not the export) failed. See log above." >&2
    exit 1
  fi
  echo "==> export attempt $attempt failed/empty; re-exporting from archive…"
  rm -rf "$IPA_DIR"
  xcodebuild -exportArchive \
    -archivePath "$ARCHIVE" \
    -exportOptionsPlist "$EXPORT_PLIST" \
    -exportPath "$IPA_DIR" \
    -allowProvisioningUpdates 2>&1 | tail -3 || true
  (( attempt++ ))
  have_ipa || sleep 4   # brief pause for the transient portal hiccup
done

SRC=$(ls -t "$IPA_DIR"/*.ipa(N) 2>/dev/null | head -1)
if [[ -z "$SRC" ]]; then
  echo "" >&2
  echo "✗ Export still failed after retries (Apple portal hiccup)." >&2
  echo "  Fastest manual fix: open the archive in Xcode and Distribute App → Ad Hoc:" >&2
  echo "    open \"$ARCHIVE\"" >&2
  exit 1
fi

cp -f "$SRC" "$OUT"
echo ""
echo "✓ Done:  $OUT  ($(date -r "$OUT" '+%H:%M:%S'))"
echo "  Drag that file into Apple Configurator to install."
