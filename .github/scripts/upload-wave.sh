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
# Upload every bundle of one wave to the Central Publisher Portal.
#
#   upload-wave.sh <wave> <release-version>
#
# Reads   central-publishing/<wave>-bundle-*.zip
# Writes  <wave>-ids.txt          deployment ids for this wave
#         deployment-ids.txt      appended: every deployment, for cleanup
#         bundle-deployments.tsv  appended: bundle -> size -> deployment id

set -euo pipefail

WAVE=${1:?usage: upload-wave.sh <wave> <release-version>}
RELEASE_VERSION=${2:?missing release version}
PUBLISH="$(cd "$(dirname "$0")" && pwd)/central-publish.sh"
SUMMARY=${GITHUB_STEP_SUMMARY:-/dev/null}

shopt -s nullglob
bundles=(central-publishing/"$WAVE"-bundle-*.zip)

# A wave that matches nothing would upload nothing and exit 0, and the release
# would be declared successful with those artifacts missing from Central.
if [ ${#bundles[@]} -eq 0 ]; then
  echo "ERROR: no bundles matched central-publishing/$WAVE-bundle-*.zip" >&2
  exit 1
fi

: > "$WAVE-ids.txt"

for bundle in "${bundles[@]}"; do
  base=$(basename "$bundle" .zip)
  bytes=$(stat -c%s "$bundle")
  echo "uploading $bundle ($bytes bytes)"
  id=$("$PUBLISH" upload "$bundle" "legend-engine-${RELEASE_VERSION}-${base}" < /dev/null)
  echo "$id" >> "$WAVE-ids.txt"
  echo "$id" >> deployment-ids.txt
  printf '%s\t%s\t%s\n' "$base.zip" "$bytes" "$id" >> bundle-deployments.tsv
  echo "uploaded $bundle as deployment $id"
  # shellcheck disable=SC2016  # the backticks are markdown, not a subshell
  printf '| %s | %s MB | `%s` |\n' "$base.zip" "$((bytes / 1000000))" "$id" >> "$SUMMARY"
done

echo "uploaded ${#bundles[@]} bundle(s) for $WAVE"
