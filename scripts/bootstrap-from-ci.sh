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

# Bootstrap a local checkout from the finos/legend-engine `build.yml` GitHub
# Actions artifacts instead of running a full `mvn clean install`.
#
# Downloads and installs:
#   - m2-repo:      ~/.m2/repository/org/finos/legend/engine/  (built jars)
#   - build-output: every module's target/ (compiled classes, generated
#                    sources, etc. — not needed for -pl builds against the
#                    local repo, but useful for IDE indexing and running
#                    tests in place)
#
# Requires: gh CLI, authenticated, with read access to finos/legend-engine.
#
# Usage: scripts/bootstrap-from-ci.sh [--target-only|--m2-only] [worktree-path]
#
# Caveat: both artifacts are retained for only 1 day (retention-days: 1 in
# build.yml as of 2026-09-01), and only exist for commits built on
# finos/legend-engine's own master (fork/PR builds don't upload them, and
# release commits are skipped). If no run matches, this falls back to the
# newest successful master run and leaves the delta to be built normally.

set -euo pipefail

MODE="both"
WT_PATH="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

for arg in "$@"; do
    case "$arg" in
        --target-only) MODE="target" ;;
        --m2-only) MODE="m2" ;;
        *) WT_PATH="$arg" ;;
    esac
done

REPO="finos/legend-engine"
LOCAL_M2="${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}"

command -v gh >/dev/null || { echo "error: gh CLI is required" >&2; exit 1; }

echo "==> finding the newest finos/legend-engine master build reachable from HEAD"
BASE=""
if git -C "$WT_PATH" remote get-url upstream >/dev/null 2>&1; then
    git -C "$WT_PATH" fetch upstream master --quiet || true
    BASE="$(git -C "$WT_PATH" merge-base HEAD upstream/master 2>/dev/null || true)"
fi

RUN_ID=""
if [ -n "$BASE" ]; then
    RUN_ID="$(gh run list -R "$REPO" --workflow build.yml --branch master --status success -L 50 \
        --json databaseId,headSha -q ".[] | select(.headSha==\"$BASE\") | .databaseId" | head -1)"
fi

if [ -z "$RUN_ID" ]; then
    echo "    no run found for merge-base ${BASE:-<no upstream remote>}, falling back to newest successful master run"
    RUN_ID="$(gh run list -R "$REPO" --workflow build.yml --branch master --status success -L 1 \
        --json databaseId -q '.[0].databaseId')"
fi

if [ -z "$RUN_ID" ]; then
    echo "error: could not find any successful build.yml run on master" >&2
    exit 1
fi
echo "    using run $RUN_ID"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

if [ "$MODE" = "both" ] || [ "$MODE" = "m2" ]; then
    echo "==> downloading m2-repo artifact"
    mkdir -p "$LOCAL_M2/org/finos/legend/engine"
    gh run download -R "$REPO" "$RUN_ID" -n m2-repo -D "$LOCAL_M2/org/finos/legend/engine"
    echo "    installed into $LOCAL_M2/org/finos/legend/engine"
fi

if [ "$MODE" = "both" ] || [ "$MODE" = "target" ]; then
    echo "==> downloading build-output artifact"
    gh run download -R "$REPO" "$RUN_ID" -n build-output -D "$TMP/build-output"

    # The upload glob (~/**/target/) bakes in the runner's home-relative
    # checkout path, e.g. work/legend-engine/legend-engine/<module>/target.
    # Find that prefix by locating a directory that also exists in this
    # worktree, then splice target/ trees in under the matching module.
    echo "==> locating checkout-path prefix inside the archive"
    SRC_TARGET_DIR="$(find "$TMP/build-output" -type d -name target -not -path '*/target/*' | head -1)"
    if [ -z "$SRC_TARGET_DIR" ]; then
        echo "error: no target/ directories found in build-output artifact" >&2
        exit 1
    fi
    ARCHIVE_ROOT="$TMP/build-output"

    count=0
    while IFS= read -r -d '' t; do
        rel="${t#"$ARCHIVE_ROOT"/}"
        module_rel="$(dirname "$rel")"
        dest="$WT_PATH/$module_rel/target"
        if [ -d "$WT_PATH/$module_rel" ]; then
            rm -rf "$dest"
            mkdir -p "$dest"
            cp -a "$t/." "$dest/"
            count=$((count + 1))
        fi
    done < <(find "$ARCHIVE_ROOT" -type d -name target -not -path '*/target/*' -print0)
    echo "    spliced $count target/ directories into $WT_PATH"

    echo "==> touching target/ trees so they read as newer than checked-out sources"
    find "$WT_PATH" -path '*/target/*' -exec touch {} + 2>/dev/null || true
fi

echo "==> resolving third-party dependencies and plugins"
(cd "$WT_PATH" && mvn -q de.qaware.maven:go-offline-maven-plugin:resolve-dependencies) || \
    echo "    warning: dependency resolution step failed, run it manually"

echo "==> done"
