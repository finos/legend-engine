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

import pytest


@pytest.fixture
def write_registry(tmp_path, monkeypatch):
    """Factory fixture: write a projects.json registry and point PURE_DEV_PROJECTS_JSON at it.

    Returns a function taking the registry dict (top-level shape: {"projects": [...]}, or the
    legacy {"layers": [...]}) and returning its path.
    """
    def _write(data):
        path = tmp_path / "projects.json"
        path.write_text(json.dumps(data))
        monkeypatch.setenv("PURE_DEV_PROJECTS_JSON", str(path))
        return path

    return _write


@pytest.fixture
def write_extra_registries(tmp_path, monkeypatch):
    """Factory fixture: write one or more extra registry files and point
    PURE_DEV_EXTRA_PROJECTS_JSON at them (os.pathsep-joined, in the given order).

    Returns a function taking *registry dicts (same shape as write_registry) and returning the list
    of paths written.
    """
    def _write(*datas):
        paths = []
        for i, data in enumerate(datas):
            path = tmp_path / ("extra-projects-%d.json" % i)
            path.write_text(json.dumps(data))
            paths.append(str(path))
        monkeypatch.setenv("PURE_DEV_EXTRA_PROJECTS_JSON", os.pathsep.join(paths))
        return paths

    return _write
