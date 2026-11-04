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
import json
import os
import zipfile

import pytest

from engine_review import modules, resume, run, scope

POM = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    {parent}
    {group}
    <artifactId>{artifact}</artifactId>
    {properties}
    <dependencies>{deps}</dependencies>
</project>
"""


def dep(group, artifact, scope_=None):
    return "<dependency><groupId>%s</groupId><artifactId>%s</artifactId>%s</dependency>" % (
        group, artifact, "<scope>%s</scope>" % scope_ if scope_ else "")


def write_pom(root, path, artifact, group=None, parent=None, deps=(), properties=""):
    directory = os.path.join(root, path)
    os.makedirs(directory, exist_ok=True)
    with open(os.path.join(directory, "pom.xml"), "w") as f:
        f.write(POM.format(
            parent="<parent><groupId>%s</groupId><artifactId>%s</artifactId></parent>" % parent if parent else "",
            group="<groupId>%s</groupId>" % group if group else "", artifact=artifact,
            properties="<properties>%s</properties>" % properties if properties else "", deps="".join(deps)))


@pytest.fixture
def downstream(tmp_path):
    """A repository outside legend-engine that consumes the EMIT framework as a dependency."""
    root = str(tmp_path / "repo")
    parent = ("com.example", "example")
    write_pom(root, "", "example", group="com.example", properties="<legend.engine.version>4.152.0</legend.engine.version>")
    write_pom(root, "core", "example-core", parent=parent)
    write_pom(root, "store", "example-store", parent=parent, deps=[dep("${project.groupId}", "example-core")])
    write_pom(root, "unrelated", "example-unrelated", parent=parent)
    write_pom(root, "emit-tests", "example-emit-tests", parent=parent, deps=[
        dep(modules.ENGINE_GROUP, "legend-engine-emit-junit", "test"), dep("com.example", "example-store", "test")])
    test_dir = os.path.join(root, "emit-tests", "src", "test", "java", "com", "example", "emit", "deposits")
    os.makedirs(test_dir)
    open(os.path.join(test_dir, "DepositsEMITTests.java"), "w").close()
    write_pom(root, "emit-support", "example-emit-support", parent=parent, deps=[dep(modules.ENGINE_GROUP, "legend-engine-emit")])
    return root


def test_reactor_edges_follow_the_repos_own_group(downstream):
    reactor = modules.Reactor(downstream)
    assert reactor.groups == ["com.example"]
    assert reactor.upstream(["example-emit-tests"]) == {"example-emit-tests", "example-store", "example-core", "example"}
    assert "example-unrelated" in reactor.downstream(["example"])
    assert "example-unrelated" not in reactor.downstream(["example-core"])


def test_suites_are_discovered_without_config(downstream):
    config = scope.resolve_config(downstream)
    assert config["source"].startswith("defaults")
    assert config["framework"] == scope.load_config()["framework"]
    found = scope.suites(config, modules.Reactor(downstream))
    assert found == {"emit-tests": {"module": "emit-tests", "tests": scope.DEFAULT_TESTS, "lenses": scope.DEFAULT_LENSES}}
    assert not run.framework_in_reactor(modules.Reactor(downstream), config)


def test_repo_config_annotates_discovered_suites_and_keeps_the_engine_framework(downstream):
    os.makedirs(os.path.join(downstream, ".engine-review"))
    with open(os.path.join(downstream, scope.REPO_CONFIG), "w") as f:
        json.dump({"suites": {"deposits": {"module": "emit-tests", "lenses": ["sql"]}},
                   "rules": [{"lens": "sql", "paths": ["store/**"], "why": "store"}],
                   "framework": ["elsewhere"]}, f)
    config = scope.resolve_config(downstream)
    assert config["source"] == os.path.join(downstream, scope.REPO_CONFIG)
    assert config["framework"] == scope.load_config()["framework"]
    found = scope.suites(config, modules.Reactor(downstream))
    assert found == {"deposits": {"module": "emit-tests", "lenses": ["sql"], "tests": scope.DEFAULT_TESTS}}


def test_pinned_engine_version_comes_from_the_root_pom(downstream):
    assert run.pinned_engine_version(downstream, {}) == "4.152.0"
    with pytest.raises(RuntimeError, match="engine_version_property"):
        run.pinned_engine_version(downstream, {"engine_version_property": "missing.version"})


def test_pom_changes_are_flagged_for_inheritance_rebuild(downstream):
    reactor = modules.Reactor(downstream)
    changed, poms = run._changed_modules(["pom.xml", "core/src/main/java/A.java", "README.md"], reactor, reactor)
    assert changed == {"example", "example-core"}
    assert poms == {"example"}


def _jar(path, entries):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with zipfile.ZipFile(path, "w") as z:
        for entry in entries:
            z.writestr(entry, resume.COMPLETE if entry == run.SNAPSHOT_CLASS else "")


def test_released_framework_with_the_hook_needs_no_build(downstream, tmp_path):
    tail = str(tmp_path / "tail")
    _jar(run._engine_artifact(tail, "legend-engine-emit", "4.152.0"), [run.SNAPSHOT_CLASS])
    maven = run.Maven(downstream, str(tmp_path / "private"), [tail], str(tmp_path / "logs"), 1)
    info = run.ensure_framework(maven, scope.resolve_config(downstream), None, "base")
    assert info["engine_version"] == "4.152.0"


def test_framework_without_the_hook_needs_an_engine_checkout_that_has_it(downstream, tmp_path):
    tail = str(tmp_path / "tail")
    _jar(run._engine_artifact(tail, "legend-engine-emit", "4.152.0"), ["org/finos/legend/engine/test/emit/EMITTasks.class"])
    maven = run.Maven(downstream, str(tmp_path / "private"), [tail], str(tmp_path / "logs"), 1)
    with pytest.raises(RuntimeError, match="lacks the current EMIT snapshot hook"):
        run.ensure_framework(maven, scope.resolve_config(downstream), str(tmp_path / "not-engine"), "base")


def test_a_hook_without_the_completion_sentinel_counts_as_missing(downstream, tmp_path):
    jar = run._engine_artifact(str(tmp_path / "tail"), "legend-engine-emit", "4.152.0")
    os.makedirs(os.path.dirname(jar))
    with zipfile.ZipFile(jar, "w") as z:
        z.writestr(run.SNAPSHOT_CLASS, "older hook")
    assert not run._has_hook(jar)


def test_private_repository_shadows_the_tail(downstream, tmp_path):
    private, tail = str(tmp_path / "private"), str(tmp_path / "tail")
    _jar(run._engine_artifact(private, "legend-engine-emit", "4.152.0"), [run.SNAPSHOT_CLASS])
    _jar(run._engine_artifact(tail, "legend-engine-emit", "4.152.0"), [])
    maven = run.Maven(downstream, private, [tail], str(tmp_path / "logs"), 1)
    assert run._resolved(maven, "legend-engine-emit", "4.152.0", "base").startswith(private)


def test_a_changed_root_pom_rebuilds_the_root_module_too(downstream):
    reactor = modules.Reactor(downstream)
    changed, poms = run._changed_modules(["pom.xml"], reactor, reactor)
    rebuilt = reactor.downstream(poms) & reactor.upstream(["example-emit-tests"])
    assert reactor.paths(rebuilt) == [":example", "core", "emit-tests", "store"]
