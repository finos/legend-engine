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
"""The legend-engine reactor as a module graph, read straight from the pom.xml files.

Asking Maven for the reactor costs a JVM start and a full model build per question; parsing the poms
answers "which modules does X need" and "which modules see a change to Y" in milliseconds, which is
what lets the orchestrator decide per revision what to rebuild.
"""
import os
import xml.etree.ElementTree as ET

ENGINE_GROUP = "org.finos.legend.engine"
_NS = {"m": "http://maven.apache.org/POM/4.0.0"}
_SKIP_DIRS = {"target", "node_modules", ".git", ".idea", ".claude", "plugins"}


class Module:
    def __init__(self, artifact_id, path, deps):
        self.artifact_id = artifact_id
        self.path = path
        self.deps = deps

    def __repr__(self):
        return "Module(%s)" % self.path


def _text(node, tag):
    child = node.find("m:" + tag, _NS)
    return child.text.strip() if (child is not None and child.text) else None


def _engine_artifacts(nodes, own_group):
    found = set()
    for dep in nodes:
        group = _text(dep, "groupId") or own_group
        if group in (ENGINE_GROUP, "${project.groupId}"):
            artifact = _text(dep, "artifactId")
            if artifact:
                found.add(artifact)
    return found


def read_pom(pom_path, rel_path):
    root = ET.parse(pom_path).getroot()
    artifact_id = _text(root, "artifactId")
    parent = root.find("m:parent", _NS)
    group = _text(root, "groupId") or (_text(parent, "groupId") if parent is not None else None)
    deps = _engine_artifacts(root.findall("m:dependencies/m:dependency", _NS), group)
    deps |= _engine_artifacts(root.findall("m:build/m:plugins/m:plugin", _NS), group)
    deps |= _engine_artifacts(root.findall("m:build/m:plugins/m:plugin/m:dependencies/m:dependency", _NS), group)
    if parent is not None and _text(parent, "groupId") == ENGINE_GROUP:
        deps.add(_text(parent, "artifactId"))
    return Module(artifact_id, rel_path, deps - {artifact_id})


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
        self._dependents = {}
        for module in self.by_artifact.values():
            for dep in module.deps:
                self._dependents.setdefault(dep, set()).add(module.artifact_id)

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
        return sorted(self.by_artifact[a].path for a in artifact_ids if a in self.by_artifact and self.by_artifact[a].path)
