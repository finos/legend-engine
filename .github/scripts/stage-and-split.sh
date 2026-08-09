#!/usr/bin/env bash
#
# Copyright 2026 Goldman Sachs
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Stage legend-engine artifacts from a local Maven repository and split them
# into Central Publisher Portal bundles that fit under the Portal's 1GiB
# upload limit. A single oversized bundle is rejected by nginx with a bare
# "413 Payload Too Large" before the API ever sees it.
#
# Usage: stage-and-split.sh <m2-engine-dir> <release-version> <out-dir> [max-bytes]
#
# Env:
#   CENTRAL_EXCLUDE_GLOBS  space-separated find(1) -name globs to drop from the
#                          bundle. EMPTY BY DEFAULT ON PURPOSE: every artifact
#                          currently in the local repo is also currently on
#                          Central, and Central is immutable, so dropping one
#                          silently removes a coordinate consumers already
#                          resolve. Only populate this once the coordinate has
#                          been confirmed unused downstream.

set -euo pipefail

M2_ENGINE_DIR=${1:?usage: stage-and-split.sh <m2-engine-dir> <version> <out-dir> [max-bytes]}
RELEASE_VERSION=${2:?missing release version}
OUT_DIR=${3:?missing output dir}
MAX_BYTES=${4:-1000000000}

# The Portal rejects anything above 1GiB at the proxy. MAX_BYTES is the packing
# target; this is the backstop checked against the produced zip.
HARD_LIMIT=$((1024 * 1024 * 1024))

STAGING="$OUT_DIR/central-staging"
BUNDLES="$OUT_DIR/central-publishing"
ROOT=org/finos/legend/engine

[ -d "$M2_ENGINE_DIR" ] || { echo "ERROR: $M2_ENGINE_DIR does not exist" >&2; exit 1; }

rm -rf "$STAGING" "$BUNDLES"
mkdir -p "$STAGING/$ROOT" "$BUNDLES"
cp -R "$M2_ENGINE_DIR/." "$STAGING/$ROOT/"

# Keep only directories whose name is exactly the release version. Defensive:
# the build job already purges the local repo before building the release tag,
# but a stale version leaking in here would be published irreversibly.
find "$STAGING/$ROOT" -mindepth 2 -type d \
  -regextype posix-extended \
  -regex '.*/[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.+-]+)?$' \
  ! -name "$RELEASE_VERSION" -prune -exec rm -rf {} +

# Local-repo bookkeeping, never part of a deployment. Targeted by name rather
# than by "*.xml" so a real .xml artifact could never be swept up.
find "$STAGING" -type f \
  \( -name '_remote.repositories' \
     -o -name 'maven-metadata-local.xml' \
     -o -name 'maven-metadata-*.xml' \
     -o -name '*.lastUpdated' \
     -o -name 'resolver-status.properties' \) -delete

for glob in ${CENTRAL_EXCLUDE_GLOBS:-}; do
  echo "excluding: $glob"
  find "$STAGING" -type f -name "$glob" -delete
done

find "$STAGING/$ROOT" -mindepth 1 -type d -empty -delete

# An artifact coordinate is one <artifactId>/<version> directory. It is the
# unit of splitting: a jar must never be separated from its .pom, .asc and
# checksums, or the bundle validates but leaves a broken artifact on Central.
cd "$STAGING"
mapfile -t COORDS < <(find "$ROOT" -mindepth 2 -maxdepth 2 -type d -name "$RELEASE_VERSION" | sort)

if [ ${#COORDS[@]} -eq 0 ]; then
  echo "ERROR: no artifacts found for version $RELEASE_VERSION under $M2_ENGINE_DIR" >&2
  exit 1
fi

# Central requires md5 and sha1; sha256 and sha512 are optional but are already
# published for prior releases, so they are kept for parity.
while IFS= read -r -d '' file; do
  md5sum    "$file" | awk '{print $1}' > "$file.md5"
  sha1sum   "$file" | awk '{print $1}' > "$file.sha1"
  sha256sum "$file" | awk '{print $1}' > "$file.sha256"
  sha512sum "$file" | awk '{print $1}' > "$file.sha512"
done < <(find "$STAGING" -type f \
  ! -name '*.md5' ! -name '*.sha1' ! -name '*.sha256' \
  ! -name '*.sha512' ! -name '*.asc' -print0)

STAGED_BYTES=$(du -sb "$STAGING" | cut -f1)
STAGED_FILES=$(find "$STAGING" -type f | wc -l)
echo "staged ${#COORDS[@]} coordinates: $((STAGED_BYTES / 1000000)) MB, $STAGED_FILES files"

# --- split ---------------------------------------------------------------
# Everything measured here is written to bundle-report.md / *.tsv as well as
# the log. When a release fails at the Portal the first question is always
# "how big was it and what was in it", and the runner is gone by then.
BUNDLES_TSV="$BUNDLES/bundles.tsv"
CONTENTS_TSV="$BUNDLES/bundle-contents.tsv"
COORDS_TSV="$BUNDLES/coordinates.tsv"
REPORT="$BUNDLES/bundle-report.md"

printf 'bundle\tfiles\tzip_bytes\tstaged_bytes\tsha256\n' > "$BUNDLES_TSV"
printf 'bundle\tcoordinate\tbytes\n' > "$CONTENTS_TSV"
printf 'coordinate\tbytes\n' > "$COORDS_TSV"

idx=1
size=0
list=$(mktemp)
coordlist=$(mktemp)
sorted_coords=""
trap 'rm -f "$list" "$coordlist"; [ -n "${sorted_coords:-}" ] && rm -f "$sorted_coords"' EXIT

mb()  { awk -v b="$1" 'BEGIN { printf "%.1f", b / 1000000 }'; }
pct() { awk -v a="$1" -v b="$2" 'BEGIN { printf "%.1f", 100 * a / b }'; }

flush() {
  [ -s "$list" ] || return 0
  local name out zipsize sha files
  name="central-bundle-$(printf '%02d' "$idx")"
  out="$BUNDLES/$name.zip"
  zip -q -X "$out" -@ < "$list"
  zipsize=$(stat -c%s "$out")
  files=$(wc -l < "$list")
  sha=$(sha256sum "$out" | cut -d' ' -f1)

  if [ "$zipsize" -gt "$HARD_LIMIT" ]; then
    echo "ERROR: $out is $zipsize bytes ($(mb "$zipsize") MB), above the Portal's" >&2
    echo "ERROR: ${HARD_LIMIT}-byte limit. Lower max-bytes (currently $MAX_BYTES) and re-run." >&2
    exit 1
  fi

  printf '%s\t%s\t%s\t%s\t%s\n' "$name" "$files" "$zipsize" "$size" "$sha" >> "$BUNDLES_TSV"
  awk -v b="$name" -F'\t' '{ printf "%s\t%s\t%s\n", b, $1, $2 }' "$coordlist" >> "$CONTENTS_TSV"

  echo "bundle $idx: $files files, $(mb "$zipsize") MB zipped ($(pct "$zipsize" "$HARD_LIMIT")% of Portal limit), sha256 $sha"
  idx=$((idx + 1))
  size=0
  : > "$list"
  : > "$coordlist"
}

for dir in "${COORDS[@]}"; do
  dirsize=$(du -sb "$dir" | cut -f1)
  printf '%s\t%s\n' "$dir" "$dirsize" >> "$COORDS_TSV"

  if [ "$dirsize" -gt "$MAX_BYTES" ]; then
    echo "ERROR: $dir alone is $(mb "$dirsize") MB, above the ${MAX_BYTES}-byte" >&2
    echo "ERROR: bundle target. A single coordinate cannot be split across bundles." >&2
    echo "ERROR: shrink the artifact, stop publishing it, or request a Portal limit" >&2
    echo "ERROR: increase from central-support@sonatype.com." >&2
    exit 1
  fi
  if [ $((dirsize * 100)) -gt $((MAX_BYTES * 80)) ]; then
    echo "WARNING: $dir is $(mb "$dirsize") MB, $(pct "$dirsize" "$MAX_BYTES")% of the bundle target." >&2
    echo "WARNING: it will hard-fail the release once it exceeds $((MAX_BYTES / 1000000)) MB." >&2
  fi
  if [ "$size" -gt 0 ] && [ $((size + dirsize)) -gt "$MAX_BYTES" ]; then
    flush
  fi
  find "$dir" -type f >> "$list"
  printf '%s\t%s\n' "$dir" "$dirsize" >> "$coordlist"
  size=$((size + dirsize))
done
flush

NBUNDLES=$((idx - 1))
TOTAL_ZIP=$(awk -F'\t' 'NR > 1 { s += $3 } END { print s + 0 }' "$BUNDLES_TSV")
LARGEST_ZIP=$(awk -F'\t' 'NR > 1 && $3 > m { m = $3 } END { print m + 0 }' "$BUNDLES_TSV")

{
  echo "## Central bundle report"
  echo
  echo "| | |"
  echo "|---|---|"
  echo "| Release version | \`$RELEASE_VERSION\` |"
  echo "| Coordinates staged | ${#COORDS[@]} |"
  echo "| Files staged | $STAGED_FILES |"
  echo "| Staged size | $(mb "$STAGED_BYTES") MB |"
  echo "| Bundles produced | $NBUNDLES |"
  echo "| Total upload size | $(mb "$TOTAL_ZIP") MB |"
  echo "| Largest bundle | $(mb "$LARGEST_ZIP") MB ($(pct "$LARGEST_ZIP" "$HARD_LIMIT")% of the ${HARD_LIMIT}-byte Portal limit) |"
  echo "| Packing target | $(mb "$MAX_BYTES") MB per bundle |"
  echo
  echo "### Bundles"
  echo
  echo "| Bundle | Files | Upload size | % of Portal limit | SHA-256 |"
  echo "|---|---:|---:|---:|---|"
  awk -F'\t' -v lim="$HARD_LIMIT" 'NR > 1 {
    printf "| %s.zip | %s | %.1f MB | %.1f%% | `%s` |\n", $1, $2, $3 / 1000000, 100 * $3 / lim, $5
  }' "$BUNDLES_TSV"
  echo
  echo "### 20 largest coordinates"
  echo
  echo "| Coordinate | Size | % of packing target |"
  echo "|---|---:|---:|"
  sorted_coords=$(mktemp)
  tail -n +2 "$COORDS_TSV" | sort -t$'\t' -k2 -rn > "$sorted_coords"
  head -20 "$sorted_coords" | awk -F'\t' -v tgt="$MAX_BYTES" '{
    flag = (100 * $2 / tgt >= 80) ? " :warning:" : ""
    printf "| `%s` | %.1f MB | %.1f%%%s |\n", $1, $2 / 1000000, 100 * $2 / tgt, flag
  }'
  rm -f "$sorted_coords"
  sorted_coords=""
  echo
  echo "A coordinate cannot be split across bundles, so any single one reaching"
  echo "$(mb "$MAX_BYTES") MB fails the release outright."
} > "$REPORT"

echo
cat "$REPORT"
echo
echo "wrote $NBUNDLES bundle(s) to $BUNDLES"
echo "report: $REPORT"
