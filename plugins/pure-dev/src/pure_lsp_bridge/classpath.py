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
"""Classpath resolution mirroring legend-pure-lsp-vscode's extension.ts
(resolveServerClasspath / resolveClasspathFileEntries), so a Maven
`dependency:build-classpath` file works the same way here as it does for
the VS Code extension: it supplies the *host* jars (e.g. a full
legend-engine checkout), and the server's own dependency-classpath entries
(e.g. legend-pure-* jars resolved from another project's classpath) are
excluded when the host already supplies the same filename, so there aren't
two copies of the Pure repos on one classpath.
"""
import os
import re
import sys

_VERSION_SUFFIX_RE = re.compile(r"-\d[^-]*(-SNAPSHOT)?\.jar$")
_VERSION_CAPTURE_RE = re.compile(r"-(\d[^-]*)(-SNAPSHOT)?\.jar$")


def _is_wildcard(entry):
    return entry.endswith("/*") or entry.endswith("\\*")


def _expand(entry, base_dir):
    entry = entry.strip()
    if entry == "~":
        entry = os.path.expanduser("~")
    elif entry.startswith("~/"):
        entry = os.path.expanduser(entry)
    if not os.path.isabs(entry):
        entry = os.path.join(base_dir, entry)
    return os.path.normpath(entry)


def _unique(entries):
    seen = set()
    result = []
    for entry in entries:
        if entry not in seen:
            seen.add(entry)
            result.append(entry)
    return result


def read_classpath_file(path):
    """Parse a Maven dependency:build-classpath file: entries may be on one
    line joined by os.pathsep, or one per line, or both. Relative entries
    resolve against the file's own directory. Raises FileNotFoundError with
    a descriptive message on any missing entry, matching the VS Code
    extension's fail-fast behavior rather than silently dropping jars.
    """
    path = os.path.abspath(path)
    if not os.path.isfile(path):
        raise FileNotFoundError("classpath file does not exist or is not a file: %s" % path)
    base_dir = os.path.dirname(path)

    with open(path, "r") as f:
        content = f.read()

    raw_entries = []
    for line in content.splitlines():
        for piece in line.split(os.pathsep):
            piece = piece.strip()
            if piece:
                raw_entries.append(piece)

    entries = _unique(_expand(e, base_dir) for e in raw_entries)

    for entry in entries:
        if _is_wildcard(entry):
            parent = entry[:-2]
            if not os.path.isdir(parent):
                raise FileNotFoundError(
                    "classpath file contains a wildcard with a missing parent directory: %s" % parent)
            continue
        if not os.path.exists(entry):
            raise FileNotFoundError("classpath file contains a missing entry: %s" % entry)

    return entries


def read_dependency_entries(path):
    """Like read_classpath_file, but also accepts a plain directory of jars (e.g.
    legend-pure-lsp-server's own target/dependency) for a classpath-less plain launch, where there's
    no Maven classpath to extract a dependency list from in the first place. Sorted by filename for
    determinism.
    """
    if os.path.isdir(path):
        return [os.path.join(path, name) for name in sorted(os.listdir(path)) if name.endswith(".jar")]
    return read_classpath_file(path)


def _artifact(jar_name):
    """legend-pure-m3-core-5.97.3-SNAPSHOT.jar -> legend-pure-m3-core."""
    return _VERSION_SUFFIX_RE.sub("", jar_name)


def _version_key(jar_name):
    """Sortable version tuple, or None when the version isn't purely numeric.

    Returning None for anything unparseable keeps the newer-wins rule below
    conservative: an artifact whose version can't be compared is never dropped.
    """
    match = _VERSION_CAPTURE_RE.search(jar_name)
    if not match:
        return None
    pieces = match.group(1).split(".")
    if not all(p.isdigit() for p in pieces):
        return None
    # A -SNAPSHOT sorts below the release it precedes.
    return tuple(int(p) for p in pieces) + (0 if match.group(2) else 1,)


def _interpreted_siblings(entries, already_supplied):
    """Interpreted-engine counterparts of the `-extension-compiled-` jars on `entries`.

    A host classpath is computed from a Maven module that runs Pure *compiled*, so it
    carries only the compiled extension jars. The LSP runs the *interpreted* engine,
    whose natives live in the parallel `-extension-interpreted-` artifacts; without
    them a go() dies at the first native the extension would have registered (e.g.
    readFile, which lives in ...-interpreted-functions-unclassified) with
    "not supported by this execution platform" rather than a missing-class error.

    Only siblings already resolved into the local Maven repository are added - this
    never triggers a download, so an artifact that was never published simply stays
    absent, exactly as it is today.
    """
    extra = []
    for entry in entries:
        marker = os.sep + ".m2" + os.sep + "repository" + os.sep
        idx = entry.find(marker)
        if idx < 0 or "-extension-compiled-" not in entry or not entry.endswith(".jar"):
            continue
        repo_root = entry[:idx + len(marker)]
        parts = entry[idx + len(marker):].split(os.sep)
        if len(parts) < 3:
            continue
        version, artifact = parts[-2], parts[-3]
        group_dirs = parts[:-3]
        sibling = artifact.replace("-extension-compiled-", "-extension-interpreted-")
        if sibling in already_supplied:
            continue
        path = os.path.join(repo_root, *(group_dirs + [sibling, version,
                                                       "%s-%s.jar" % (sibling, version)]))
        if os.path.isfile(path):
            extra.append(path)
    return _unique(extra)


def _host_newer_artifacts(host_entries):
    """artifact -> highest version key the host classpath supplies for it."""
    best = {}
    for entry in host_entries:
        name = os.path.basename(entry)
        key = _version_key(name)
        if key is None:
            continue
        artifact = _artifact(name)
        if artifact not in best or key > best[artifact]:
            best[artifact] = key
    return best


def resolve_server_classpath(server_jar, dep_entries, host_entries,
                             prefer_server_pure_jars=False):
    """entries = [server jar] + server's own runtime deps + host entries.

    `dep_entries` is a flat list of already-resolved jar paths supplying the LSP server's own
    runtime deps (LSP4J, the Pure interpreter, etc.) - typically another project's classpath (e.g.
    legend-engine's), borrowed when the project being launched doesn't already carry them itself.
    Pass an empty list when the target project's own `host_entries` already supplies everything the
    server jar needs (true whenever the project's classpath-anchor module transitively depends on
    legend-engine's pure-ide-light-http-server, which pulls legend-pure-lsp-server and its deps
    along with it - confirmed true for legend-engine).

    When host_entries is non-empty, a dep_entries jar is dropped only if the
    host classpath already provides a jar with the exact same filename (a true
    duplicate/version conflict) - not just because it happens to be named
    `legend-pure-*`. The LSP server itself depends on legend-pure-* jars a host
    like legend-engine may never pull in transitively (e.g.
    legend-pure-runtime-java-engine-mixed, used internally by LegendPureSession);
    blanket-dropping every legend-pure-* jar would silently strip those and the
    server would fail with a NoClassDefFoundError at runtime instead.

    Exact-filename matching only catches a duplicate when both sides pin the same
    legend-pure version. A host whose pinned legend-pure differs from the one dep_entries
    was resolved against (e.g. a PURE_DEV_EXTRA_PROJECTS_JSON project pins a newer legend-pure
    than dep_entries was borrowed from, say a legend-engine classpath resolving an older
    -SNAPSHOT) puts two differently-versioned copies of the same artifact on the classpath, and
    the Pure runtime aborts at startup with "Invalid URLs for '/platform/...' - different
    content". Set prefer_server_pure_jars to resolve that by artifact name instead of filename:
    every host `legend-pure-*` entry whose artifact dep_entries also supplies
    is dropped, so the platform repos come from exactly one place - the version dep_entries
    was resolved against. Host legend-pure artifacts dep_entries does NOT supply are still
    kept, for the same NoClassDefFoundError reason.

    Third-party artifacts (everything not `legend-pure-*`) resolve the other way:
    a dep_entries jar is dropped when the host supplies a strictly newer
    version of the same artifact, because dep_entries sits ahead of the
    host entries and would otherwise downgrade a library the host's own code was
    compiled against. commons-lang3 is the case that forced this - the server's
    own deps can pin an older version than a host project resolves, and if the
    host's own code calls a commons-lang3 method only added in the newer version
    (e.g. Range.of(), added in 3.13), the older version winning causes a
    NoSuchMethodError at first use.
    """
    entries = [server_jar]
    dep_artifacts = set()
    if dep_entries:
        if host_entries:
            host_basenames = {os.path.basename(p) for p in host_entries}
            host_versions = _host_newer_artifacts(host_entries)
            server_deps = []
            for path in sorted(dep_entries, key=os.path.basename):
                name = os.path.basename(path)
                if not name.endswith(".jar") or name in host_basenames:
                    continue
                artifact = _artifact(name)
                own = _version_key(name)
                host_best = host_versions.get(artifact)
                if (not artifact.startswith("legend-pure-")
                        and own is not None and host_best is not None
                        and host_best > own):
                    print("[classpath] dropping %s; host classpath supplies a newer %s"
                          % (name, artifact), file=sys.stderr)
                    continue
                server_deps.append(path)
            dep_artifacts = {_artifact(os.path.basename(p)) for p in server_deps}
            entries.extend(server_deps)
        else:
            entries.extend(dep_entries)

    if prefer_server_pure_jars and dep_artifacts:
        host_entries = [
            e for e in host_entries
            if not (_artifact(os.path.basename(e)).startswith("legend-pure-")
                    and _artifact(os.path.basename(e)) in dep_artifacts)
        ]

    entries.extend(host_entries)
    entries = _unique(entries)

    supplied = {_artifact(os.path.basename(e)) for e in entries}
    siblings = _interpreted_siblings(entries, supplied)
    if siblings:
        print("[classpath] adding %d interpreted-engine extension jar(s): %s"
              % (len(siblings), ", ".join(sorted(os.path.basename(s) for s in siblings))),
              file=sys.stderr)
    entries.extend(siblings)
    return _unique(entries)
