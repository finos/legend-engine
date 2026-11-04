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
"""A Maven reactor as a module graph, read straight from the pom.xml files.

Asking Maven for the reactor costs a JVM start and a full model build per question; parsing the poms
answers "which modules does X need" and "which modules see a change to Y" in milliseconds, which is
what lets the orchestrator decide per revision what to rebuild. Any repository works: an edge is a
dependency, plugin or parent whose groupId:artifactId is a module of the same reactor.
"""
import os
import re
import xml.etree.ElementTree as ET

ENGINE_GROUP = "org.finos.legend.engine"
EMIT_FRAMEWORK = {(ENGINE_GROUP, "legend-engine-emit"), (ENGINE_GROUP, "legend-engine-emit-junit")}
_NS = {"m": "http://maven.apache.org/POM/4.0.0"}
# Poms never sit under src/, and skipping it avoids walking tens of thousands of corpus resources.
_SKIP_DIRS = {"target", "node_modules", "src"}
_PROPERTY = re.compile(r"^\$\{([^}]+)\}$")


class Module:
    def __init__(self, group_id, artifact_id, path, deps, parent):
        self.group_id = group_id
        self.artifact_id = artifact_id
        self.path = path
        self.raw_deps = deps
        self.parent = parent
        self.deps = set()

    def __repr__(self):
        return "Module(%s)" % self.path


def _text(node, tag):
    child = node.find("m:" + tag, _NS)
    return child.text.strip() if (child is not None and child.text) else None


def pom_properties(root):
    props = root.find("m:properties", _NS)
    if props is None:
        return {}
    return {child.tag.split("}", 1)[-1]: (child.text or "").strip() for child in props}


def pom_property(pom_path, name):
    """A property declared in one pom's <properties>, or None."""
    return pom_properties(ET.parse(pom_path).getroot()).get(name)


def _coordinates(nodes, group, parent_group, props):
    found = set()
    for node in nodes:
        artifact = _text(node, "artifactId")
        if not artifact:
            continue
        dep_group = _text(node, "groupId") or group
        match = _PROPERTY.match(dep_group)
        if match:
            name = match.group(1)
            if name in ("project.groupId", "pom.groupId", "groupId"):
                dep_group = group
            elif name in ("project.parent.groupId", "parent.groupId"):
                dep_group = parent_group
            else:
                dep_group = props.get(name)
        found.add((dep_group, artifact))
    return found


def read_pom(pom_path, rel_path):
    root = ET.parse(pom_path).getroot()
    artifact_id = _text(root, "artifactId")
    parent_node = root.find("m:parent", _NS)
    parent_group = _text(parent_node, "groupId") if parent_node is not None else None
    group = _text(root, "groupId") or parent_group
    props = pom_properties(root)
    deps = _coordinates(root.findall("m:dependencies/m:dependency", _NS), group, parent_group, props)
    deps |= _coordinates(root.findall("m:build/m:plugins/m:plugin", _NS), group, parent_group, props)
    deps |= _coordinates(root.findall("m:build/m:plugins/m:plugin/m:dependencies/m:dependency", _NS), group, parent_group, props)
    parent = (parent_group, _text(parent_node, "artifactId")) if parent_node is not None else None
    return Module(group, artifact_id, rel_path, deps, parent)


class Reactor:
    def __init__(self, repo_root):
        self.repo_root = os.path.abspath(repo_root)
        self.by_artifact = {}
        self.by_path = {}
        for dirpath, dirnames, filenames in os.walk(self.repo_root):
            dirnames[:] = sorted(d for d in dirnames if d not in _SKIP_DIRS and not d.startswith("."))
            if "pom.xml" in filenames:
                rel = os.path.relpath(dirpath, self.repo_root)
                rel = "" if rel == "." else rel
                module = read_pom(os.path.join(dirpath, "pom.xml"), rel)
                if module.artifact_id:
                    self.by_artifact[module.artifact_id] = module
                    self.by_path[rel] = module
        by_coordinate = {(m.group_id, m.artifact_id): m.artifact_id for m in self.by_artifact.values()}
        self._dependents = {}
        for module in self.by_artifact.values():
            wanted = set(module.raw_deps) | ({module.parent} if module.parent else set())
            for group, artifact in wanted:
                # An unresolvable groupId property still names a reactor module if the artifactId does.
                target = by_coordinate.get((group, artifact)) or (artifact if group is None and artifact in self.by_artifact else None)
                if target and target != module.artifact_id:
                    module.deps.add(target)
                    self._dependents.setdefault(target, set()).add(module.artifact_id)

    @staticmethod
    def read_root(repo_root):
        pom = os.path.join(repo_root, "pom.xml")
        return read_pom(pom, "") if os.path.exists(pom) else None

    @property
    def root(self):
        return self.by_path.get("")

    @property
    def groups(self):
        return sorted({m.group_id for m in self.by_artifact.values() if m.group_id})

    def module_of(self, file_path):
        """The innermost module containing a repo-relative file path, or None."""
        path = os.path.dirname(file_path)
        while True:
            if path in self.by_path:
                return self.by_path[path]
            if not path:
                return None
            path = os.path.dirname(path)

    def upstream(self, artifact_ids):
        """artifact_ids plus everything they transitively need (Maven's -am)."""
        seen, stack = set(), list(artifact_ids)
        while stack:
            current = stack.pop()
            if current in seen or current not in self.by_artifact:
                continue
            seen.add(current)
            stack.extend(self.by_artifact[current].deps)
        return seen

    def downstream(self, artifact_ids):
        """artifact_ids plus everything that transitively needs them (Maven's -amd)."""
        seen, stack = set(), list(artifact_ids)
        while stack:
            current = stack.pop()
            if current in seen:
                continue
            seen.add(current)
            stack.extend(self._dependents.get(current, ()))
        return seen

    def paths(self, artifact_ids):
        """-pl selectors; the root module has no relative path, so it is selected by artifactId."""
        return sorted((self.by_artifact[a].path or ":" + a) for a in artifact_ids if a in self.by_artifact)

    def emit_modules(self):
        """Modules that depend on the EMIT framework and hold *EMITTests classes: the corpus suites."""
        found = []
        for module in self.by_artifact.values():
            if not (module.raw_deps & EMIT_FRAMEWORK) or (module.group_id, module.artifact_id) in EMIT_FRAMEWORK:
                continue
            test_root = os.path.join(self.repo_root, module.path, "src", "test", "java")
            if _has_emit_tests(test_root):
                found.append(module)
        return sorted(found, key=lambda m: m.path)


def _has_emit_tests(test_root):
    for _, _, filenames in os.walk(test_root):
        if any(f.endswith("EMITTests.java") for f in filenames):
            return True
    return False
