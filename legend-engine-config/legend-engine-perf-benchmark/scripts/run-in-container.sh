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
#
# Runs the benchmark inside a Linux container, so a second environment can be measured and recorded
# without a second machine. The baseline keeps one entry per environment, so this adds to the file
# rather than replacing what is already there.
#
#   scripts/run-in-container.sh                     compare against this environment's entry
#   scripts/run-in-container.sh --rebase            record this environment's entry
#   scripts/run-in-container.sh --iters 8           any benchmark flag is passed through
#
# The container mounts the repository and the Maven repository at the same paths they have on the
# host, so the classpath resolved outside works unchanged inside.
set -euo pipefail

IMAGE="${PERF_IMAGE:-eclipse-temurin:17-jdk}"
HEAP="${PERF_HEAP:-6g}"
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
MODULE_PATH="legend-engine-config/legend-engine-perf-benchmark"
M2="${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}"
# A chained local repository, as the build guide suggests for isolating a branch, splits the jars
# across two directories; both have to be visible inside the container.
TAIL="${MAVEN_REPO_LOCAL_TAIL:-}"
CP_FILE="$(mktemp -t perf-cp)"

echo "resolving classpath on the host"
(cd "$REPO" && mvn -B -q -pl "$MODULE_PATH" dependency:build-classpath \
    -Dmdep.outputFile="$CP_FILE" -Dmaven.repo.local="$M2" \
    ${TAIL:+-Dmaven.repo.local.tail="$TAIL"})

echo "running in $IMAGE"
docker run --rm \
  -v "$REPO:$REPO" \
  -v "$M2:$M2:ro" \
  ${TAIL:+-v "$TAIL:$TAIL:ro"} \
  -v "$CP_FILE:$CP_FILE:ro" \
  -w "$REPO/$MODULE_PATH" \
  "$IMAGE" \
  sh -c "java -Xmx$HEAP -Xss8m -cp \"target/classes:\$(cat $CP_FILE)\" org.finos.legend.engine.perf.PipelineBench $*"
