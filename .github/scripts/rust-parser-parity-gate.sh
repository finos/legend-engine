#!/usr/bin/env bash
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

# The Rust parser parity gate: merges the summaries RustGrammarParity writes when each test JVM exits
# (<report dir>/summary/*.json) and fails when
#   - a comparison was unexpected: neither a match nor named by the accepted list;
#   - the native library was unavailable, or nothing was compared;
#   - an accepted list entry was never used: it matches now, or its input is gone; remove it.
#
# usage: rust-parser-parity-gate.sh <report dir> <accepted list>

set -euo pipefail

dir=$1
list=$2

shopt -s nullglob
summaries=("$dir"/summary/*.json)
if [ ${#summaries[@]} -eq 0 ]; then
    echo "No parity summary in $dir/summary: did the tests run with the Rust parser in shadow mode?"
    exit 1
fi

merged=$(jq -s '
    def total(f): map(f // {}) | reduce .[] as $c ({}; reduce ($c | to_entries[]) as $e (.; .[$e.key] += $e.value));
    {
        counts: total(.counts),
        modelCounts: total(.modelCounts),
        unexpected: (map(.unexpected) | add | unique_by(.key)),
        hit: (map(.acceptedHit) | add | unique),
        unlinked: total(.unlinked)
    }' "${summaries[@]}")

echo "Rust parser parity, ${#summaries[@]} test JVMs:"
echo "  sections and expressions: $(jq -c '.counts' <<< "$merged")"
echo "  whole models: $(jq -c '.modelCounts' <<< "$merged")"
echo "  grammars this library doesn't link: $(jq -c '.unlinked' <<< "$merged")"

failed=0

unexpected=$(jq '.unexpected | length' <<< "$merged")
if [ "$unexpected" -gt 0 ]; then
    echo "::error::$unexpected unexpected comparison(s): fix them, or name them in $list"
    jq -r '.unexpected[] | "  \(.key)  \(.outcome)  (\(.input // "-"), \(.sourceId // "-")): \(.reason)"' <<< "$merged"
    failed=1
fi

unavailable=$(jq '(.counts.UNAVAILABLE // 0) + (.modelCounts.UNAVAILABLE // 0)' <<< "$merged")
if [ "$unavailable" -gt 0 ]; then
    echo "::error::the Rust parser was unavailable for $unavailable comparison(s)"
    failed=1
fi

compared=$(jq '(.counts.MATCH // 0) + (.counts.ERROR_MATCH // 0)' <<< "$merged")
if [ "$compared" -eq 0 ]; then
    echo "::error::nothing was compared"
    failed=1
fi

stale=$(jq -r --rawfile list "$list" '
    .hit as $hit
    | $list | split("\n") | map(sub("^\\s+"; "")) | map(select(length > 0 and (startswith("#") | not)))
    | map(split(" ")[0]) | map(select(. as $key | $hit | index($key) | not)) | .[]' <<< "$merged")
if [ -n "$stale" ]; then
    echo "::error::accepted list entries no comparison used; remove them from $list:"
    sed 's/^/  /' <<< "$stale"
    failed=1
fi

exit $failed
