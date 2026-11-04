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
import fnmatch
import glob
import json
import os
import shutil

import pytest

from engine_review import resume, run

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
MODULE = "corpus"
RUNNER = """package com.example.emit.{pkg};

public class {name}
{{
    Stream<DynamicContainer> emit()
    {{
        return EMITTestSuiteBuilder.{call};
    }}
}}
"""


def corpus(tree, projects):
    """projects: runner class name -> (classpath root, model names); a None root makes a named-models runner."""
    for name, (root, models) in projects.items():
        java = os.path.join(tree, MODULE, "src", "test", "java", "com", "example", "emit", name.lower())
        os.makedirs(java, exist_ok=True)
        call = 'testContainers("%s")' % root if root else 'tests("emit-models/x", "a", "b")'
        with open(os.path.join(java, name + ".java"), "w") as f:
            f.write(RUNNER.format(pkg=name.lower(), name=name, call=call))
        for model in models:
            directory = os.path.join(tree, MODULE, "src", "test", "resources", root or "emit-models/x")
            os.makedirs(directory, exist_ok=True)
            open(os.path.join(directory, model + ".emit.yaml"), "w").close()


def write_model(suite_dir, model, complete=True):
    directory = os.path.join(suite_dir, model)
    os.makedirs(directory, exist_ok=True)
    with open(os.path.join(directory, "model.json"), "w") as f:
        f.write("{}")
    if complete:
        open(os.path.join(directory, resume.COMPLETE), "w").close()


class FakeMaven:
    """Plays surefire for a snapshot pass: each selected runner writes its models and a report, unless it crashes."""

    def __init__(self, tree, logs, crash=()):
        self.tree, self.logs, self.crash, self.calls = tree, logs, dict.fromkeys(crash, 1), []

    def run(self, name, args, parallel=True):
        os.makedirs(self.logs, exist_ok=True)
        props = dict(a[2:].split("=", 1) for a in args if a.startswith("-D") and "=" in a)
        out = props["legend.emit.snapshot.dir"]
        reports = os.path.join(self.tree, MODULE, "target", "surefire-reports")
        shutil.rmtree(os.path.join(self.tree, MODULE, "target"), ignore_errors=True)
        os.makedirs(reports)
        if "surefire.includesFile" in props:
            with open(props["surefire.includesFile"]) as f:
                patterns = [line.strip() for line in f if line.strip()]
        else:
            patterns = ["**/%s.java" % p for p in props["test"].split(",")]
        selected = []
        log_lines = []
        for runner in resume.runners(self.tree, MODULE):
            path = runner.fqcn.replace(".", "/") + ".java"
            if not any(fnmatch.fnmatch(path, p) or fnmatch.fnmatch(os.path.basename(path), p.split("/")[-1]) for p in patterns):
                continue
            selected.append(runner.simple_name)
            yamls = sorted(glob.glob(os.path.join(self.tree, MODULE, "src", "test", "resources", runner.root or "emit-models/x", "*.emit.yaml")))
            suite_dir = os.path.join(out, runner.snapshot_dir or "emit-models_x")
            if self.crash.get(runner.simple_name):
                self.crash[runner.simple_name] -= 1
                write_model(suite_dir, os.path.basename(yamls[0])[:-len(".emit.yaml")], complete=False)
                log_lines.append("[ERROR] The forked VM terminated without properly saying goodbye. VM crash or System.exit called?")
                continue
            for yaml in yamls:
                write_model(suite_dir, os.path.basename(yaml)[:-len(".emit.yaml")])
            open(os.path.join(reports, "TEST-%s.xml" % runner.fqcn), "w").close()
        self.calls.append(selected)
        with open(os.path.join(self.logs, name + ".log"), "w") as f:
            f.write("\n".join(log_lines + ["BUILD SUCCESS"]))
        return os.path.join(self.logs, name + ".log")


@pytest.fixture
def tree(tmp_path):
    root = str(tmp_path / "tree")
    corpus(root, {"AlphaEMITTests": ("emit-models/alpha", ["a1", "a2"]),
                  "BetaEMITTests": ("emit-models/beta", ["b1", "b2", "b3"]),
                  "GammaEMITTests": ("emit-models/gamma", ["g1"])})
    return root


def test_snapshot_dir_name_mirrors_the_hook():
    assert resume.snapshot_dir_name("relational-emit-models/") == "relational-emit-models"
    assert resume.snapshot_dir_name("emit-models/amber-apiary") == "emit-models_amber-apiary"


def test_runners_map_classes_to_roots_and_model_counts(tmp_path):
    root = str(tmp_path)
    corpus(root, {"AlphaEMITTests": ("emit-models/alpha", ["a1", "a2"]), "NamedEMITTests": (None, ["a"])})
    found = {r.simple_name: r for r in resume.runners(root, MODULE)}
    assert (found["AlphaEMITTests"].fqcn, found["AlphaEMITTests"].root, found["AlphaEMITTests"].expected) == \
        ("com.example.emit.alphaemittests.AlphaEMITTests", "emit-models/alpha", 2)
    assert found["NamedEMITTests"].root is None


def test_engine_runners_resolve_their_corpus():
    found = {r.simple_name: r for r in resume.runners(REPO, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit")}
    assert found["RelationalEMITTests"].snapshot_dir == "relational-emit-models"
    assert found["RelationalEMITTests"].expected > 0


def test_pending_skips_finished_and_fully_snapshotted_runners(tree, tmp_path):
    out = str(tmp_path / "out")
    runners = {r.simple_name: r for r in resume.runners(tree, MODULE)}
    write_model(os.path.join(out, "emit-models_alpha"), "a1")
    write_model(os.path.join(out, "emit-models_alpha"), "a2")
    write_model(os.path.join(out, "emit-models_beta"), "b1")
    write_model(os.path.join(out, "emit-models_beta"), "b2", complete=False)
    pending = resume.pending(list(runners.values()), out, {runners["GammaEMITTests"].fqcn})
    assert [r.simple_name for r in pending] == ["BetaEMITTests"]
    assert resume.gaps([runners["GammaEMITTests"]], out) == [{"runner": runners["GammaEMITTests"].fqcn, "root": "emit-models/gamma", "expected": 1, "complete": 0}]


def test_crash_lines_are_found(tmp_path):
    log = tmp_path / "pass.log"
    log.write_text("[INFO] ok\n[ERROR] java.lang.OutOfMemoryError: Java heap space\n[ERROR] Crashed tests:\n")
    assert len(resume.crashes(str(log))) == 2


def test_marker_digest_tracks_inputs_and_completion():
    marker = {"key": {"sha": "a"}, "complete": True}
    assert resume.digest(marker) == resume.digest(dict(marker))
    assert resume.digest(marker) != resume.digest(dict(marker, complete=False))
    assert resume.digest(None) is None


def test_volatile_paths_are_flagged():
    assert resume.volatile_reason("/tmp/engine-review-run") is not None
    assert resume.volatile_reason("/var/tmp/x/y") is not None


def test_disk_and_memory_preflight(tmp_path):
    assert resume.disk_shortfall(str(tmp_path / "new" / "dir"), 0, 3, 1.5) == 0
    assert resume.disk_shortfall(str(tmp_path), 10 ** 9, 3, 1.5) > 0
    meminfo = tmp_path / "meminfo"
    meminfo.write_text("MemTotal: 64000000 kB\nMemAvailable: 10485760 kB\n")
    no_jacoco = ["-DforkCount=1", "-DargLine=-Xmx7g", "-Djacoco.skip=true"]
    assert resume.fork_memory_warning(["-DforkCount=4", "-DargLine=-Xmx7g", "-Djacoco.skip=true"], str(meminfo), "-Xmx1g") is not None
    assert resume.fork_memory_warning(no_jacoco, str(meminfo), "-Xmx1g") is None
    assert resume.fork_memory_warning(["-DforkCount=1", "-DargLine=-Xmx7g"], str(meminfo), "-Xmx1g") is not None
    assert resume.fork_memory_warning(no_jacoco, str(meminfo), "-Xmx4g") is not None
    assert resume.fork_memory_warning(["-Djacoco.skip=true"], str(meminfo)) is None


def test_run_lock_refuses_a_second_live_run(tmp_path):
    out = str(tmp_path / "run")
    with resume.RunLock(out):
        with pytest.raises(RuntimeError, match="another engine-review run"):
            resume.RunLock(out).__enter__()
    assert not os.path.exists(os.path.join(out, resume.LOCK))


def test_crashed_runner_is_retried_and_a_complete_pass_is_skipped(tree, tmp_path):
    out, logs = str(tmp_path / "out"), str(tmp_path / "logs")
    maven = FakeMaven(tree, logs, crash=["BetaEMITTests"])
    marker = run.snapshot(maven, "base", [(MODULE, ["*EMITTests"])], out, key={"sha": "abc"})
    assert maven.calls == [["AlphaEMITTests", "BetaEMITTests", "GammaEMITTests"], ["BetaEMITTests"]]
    assert marker["complete"] and marker["models"] == 6 and marker["crashes"]
    run.snapshot(maven, "base", [(MODULE, ["*EMITTests"])], out, key={"sha": "abc"})
    assert len(maven.calls) == 2


def test_an_interrupted_pass_resumes_only_unfinished_runners(tree, tmp_path):
    out, logs = str(tmp_path / "out"), str(tmp_path / "logs")
    crashing = FakeMaven(tree, logs, crash=["BetaEMITTests", "BetaEMITTests"])
    crashing.crash["BetaEMITTests"] = 2
    with pytest.raises(RuntimeError, match="incomplete: 1 runner class"):
        run.snapshot(crashing, "base", [(MODULE, ["*EMITTests"])], out, key={"sha": "abc"})
    assert json.load(open(os.path.join(out, resume.MARKER)))["complete"] is False
    healthy = FakeMaven(tree, logs)
    marker = run.snapshot(healthy, "base", [(MODULE, ["*EMITTests"])], out, key={"sha": "abc"})
    assert healthy.calls == [["BetaEMITTests"]]
    assert marker["complete"] and marker["models"] == 6


def test_changed_inputs_or_uncommitted_head_redo_the_pass(tree, tmp_path):
    out, logs = str(tmp_path / "out"), str(tmp_path / "logs")
    maven = FakeMaven(tree, logs)
    run.snapshot(maven, "base", [(MODULE, ["*EMITTests"])], out, key={"sha": "abc"})
    run.snapshot(maven, "base", [(MODULE, ["*EMITTests"])], out, key={"sha": "def"})
    run.snapshot(maven, "head", [(MODULE, ["*EMITTests"])], out, key={"sha": run.WORKTREE})
    run.snapshot(maven, "head", [(MODULE, ["*EMITTests"])], out, key={"sha": run.WORKTREE})
    assert len(maven.calls) == 4


def test_a_narrowed_pass_only_expects_its_selected_runners(tree, tmp_path):
    out, logs = str(tmp_path / "out"), str(tmp_path / "logs")
    maven = FakeMaven(tree, logs)
    marker = run.snapshot(maven, "base", [(MODULE, ["AlphaEMITTests", "Gamma*"])], out, key={"sha": "abc"})
    assert maven.calls == [["AlphaEMITTests", "GammaEMITTests"]]
    assert marker["complete"] and marker["models"] == 3


def test_selected_understands_surefire_patterns(tree):
    found = resume.runners(tree, MODULE)
    names = lambda tests: [r.simple_name for r in resume.selected(found, tests)]
    assert names(["*EMITTests"]) == ["AlphaEMITTests", "BetaEMITTests", "GammaEMITTests"]
    assert names(["BetaEMITTests#emit"]) == ["BetaEMITTests"]
    assert names(["com/example/emit/alphaemittests/AlphaEMITTests.java"]) == ["AlphaEMITTests"]
