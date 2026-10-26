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
"""Build two engine revisions and snapshot the EMIT corpus on each.

The measuring instrument - the snapshot hook in the EMIT framework, and the corpus - is copied from the
checkout this tool runs in onto both revisions, so the two snapshots differ only by product code. Each revision builds
into its own local repository under a fixed `review-SNAPSHOT` version: the base builds the corpus
modules' whole closure once and is cached by commit; the head rebuilds only the modules the change
touches and reads everything else through the base repository. Nothing is written to ~/.m2.
"""
import glob
import json
import os
import re
import shutil
import subprocess
import time

from engine_review.modules import Reactor
from engine_review.scope import WORKTREE, changed_files, git, load_config, resolve

REVISION = "review-SNAPSHOT"
CACHE = os.environ.get("ENGINE_REVIEW_CACHE", os.path.join(os.path.expanduser("~"), ".cache", "legend-engine-review"))
M2 = os.path.join(os.path.expanduser("~"), ".m2", "repository")
FRAMEWORK_ROOT = "legend-engine-core/legend-engine-core-emit"
PRODUCT_MARKER = ".engine-review-product.json"
SHORT = 11


def log(message):
    print("[engine-review] " + message, flush=True)


def java_home():
    configured = os.environ.get("ENGINE_REVIEW_JAVA_HOME")
    if configured:
        return configured
    try:
        return subprocess.run(["/usr/libexec/java_home", "-v", "17"], check=True, capture_output=True, text=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return os.environ.get("JAVA_HOME")


class Maven:
    def __init__(self, tree, repo, tails, logs, jobs):
        self.tree, self.repo, self.tails, self.logs, self.jobs = tree, repo, tails, logs, jobs

    def run(self, name, args, parallel=True):
        os.makedirs(self.logs, exist_ok=True)
        log_path = os.path.join(self.logs, name + ".log")
        base = ["mvn", "-B", "-Drevision=" + REVISION, "-Dmaven.gitcommitid.nativegit=true", "-Dcheckstyle.skip",
                "-Dmaven.repo.local=" + self.repo, "-Dmaven.repo.local.tail=" + ",".join(self.tails)]
        command = base + (["-T", str(self.jobs)] if (parallel and self.jobs > 1) else []) + args
        env = dict(os.environ)
        home = java_home()
        if home:
            env["JAVA_HOME"] = home
        started = time.time()
        log("mvn %s  (log: %s)" % (" ".join(args), log_path))
        with open(log_path, "w") as out:
            code = subprocess.run(command, cwd=self.tree, stdout=out, stderr=subprocess.STDOUT, env=env).returncode
        with open(log_path) as f:
            text = f.read()
        succeeded = code == 0 and "BUILD SUCCESS" in text
        if not succeeded and parallel and self.jobs > 1 and ("zip file is empty" in text or "ZipException" in text):
            resume = re.search(r"-rf (:\S+)", text)
            log("parallel build produced an empty jar; resuming serially" + (" from " + resume.group(1) if resume else ""))
            return self.run(name + "-serial", args + (["-rf", resume.group(1)] if resume else []), parallel=False)
        log("%s in %.0fs" % ("ok" if succeeded else "FAILED", time.time() - started))
        if not succeeded:
            raise RuntimeError("Maven step '%s' failed; see %s" % (name, log_path))
        return log_path


def prepare_tree(repo, sha):
    tree = os.path.join(CACHE, "worktrees", sha[:SHORT])
    if not os.path.isdir(tree):
        log("creating worktree %s at %s" % (tree, sha[:SHORT]))
        git(repo, "worktree", "add", "--detach", "--force", tree, sha)
    else:
        git(tree, "checkout", "--detach", "--force", sha)
        git(tree, "clean", "-fdq", "--", FRAMEWORK_ROOT)
    local_mvn = os.path.join(repo, ".mvn")
    if os.path.isdir(local_mvn):
        shutil.copytree(local_mvn, os.path.join(tree, ".mvn"), dirs_exist_ok=True)
    return tree


def _replace_tree(source, target):
    if os.path.isdir(target):
        shutil.rmtree(target)
    shutil.copytree(source, target, ignore=shutil.ignore_patterns("target"))


def overlay(tool_root, tree, suite_modules, instrument_files):
    """Copy the snapshot hook and each suite's src/test from the tool checkout onto a revision. The rest of the EMIT
    framework stays the revision's own: EMITTasks calls product APIs that move between revisions."""
    if os.path.realpath(tool_root) == os.path.realpath(tree):
        return []
    copied = []
    for path in instrument_files:
        target = os.path.join(tree, path)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        shutil.copy(os.path.join(tool_root, path), target)
        copied.append(path)
    for module in suite_modules:
        source = os.path.join(tool_root, module, "src", "test")
        if os.path.isdir(source) and os.path.isdir(os.path.join(tree, module)):
            _replace_tree(source, os.path.join(tree, module, "src", "test"))
            copied.append(module + "/src/test")
    return copied


def _product_built(repo, suite_modules):
    marker = os.path.join(repo, PRODUCT_MARKER)
    if not os.path.exists(marker):
        return False
    with open(marker) as f:
        return set(suite_modules) <= set(json.load(f)["suites"])


def snapshot(maven, name, suites, out, cross_read=None):
    """One Maven invocation per suite module, each into <out>/<module>/: modules may share a classpath root name."""
    if os.path.isdir(out):
        shutil.rmtree(out)
    for module, tests in suites:
        module_name = os.path.basename(module)
        args = ["clean", "test", "-pl", module, "-Dtest=" + ",".join(tests),
                "-Dsurefire.failIfNoSpecifiedTests=false", "-DfailIfNoTests=false", "-Dmaven.test.failure.ignore=true",
                "-Dlegend.emit.snapshot.dir=" + os.path.join(out, module_name)]
        if cross_read:
            args.append("-Dlegend.emit.snapshot.crossRead=" + os.path.join(cross_read, module_name))
        maven.run("%s-%s" % (name, module_name), args, parallel=False)
        reports = os.path.join(maven.tree, module, "target", "surefire-reports")
        if os.path.isdir(reports):
            target = os.path.join(out, "_surefire", module_name)
            os.makedirs(target, exist_ok=True)
            for report in glob.glob(os.path.join(reports, "TEST-*.xml")):
                shutil.copy(report, target)


def run(tool_root, base_rev, head_rev, suites=None, rebuild="changed", jobs=4, out=None, forward=True):
    config = load_config()
    base = resolve(tool_root, base_rev)
    head = resolve(tool_root, head_rev)
    reactor = Reactor(tool_root)
    suite_names = suites or [name for name, suite in config["suites"].items() if suite["lenses"]]
    suite_modules = [config["suites"][name]["module"] for name in suite_names]
    suite_runs = [(config["suites"][name]["module"], config["suites"][name]["tests"]) for name in suite_names]
    instrument = config["framework"]
    instrument_ids = {reactor.by_path[p].artifact_id for p in instrument + suite_modules}
    closure = reactor.upstream(reactor.by_path[p].artifact_id for p in suite_modules)

    out = out or os.path.join(CACHE, "runs", "%s-%s-%s" % (base[:SHORT], "worktree" if head == WORKTREE else head[:SHORT], time.strftime("%Y%m%d-%H%M%S")))
    logs = os.path.join(out, "logs")
    os.makedirs(logs, exist_ok=True)
    manifest = {"base": base, "head": head, "suites": suite_names, "tool_root": tool_root, "rebuild": rebuild, "caveats": []}

    base_tree = prepare_tree(tool_root, base)
    manifest["base_overlay"] = overlay(tool_root, base_tree, suite_modules, config["instrument_files"])
    base_repo = os.path.join(CACHE, "m2", base[:SHORT])
    base_maven = Maven(base_tree, base_repo, [M2], logs, jobs)
    if _product_built(base_repo, suite_modules):
        log("base product already built in " + base_repo)
    else:
        base_maven.run("base-product", ["clean", "install", "-DskipTests", "-pl", ",".join(suite_modules), "-am"])
        with open(os.path.join(base_repo, PRODUCT_MARKER), "w") as f:
            json.dump({"suites": suite_modules, "sha": base}, f)
    base_maven.run("base-instrument", ["clean", "install", "-DskipTests", "-pl", ",".join(instrument)], parallel=False)

    head_tree = tool_root if head == WORKTREE else prepare_tree(tool_root, head)
    manifest["head_overlay"] = overlay(tool_root, head_tree, suite_modules, config["instrument_files"])
    head_repo = os.path.join(CACHE, "m2", "worktree" if head == WORKTREE else head[:SHORT] + "-on-" + base[:SHORT])
    head_maven = Maven(head_tree, head_repo, [base_repo, M2], logs, jobs)
    changed = {m.artifact_id for m in (reactor.module_of(f) for f in changed_files(tool_root, base, head)) if m is not None}
    rebuilt = (reactor.downstream(changed) if rebuild == "dependents" else changed) & closure
    rebuilt -= instrument_ids
    outside = sorted(changed - closure - instrument_ids)
    manifest["head_rebuilt"] = sorted(rebuilt)
    skipped_dependents = sorted((reactor.downstream(changed) & closure) - rebuilt - instrument_ids)
    if skipped_dependents:
        manifest["caveats"].append("%d module(s) depend on a changed module but were not rebuilt (--rebuild changed); a signature change would surface as linkage errors in head: %s" % (len(skipped_dependents), ", ".join(skipped_dependents)))
    manifest["changed_outside_corpus_closure"] = outside
    if outside:
        manifest["caveats"].append("%d changed module(s) are outside the corpus closure, so EMIT cannot observe them: %s" % (len(outside), ", ".join(outside)))
    head_marker = os.path.join(head_repo, PRODUCT_MARKER)
    head_key = {"suites": suite_modules, "sha": head, "base": base, "rebuilt": sorted(rebuilt)}
    if head != WORKTREE and os.path.exists(head_marker) and json.load(open(head_marker)) == head_key:
        log("head product already built in " + head_repo)
    else:
        shutil.rmtree(os.path.join(head_repo, "org", "finos", "legend", "engine"), ignore_errors=True)
        if rebuilt:
            head_maven.run("head-product", ["clean", "install", "-DskipTests", "-pl", ",".join(reactor.paths(rebuilt))])
        else:
            log("no product module in the corpus closure changed; head reuses the base product")
        os.makedirs(head_repo, exist_ok=True)
        with open(head_marker, "w") as f:
            json.dump(head_key, f)
    head_maven.run("head-instrument", ["clean", "install", "-DskipTests", "-pl", ",".join(instrument)], parallel=False)

    snapshot(base_maven, "base-snapshot", suite_runs, os.path.join(out, "base"))
    snapshot(head_maven, "head-snapshot", suite_runs, os.path.join(out, "head"), cross_read=os.path.join(out, "base"))
    if forward:
        snapshot(base_maven, "base-crossread", suite_runs, os.path.join(out, "base-xread"), cross_read=os.path.join(out, "head"))

    with open(os.path.join(out, "run.json"), "w") as f:
        json.dump(manifest, f, indent=2)
    return out, manifest
