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
# Central Publisher Portal API driver.
#
#   central-publish.sh upload <bundle.zip> <deployment-name>  -> prints deployment id
#   central-publish.sh await  <deployment-id> [target-state]  -> default VALIDATED
#   central-publish.sh state  <deployment-id>                 -> prints current state
#   central-publish.sh publish <deployment-id>
#   central-publish.sh drop    <deployment-id>
#   central-publish.sh cleanup <deployment-id>                -> drop if droppable
#   central-publish.sh await-central <artifactId> <version>   -> wait for repo1
#
# Uploads are always USER_MANAGED. With AUTOMATIC, a release split across N
# bundles has no safe failure mode: if bundle 5 fails validation, bundles 1-4
# are already irreversibly on Central. USER_MANAGED lets every bundle reach
# VALIDATED before any of them is published, and lets a failed run drop the
# validated-but-unpublished ones.
#
# Env:
#   CI_DEPLOY_USERNAME, CI_DEPLOY_PASSWORD  Portal token (required)
#   CENTRAL_API_BASE                        override for testing
#   CENTRAL_POLL_INTERVAL                   seconds between status polls (60)
#   CENTRAL_POLL_TIMEOUT                    seconds before giving up (3600)
#   CENTRAL_REPO_BASE                       read-side repository (repo1)
#   CENTRAL_GROUP_PATH                      group as a path, for await-central
#   CENTRAL_STATUS_DIR                      if set, raw status JSON is kept here
#   CENTRAL_ERROR_LINES                     max validation errors printed (200)

set -euo pipefail

CENTRAL_API_BASE=${CENTRAL_API_BASE:-https://central.sonatype.com/api/v1/publisher}
CENTRAL_POLL_INTERVAL=${CENTRAL_POLL_INTERVAL:-60}
CENTRAL_POLL_TIMEOUT=${CENTRAL_POLL_TIMEOUT:-3600}
CENTRAL_REPO_BASE=${CENTRAL_REPO_BASE:-https://repo1.maven.org/maven2}
CENTRAL_GROUP_PATH=${CENTRAL_GROUP_PATH:-org/finos/legend/engine}
CENTRAL_ERROR_LINES=${CENTRAL_ERROR_LINES:-200}

: "${CI_DEPLOY_USERNAME:?CI_DEPLOY_USERNAME is not set}"
: "${CI_DEPLOY_PASSWORD:?CI_DEPLOY_PASSWORD is not set}"

# -w0 because base64 wraps at 76 columns and a token longer than that would put
# a newline inside the Authorization header. '%s:%s' because the credentials
# are data, not a printf format string -- a '%' in the password corrupts it.
auth_token() {
  printf '%s:%s' "$CI_DEPLOY_USERNAME" "$CI_DEPLOY_PASSWORD" | base64 -w0
}

urlencode() {
  jq -rn --arg v "$1" '$v|@uri'
}

save_status() {
  local id=$1 response=$2
  [ -n "${CENTRAL_STATUS_DIR:-}" ] || return 0
  mkdir -p "$CENTRAL_STATUS_DIR"
  printf '%s' "$response" > "$CENTRAL_STATUS_DIR/$id.json"
}

# The Portal returns every validation error in a single JSON blob. Echoing that
# raw produces one enormous line, which the Actions runner drops outright: the
# 4.139.1 release failed validation and the log said only "FAILED", with no
# indication of why. Print one error per line, capped, and keep the raw
# response so a post-mortem has the whole thing.
report_errors() {
  local id=$1 response=$2 formatted total
  save_status "$id" "$response"
  formatted=$(jq -r '
    if (.errors | type) == "object" then
      .errors | to_entries[] as $e | $e.value[] | "\($e.key): \(.)"
    elif (.errors | type) == "array" then
      .errors[] | tostring
    else empty end' <<< "$response" 2>/dev/null || true)

  if [ -z "$formatted" ]; then
    echo "ERROR: the Portal returned no error detail for $id" >&2
    return 0
  fi

  total=$(printf '%s\n' "$formatted" | wc -l)
  printf '%s\n' "$formatted" | awk -v n="$CENTRAL_ERROR_LINES" 'NR <= n { print "ERROR:   " $0 }' >&2
  if [ "$total" -gt "$CENTRAL_ERROR_LINES" ]; then
    echo "ERROR:   ... and $((total - CENTRAL_ERROR_LINES)) more errors;" >&2
    echo "ERROR:   the full response is in the central-bundle-report artifact." >&2
  fi
}

cmd_upload() {
  local bundle=$1 name=$2 body code
  [ -f "$bundle" ] || { echo "ERROR: no such bundle: $bundle" >&2; exit 1; }
  body=$(mktemp)
  code=$(curl -sS -o "$body" -w '%{http_code}' -X POST \
    -H "Authorization: Bearer $(auth_token)" \
    --form "bundle=@$bundle" \
    "$CENTRAL_API_BASE/upload?publishingType=USER_MANAGED&name=$(urlencode "$name")")

  if [[ "$code" != 2* ]]; then
    echo "ERROR: upload of $bundle failed with HTTP $code" >&2
    echo "--- response body ---" >&2
    cat "$body" >&2
    echo >&2
    rm -f "$body"
    exit 1
  fi

  # The API returns the deployment id as a bare string, not JSON.
  tr -d '[:space:]' < "$body"
  echo
  rm -f "$body"
}

cmd_await() {
  local id=$1 target=${2:-VALIDATED}
  local deadline=$((SECONDS + CENTRAL_POLL_TIMEOUT))
  local state response consecutive_errors=0

  while :; do
    if [ "$SECONDS" -ge "$deadline" ]; then
      echo "ERROR: deployment $id did not reach $target within ${CENTRAL_POLL_TIMEOUT}s" >&2
      echo "ERROR: last observed state: ${state:-<none>}" >&2
      exit 1
    fi

    response=$(curl -sS -X POST -H "Authorization: Bearer $(auth_token)" \
      "$CENTRAL_API_BASE/status?id=$(urlencode "$id")" || true)

    # A transient API error yields an empty or non-JSON body. Retrying is
    # correct; treating it as a state is not -- jq would emit "null" and the
    # loop would spin against it until the job timed out.
    state=$(jq -r 'if type == "object" then (.deploymentState // empty) else empty end' <<< "$response" 2>/dev/null || true)
    if [ -z "$state" ]; then
      consecutive_errors=$((consecutive_errors + 1))
      echo "warning: unreadable status response for $id (attempt $consecutive_errors)" >&2
      if [ "$consecutive_errors" -ge 10 ]; then
        echo "ERROR: 10 consecutive unreadable status responses for $id" >&2
        echo "ERROR: last response: $response" >&2
        exit 1
      fi
      sleep "$CENTRAL_POLL_INTERVAL"
      continue
    fi
    consecutive_errors=0

    echo "deployment $id is $state"
    case "$state" in
      "$target")
        return 0
        ;;
      FAILED)
        echo "ERROR: deployment $id FAILED validation:" >&2
        report_errors "$id" "$response"
        # Deliberately not dropped: Sonatype support needs a FAILED
        # deployment's files to diagnose it.
        exit 1
        ;;
      PUBLISHED)
        # Only reachable when waiting for something else; PUBLISHED is terminal.
        echo "ERROR: deployment $id is already PUBLISHED, cannot reach $target" >&2
        exit 1
        ;;
    esac

    sleep "$CENTRAL_POLL_INTERVAL"
  done
}

# Single status read, no waiting. Used by failure cleanup to tell a
# droppable VALIDATED deployment from a FAILED one that must be kept.
cmd_state() {
  local id=$1 response state
  response=$(curl -sS -X POST -H "Authorization: Bearer $(auth_token)" \
    "$CENTRAL_API_BASE/status?id=$(urlencode "$id")" || true)
  state=$(jq -r 'if type == "object" then (.deploymentState // empty) else empty end' <<< "$response" 2>/dev/null || true)
  echo "${state:-UNKNOWN}"
}

# Wave 2 bundles can only validate once their parent POMs are resolvable from
# Central, so the release has to wait for wave 1 to become readable on the
# read side (repo1) -- reaching PUBLISHED on the Portal happens first, and is
# not the same thing.
cmd_await_central() {
  local artifact=$1 version=$2 url code
  local deadline=$((SECONDS + CENTRAL_POLL_TIMEOUT))
  url="$CENTRAL_REPO_BASE/$CENTRAL_GROUP_PATH/$artifact/$version/$artifact-$version.pom"

  while :; do
    code=$(curl -sS -o /dev/null -w '%{http_code}' "$url" || echo 000)
    if [ "$code" = "200" ]; then
      echo "$artifact:$version is readable on Central"
      return 0
    fi
    if [ "$SECONDS" -ge "$deadline" ]; then
      echo "ERROR: $artifact:$version did not appear on Central within" >&2
      echo "ERROR: ${CENTRAL_POLL_TIMEOUT}s (last HTTP $code, $url)" >&2
      exit 1
    fi
    echo "$artifact:$version not yet on Central (HTTP $code)"
    sleep "$CENTRAL_POLL_INTERVAL"
  done
}

# `drop` answers HTTP 400 while a deployment is still validating -- that is what
# happened to bundle 3 of the failed release, which was left behind. Wait for a
# terminal state, then drop only what is safe to drop. This never returns
# non-zero: it runs in a failure handler and must not mask the original error.
cmd_cleanup() {
  local id=$1 state
  local deadline=$((SECONDS + CENTRAL_POLL_TIMEOUT))

  while :; do
    state=$(cmd_state "$id")
    case "$state" in
      VALIDATED)
        cmd_drop "$id"
        return 0
        ;;
      FAILED)
        # Kept on purpose: Sonatype support needs a FAILED deployment's files.
        echo "leaving deployment $id in state FAILED"
        return 0
        ;;
      PUBLISHED|PUBLISHING|UNKNOWN)
        echo "leaving deployment $id in state $state"
        return 0
        ;;
    esac
    if [ "$SECONDS" -ge "$deadline" ]; then
      echo "warning: deployment $id still $state after ${CENTRAL_POLL_TIMEOUT}s, not dropped" >&2
      return 0
    fi
    echo "deployment $id is $state, waiting for a terminal state before dropping"
    sleep "$CENTRAL_POLL_INTERVAL"
  done
}

cmd_publish() {
  local id=$1 body code
  body=$(mktemp)
  code=$(curl -sS -o "$body" -w '%{http_code}' -X POST \
    -H "Authorization: Bearer $(auth_token)" \
    "$CENTRAL_API_BASE/deployment/$(urlencode "$id")")
  if [[ "$code" != 2* ]]; then
    echo "ERROR: publish of $id failed with HTTP $code" >&2
    cat "$body" >&2
    rm -f "$body"
    exit 1
  fi
  rm -f "$body"
  echo "requested publish of $id"
}

cmd_drop() {
  local id=$1 body code
  body=$(mktemp)
  code=$(curl -sS -o "$body" -w '%{http_code}' -X DELETE \
    -H "Authorization: Bearer $(auth_token)" \
    "$CENTRAL_API_BASE/deployment/$(urlencode "$id")")
  rm -f "$body"
  if [[ "$code" != 2* ]]; then
    echo "warning: could not drop deployment $id (HTTP $code)" >&2
    return 0
  fi
  echo "dropped deployment $id"
}

case "${1:-}" in
  upload)  cmd_upload  "${2:?missing bundle}" "${3:?missing deployment name}" ;;
  await)   cmd_await   "${2:?missing deployment id}" "${3:-VALIDATED}" ;;
  state)   cmd_state   "${2:?missing deployment id}" ;;
  publish) cmd_publish "${2:?missing deployment id}" ;;
  drop)    cmd_drop    "${2:?missing deployment id}" ;;
  cleanup) cmd_cleanup "${2:?missing deployment id}" ;;
  await-central)
           cmd_await_central "${2:?missing artifactId}" "${3:?missing version}" ;;
  *)
    echo "usage: central-publish.sh {upload <zip> <name>|await <id> [state]|state <id>|publish <id>|drop <id>|cleanup <id>|await-central <artifactId> <version>}" >&2
    exit 2
    ;;
esac
