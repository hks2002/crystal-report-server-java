#!/usr/bin/env bash
# Install Crystal Reports JARs from lib/ into local Maven repository
# Usage:  ./install-libs.sh
# After running this once, pom.xml no longer needs scope=system / systemPath

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"
GROUP_ID='crystal-reports'
REPO_ROOT="$HOME/.m2/repository/crystal-reports"

if [ ! -d "$LIB_DIR" ]; then
  echo "ERROR: lib directory not found at $LIB_DIR" >&2
  exit 1
fi

if [ -d "$REPO_ROOT" ]; then
  echo "Cleaning stale markers under $REPO_ROOT ..."
  find "$REPO_ROOT" -name '*.lastUpdated' -type f -delete
fi

declare -a LIB_NAMES=(
  'com.azalea.ufl.barcode'
  'CrystalCommon2'
  'CrystalReportsRuntime'
  'cvom'
  'DatabaseConnectors'
  'JDBInterface'
  'keycodeDecoder'
  'logging'
  'pfjgraphics'
  'QueryBuilder'
)

declare -a LIB_VERSIONS=(
  '1.0'
  '12.2.233.5802'
  '12.2.233.5802'
  '12.2.233.5802'
  '12.2.233.5802'
  '12.2.233.5802'
  '12.2.233.5802'
  '12.2.233.5802'
  '12.2.233.5802'
  '12.2.233.5802'
)

declare -a LIB_JARS=(
  'com.azalea.ufl.barcode.1.0.jar'
  'CrystalCommon2.jar'
  'CrystalReportsRuntime.jar'
  'cvom.jar'
  'DatabaseConnectors.jar'
  'JDBInterface.jar'
  'keycodeDecoder.jar'
  'logging.jar'
  'pfjgraphics.jar'
  'QueryBuilder.jar'
)

echo "Installing Crystal Reports JARs to local Maven repository..."
echo "lib dir: $LIB_DIR"
echo ""

INSTALLED=0
SKIPPED=0

for i in "${!LIB_NAMES[@]}"; do
  NAME="${LIB_NAMES[$i]}"
  VERSION="${LIB_VERSIONS[$i]}"
  JAR="${LIB_JARS[$i]}"
  JAR_PATH="$LIB_DIR/$JAR"

  if [ ! -f "$JAR_PATH" ]; then
    echo "WARNING: JAR not found, skipping: $JAR"
    SKIPPED=$((SKIPPED + 1))
    continue
  fi

  INSTALLED_JAR="$REPO_ROOT/$NAME/$VERSION/$NAME-$VERSION.jar"
  if [ -f "$INSTALLED_JAR" ]; then
    echo "[SKIP] $NAME:$VERSION (already installed)"
    SKIPPED=$((SKIPPED + 1))
    continue
  fi

  echo "[INSTALL] $NAME:$VERSION <- $JAR"
  "$SCRIPT_DIR/mvnw" -f "$SCRIPT_DIR/pom.xml" install:install-file \
    -Dfile="$JAR_PATH" \
    -DgroupId="$GROUP_ID" \
    -DartifactId="$NAME" \
    -Dversion="$VERSION" \
    -Dpackaging=jar \
    -DgeneratePom=true

  if [ $? -ne 0 ]; then
    echo "ERROR: Failed to install $JAR" >&2
    exit 1
  fi
  INSTALLED=$((INSTALLED + 1))
done

echo ""
echo "Done. Installed: $INSTALLED, Skipped: $SKIPPED"
echo "You can now build with:  ./mvnw clean package"