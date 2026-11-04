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
"""Which review lenses a change needs, which modules it rebuilds, and what EMIT cannot see."""
import json
import os
import re
import subprocess

from engine_review.modules import ENGINE_GROUP, Reactor

WORKTREE = "WORKTREE"
PLUGIN_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CONFIG = os.path.join(PLUGIN_ROOT, "config", "lenses.json")
REPO_CONFIG = os.path.join(".engine-review", "lenses.json")
DEFAULT_TESTS = ["*EMITTests"]
DEFAULT_LENSES = ["sql", "protocol"]
# Where the EMIT framework and its snapshot hook live in legend-engine; repo configs never override these.
ENGINE_KEYS = ("framework", "instrument_files")


def load_config(path=CONFIG):
    with open(path) as f:
        return json.load(f)


def resolve_config(repo, path=None):
    """The config for the repository under review: --config, else <repo>/.engine-review/lenses.json, else the
    bundled legend-engine config when the repo is legend-engine, else discovery-only defaults."""
    bundled = load_config()
    if path:
        source = os.path.abspath(path)
    elif os.path.exists(os.path.join(repo, REPO_CONFIG)):
        source = os.path.join(repo, REPO_CONFIG)
    elif _is_engine(repo):
        source = CONFIG
    else:
        source = None
    config = load_config(source) if source else {}
    for key in ENGINE_KEYS:
        config[key] = bundled[key]
    for key in ("ignore", "rules", "blind_spots"):
        config.setdefault(key, [])
    config.setdefault("suites", {})
    config["source"] = source or "defaults (no .engine-review/lenses.json)"
    return config


def _is_engine(repo):
    root = Reactor.read_root(repo)
    return root is not None and root.group_id == ENGINE_GROUP and root.artifact_id == "legend-engine"


def suites(config, reactor):
    """Every EMIT suite in the reactor. Discovered modules get default lenses and the *EMITTests pattern; a config
    entry for the same module names it and may set lenses, tests, or a note. Configured modules that are not
    discovered (e.g. absent at this revision) are kept so the caller can report them."""
    by_module = {}
    for name, suite in config.get("suites", {}).items():
        by_module[suite["module"]] = dict(suite, name=name)
    for module in reactor.emit_modules():
        by_module.setdefault(module.path, {"name": os.path.basename(module.path), "module": module.path})
    out = {}
    for module, suite in sorted(by_module.items()):
        entry = dict(suite)
        entry.setdefault("tests", DEFAULT_TESTS)
        entry.setdefault("lenses", DEFAULT_LENSES)
        out[entry.pop("name")] = entry
    return out


def glob_regex(pattern):
    out, index = [], 0
    while index < len(pattern):
        if pattern.startswith("**/", index):
            out.append("(?:.*/)?")
            index += 3
        elif pattern.startswith("**", index):
            out.append(".*")
            index += 2
        elif pattern[index] == "*":
            out.append("[^/]*")
            index += 1
        elif pattern[index] == "?":
            out.append("[^/]")
            index += 1
        else:
            out.append(re.escape(pattern[index]))
            index += 1
    return re.compile("^" + "".join(out) + "$")


def matches(path, patterns):
    return any(glob_regex(p).match(path) for p in patterns)


def git(repo, *args):
    return subprocess.run(["git", "-C", repo] + list(args), check=True, capture_output=True, text=True).stdout


def resolve(repo, rev):
    return rev if rev == WORKTREE else git(repo, "rev-parse", "--verify", rev + "^{commit}").strip()


def changed_files(repo, base, head):
    if head == WORKTREE:
        files = git(repo, "diff", "--name-only", base).split()
        files += git(repo, "ls-files", "--others", "--exclude-standard").split()
    else:
        files = git(repo, "diff", "--name-only", base, head).split()
    return sorted(set(files))


def changed_lines(repo, base, head, path):
    args = ["diff", "-U0", base] + ([] if head == WORKTREE else [head]) + ["--", path]
    return [line[1:] for line in git(repo, *args).splitlines() if line[:1] in "+-" and not line.startswith(("+++", "---"))]


def classify(repo, base, head, config=None):
    config = config or resolve_config(repo)
    files = changed_files(repo, base, head)
    lenses, blind_spots = {}, {}
    for path in files:
        for spot in config["blind_spots"]:
            if matches(path, spot["paths"]) and not matches(path, spot.get("exclude", [])):
                blind_spots.setdefault(spot["note"], []).append(path)
        if matches(path, config.get("ignore", [])):
            continue
        lines = changed_lines(repo, base, head, path) if (path.endswith(".java") and "/src/main/" in path) else []
        for rule in config["rules"]:
            hit = matches(path, rule["paths"])
            reason = rule["why"]
            if not hit and rule.get("content") and lines:
                found = sorted({p for p in rule["content"] for line in lines if re.search(p, line)})
                if found:
                    hit, reason = True, "changed lines mention " + ", ".join(found)
            if hit:
                lenses.setdefault(rule["lens"], {}).setdefault(path, reason)
    reactor = Reactor(repo)
    modules = sorted({m.artifact_id for m in (reactor.module_of(f) for f in files) if m is not None})
    all_suites = suites(config, reactor)
    selected = [name for name, suite in all_suites.items() if set(suite["lenses"]) & set(lenses)]
    return {
        "base": base,
        "head": head,
        "config": config["source"],
        "files": files,
        "lenses": {lens: [{"path": p, "why": w} for p, w in sorted(hits.items())] for lens, hits in sorted(lenses.items())},
        "blind_spots": [{"note": note, "paths": paths} for note, paths in blind_spots.items()],
        "modules": modules,
        "suites": selected,
        "available_suites": {name: suite["module"] for name, suite in all_suites.items()},
    }
