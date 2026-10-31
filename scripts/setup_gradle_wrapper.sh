#!/usr/bin/env bash
# Tải nguyên bản Gradle Wrapper v8.13.0 chính thức, giúp project dùng ./gradlew.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="v8.13.0"
BASE="https://raw.githubusercontent.com/gradle/gradle/${VERSION}"
fetch() {
  local relative="$1"
  local dst="$ROOT/$relative"
  mkdir -p "$(dirname "$dst")"
  local temp="${dst}.download"
  curl --fail --location --show-error --silent --retry 3 --connect-timeout 20 "$BASE/$relative" --output "$temp"
  if [ ! -s "$temp" ]; then echo "ERROR: Empty download: $relative" >&2; rm -f "$temp"; exit 1; fi
  mv "$temp" "$dst"
}
echo "Downloading official Gradle Wrapper ${VERSION}..."
fetch "gradlew"
fetch "gradlew.bat"
fetch "gradle/wrapper/gradle-wrapper.jar"
chmod +x "$ROOT/gradlew"
# Basic integrity structure check (not a cryptographic signature).
if ! unzip -tqq "$ROOT/gradle/wrapper/gradle-wrapper.jar"; then
  echo "ERROR: gradle-wrapper.jar is not a valid ZIP/JAR" >&2; exit 1
fi
if ! unzip -Z -1 "$ROOT/gradle/wrapper/gradle-wrapper.jar" | grep -q '^org/gradle/wrapper/GradleWrapperMain.class$'; then
  echo "ERROR: GradleWrapperMain class missing" >&2; exit 1
fi
echo "Gradle Wrapper installed. Run: ./gradlew --version"
echo "Remember to commit gradlew, gradlew.bat, gradle-wrapper.jar and their properties."
