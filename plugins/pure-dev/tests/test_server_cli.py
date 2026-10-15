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
"""server_cli.main() wires together arg-parsing, classpath resolution, and bridge_module.run(). The
actual run() call launches a real JVM (out of scope for unit tests, same as bridge.Bridge's
subprocess-spawning paths) - here bridge_module.run is replaced with a spy so this test exercises
just the plumbing: path resolution, classpath-file loading, and the arguments handed to run().
"""
import json
import os
import signal

import pytest

from pure_lsp_bridge import server_cli


@pytest.fixture
def restore_signal_handlers():
    """server_cli.main() installs real SIGTERM/SIGINT handlers; restore whatever was there before
    this test so the change doesn't leak into the rest of the test session."""
    previous = {sig: signal.getsignal(sig) for sig in (signal.SIGTERM, signal.SIGINT)}
    yield
    for sig, handler in previous.items():
        signal.signal(sig, handler)


def test_main_resolves_classpath_and_invokes_bridge_run(tmp_path, monkeypatch, restore_signal_handlers):
    repo_root = tmp_path / "repo"
    repo_root.mkdir()
    server_jar = tmp_path / "server.jar"
    server_jar.write_text("")

    captured = {}

    def fake_run(**kwargs):
        captured.update(kwargs)

    monkeypatch.setattr(server_cli.bridge_module, "run", fake_run)
    monkeypatch.setattr(server_cli.os, "setsid", lambda: (_ for _ in ()).throw(OSError()))

    server_cli.main([
        "--repo-root", str(repo_root),
        "--server-jar", str(server_jar),
        "--port", "12345",
    ])

    assert captured["repo_roots"] == [str(repo_root)]
    assert captured["port"] == 12345
    assert str(server_jar) in captured["classpath"]
    assert captured["socket_port"] is None


def test_main_loads_host_classpath_file(tmp_path, monkeypatch, restore_signal_handlers):
    repo_root = tmp_path / "repo"
    repo_root.mkdir()
    server_jar = tmp_path / "server.jar"
    server_jar.write_text("")
    host_jar = tmp_path / "host.jar"
    host_jar.write_text("")
    classpath_file = tmp_path / "cp.txt"
    classpath_file.write_text(str(host_jar))

    captured = {}
    monkeypatch.setattr(server_cli.bridge_module, "run", lambda **kwargs: captured.update(kwargs))
    monkeypatch.setattr(server_cli.os, "setsid", lambda: (_ for _ in ()).throw(OSError()))

    server_cli.main([
        "--repo-root", str(repo_root),
        "--server-jar", str(server_jar),
        "--classpath-file", str(classpath_file),
        "--socket-port", "9200",
    ])

    assert str(host_jar) in captured["classpath"]
    assert captured["socket_port"] == 9200


def test_main_loads_dependency_classpath_file(tmp_path, monkeypatch, restore_signal_handlers):
    """--dependency-classpath-file supplies the server's own runtime deps (e.g. borrowed from
    legend-engine's classpath) separately from --classpath-file's host jars."""
    repo_root = tmp_path / "repo"
    repo_root.mkdir()
    server_jar = tmp_path / "server.jar"
    server_jar.write_text("")
    dep_jar = tmp_path / "dep.jar"
    dep_jar.write_text("")
    dependency_classpath_file = tmp_path / "dep-cp.txt"
    dependency_classpath_file.write_text(str(dep_jar))

    captured = {}
    monkeypatch.setattr(server_cli.bridge_module, "run", lambda **kwargs: captured.update(kwargs))
    monkeypatch.setattr(server_cli.os, "setsid", lambda: (_ for _ in ()).throw(OSError()))

    server_cli.main([
        "--repo-root", str(repo_root),
        "--server-jar", str(server_jar),
        "--dependency-classpath-file", str(dependency_classpath_file),
        "--port", "12346",
    ])

    assert str(dep_jar) in captured["classpath"]


def test_on_start_writes_metadata_sidecar(tmp_path, monkeypatch, restore_signal_handlers):
    """bridge_module.run's on_start callback is server_cli's hook for writing the /tmp/pure_lsp_server_
    <port>.json sidecar that bridge.py's /health merges in - fake_run below invokes it exactly like the
    real bridge_module.run would, with a fake Bridge standing in for the real JVM-backed one."""
    repo_root = tmp_path / "repo"
    repo_root.mkdir()
    server_jar = tmp_path / "server.jar"
    server_jar.write_text("")

    class FakeProc:
        pid = 4242

    class FakeClient:
        proc = FakeProc()

    class FakeBridge:
        client = FakeClient()

    def fake_run(**kwargs):
        kwargs["on_start"](FakeBridge())

    monkeypatch.setattr(server_cli.bridge_module, "run", fake_run)
    monkeypatch.setattr(server_cli.os, "setsid", lambda: (_ for _ in ()).throw(OSError()))

    port = 54321
    metadata_path = "/tmp/pure_lsp_server_%d.json" % port
    if os.path.exists(metadata_path):
        os.remove(metadata_path)
    try:
        server_cli.main([
            "--repo-root", str(repo_root),
            "--server-jar", str(server_jar),
            "--port", str(port),
            "--java", "java",
            "--jvm-arg=-Dfoo=bar",
        ])

        assert os.path.isfile(metadata_path)
        with open(metadata_path) as f:
            data = json.load(f)
        assert data["pid"] == 4242
        assert data["port"] == port
        assert data["socket_port"] is None
        assert data["repo_roots"] == [str(repo_root)]
        assert data["jvm_args"] == ["-Dfoo=bar"]
        assert data["java"] == "java"
        assert "started_at" in data
    finally:
        if os.path.exists(metadata_path):
            os.remove(metadata_path)


def test_shutdown_removes_metadata_sidecar(tmp_path, monkeypatch, restore_signal_handlers):
    repo_root = tmp_path / "repo"
    repo_root.mkdir()
    server_jar = tmp_path / "server.jar"
    server_jar.write_text("")

    monkeypatch.setattr(server_cli.bridge_module, "run", lambda **kwargs: None)
    monkeypatch.setattr(server_cli.os, "setsid", lambda: (_ for _ in ()).throw(OSError()))

    port = 54322
    metadata_path = "/tmp/pure_lsp_server_%d.json" % port
    with open(metadata_path, "w") as f:
        f.write("{}")

    try:
        server_cli.main([
            "--repo-root", str(repo_root),
            "--server-jar", str(server_jar),
            "--port", str(port),
        ])

        shutdown_handler = signal.getsignal(signal.SIGTERM)
        with pytest.raises(SystemExit):
            shutdown_handler(signal.SIGTERM, None)

        assert not os.path.exists(metadata_path)
    finally:
        if os.path.exists(metadata_path):
            os.remove(metadata_path)


def test_shutdown_metadata_removal_is_best_effort_when_file_absent(tmp_path, monkeypatch, restore_signal_handlers):
    """Removal must never raise even if the sidecar was never written (e.g. main() failed before
    on_start ran) or was already removed - shutdown must still terminate cleanly."""
    repo_root = tmp_path / "repo"
    repo_root.mkdir()
    server_jar = tmp_path / "server.jar"
    server_jar.write_text("")

    monkeypatch.setattr(server_cli.bridge_module, "run", lambda **kwargs: None)
    monkeypatch.setattr(server_cli.os, "setsid", lambda: (_ for _ in ()).throw(OSError()))

    port = 54323
    metadata_path = "/tmp/pure_lsp_server_%d.json" % port
    if os.path.exists(metadata_path):
        os.remove(metadata_path)

    server_cli.main([
        "--repo-root", str(repo_root),
        "--server-jar", str(server_jar),
        "--port", str(port),
    ])

    shutdown_handler = signal.getsignal(signal.SIGTERM)
    with pytest.raises(SystemExit):
        shutdown_handler(signal.SIGTERM, None)
