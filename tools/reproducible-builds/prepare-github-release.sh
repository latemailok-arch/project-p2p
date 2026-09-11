#!/usr/bin/env bash

set -euo pipefail

RELEASE_DIR="${1:?usage: prepare-github-release.sh RELEASE_DIR}"

if [ ! -d "$RELEASE_DIR" ]; then
  echo "error: release directory not found" >&2
  exit 1
fi
RELEASE_DIR="$(cd "$RELEASE_DIR" && pwd)"

if command -v sha256sum >/dev/null 2>&1; then
  SHA256=(sha256sum)
elif command -v shasum >/dev/null 2>&1; then
  SHA256=(shasum -a 256)
else
  echo "error: sha256sum or shasum is required" >&2
  exit 1
fi

required=(
  BUILDINFO.json
  SHA256SUMS
  SHA256SUMS.unsigned
  pingchat-android-arm64-unsigned.apk
  pingchat-android-arm64.apk
  pingchat-android-armv7-unsigned.apk
  pingchat-android-play-upload.aab
  pingchat-android-release-unsigned.aab
  pingchat-android-universal-unsigned.apk
  pingchat-android-universal.apk
  pingchat-android-wear-play-upload.aab
  pingchat-android-wear-release-unsigned.aab
  pingchat-android-wear-unsigned.apk
  pingchat-android-wear.apk
  pingchat-android-x86-unsigned.apk
  pingchat-android-x86_64-unsigned.apk
  pingchat-android-x86_64.apk
)
for artifact in "${required[@]}"; do
  if [ ! -f "$RELEASE_DIR/$artifact" ]; then
    echo "error: required release artifact missing: $artifact" >&2
    exit 1
  fi
done

for artifact_path in "$RELEASE_DIR"/*; do
  if [ ! -f "$artifact_path" ]; then
    echo "error: unexpected non-file in release directory: $(basename "$artifact_path")" >&2
    exit 1
  fi
  artifact="$(basename "$artifact_path")"
  case "$artifact" in
    BUILDINFO.json|SHA256SUMS|SHA256SUMS.unsigned|\
    pingchat-android-arm64-unsigned.apk|pingchat-android-arm64.apk|\
    pingchat-android-armv7-unsigned.apk|\
    pingchat-android-play-upload.aab|pingchat-android-release-unsigned.aab|\
    pingchat-android-universal-unsigned.apk|pingchat-android-universal.apk|\
    pingchat-android-wear-play-upload.aab|pingchat-android-wear-release-unsigned.aab|\
    pingchat-android-wear-unsigned.apk|pingchat-android-wear.apk|\
    pingchat-android-x86-unsigned.apk|\
    pingchat-android-x86_64-unsigned.apk|pingchat-android-x86_64.apk)
      ;;
    *)
      echo "error: unexpected release artifact: $artifact" >&2
      exit 1
      ;;
  esac
done

for destination in PINGCHAT_BUILDINFO.json PINGCHAT_SHA256SUMS PINGCHAT_SHA256SUMS.unsigned; do
  if [ -e "$RELEASE_DIR/$destination" ]; then
    echo "error: public release manifest already exists: $destination" >&2
    exit 1
  fi
done

(
  cd "$RELEASE_DIR"
  "${SHA256[@]}" -c SHA256SUMS
)

mv "$RELEASE_DIR/BUILDINFO.json" "$RELEASE_DIR/PINGCHAT_BUILDINFO.json"
mv "$RELEASE_DIR/SHA256SUMS.unsigned" "$RELEASE_DIR/PINGCHAT_SHA256SUMS.unsigned"
sed \
  -e 's/  BUILDINFO.json$/  PINGCHAT_BUILDINFO.json/' \
  -e 's/  SHA256SUMS.unsigned$/  PINGCHAT_SHA256SUMS.unsigned/' \
  "$RELEASE_DIR/SHA256SUMS" > "$RELEASE_DIR/PINGCHAT_SHA256SUMS"
rm "$RELEASE_DIR/SHA256SUMS"

(
  cd "$RELEASE_DIR"
  "${SHA256[@]}" -c PINGCHAT_SHA256SUMS
)

echo "Release assets are checksummed and ready for manual GitHub publication."
