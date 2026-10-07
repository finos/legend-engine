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
"""Tests for bridge-port resolution.

Every tool used to hardcode its own default and they disagreed: 8991 in server_cli/client_cli and
pure-lsp-launch-engine, 8992 in the sync hook and pure-lsp-option.
Nothing ever bound 8992, so the sync hook - designed to no-op silently when it can't reach a
bridge - was invisible against every session the launchers actually produce. These tests pin the
resolution order so that can't come back.
"""
import json

import pytest

from pure_lsp_bridge import discovery


@pytest.fixture
def sidecars(tmp_path, monkeypatch):
    """Redirect the sidecar glob into tmp_path and return a writer for fake sidecars."""
    monkeypatch.setattr(discovery, "SIDECAR_GLOB", str(tmp_path / "pure_lsp_server_*.json"))

    def _write(port, started_at, **extra):
        payload = {"port": port, "started_at": started_at}
        payload.update(extra)
        (tmp_path / ("pure_lsp_server_%s.json" % port)).write_text(json.dumps(payload))

    return _write


def _probe(*listening):
    live = set(listening)
    return lambda port: port in live


def test_env_override_wins_over_everything(sidecars, monkeypatch):
    sidecars(8991, 100)
    monkeypatch.setenv("PURE_LSP_PORT", "9999")
    assert discovery.resolve_port(probe=_probe(8991, 9999)) == 9999


def test_garbage_env_override_falls_through_rather_than_raising(sidecars, monkeypatch):
    sidecars(8991, 100)
    monkeypatch.setenv("PURE_LSP_PORT", "not-a-port")
    assert discovery.resolve_port(probe=_probe(8991)) == 8991


def test_discovers_a_live_bridge_from_its_sidecar(sidecars, monkeypatch):
    monkeypatch.delenv("PURE_LSP_PORT", raising=False)
    sidecars(8993, 100)
    assert discovery.resolve_port(probe=_probe(8993)) == 8993


def test_most_recently_started_live_bridge_wins(sidecars, monkeypatch):
    monkeypatch.delenv("PURE_LSP_PORT", raising=False)
    sidecars(8991, 100)
    sidecars(8993, 200)  # newer
    assert discovery.resolve_port(probe=_probe(8991, 8993)) == 8993


def test_sidecar_for_a_dead_bridge_is_ignored(sidecars, monkeypatch):
    """A sidecar outlives a SIGKILLed bridge, so the file existing is not evidence of a listener."""
    monkeypatch.delenv("PURE_LSP_PORT", raising=False)
    sidecars(8993, 200)  # newer, but nothing is listening on it
    sidecars(8991, 100)
    assert discovery.resolve_port(probe=_probe(8991)) == 8991


def test_falls_back_to_default_when_nothing_is_live(sidecars, monkeypatch):
    monkeypatch.delenv("PURE_LSP_PORT", raising=False)
    sidecars(8993, 200)
    assert discovery.resolve_port(probe=_probe()) == discovery.DEFAULT_PORT == 8991


def test_malformed_sidecars_are_skipped(tmp_path, monkeypatch, sidecars):
    monkeypatch.delenv("PURE_LSP_PORT", raising=False)
    (tmp_path / "pure_lsp_server_bad.json").write_text("{not json")
    (tmp_path / "pure_lsp_server_list.json").write_text("[]")
    (tmp_path / "pure_lsp_server_noport.json").write_text('{"started_at": 999}')
    (tmp_path / "pure_lsp_server_strport.json").write_text('{"port": "8992", "started_at": 999}')
    sidecars(8991, 100)
    assert discovery.discover_ports(probe=_probe(8991)) == [8991]


def test_sidecar_without_started_at_still_usable(sidecars, monkeypatch, tmp_path):
    monkeypatch.delenv("PURE_LSP_PORT", raising=False)
    (tmp_path / "pure_lsp_server_8991.json").write_text('{"port": 8991}')
    assert discovery.resolve_port(probe=_probe(8991)) == 8991
