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
import argparse
import os

from pure_lsp_bridge import project_cli


def _args(**overrides):
    ns = argparse.Namespace(project=None, source=None, all_source=False, json=False,
                            force=False, path=False, module=None)
    for k, v in overrides.items():
        setattr(ns, k, v)
    return ns


def _linear_registry():
    return {
        "projects": [
            {"name": "base", "root": "base", "dependsOn": [], "module": "base-mod"},
            {"name": "mid", "root": "mid", "dependsOn": ["base"], "module": None},
            {"name": "top", "root": "top", "dependsOn": ["mid"], "module": "top-mod"},
        ]
    }


def test_cmd_list_prints_a_row_per_layer(write_registry, capsys):
    write_registry(_linear_registry())
    rc = project_cli.cmd_list(_args())
    assert rc == 0
    out = capsys.readouterr().out
    assert "base" in out and "mid" in out and "top" in out


def test_cmd_roots_default_sources_full_chain(write_registry, capsys, tmp_path, monkeypatch):
    monkeypatch.setenv("HOME", str(tmp_path))
    write_registry(_linear_registry())
    # Make the checkouts actually exist (under the sandboxed $HOME) so cmd_roots doesn't warn/skip.
    for name in ("base", "mid", "top"):
        os.makedirs(os.path.join(str(tmp_path), name), exist_ok=True)

    rc = project_cli.cmd_roots(_args(project="top"))
    out = capsys.readouterr().out
    assert rc == 0
    assert out.count("--repo-root") == 3


def test_cmd_roots_warns_and_skips_missing_checkout(write_registry, capsys):
    write_registry({"projects": [
        {"name": "solo", "root": "definitely-does-not-exist-xyz", "dependsOn": [], "module": None},
    ]})
    rc = project_cli.cmd_roots(_args(project="solo"))
    captured = capsys.readouterr()
    assert rc == 0
    assert "--repo-root" not in captured.out
    assert "WARNING" in captured.err


def test_cmd_roots_explicit_source_narrows_set(write_registry, tmp_path, monkeypatch, capsys):
    monkeypatch.setenv("HOME", str(tmp_path))
    write_registry(_linear_registry())
    for name in ("base", "mid", "top"):
        os.makedirs(os.path.join(str(tmp_path), name), exist_ok=True)

    rc = project_cli.cmd_roots(_args(project="top", source=["base"]))
    out = capsys.readouterr().out
    assert rc == 0
    # base + top (target always force-included) = 2 --repo-root pairs.
    assert out.count("--repo-root") == 2


def test_cmd_roots_errors_when_target_undetectable(write_registry, monkeypatch, capsys):
    write_registry(_linear_registry())
    monkeypatch.delenv("PURE_DEV_PROJECT", raising=False)
    monkeypatch.chdir("/tmp")
    rc = project_cli.cmd_roots(_args(project=None))
    assert rc == 2
    assert "could not determine target" in capsys.readouterr().err


def test_cmd_classpath_path_flag_never_computes(write_registry, monkeypatch, tmp_path, capsys):
    write_registry(_linear_registry())
    monkeypatch.setenv("XDG_CACHE_HOME", str(tmp_path / "cache"))
    rc = project_cli.cmd_classpath(_args(project="top", path=True))
    out = capsys.readouterr().out.strip()
    assert rc == 0
    assert out.endswith("classpath-top.txt")


def test_cmd_classpath_errors_with_no_classpath_layer(write_registry, monkeypatch, tmp_path, capsys):
    write_registry({"projects": [
        {"name": "solo", "root": "solo", "dependsOn": [], "module": None},
    ]})
    monkeypatch.setenv("XDG_CACHE_HOME", str(tmp_path / "cache"))
    rc = project_cli.cmd_classpath(_args(project="solo"))
    assert rc == 1
    assert "no classpath-anchor module" in capsys.readouterr().err


def test_cmd_classpath_skips_recompute_when_cache_fresh(write_registry, monkeypatch, tmp_path, capsys):
    monkeypatch.setenv("HOME", str(tmp_path))
    write_registry(_linear_registry())
    monkeypatch.setenv("XDG_CACHE_HOME", str(tmp_path / "cache"))
    build_root = os.path.join(str(tmp_path), "top")
    os.makedirs(build_root, exist_ok=True)

    cache_file, stamp_file = project_cli._cache_paths("top")
    os.makedirs(os.path.dirname(cache_file), exist_ok=True)
    with open(cache_file, "w") as f:
        f.write("/some/existing.jar")
    with open(stamp_file, "w") as f:
        f.write(project_cli._stamp({"path": build_root, "module": "top-mod"}))

    rc = project_cli.cmd_classpath(_args(project="top"))
    out = capsys.readouterr().out.strip()
    assert rc == 0
    assert out == cache_file


def test_main_list_command(write_registry, capsys):
    write_registry(_linear_registry())
    rc = project_cli.main(["list"])
    assert rc == 0
    assert "base" in capsys.readouterr().out


def _launch_registry():
    return {
        "projects": [
            {"name": "plain", "root": "plain", "dependsOn": [], "module": None},
            {"name": "configured", "root": "configured", "dependsOn": [], "module": "mod",
             "launch": {"bridgePort": 8993, "socketPort": 9199, "sqlE2eCorpus": "a/b/resources"}},
        ]
    }


def test_launch_config_uses_the_projects_launch_block(write_registry, tmp_path, monkeypatch, capsys):
    monkeypatch.setenv("HOME", str(tmp_path))
    write_registry(_launch_registry())
    rc = project_cli.cmd_launch_config(_args(project="configured"))
    assert rc == 0
    fields = capsys.readouterr().out.rstrip("\n").split("\t")
    root = os.path.join(str(tmp_path), "configured")
    assert fields == ["configured", root, "8993", "9199",
                      os.path.join(root, "welcome.pure"),
                      os.path.join(root, "a/b/resources")]


def test_launch_config_falls_back_when_no_launch_block(write_registry, tmp_path, monkeypatch, capsys):
    monkeypatch.setenv("HOME", str(tmp_path))
    write_registry(_launch_registry())
    rc = project_cli.cmd_launch_config(_args(project="plain"))
    assert rc == 0
    fields = capsys.readouterr().out.rstrip("\n").split("\t")
    assert fields[2:4] == [str(project_cli.DEFAULT_BRIDGE_PORT),
                           str(project_cli.DEFAULT_SOCKET_PORT)]
    # No corpus configured: trailing field is empty, so the launcher skips the jvm-arg entirely.
    assert fields[5] == ""


def test_launch_config_rejects_unknown_project(write_registry, monkeypatch, capsys):
    write_registry(_launch_registry())
    monkeypatch.delenv("PURE_DEV_PROJECT", raising=False)
    rc = project_cli.cmd_launch_config(_args(project="nope"))
    assert rc == 2
    assert "unknown project" in capsys.readouterr().err


def test_launch_config_always_emits_six_fields(write_registry, tmp_path, monkeypatch, capsys):
    monkeypatch.setenv("HOME", str(tmp_path))
    write_registry(_launch_registry())
    project_cli.main(["launch-config", "plain"])
    assert len(capsys.readouterr().out.rstrip("\n").split("\t")) == 6
