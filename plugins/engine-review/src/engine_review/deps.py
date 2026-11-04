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
"""Find, in seconds and before hours of builds, external artifacts the corpus closure pins that no repository has.

Only direct, managed pins of the closure's modules are checked: transitive dependencies need the full model
build this check exists to avoid. That still catches the common case, an internal artifact pinned in the root
pom that is published only to a repository this machine cannot reach.
"""
import os
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor

_NS = {"m": "http://maven.apache.org/POM/4.0.0"}
_SETTINGS_NS = "{http://maven.apache.org/SETTINGS/1.0.0}"
FOUND, MISSING, UNKNOWN = "found", "missing", "unknown"


def _text(node, tag):
    child = node.find("m:" + tag, _NS)
    return child.text.strip() if (child is not None and child.text) else None


def managed_versions(effective_pom):
    """(groupId, artifactId) -> version from an effective pom's dependency and plugin management."""
    root = ET.parse(effective_pom).getroot()
    if root.tag.split("}")[-1] == "projects":
        root = root.find("m:project", _NS)
    versions = {}
    for path in ("m:dependencyManagement/m:dependencies/m:dependency", "m:build/m:pluginManagement/m:plugins/m:plugin", "m:build/m:plugins/m:plugin"):
        for node in root.findall(path, _NS):
            group = _text(node, "groupId") or ("org.apache.maven.plugins" if "plugin" in path else None)
            artifact, version = _text(node, "artifactId"), _text(node, "version")
            if group and artifact and version and "${" not in version:
                versions[(group, artifact)] = version
    return versions


def repository_urls(settings_path):
    """Every http(s) mirror and repository URL in a Maven settings file."""
    if not os.path.exists(settings_path):
        return []
    urls = []
    for node in ET.parse(settings_path).getroot().iter():
        if node.tag.replace(_SETTINGS_NS, "") == "url" and node.text and node.text.strip().startswith("http"):
            url = node.text.strip().rstrip("/") + "/"
            if url not in urls:
                urls.append(url)
    return urls


def pom_path(group, artifact, version):
    return "/".join(group.split(".") + [artifact, version, "%s-%s.pom" % (artifact, version)])


def head(url, timeout=20):
    """found on 200, missing on 404/410 or an unreachable host, unknown when the answer proves nothing (auth, proxy)."""
    try:
        urllib.request.urlopen(urllib.request.Request(url, method="HEAD"), timeout=timeout)
        return FOUND
    except urllib.error.HTTPError as e:
        return MISSING if e.code in (404, 410) else UNKNOWN
    except urllib.error.URLError as e:
        return UNKNOWN if "proxy" in str(e.reason).lower() else MISSING
    except OSError:
        return MISSING


def unresolvable(coordinates, local_repos, urls, probe=head, workers=16):
    """Coordinates that are in no local repository and that every remote repository answers as missing."""
    remote = []
    for coordinate in sorted(coordinates):
        relative = pom_path(*coordinate)
        if not any(os.path.exists(os.path.join(repo, relative)) for repo in local_repos):
            remote.append(coordinate)
    if not urls:
        return []

    def missing_everywhere(coordinate):
        relative = pom_path(*coordinate)
        for url in urls:
            if probe(url + relative) != MISSING:
                return False
        return True

    with ThreadPoolExecutor(max_workers=workers) as pool:
        return [c for c, missing in zip(remote, pool.map(missing_everywhere, remote)) if missing]


def closure_pins(reactor, closure, versions):
    """Managed external (groupId, artifactId, version) the closure's modules name directly."""
    own = set(reactor.groups)
    pins = set()
    for artifact_id in closure:
        module = reactor.by_artifact.get(artifact_id)
        if module is None:
            continue
        for group, artifact in module.raw_deps:
            if group and group not in own and (group, artifact) in versions:
                pins.add((group, artifact, versions[(group, artifact)]))
    return pins
