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
                return self.send(200, json.dumps({"deploymentState": "FAILED", "errors": ["missing signature"]}), "application/json")
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
