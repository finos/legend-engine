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
"""Recoverability for long snapshot passes: which models finished, which runner classes must run again,
whether a pass silently lost models, and whether the machine can hold the run at all.

A corpus of thousands of models takes hours per pass, and a pass run with test failures ignored reports
BUILD SUCCESS even when a fork dies. The snapshot hook writes complete.json last in each model
directory, so completeness is exact; everything here builds on that.
"""
import fnmatch
import glob
import hashlib
import json
import os
import re
import shutil
import socket
import time

COMPLETE = "complete.json"
MARKER = ".engine-review-pass.json"
LOCK = ".engine-review.lock"
_PACKAGE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)
_ROOT_CALL = re.compile(r'EMITTestSuiteBuilder\s*\.\s*(?:testContainers|tests)\s*\(\s*"([^"]+)"\s*\)')
CRASH_PATTERNS = (
    "java.lang.OutOfMemoryError",
    "The forked VM terminated without properly saying goodbye",
    "Crashed tests",
    "There was a timeout or other error in the fork",
)
_VOLATILE_ROOTS = ("/tmp", "/var/tmp", "/dev/shm")


class Runner:
    def __init__(self, fqcn, root, expected):
        self.fqcn, self.root, self.expected = fqcn, root, expected

    @property
    def simple_name(self):
        return self.fqcn.rsplit(".", 1)[-1]

    @property
    def snapshot_dir(self):
        return snapshot_dir_name(self.root) if self.root else None

    def __repr__(self):
        return "Runner(%s -> %s, %s models)" % (self.fqcn, self.root, self.expected)


def snapshot_dir_name(classpath_root):
    """The suite directory EMITSnapshot writes for a classpath root (mirrors EMITSnapshot.fileName)."""
    return re.sub(r"[^A-Za-z0-9._-]", "_", classpath_root.strip("/").replace("::", "."))


def runners(tree, module):
    """Every *EMITTests class of a suite module with the classpath root it drives and how many models live there.
    A runner that selects named models, or builds its root at runtime, has no root: it cannot be resumed alone."""
    found = []
    resources = os.path.join(tree, module, "src", "test", "resources")
    for path in sorted(glob.glob(os.path.join(tree, module, "src", "test", "java", "**", "*EMITTests.java"), recursive=True)):
        with open(path, encoding="utf-8", errors="replace") as f:
            source = f.read()
        package = _PACKAGE.search(source)
        name = os.path.basename(path)[:-len(".java")]
        fqcn = (package.group(1) + "." + name) if package else name
        roots = _ROOT_CALL.findall(source)
        root = roots[0] if len(roots) == 1 else None
        expected = len(glob.glob(os.path.join(resources, root.strip("/"), "**", "*.emit.yaml"), recursive=True)) if root else 0
        found.append(Runner(fqcn, root, expected))
    return found


def selected(runner_list, tests):
    """The runners a surefire -Dtest list selects (class names or globs, optionally package-qualified or #method)."""
    def hit(runner, pattern):
        pattern = pattern.split("#", 1)[0].replace("/", ".")
        if pattern.endswith(".java"):
            pattern = pattern[:-len(".java")]
        return fnmatch.fnmatchcase(runner.simple_name, pattern) or fnmatch.fnmatchcase(runner.fqcn, pattern) \
            or fnmatch.fnmatchcase(runner.fqcn, "*." + pattern)
    return [r for r in runner_list if any(hit(r, p) for p in tests)]


def completed_models(suite_dir):
    if not os.path.isdir(suite_dir):
        return 0
    return sum(1 for name in os.listdir(suite_dir) if os.path.exists(os.path.join(suite_dir, name, COMPLETE)))


def pending(runner_list, module_out, finished):
    """Runners that must run (again): not finished in this pass, and - when their models are known - short of
    complete models. A finished runner short of models is a corpus gap, not a crash, and is reported instead."""
    out = []
    for runner in runner_list:
        if runner.fqcn in finished:
            continue
        if runner.root and runner.expected and completed_models(os.path.join(module_out, runner.snapshot_dir)) >= runner.expected:
            continue
        out.append(runner)
    return out


def clear(runner_list, module_out):
    for runner in runner_list:
        if runner.snapshot_dir:
            shutil.rmtree(os.path.join(module_out, runner.snapshot_dir), ignore_errors=True)


def includes_file(runner_list, path):
    """Surefire include patterns for a subset of runners; thousands of names would overflow a -Dtest argument."""
    with open(path, "w") as f:
        f.write("".join("**/%s.java\n" % r.fqcn.replace(".", "/") for r in runner_list))
    return path


def finished_classes(report_dir):
    """Classes surefire wrote a report for; a class whose fork died has none."""
    names = set()
    for report in glob.glob(os.path.join(report_dir, "TEST-*.xml")):
        names.add(os.path.basename(report)[len("TEST-"):-len(".xml")])
    return names


def crashes(log_path, limit=20):
    found = []
    try:
        with open(log_path, errors="replace") as f:
            for line in f:
                if any(p in line for p in CRASH_PATTERNS):
                    found.append(line.strip()[:300])
                    if len(found) >= limit:
                        break
    except OSError:
        pass
    return found


def gaps(runner_list, module_out):
    out = []
    for runner in runner_list:
        if runner.root and runner.expected:
            done = completed_models(os.path.join(module_out, runner.snapshot_dir))
            if done < runner.expected:
                out.append({"runner": runner.fqcn, "root": runner.root, "expected": runner.expected, "complete": done})
    return out


def load_marker(out):
    path = os.path.join(out, MARKER)
    if not os.path.exists(path):
        return None
    with open(path) as f:
        return json.load(f)


def write_marker(out, marker):
    os.makedirs(out, exist_ok=True)
    temp = os.path.join(out, MARKER + ".tmp")
    with open(temp, "w") as f:
        json.dump(marker, f, indent=2)
    os.replace(temp, os.path.join(out, MARKER))


def digest(marker):
    """What a dependent pass needs to know about the pass it cross-reads: its inputs and that it completed."""
    if marker is None:
        return None
    return hashlib.sha256(json.dumps([marker.get("key"), marker.get("complete")], sort_keys=True).encode()).hexdigest()[:16]


def corpus_digest(tool_root, modules):
    """Changes when a corpus file (each suite module's src/test) is added, removed, resized or touched."""
    h = hashlib.sha256()
    for module in modules:
        for dirpath, dirnames, filenames in os.walk(os.path.join(tool_root, module, "src", "test")):
            dirnames.sort()
            for name in sorted(filenames):
                path = os.path.join(dirpath, name)
                st = os.stat(path)
                h.update(("%s\0%d\0%d\n" % (os.path.relpath(path, tool_root), st.st_size, int(st.st_mtime))).encode())
    return h.hexdigest()[:16]


def volatile_reason(path):
    """Why a path would not survive a workspace restart, or None."""
    real = os.path.realpath(path)
    for root in _VOLATILE_ROOTS:
        if real == root or real.startswith(root + "/"):
            return "%s resolves to %s, which is wiped on restart" % (path, real)
    best, fstype = "", None
    try:
        with open("/proc/mounts") as f:
            for line in f:
                parts = line.split()
                if len(parts) >= 3 and (real == parts[1] or real.startswith(parts[1].rstrip("/") + "/")) and len(parts[1]) > len(best):
                    best, fstype = parts[1], parts[2]
    except OSError:
        return None
    return "%s is on tmpfs (%s)" % (path, best) if fstype == "tmpfs" else None


def disk_shortfall(path, models, passes, mb_per_model):
    """MB missing on path's filesystem for the expected snapshot volume, or 0."""
    existing = path
    while not os.path.exists(existing):
        existing = os.path.dirname(existing) or "/"
    need = models * passes * mb_per_model
    free = shutil.disk_usage(existing).free / (1024 * 1024)
    return max(0, int(need - free))


FORK_OVERHEAD_MB = 1300
JACOCO_OVERHEAD_MB = 1100


def _xmx_mb(text):
    m = re.search(r"-Xmx(\d+)([gGmMkK]?)", text or "")
    if not m:
        return None
    unit = m.group(2).lower()
    return int(m.group(1)) * {"g": 1024, "m": 1, "k": 1.0 / 1024, "": 1.0 / (1024 * 1024)}[unit]


def fork_memory_warning(snapshot_args, meminfo="/proc/meminfo", maven_opts=None):
    """A warning when the snapshot forks plus the Maven JVM would exceed the memory available now, or None. A fork's
    RSS runs about 1.3 GB over its heap, and about 2.4 GB with the JaCoCo agent many parent poms attach."""
    forks, heap = 1, None
    for arg in snapshot_args:
        m = re.match(r"-DforkCount=(\d+)(C?)$", arg)
        if m:
            forks = int(m.group(1)) * ((os.cpu_count() or 1) if m.group(2) else 1)
        if arg.startswith("-DargLine="):
            heap = _xmx_mb(arg)
    if heap is None:
        return None
    per_fork = heap + FORK_OVERHEAD_MB + (0 if "-Djacoco.skip=true" in snapshot_args else JACOCO_OVERHEAD_MB)
    maven = (_xmx_mb(os.environ.get("MAVEN_OPTS", "") if maven_opts is None else maven_opts) or 1024) + 512
    try:
        with open(meminfo) as f:
            available = next(int(line.split()[1]) // 1024 for line in f if line.startswith("MemAvailable:"))
    except (OSError, StopIteration):
        return None
    need = int(forks * per_fork + maven)
    if need > available:
        return ("%d fork(s) x ~%d MB (heap + JVM overhead%s) + ~%d MB for Maven need ~%d MB, more than the %d MB available now; "
                "lower forkCount or -Xmx in snapshot_args" % (forks, per_fork, "" if "-Djacoco.skip=true" in snapshot_args else " + JaCoCo",
                                                             maven, need, available))
    return None


class RunLock:
    """One run per output directory; the file also tells cleanup tools the directory is live."""

    def __init__(self, out):
        self.path = os.path.join(out, LOCK)

    def __enter__(self):
        os.makedirs(os.path.dirname(self.path), exist_ok=True)
        if os.path.exists(self.path):
            with open(self.path) as f:
                held = json.load(f)
            if held.get("host") == socket.gethostname() and _alive(held.get("pid")):
                raise RuntimeError("another engine-review run (pid %s, started %s) is using %s" % (held["pid"], held.get("started"), os.path.dirname(self.path)))
        with open(self.path, "w") as f:
            json.dump({"pid": os.getpid(), "host": socket.gethostname(), "started": time.strftime("%Y-%m-%dT%H:%M:%S")}, f)
        return self

    def __exit__(self, *exc):
        try:
            os.remove(self.path)
        except OSError:
            pass
        return False


def _alive(pid):
    if not isinstance(pid, int):
        return False
    try:
        os.kill(pid, 0)
        return True
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
