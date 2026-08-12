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
# Wait for every deployment in a wave to reach a target state.
#
#   await-wave.sh <ids-file> [target-state]
#
# Every deployment is awaited even after one of them fails. The failed 4.139.1
# release aborted on the first bundle, so the outcome of the other two was
# never reported and the post-mortem had a third of the evidence it needed.

set -euo pipefail

IDS_FILE=${1:?usage: await-wave.sh <ids-file> [target-state]}
TARGET=${2:-VALIDATED}
PUBLISH="$(cd "$(dirname "$0")" && pwd)/central-publish.sh"

[ -s "$IDS_FILE" ] || { echo "ERROR: $IDS_FILE is empty or missing" >&2; exit 1; }

failed=0
while read -r id; do
  [ -n "$id" ] || continue
  "$PUBLISH" await "$id" "$TARGET" < /dev/null || failed=$((failed + 1))
done < "$IDS_FILE"

if [ "$failed" -ne 0 ]; then
  echo "ERROR: $failed of $(grep -c . "$IDS_FILE") deployment(s) did not reach $TARGET" >&2
  exit 1
fi

echo "all deployments in $IDS_FILE reached $TARGET"
