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
"""Work out which bridge HTTP port a client should talk to.

Every tool used to hardcode its own default: 8991 in server_cli/client_cli and
pure-lsp-launch-engine, and 8992 in the PostToolUse sync hook and pure-lsp-option. Nothing ever
listened on 8992, so the sync hook - whose whole design is
to exit 0 silently when it can't reach a bridge - was an unobservable no-op against every
session the launchers actually produce. Resolving through here instead of a literal is what
stops that class of bug coming back.

Order:
  1. $PURE_LSP_PORT           - an explicit override always wins.
  2. A live bridge discovered from the /tmp/pure_lsp_server_<port>.json sidecar server_cli
     writes at launch. Most recently started listening bridge wins.
  3. DEFAULT_PORT.
"""
import glob
import json
import os
import socket

DEFAULT_PORT = 8991

# server_cli._write_metadata_sidecar writes one of these per bridge, keyed by HTTP port.
SIDECAR_GLOB = "/tmp/pure_lsp_server_*.json"


def _listening(port, timeout=0.15):
    """Cheap loopback liveness probe. A sidecar outlives a SIGKILLed bridge, so the file
    existing is not evidence the port is served."""
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=timeout):
            return True
    except OSError:
        return False


def discover_ports(probe=_listening):
    """Ports of bridges that have a sidecar AND are actually accepting connections,
    most-recently-started first."""
    candidates = []
    for path in glob.glob(SIDECAR_GLOB):
        try:
            with open(path) as f:
                data = json.load(f)
        except (OSError, ValueError):
            continue  # unreadable/half-written sidecar is normal - skip it
        if not isinstance(data, dict):
            continue
        port = data.get("port")
        if not isinstance(port, int):
            continue
        started_at = data.get("started_at")
        candidates.append((started_at if isinstance(started_at, (int, float)) else 0, port))
    candidates.sort(reverse=True)
    seen = set()
    live = []
    for _, port in candidates:
        if port in seen:
            continue
        seen.add(port)
        if probe(port):
            live.append(port)
    return live


def resolve_port(default=DEFAULT_PORT, probe=_listening):
    env = os.environ.get("PURE_LSP_PORT")
    if env:
        try:
            return int(env)
        except ValueError:
            pass  # a garbage override shouldn't be fatal; fall through to discovery
    live = discover_ports(probe=probe)
    if live:
        return live[0]
    return default
