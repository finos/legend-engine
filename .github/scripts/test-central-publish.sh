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
# Drives central-publish.sh against a local mock of the Central Publisher
# Portal API. No network, no credentials, nothing reaches Central. The mock
# asserts on the Authorization header and the publishingType, which is what
# makes the base64/printf and USER_MANAGED fixes actually verifiable.

set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")" && pwd)/central-publish.sh"
TMP=$(mktemp -d)
trap 'kill "${SERVER_PID:-}" 2>/dev/null || true; rm -rf "$TMP"' EXIT

# Deliberately awkward credentials:
#  - a '%' in the password catches using it as a printf format string
#  - 76 combined bytes push base64 past its 76-column wrap point
export CI_DEPLOY_USERNAME='finos-ci-deploy-user-0123456789'
export CI_DEPLOY_PASSWORD='tok%en-abcdefghijklmnopqrstuvwxyz-0123456789'
export EXPECTED_AUTH="$CI_DEPLOY_USERNAME:$CI_DEPLOY_PASSWORD"

fail=0
check() { if [ "$2" = "$3" ]; then echo "ok   $1"; else echo "FAIL $1: expected '$3' got '$2'"; fail=1; fi; }

cat > "$TMP/mock.py" <<'PY'
import base64, http.server, json, os, re, sys, threading, urllib.parse

EXPECTED = os.environ["EXPECTED_AUTH"]
errors = []
deploys = {}
repo_reads = {}
counter = [0]
lock = threading.Lock()


class Handler(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *a):
        pass

    def send(self, code, body, ctype="text/plain"):
        raw = body.encode()
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def check_auth(self):
        header = self.headers.get("Authorization", "")
        if not header.startswith("Bearer "):
            errors.append("Authorization header missing or not Bearer: %r" % header)
            return False
        token = header[len("Bearer "):]
        if re.search(r"\s", token):
            errors.append("whitespace inside base64 token: %r" % token)
            return False
        try:
            decoded = base64.b64decode(token, validate=True).decode()
        except Exception:
            errors.append("token is not valid base64: %r" % token)
            return False
        if decoded != EXPECTED:
            errors.append("token decodes to %r, expected %r" % (decoded, EXPECTED))
            return False
        return True

    def body(self):
        n = int(self.headers.get("Content-Length", 0))
        return self.rfile.read(n) if n else b""

    def do_DELETE(self):
        path = urllib.parse.urlparse(self.path).path
        if not self.check_auth():
            return self.send(401, "unauthorized")
        m = re.match(r"^/deployment/(.+)$", path)
        if m and m.group(1) in deploys:
            deploys[m.group(1)]["dropped"] = True
            return self.send(204, "")
        return self.send(404, "not found")

    def do_GET(self):
        if self.path == "/_report":
            return self.send(200, json.dumps({"errors": errors, "deploys": deploys}), "application/json")
        # Stands in for repo1.maven.org: a freshly published artifact is not
        # readable immediately, so the first two reads 404.
        if self.path.startswith("/repo/"):
            with lock:
                repo_reads[self.path] = repo_reads.get(self.path, 0) + 1
                n = repo_reads[self.path]
            if "never" in self.path or n <= 2:
                return self.send(404, "not found")
            return self.send(200, "<project/>", "text/xml")
        return self.send(404, "not found")

    def do_POST(self):
        parsed = urllib.parse.urlparse(self.path)
        query = urllib.parse.parse_qs(parsed.query)
        payload = self.body()
        if not self.check_auth():
            return self.send(401, "unauthorized")

        if parsed.path == "/upload":
            publishing_type = (query.get("publishingType") or [None])[0]
            if publishing_type != "USER_MANAGED":
                errors.append("publishingType was %r, expected USER_MANAGED" % publishing_type)
            name = (query.get("name") or [None])[0]
            if b"PK\x03\x04" not in payload:
                errors.append("uploaded body for %r contains no zip" % name)
            with lock:
                counter[0] += 1
                deployment_id = "deployment-%02d" % counter[0]
            deploys[deployment_id] = {
                "name": name,
                "publishingType": publishing_type,
                "polls": 0,
                "publishRequested": False,
                "dropped": False,
                "bytes": len(payload),
            }
            return self.send(201, deployment_id)

        if parsed.path == "/status":
            deployment_id = (query.get("id") or [None])[0]
            deployment = deploys.get(deployment_id)
            if deployment is None:
                return self.send(404, "unknown deployment")
            deployment["polls"] += 1
            name = deployment["name"] or ""
            polls = deployment["polls"]

            if "flaky" in name and polls <= 3:
                # What a proxy hiccup actually looks like: HTML, not JSON.
                return self.send(502, "<html><body><center><h1>502 Bad Gateway</h1></center></body></html>", "text/html")
            if "fail" in name:
                return self.send(200, json.dumps({
                    "deploymentId": deployment_id,
                    "deploymentState": "FAILED",
                    "errors": {
                        "org/finos/legend/engine/legend-engine-core/4.100.0/legend-engine-core-4.100.0.pom": [
                            "Parent POM not found: org.finos.legend.engine:legend-engine:4.100.0",
                        ],
                        "org/finos/legend/engine/legend-engine-config/4.100.0/legend-engine-config-4.100.0.pom": [
                            "Parent POM not found: org.finos.legend.engine:legend-engine:4.100.0",
                        ],
                    },
                }), "application/json")
            if "hang" in name:
                return self.send(200, json.dumps({"deploymentState": "VALIDATING"}), "application/json")

            if deployment["publishRequested"]:
                state = "PUBLISHING" if polls % 2 == 0 else "PUBLISHED"
            elif polls <= 1:
                state = "PENDING"
            else:
                state = "VALIDATED"
            return self.send(200, json.dumps({"deploymentState": state}), "application/json")

        m = re.match(r"^/deployment/(.+)$", parsed.path)
        if m:
            deployment = deploys.get(m.group(1))
            if deployment is None:
                return self.send(404, "unknown deployment")
            deployment["publishRequested"] = True
            deployment["polls"] = 0
            return self.send(204, "")

        return self.send(404, "not found")


server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
with open(sys.argv[1], "w") as fh:
    fh.write(str(server.server_address[1]))
server.serve_forever()
PY

python3 "$TMP/mock.py" "$TMP/port" &
SERVER_PID=$!
for _ in $(seq 1 50); do [ -s "$TMP/port" ] && break; sleep 0.1; done
[ -s "$TMP/port" ] || { echo "FAIL mock server did not start"; exit 1; }

export CENTRAL_API_BASE="http://127.0.0.1:$(cat "$TMP/port")"
export CENTRAL_POLL_INTERVAL=1
export CENTRAL_POLL_TIMEOUT=30
export CENTRAL_STATUS_DIR="$TMP/status"

printf 'PK\x03\x04fake zip content' > "$TMP/bundle.zip"

echo "=== assertions ==="

id1=$(bash "$SCRIPT" upload "$TMP/bundle.zip" "legend-engine-4.100.0-bundle-01")
check "upload returns a deployment id" "$id1" "deployment-01"

bash "$SCRIPT" await "$id1" VALIDATED > "$TMP/await1.log" 2>&1 \
  && echo "ok   await reaches VALIDATED" \
  || { echo "FAIL await did not reach VALIDATED"; cat "$TMP/await1.log"; fail=1; }
check "await tolerates non-terminal states first" \
  "$(grep -c 'is PENDING' "$TMP/await1.log")" 1

bash "$SCRIPT" publish "$id1" > /dev/null
bash "$SCRIPT" await "$id1" PUBLISHED > "$TMP/await2.log" 2>&1 \
  && echo "ok   await reaches PUBLISHED after publish" \
  || { echo "FAIL await did not reach PUBLISHED"; cat "$TMP/await2.log"; fail=1; }

# transient 502s with an HTML body must be retried, not parsed as a state
id2=$(bash "$SCRIPT" upload "$TMP/bundle.zip" "legend-engine-4.100.0-bundle-flaky")
bash "$SCRIPT" await "$id2" VALIDATED > "$TMP/await3.log" 2>&1 \
  && echo "ok   await survives transient API errors" \
  || { echo "FAIL await died on a transient error"; cat "$TMP/await3.log"; fail=1; }
check "transient errors were retried, not treated as state" \
  "$(grep -c 'unreadable status response' "$TMP/await3.log")" 3

# a FAILED deployment must abort, and must not be dropped
id3=$(bash "$SCRIPT" upload "$TMP/bundle.zip" "legend-engine-4.100.0-bundle-fail")
if bash "$SCRIPT" await "$id3" VALIDATED > "$TMP/await4.log" 2>&1; then
  echo "FAIL await returned success on a FAILED deployment"; fail=1
else
  echo "ok   FAILED deployment aborts"
fi

# The 4.139.1 post-mortem: the raw status JSON was echoed as one enormous line
# and the runner dropped it, so the log said only "FAILED" and the release was
# undiagnosable. Every validation error must appear as its own line.
check "failure log names the failing file" \
  "$(grep -c 'legend-engine-core-4.100.0.pom' "$TMP/await4.log")" 1
check "failure log carries the Portal's message" \
  "$(grep -c 'Parent POM not found' "$TMP/await4.log")" 2
check "failure log puts each error on its own line" \
  "$(awk 'length > 200' "$TMP/await4.log" | wc -l)" 0
check "raw status JSON kept for post-mortem" \
  "$([ -s "$CENTRAL_STATUS_DIR/$id3.json" ] && echo yes || echo no)" yes
check "kept JSON is the untouched API response" \
  "$(jq -r '.deploymentState' "$CENTRAL_STATUS_DIR/$id3.json")" FAILED

# --- wave 1 must be readable on Central before wave 2 is uploaded ---------
export CENTRAL_REPO_BASE="$CENTRAL_API_BASE/repo"
bash "$SCRIPT" await-central legend-engine 4.100.0 > "$TMP/repo1.log" 2>&1 \
  && echo "ok   await-central waits for the artifact to appear" \
  || { echo "FAIL await-central never saw the artifact"; cat "$TMP/repo1.log"; fail=1; }
check "await-central polled past the initial 404s" \
  "$(grep -c 'not yet on Central' "$TMP/repo1.log")" 2

if CENTRAL_POLL_TIMEOUT=3 bash "$SCRIPT" await-central legend-engine-never 4.100.0 \
     > "$TMP/repo2.log" 2>&1; then
  echo "FAIL await-central succeeded on an artifact that never appeared"; fail=1
else
  echo "ok   await-central times out rather than hanging"
fi

# --- cleanup ---------------------------------------------------------------
# `drop` on a deployment still VALIDATING returns HTTP 400, which is what
# happened to bundle 3 of the failed release. Cleanup must wait for a terminal
# state, and must never turn a real failure into a cleanup failure.
id7=$(bash "$SCRIPT" upload "$TMP/bundle.zip" "legend-engine-4.100.0-bundle-07")
bash "$SCRIPT" cleanup "$id7" > "$TMP/cleanup1.log" 2>&1 \
  && echo "ok   cleanup succeeds on a droppable deployment" \
  || { echo "FAIL cleanup errored"; cat "$TMP/cleanup1.log"; fail=1; }

bash "$SCRIPT" cleanup "$id3" > "$TMP/cleanup2.log" 2>&1 \
  && echo "ok   cleanup succeeds on a FAILED deployment" \
  || { echo "FAIL cleanup errored on FAILED deployment"; cat "$TMP/cleanup2.log"; fail=1; }

id8=$(bash "$SCRIPT" upload "$TMP/bundle.zip" "legend-engine-4.100.0-bundle-hang-2")
if CENTRAL_POLL_TIMEOUT=3 bash "$SCRIPT" cleanup "$id8" > "$TMP/cleanup3.log" 2>&1; then
  echo "ok   cleanup of a stuck deployment does not fail the job"
else
  echo "FAIL cleanup of a stuck deployment returned non-zero"; cat "$TMP/cleanup3.log"; fail=1
fi

# --- the wave drivers the workflow calls ----------------------------------
SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
WORK="$TMP/work"
mkdir -p "$WORK/central-publishing"
export GITHUB_STEP_SUMMARY="$WORK/summary.md"
for n in 01 02; do cp "$TMP/bundle.zip" "$WORK/central-publishing/wave2-bundle-$n.zip"; done
cp "$TMP/bundle.zip" "$WORK/central-publishing/wave1-bundle-01.zip"
: > "$WORK/deployment-ids.txt"
: > "$WORK/bundle-deployments.tsv"

(cd "$WORK" && bash "$SCRIPTS_DIR/upload-wave.sh" wave2 4.100.0) > "$TMP/uw.log" 2>&1 \
  && echo "ok   upload-wave uploads a wave" \
  || { echo "FAIL upload-wave errored"; cat "$TMP/uw.log"; fail=1; }
check "upload-wave records one id per bundle" \
  "$(wc -l < "$WORK/wave2-ids.txt")" 2
check "upload-wave appends to the combined id list" \
  "$(wc -l < "$WORK/deployment-ids.txt")" 2
check "upload-wave names the deployment after the bundle" \
  "$(curl -sS "$CENTRAL_API_BASE/_report" | jq -r '[.deploys[] | select(.name == "legend-engine-4.100.0-wave2-bundle-01")] | length')" 1

# An empty wave would otherwise upload nothing and silently "succeed",
# publishing a release that is missing every artifact in it.
if (cd "$WORK" && bash "$SCRIPTS_DIR/upload-wave.sh" wave9 4.100.0) > /dev/null 2>&1; then
  echo "FAIL upload-wave accepted a wave with no bundles"; fail=1
else
  echo "ok   upload-wave aborts when a wave has no bundles"
fi

# The failed release only ever reported bundle 1: the loop aborted on the first
# failure, so the states of bundles 2 and 3 were never learned.
idf1=$(bash "$SCRIPT" upload "$TMP/bundle.zip" "legend-engine-4.100.0-bundle-fail-a")
idf2=$(bash "$SCRIPT" upload "$TMP/bundle.zip" "legend-engine-4.100.0-bundle-fail-b")
printf '%s\n%s\n' "$idf1" "$idf2" > "$TMP/failing-ids.txt"
if bash "$SCRIPTS_DIR/await-wave.sh" "$TMP/failing-ids.txt" VALIDATED > "$TMP/aw.log" 2>&1; then
  echo "FAIL await-wave succeeded despite failing bundles"; fail=1
else
  echo "ok   await-wave fails when a bundle fails"
fi
check "await-wave reports every failing bundle, not just the first" \
  "$(grep -c 'FAILED validation' "$TMP/aw.log")" 2

bash "$SCRIPTS_DIR/await-wave.sh" "$WORK/wave2-ids.txt" VALIDATED > "$TMP/aw2.log" 2>&1 \
  && echo "ok   await-wave succeeds when every bundle validates" \
  || { echo "FAIL await-wave failed on healthy bundles"; cat "$TMP/aw2.log"; fail=1; }

# a deployment that never validates must hit the deadline, not spin forever
start=$SECONDS
if CENTRAL_POLL_TIMEOUT=3 bash "$SCRIPT" await \
     "$(bash "$SCRIPT" upload "$TMP/bundle.zip" "legend-engine-4.100.0-bundle-hang")" \
     VALIDATED > "$TMP/await5.log" 2>&1; then
  echo "FAIL stuck deployment did not time out"; fail=1
else
  elapsed=$((SECONDS - start))
  if [ "$elapsed" -le 20 ]; then
    echo "ok   stuck deployment times out after ${elapsed}s"
  else
    echo "FAIL timeout took ${elapsed}s, deadline not honoured"; fail=1
  fi
fi
check "timeout message names the deadline" \
  "$(grep -c 'did not reach VALIDATED within' "$TMP/await5.log")" 1

# drop must work for cleanup on failure
id6=$(bash "$SCRIPT" upload "$TMP/bundle.zip" "legend-engine-4.100.0-bundle-06")
bash "$SCRIPT" drop "$id6" > /dev/null
report=$(curl -sS "$CENTRAL_API_BASE/_report")
check "drop marks the deployment dropped" \
  "$(jq -r --arg id "$id6" '.deploys[$id].dropped' <<< "$report")" true
check "FAILED deployment was NOT dropped" \
  "$(jq -r --arg id "$id3" '.deploys[$id].dropped' <<< "$report")" false

# the assertions the mock itself made
check "every request carried a well-formed auth header" \
  "$(jq -r '[.errors[] | select(test("token|Authorization"))] | length' <<< "$report")" 0
check "no server-side protocol errors at all" \
  "$(jq -r '.errors | length' <<< "$report")" 0
check "every upload was USER_MANAGED" \
  "$(jq -r '[.deploys[] | select(.publishingType != "USER_MANAGED")] | length' <<< "$report")" 0
check "deployment name reached the API" \
  "$(jq -r --arg id "$id1" '.deploys[$id].name' <<< "$report")" "legend-engine-4.100.0-bundle-01"

echo
if [ "$fail" -eq 0 ]; then echo "ALL PASS"; else echo "FAILURES"; exit 1; fi
