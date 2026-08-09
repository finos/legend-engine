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

set -euo pipefail

CENTRAL_API_BASE=${CENTRAL_API_BASE:-https://central.sonatype.com/api/v1/publisher}
CENTRAL_POLL_INTERVAL=${CENTRAL_POLL_INTERVAL:-60}
CENTRAL_POLL_TIMEOUT=${CENTRAL_POLL_TIMEOUT:-3600}

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
        echo "ERROR: deployment $id FAILED" >&2
        echo "$response" >&2
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
  *)
    echo "usage: central-publish.sh {upload <zip> <name>|await <id> [state]|state <id>|publish <id>|drop <id>}" >&2
    exit 2
    ;;
esac
