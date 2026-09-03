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

# Create a git worktree for parallel feature development, seeded with an
# isolated Maven local repository so builds in this worktree never collide
# with builds in any other worktree or checkout that share the same
# <version>-SNAPSHOT coordinates.
#
# Usage: scripts/wt-new.sh <feature-branch-name> [path]
#
# What it does:
#   1. git worktree add <path> -b <feature-branch-name>
#   2. Seeds <path>/**/target/ by copying it from this checkout (so the new
#      worktree starts with already-compiled classes) and touches it so it
#      reads as newer than the freshly checked-out sources.
#   3. Seeds a private local Maven repo at ~/.m2-wt/<feature-branch-name>:
#        - third-party deps are hardlinked (cheap, and safe: those jars are
#          never rewritten in place once resolved)
#        - org/finos/legend/engine and org/finos/legend/pure are real
#          copies (these get overwritten by every `mvn install` in the
#          worktree, so they must not share inodes with the source repo)
#   4. Writes <path>/.mvn/maven.config pointing -Dmaven.repo.local at that
#      private repo. .mvn/** is gitignored, so this is invisible to git and
#      picked up automatically by any `mvn` invocation (and IntelliJ import)
#      run from inside the worktree.
#
# Cleanup: git worktree remove <path> && rm -rf ~/.m2-wt/<feature-branch-name>

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "usage: $0 <feature-branch-name> [worktree-path]" >&2
    exit 1
fi

FEATURE="$1"
SRC_ROOT="$(git rev-parse --show-toplevel)"
WT_PATH="${2:-$(dirname "$SRC_ROOT")/$(basename "$SRC_ROOT")-${FEATURE}}"
LOCAL_M2="${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}"
WT_M2="$HOME/.m2-wt/${FEATURE}"

if [ -e "$WT_PATH" ]; then
    echo "error: $WT_PATH already exists" >&2
    exit 1
fi

if [ -e "$WT_M2" ]; then
    echo "error: $WT_M2 already exists (private repo for feature '$FEATURE' already seeded)" >&2
    exit 1
fi

echo "==> creating worktree at $WT_PATH (branch $FEATURE)"
git -C "$SRC_ROOT" worktree add "$WT_PATH" -b "$FEATURE"

echo "==> seeding target/ directories from $SRC_ROOT"
target_count=0
while IFS= read -r -d '' t; do
    rel="${t#"$SRC_ROOT"/}"
    dest="$WT_PATH/$rel"
    mkdir -p "$(dirname "$dest")"
    cp -a "$t" "$dest"
    target_count=$((target_count + 1))
done < <(find "$SRC_ROOT" -type d -name target -not -path '*/target/*' -print0)
echo "    copied $target_count target/ directories"

echo "==> touching copied target/ trees so they read as newer than checked-out sources"
find "$WT_PATH" -path '*/target/*' -exec touch {} + 2>/dev/null || true

echo "==> seeding private local repo at $WT_M2"
mkdir -p "$WT_M2"
if [ -d "$LOCAL_M2" ]; then
    # Hardlink everything cheaply, then break the links Maven mutates in
    # place, then take real copies of the coordinates this repo builds
    # (they get overwritten by every install and must not share inodes
    # with the source repo's copy).
    cp -al "$LOCAL_M2/." "$WT_M2/" 2>/dev/null || cp -a "$LOCAL_M2/." "$WT_M2/"

    find "$WT_M2" \( -name '_remote.repositories' -o -name 'resolver-status.properties' -o -name 'maven-metadata-local.xml' \) -print0 \
        | while IFS= read -r -d '' f; do
            tmp="${f}.wtcopy"
            cp --remove-destination "$f" "$tmp" 2>/dev/null && mv "$tmp" "$f" || true
        done

    for coord in org/finos/legend/engine org/finos/legend/pure; do
        if [ -d "$LOCAL_M2/$coord" ]; then
            rm -rf "${WT_M2:?}/$coord"
            cp -a "$LOCAL_M2/$coord" "$WT_M2/$coord"
        fi
    done
else
    echo "    warning: $LOCAL_M2 does not exist, private repo starts empty"
fi

echo "==> writing $WT_PATH/.mvn/maven.config"
mkdir -p "$WT_PATH/.mvn"
echo "-Dmaven.repo.local=$WT_M2" > "$WT_PATH/.mvn/maven.config"

echo "==> done"
echo "worktree:     $WT_PATH"
echo "private repo: $WT_M2"
echo
echo "cleanup: git worktree remove $WT_PATH && rm -rf $WT_M2"
