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
# Builds a synthetic ~/.m2 engine tree with known sizes and asserts the
# invariants that matter for a Central release. No network, runs in seconds.

set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")" && pwd)/stage-and-split.sh"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

VERSION=4.100.0
STALE=4.99.0
M2="$TMP/m2/org/finos/legend/engine"
MAX=$((30 * 1000 * 1000))   # 30MB so the test runs fast

fail=0
check() { if [ "$2" = "$3" ]; then echo "ok   $1"; else echo "FAIL $1: expected '$3' got '$2'"; fail=1; fi; }

mk() { # mk <artifact> <version> <jar-size-mb>
  # NB: separate `local` statements on purpose -- bash declares all names in a
  # single `local` before assigning, so `local a=$1 d="$M2/$a"` breaks under set -u.
  local a=$1
  local v=$2
  local mb=$3
  local d="$M2/$a/$v"
  mkdir -p "$d"
  head -c $((mb * 1000 * 1000)) /dev/urandom > "$d/$a-$v.jar"
  printf '<project><artifactId>%s</artifactId></project>' "$a" > "$d/$a-$v.pom"
  head -c 2000 /dev/urandom > "$d/$a-$v-sources.jar"
  head -c 2000 /dev/urandom > "$d/$a-$v-javadoc.jar"
  for f in "$d/$a-$v".*; do echo sig > "$f.asc"; done
  echo '{}' > "$d/_remote.repositories"
  printf '<metadata/>' > "$d/maven-metadata-local.xml"
}

# 12 normal modules at 8MB -> must span multiple 30MB bundles
for i in $(seq -w 1 12); do mk "legend-engine-xt-mod$i" "$VERSION" 8; done
# stale version that must be pruned
mk legend-engine-xt-mod01 "$STALE" 8
# the server module: carries a large shaded jar plus a tests jar, both of which
# are published to Central today and must survive staging untouched
mk legend-engine-server-http-server "$VERSION" 1
head -c $((20 * 1000 * 1000)) /dev/urandom > "$M2/legend-engine-server-http-server/$VERSION/legend-engine-server-http-server-$VERSION-shaded.jar"
head -c 3000 /dev/urandom > "$M2/legend-engine-server-http-server/$VERSION/legend-engine-server-http-server-$VERSION-tests.jar"

echo "=== running ==="
bash "$SCRIPT" "$M2" "$VERSION" "$TMP/out" "$MAX"

STAGING="$TMP/out/central-staging"
BUNDLES="$TMP/out/central-publishing"

echo "=== assertions ==="

check "stale version pruned" \
  "$(find "$STAGING" -type d -name "$STALE" | wc -l)" 0
check "no _remote.repositories staged" \
  "$(find "$STAGING" -name '_remote.repositories' | wc -l)" 0
check "no maven-metadata-local.xml staged" \
  "$(find "$STAGING" -name 'maven-metadata-local.xml' | wc -l)" 0

# Parity with what is already on Central: these are published coordinates today.
check "shaded jar preserved" \
  "$(find "$STAGING" -name '*-shaded.jar' | wc -l)" 1
check "tests jar preserved" \
  "$(find "$STAGING" -name '*-tests.jar' | wc -l)" 1
check "sources jars preserved" \
  "$(find "$STAGING" -name '*-sources.jar' | wc -l)" 13
check "signatures preserved" \
  "$(find "$STAGING" -name '*.asc' | wc -l)" "$(find "$M2" -path "*/$VERSION/*" -name '*.asc' | wc -l)"

# every artifact file has all four checksums alongside it
missing=0
while IFS= read -r f; do
  for ext in md5 sha1 sha256 sha512; do
    [ -f "$f.$ext" ] || { echo "  missing .$ext for $f"; missing=$((missing + 1)); }
  done
done < <(find "$STAGING" -type f \( -name '*.jar' -o -name '*.pom' \))
check "every artifact has md5+sha1+sha256+sha512" "$missing" 0

check "no checksums of signatures" \
  "$(find "$STAGING" -name '*.asc.md5' | wc -l)" 0
check "no checksums of checksums" \
  "$(find "$STAGING" -name '*.sha1.md5' -o -name '*.md5.md5' | wc -l)" 0

# more than one bundle was produced
nbundles=$(find "$BUNDLES" -name '*.zip' | wc -l)
if [ "$nbundles" -gt 1 ]; then echo "ok   split into $nbundles bundles"; else echo "FAIL only $nbundles bundle"; fail=1; fi

for z in "$BUNDLES"/*.zip; do
  s=$(stat -c%s "$z")
  if [ "$s" -gt "$MAX" ]; then echo "FAIL $z is $s bytes > $MAX"; fail=1; fi
done
echo "ok   all bundles under limit"

# union of bundle contents == staged files, with no duplicates
for z in "$BUNDLES"/*.zip; do unzip -Z1 "$z"; done | grep -v '/$' | sort > "$TMP/in-bundles.txt"
(cd "$STAGING" && find . -type f | sed 's|^\./||' | sort) > "$TMP/staged.txt"
check "no file duplicated across bundles" \
  "$(uniq -d < "$TMP/in-bundles.txt" | wc -l)" 0
if diff -q "$TMP/staged.txt" "$TMP/in-bundles.txt" >/dev/null; then
  echo "ok   bundles cover every staged file exactly once"
else
  echo "FAIL bundle contents differ from staging:"; diff "$TMP/staged.txt" "$TMP/in-bundles.txt" | head; fail=1
fi

# no coordinate is split across two bundles
for z in "$BUNDLES"/*.zip; do unzip -Z1 "$z" | grep -v '/$' | xargs -r -n1 dirname; done | sort -u > "$TMP/dirs-all.txt"
for z in "$BUNDLES"/*.zip; do unzip -Z1 "$z" | grep -v '/$' | xargs -r -n1 dirname | sort -u; done | sort > "$TMP/dirs-per-bundle.txt"
check "no coordinate split across bundles" \
  "$(wc -l < "$TMP/dirs-per-bundle.txt")" "$(wc -l < "$TMP/dirs-all.txt")"

check "maven layout preserved at zip root" \
  "$(head -1 "$TMP/in-bundles.txt" | cut -d/ -f1-4)" "org/finos/legend/engine"

# --- size report ---------------------------------------------------------
check "report written" \
  "$([ -f "$BUNDLES/bundle-report.md" ] && echo yes || echo no)" yes
check "report row per bundle" \
  "$(tail -n +2 "$BUNDLES/bundles.tsv" | wc -l)" "$nbundles"
check "report bundle sizes match the zips on disk" \
  "$(tail -n +2 "$BUNDLES/bundles.tsv" | while IFS=$'\t' read -r name _ bytes _; do
       [ "$(stat -c%s "$BUNDLES/$name.zip")" = "$bytes" ] || echo bad
     done | wc -l)" 0
check "report sha256 matches the zips on disk" \
  "$(tail -n +2 "$BUNDLES/bundles.tsv" | while IFS=$'\t' read -r name _ _ _ sha; do
       [ "$(sha256sum "$BUNDLES/$name.zip" | cut -d' ' -f1)" = "$sha" ] || echo bad
     done | wc -l)" 0
check "every coordinate accounted for in the report" \
  "$(tail -n +2 "$BUNDLES/coordinates.tsv" | wc -l)" 13
check "every coordinate assigned to exactly one bundle" \
  "$(tail -n +2 "$BUNDLES/bundle-contents.tsv" | cut -f2 | sort | uniq -d | wc -l)" 0
check "bundle-contents covers all coordinates" \
  "$(tail -n +2 "$BUNDLES/bundle-contents.tsv" | cut -f2 | sort -u | wc -l)" 13
check "report header names the release version" \
  "$(grep -cF '| Release version | `'"$VERSION"'` |' "$BUNDLES/bundle-report.md")" 1
# the server module carries the 20MB shaded jar, so it must head the table
check "report ranks the largest coordinate first" \
  "$(grep -A4 '20 largest coordinates' "$BUNDLES/bundle-report.md" | tail -1 | grep -c 'legend-engine-server-http-server')" 1
check "report flags coordinates near the packing target" \
  "$(grep -c ':warning:' "$BUNDLES/bundle-report.md")" 0

# opt-in exclusions must actually drop files when configured
CENTRAL_EXCLUDE_GLOBS='*-tests.jar' bash "$SCRIPT" "$M2" "$VERSION" "$TMP/out-excl" "$MAX" >/dev/null
check "CENTRAL_EXCLUDE_GLOBS drops matching files" \
  "$(find "$TMP/out-excl/central-staging" -name '*-tests.jar' | wc -l)" 0

# a single oversized coordinate must fail loudly rather than emit a bad bundle
head -c $((40 * 1000 * 1000)) /dev/urandom > "$M2/legend-engine-xt-mod01/$VERSION/big.jar"
if bash "$SCRIPT" "$M2" "$VERSION" "$TMP/out2" "$MAX" >/dev/null 2>&1; then
  echo "FAIL oversized coordinate did not abort"; fail=1
else
  echo "ok   oversized single coordinate aborts"
fi
rm -f "$M2/legend-engine-xt-mod01/$VERSION/big.jar"

# an unknown release version must fail rather than publish an empty bundle
if bash "$SCRIPT" "$M2" 9.9.9 "$TMP/out3" "$MAX" >/dev/null 2>&1; then
  echo "FAIL unknown version did not abort"; fail=1
else
  echo "ok   unknown release version aborts"
fi

echo
if [ "$fail" -eq 0 ]; then echo "ALL PASS"; else echo "FAILURES"; exit 1; fi
