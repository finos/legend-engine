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
"""Build two revisions of a repository holding an EMIT corpus and snapshot the corpus on each.

The measuring instrument - the snapshot hook in the EMIT framework, and the corpus - is the same on both
revisions, so the two snapshots differ only by product code. The corpus is copied from the checkout this
tool runs against. The hook comes from one of two places:

- legend-engine itself: the framework is in the reactor, so the hook files are copied onto each revision
  and the framework is rebuilt there.
- a repository that consumes the framework as a dependency: each revision pins a legend-engine release.
  When that release's jar lacks the hook, the release's own framework source plus the hook is built under
  the release version into the revision's private local repository, where it shadows the released jar.

Each revision builds into its own local repository under a fixed `review-SNAPSHOT` version: the base
builds the corpus modules' whole closure once and is cached by commit; the head rebuilds only the modules
the change touches and reads everything else through the base repository. Nothing is written to ~/.m2.
"""
import glob
import json
import os
import re
import shutil
import subprocess
import time
import zipfile

from engine_review import deps, resume
from engine_review.modules import ENGINE_GROUP, Reactor, pom_property
from engine_review.scope import PLUGIN_ROOT, WORKTREE, changed_files, git, resolve, resolve_config
from engine_review.scope import suites as scope_suites

REVISION = "review-SNAPSHOT"
CACHE = os.environ.get("ENGINE_REVIEW_CACHE", os.path.join(os.path.expanduser("~"), ".cache", "legend-engine-review"))
M2 = os.path.join(os.path.expanduser("~"), ".m2", "repository")
PRODUCT_MARKER = ".engine-review-product.json"
SHORT = 11
ENGINE_JAVA = "17"
SNAPSHOT_CLASS = "org/finos/legend/engine/test/emit/EMITSnapshot.class"
VERSION_JSON = "legendExecutionVersion.json"
DEFAULT_VERSION_PROPERTY = "legend.engine.version"


def log(message):
    print("[engine-review] " + message, flush=True)


def java_home(version=ENGINE_JAVA):
    specific = os.environ.get("ENGINE_REVIEW_JAVA_HOME_" + version)
    if specific:
        return specific
    if version == ENGINE_JAVA and os.environ.get("ENGINE_REVIEW_JAVA_HOME"):
        return os.environ["ENGINE_REVIEW_JAVA_HOME"]
    try:
        return subprocess.run(["/usr/libexec/java_home", "-v", version], check=True, capture_output=True, text=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return os.environ.get("JAVA_HOME")


class Maven:
    def __init__(self, tree, repo, tails, logs, jobs, revision=REVISION, java=ENGINE_JAVA):
        self.tree, self.repo, self.tails, self.logs, self.jobs = tree, repo, tails, logs, jobs
        self.revision, self.java = revision, java

    def run(self, name, args, parallel=True):
        os.makedirs(self.logs, exist_ok=True)
        log_path = os.path.join(self.logs, name + ".log")
        base = ["mvn", "-B", "-Drevision=" + self.revision, "-Dmaven.gitcommitid.nativegit=true", "-Dcheckstyle.skip",
                "-Dmaven.repo.local=" + self.repo, "-Dmaven.repo.local.tail=" + ",".join(self.tails)]
        command = base + (["-T", str(self.jobs)] if (parallel and self.jobs > 1) else []) + args
        env = dict(os.environ)
        home = java_home(self.java)
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


def prepare_tree(repo, sha, clean=()):
    tree = os.path.join(CACHE, "worktrees", sha[:SHORT])
    if not os.path.isdir(tree):
        log("creating worktree %s at %s" % (tree, sha[:SHORT]))
        git(repo, "worktree", "add", "--detach", "--force", tree, sha)
    else:
        git(tree, "checkout", "--detach", "--force", sha)
        if clean:
            git(tree, "clean", "-fdq", "--", *clean)
    local_mvn = os.path.join(repo, ".mvn")
    if os.path.isdir(local_mvn):
        shutil.copytree(local_mvn, os.path.join(tree, ".mvn"), dirs_exist_ok=True)
    return tree


def overlay_targets(suite_modules, instrument_files):
    """Paths an overlay writes, which a reused worktree must clean so files a revision lacks do not linger."""
    return sorted({os.path.dirname(p) for p in instrument_files} | {os.path.join(m, "src", "test") for m in suite_modules})


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


def framework_in_reactor(reactor, config):
    return all(p in reactor.by_path for p in config["framework"])


def default_engine_repo():
    configured = os.environ.get("ENGINE_REVIEW_ENGINE_REPO")
    if configured:
        return configured
    try:
        return git(PLUGIN_ROOT, "rev-parse", "--show-toplevel").strip()
    except (OSError, subprocess.CalledProcessError):
        return None


def _engine_artifact(repo, artifact, version):
    return os.path.join(repo, *ENGINE_GROUP.split("."), artifact, version, "%s-%s.jar" % (artifact, version))


def _resolved(maven, artifact, version, name):
    """The jar Maven would use for an engine artifact (local repository first, then the tails), fetched into the
    private repository if no repository has it yet."""
    for attempt in range(2):
        for repo in [maven.repo] + list(maven.tails):
            jar = _engine_artifact(repo, artifact, version)
            if os.path.exists(jar):
                return jar
        if attempt == 0:
            maven.run("%s-fetch-%s" % (name, artifact), ["-N", "dependency:get", "-Dtransitive=false",
                                                        "-Dartifact=%s:%s:%s" % (ENGINE_GROUP, artifact, version)], parallel=False)
    raise RuntimeError("%s:%s:%s is not in any local repository after dependency:get" % (ENGINE_GROUP, artifact, version))


def _has_hook(jar):
    """The jar's EMITSnapshot exists and writes the completion sentinel that resuming relies on."""
    with zipfile.ZipFile(jar) as z:
        return SNAPSHOT_CLASS in z.namelist() and resume.COMPLETE.encode() in z.read(SNAPSHOT_CLASS)


def pinned_engine_version(tree, config):
    prop = config.get("engine_version_property", DEFAULT_VERSION_PROPERTY)
    value = pom_property(os.path.join(tree, "pom.xml"), prop)
    if not value or "${" in value:
        raise RuntimeError("cannot read the pinned legend-engine version: property '%s' in %s is %r; set engine_version_property "
                           "in .engine-review/lenses.json" % (prop, os.path.join(tree, "pom.xml"), value))
    return value


def release_sha(maven, version, name):
    """The commit a legend-engine release was built from, as recorded in its shared-core jar (release tags are not
    reliably mirrored)."""
    jar = _resolved(maven, "legend-engine-shared-core", version, name)
    with zipfile.ZipFile(jar) as z:
        return json.loads(z.read(VERSION_JSON))["git.commit.id"]


def ensure_framework(maven, config, engine_repo, name):
    """Make the EMIT framework Maven resolves for this revision one that carries the snapshot hook."""
    version = pinned_engine_version(maven.tree, config)
    jar = _resolved(maven, "legend-engine-emit", version, name)
    if _has_hook(jar):
        log("legend-engine-emit %s already carries the snapshot hook (%s)" % (version, jar))
        return {"engine_version": version, "hook": jar}
    missing = [p for p in config["instrument_files"] if not (engine_repo and os.path.exists(os.path.join(engine_repo, p)))]
    if missing:
        raise RuntimeError("legend-engine %s lacks the current EMIT snapshot hook, and the engine checkout %s does not have it (%s); "
                           "pass --engine-repo pointing at a legend-engine checkout that does" % (version, engine_repo, missing[0]))
    sha = release_sha(maven, version, name)
    try:
        git(engine_repo, "cat-file", "-e", sha + "^{commit}")
    except subprocess.CalledProcessError:
        log("fetching legend-engine %s (%s)" % (version, sha[:SHORT]))
        git(engine_repo, "fetch", "origin", sha)
    tree = prepare_tree(engine_repo, sha, clean=overlay_targets([], config["instrument_files"]))
    overlay(engine_repo, tree, [], config["instrument_files"])
    Maven(tree, maven.repo, maven.tails, maven.logs, maven.jobs, revision=version, java=ENGINE_JAVA).run(
        name + "-framework", ["clean", "install", "-DskipTests", "-pl", ",".join(config["framework"])], parallel=False)
    built = _engine_artifact(maven.repo, "legend-engine-emit", version)
    if not (os.path.exists(built) and _has_hook(built)):
        raise RuntimeError("built legend-engine-emit %s but %s has no snapshot hook" % (version, built))
    return {"engine_version": version, "engine_sha": sha, "hook": "built from %s plus the hook from %s" % (sha[:SHORT], engine_repo)}


def _product_built(repo, suite_modules):
    marker = os.path.join(repo, PRODUCT_MARKER)
    if not os.path.exists(marker):
        return False
    with open(marker) as f:
        return set(suite_modules) <= set(json.load(f)["suites"])


def _fresh_log_name(logs, name):
    candidate, attempt = name, 1
    while os.path.exists(os.path.join(logs, candidate + ".log")):
        attempt += 1
        candidate = "%s-%d" % (name, attempt)
    return candidate


def snapshot(maven, name, suites, out, cross_read=None, extra=(), key=None):
    """One Maven invocation per suite module, each into <out>/<module>/: modules may share a classpath root name.

    The pass records its inputs and progress in a marker. Rerun with the same inputs, a completed pass is skipped
    and an interrupted one reruns only the runner classes whose models did not all complete. Runner classes whose
    fork died are retried once; if they still do not finish, the pass stops rather than report missing models as
    engine differences."""
    key = dict(key or {}, suites=[[module, list(tests)] for module, tests in suites],
               cross_read=resume.digest(resume.load_marker(cross_read)) if cross_read else None)
    marker = resume.load_marker(out)
    if key.get("sha") not in (None, WORKTREE) and marker and marker["key"] == key:
        if marker["complete"]:
            log("%s already complete in %s" % (name, out))
            return marker
        log("resuming %s in %s" % (name, out))
    else:
        if os.path.isdir(out):
            shutil.rmtree(out)
        marker = {"key": key, "complete": False, "finished": {}, "crashes": [], "gaps": []}
    resume.write_marker(out, marker)
    runner_lists = {module: resume.selected(resume.runners(maven.tree, module), tests) for module, tests in suites}
    for module, tests in suites:
        module_name = os.path.basename(module)
        module_out = os.path.join(out, module_name)
        runner_list = runner_lists[module]
        for attempt in range(2):
            finished = set(marker["finished"].get(module, []))
            todo = resume.pending(runner_list, module_out, finished)
            if (runner_list and not todo) or (not runner_list and finished):
                break
            if runner_list and len(todo) < len(runner_list):
                log("%s: %d of %d runner class(es) of %s still to run" % (name, len(todo), len(runner_list), module_name))
                resume.clear(todo, module_out)
                selector = ["-Dsurefire.includesFile=" + resume.includes_file(todo, os.path.join(out, ".includes-%s.txt" % module_name))]
            else:
                shutil.rmtree(module_out, ignore_errors=True)
                selector = ["-Dtest=" + ",".join(tests)]
            args = ["clean", "test", "-pl", module] + selector + [
                "-Dsurefire.failIfNoSpecifiedTests=false", "-DfailIfNoTests=false", "-Dmaven.test.failure.ignore=true",
                "-Dlegend.emit.snapshot.dir=" + module_out] + list(extra)
            if cross_read:
                args.append("-Dlegend.emit.snapshot.crossRead=" + os.path.join(cross_read, module_name))
            log_name = _fresh_log_name(maven.logs, "%s-%s" % (name, module_name))
            try:
                maven.run(log_name, args, parallel=False)
            finally:
                reports = os.path.join(maven.tree, module, "target", "surefire-reports")
                if os.path.isdir(reports):
                    target = os.path.join(out, "_surefire", module_name)
                    os.makedirs(target, exist_ok=True)
                    for report in glob.glob(os.path.join(reports, "TEST-*.xml")):
                        shutil.copy(report, target)
                marker["finished"][module] = sorted(finished | resume.finished_classes(reports))
                log_path = os.path.join(maven.logs, log_name + ".log")
                found = resume.crashes(log_path)
                if found:
                    marker["crashes"].append({"log": log_path, "lines": found})
                resume.write_marker(out, marker)
            if not runner_list:
                break
            if attempt == 0 and resume.pending(runner_list, module_out, set(marker["finished"][module])):
                log("%s: runner class(es) of %s did not finish (fork crash or OOM?); retrying them once" % (name, module_name))
                continue
            break

    unfinished = sorted(r.fqcn for module, _ in suites
                        for r in resume.pending(runner_lists[module], os.path.join(out, os.path.basename(module)), set(marker["finished"].get(module, []))))
    marker["gaps"] = [dict(gap, module=module) for module, _ in suites
                      for gap in resume.gaps(runner_lists[module], os.path.join(out, os.path.basename(module)))]
    marker["models"] = len(glob.glob(os.path.join(out, "**", resume.COMPLETE), recursive=True))
    marker["complete"] = not unfinished and marker["models"] > 0
    resume.write_marker(out, marker)
    if not marker["models"]:
        raise RuntimeError("'%s' wrote no complete snapshot under %s: the EMIT framework on the test classpath has no current "
                           "snapshot hook, or no test ran (see the %s-* logs)" % (name, out, name))
    if unfinished:
        raise RuntimeError("'%s' is incomplete: %d runner class(es) did not finish (%s%s); crash lines are in %s. Fix snapshot_args "
                           "(fork heap, forkCount) and rerun with the same --out to resume"
                           % (name, len(unfinished), ", ".join(unfinished[:5]), " ..." if len(unfinished) > 5 else "", os.path.join(out, resume.MARKER)))
    return marker


class Plan:
    """What to snapshot and how, resolved once from the checkout the tool runs against."""

    def __init__(self, tool_root, config, suite_names, engine_repo):
        self.tool_root, self.config = tool_root, config
        reactor = Reactor(tool_root)
        available = scope_suites(config, reactor)
        names = suite_names or [n for n, s in available.items() if s["lenses"]]
        unknown = [n for n in names if n not in available]
        if unknown:
            raise RuntimeError("unknown suite(s) %s; available: %s" % (", ".join(unknown), ", ".join(available) or "none"))
        if not names:
            raise RuntimeError("no EMIT suite found in %s (a module depending on legend-engine-emit-junit with *EMITTests classes)" % tool_root)
        self.suite_names = names
        self.suite_modules = [available[n]["module"] for n in names]
        self.suite_runs = [(available[n]["module"], available[n]["tests"]) for n in names]
        self.in_reactor = framework_in_reactor(reactor, config)
        self.instrument = config["framework"] if self.in_reactor else []
        self.instrument_files = config["instrument_files"] if self.in_reactor else []
        self.engine_repo = engine_repo or default_engine_repo()
        self.java = config.get("java", ENGINE_JAVA)
        self.snapshot_args = config.get("snapshot_args", [])
        self.runners = {m: resume.selected(resume.runners(tool_root, m), tests) for m, tests in self.suite_runs}
        self.expected_models = sum(r.expected for runners in self.runners.values() for r in runners)
        self.corpus = resume.corpus_digest(tool_root, self.suite_modules)

    def tree(self, sha):
        return self.tool_root if sha == WORKTREE else prepare_tree(self.tool_root, sha, clean=overlay_targets(self.suite_modules, self.instrument_files))

    def overlay(self, tree):
        return overlay(self.tool_root, tree, self.suite_modules, self.instrument_files)

    def maven(self, tree, repo, tails, logs, jobs):
        return Maven(tree, repo, tails, logs, jobs, java=self.java)

    def instrument_step(self, maven, name):
        """In legend-engine, rebuild the revision's framework with the hook; elsewhere, make the pinned release's
        framework carry it."""
        if self.in_reactor:
            maven.run(name + "-instrument", ["clean", "install", "-DskipTests", "-pl", ",".join(self.instrument)], parallel=False)
            return {"framework": "in-reactor"}
        return ensure_framework(maven, self.config, self.engine_repo, name)

    def snapshot(self, maven, name, out, key, cross_read=None):
        return snapshot(maven, name, self.suite_runs, out, cross_read=cross_read, extra=self.snapshot_args, key=dict(key, corpus=self.corpus))

    def preflight(self, out, passes, manifest):
        """Catch, before hours of builds and passes, what would lose or stall the run."""
        for path in (out, os.path.join(CACHE, "m2"), os.path.join(CACHE, "worktrees")):
            reason = resume.volatile_reason(path)
            if reason:
                log("WARNING: " + reason)
                manifest["caveats"].append(reason + "; an interrupted run there cannot be resumed after a restart")
        mb = self.config.get("snapshot_mb_per_model", 1.5)
        short = resume.disk_shortfall(out, self.expected_models, passes, mb)
        if short:
            raise RuntimeError("%d models x %d passes x %.1f MB need %d MB more than is free under %s; free space, move --out, "
                               "or set snapshot_mb_per_model" % (self.expected_models, passes, mb, short, out))
        warning = resume.fork_memory_warning(self.snapshot_args)
        if warning:
            log("WARNING: " + warning)
            manifest["caveats"].append(warning)

    def check_dependencies(self, maven, reactor, suite_ids, name):
        """Stop before any build when a pin of the corpus closure is in no reachable repository."""
        if not self.config.get("preflight_dependencies", True):
            return
        effective = os.path.join(maven.logs, name + "-effective-pom.xml")
        maven.run(name + "-effective-pom", ["-N", "help:effective-pom", "-Doutput=" + effective], parallel=False)
        pins = deps.closure_pins(reactor, reactor.upstream(suite_ids), deps.managed_versions(effective))
        settings = os.environ.get("ENGINE_REVIEW_MAVEN_SETTINGS", os.path.join(os.path.expanduser("~"), ".m2", "settings.xml"))
        missing = deps.unresolvable(pins, [maven.repo] + list(maven.tails), deps.repository_urls(settings))
        if missing:
            raise RuntimeError("%s pins %d artifact(s) that no local or configured repository has: %s. Build them into %s (e.g. from their "
                               "release tag), or set preflight_dependencies to false to skip this check"
                               % (name, len(missing), ", ".join(":".join(c) for c in missing[:10]), maven.repo))
        log("%s: %d direct external pin(s) of the corpus closure resolve" % (name, len(pins)))

    def suite_ids(self, reactor):
        missing = [p for p in self.suite_modules if p not in reactor.by_path]
        if missing:
            raise RuntimeError("suite module(s) %s do not exist at %s; narrow with --suites" % (", ".join(missing), reactor.repo_root))
        return [reactor.by_path[p].artifact_id for p in self.suite_modules]


def _names(names, limit=10):
    return ", ".join(names[:limit]) + (" and %d more (see run.json)" % (len(names) - limit) if len(names) > limit else "")


def _changed_modules(files, head_reactor, base_reactor):
    changed, poms = set(), set()
    for path in files:
        module = head_reactor.module_of(path) or base_reactor.module_of(path)
        if module is None:
            continue
        changed.add(module.artifact_id)
        if os.path.basename(path) == "pom.xml":
            poms.add(module.artifact_id)
    return changed, poms


def run(tool_root, base_rev, head_rev, suites=None, rebuild="changed", jobs=4, out=None, forward=True, config=None, engine_repo=None):
    config = config or resolve_config(tool_root)
    base = resolve(tool_root, base_rev)
    head = resolve(tool_root, head_rev)
    plan = Plan(tool_root, config, suites, engine_repo)
    out = out or os.path.join(CACHE, "runs", "%s-%s-%s" % (base[:SHORT], "worktree" if head == WORKTREE else head[:SHORT], time.strftime("%Y%m%d-%H%M%S")))
    with resume.RunLock(out):
        return _run(plan, tool_root, base, head, rebuild, jobs, out, forward)


def _run(plan, tool_root, base, head, rebuild, jobs, out, forward):
    logs = os.path.join(out, "logs")
    os.makedirs(logs, exist_ok=True)
    log("output %s (rerun with --out %s to resume an interrupted run)" % (out, out))
    manifest = {"base": base, "head": head, "suites": plan.suite_names, "tool_root": tool_root, "rebuild": rebuild,
                "config": plan.config["source"], "framework": "in-reactor" if plan.in_reactor else "pinned legend-engine release",
                "expected_models": plan.expected_models, "caveats": []}
    plan.preflight(out, 3 if forward else 2, manifest)

    base_tree = plan.tree(base)
    manifest["base_overlay"] = plan.overlay(base_tree)
    base_reactor = Reactor(base_tree)
    base_ids = plan.suite_ids(base_reactor)
    base_repo = os.path.join(CACHE, "m2", base[:SHORT])
    base_maven = plan.maven(base_tree, base_repo, [M2], logs, jobs)

    head_tree = plan.tree(head)
    manifest["head_overlay"] = plan.overlay(head_tree)
    head_reactor = Reactor(head_tree)
    head_ids = plan.suite_ids(head_reactor)
    head_repo = os.path.join(CACHE, "m2", "worktree" if head == WORKTREE else head[:SHORT] + "-on-" + base[:SHORT])
    head_maven = plan.maven(head_tree, head_repo, [base_repo, M2], logs, jobs)
    plan.check_dependencies(base_maven, base_reactor, base_ids, "base")
    plan.check_dependencies(head_maven, head_reactor, head_ids, "head")

    if not plan.in_reactor:
        manifest["base_framework"] = plan.instrument_step(base_maven, "base")
    if _product_built(base_repo, plan.suite_modules):
        log("base product already built in " + base_repo)
    else:
        base_maven.run("base-product", ["clean", "install", "-DskipTests", "-pl", ",".join(plan.suite_modules), "-am"])
        with open(os.path.join(base_repo, PRODUCT_MARKER), "w") as f:
            json.dump({"suites": plan.suite_modules, "sha": base}, f)
    if plan.in_reactor:
        plan.instrument_step(base_maven, "base")

    closure = head_reactor.upstream(head_ids)
    instrument_ids = {head_reactor.by_path[p].artifact_id for p in plan.instrument if p in head_reactor.by_path} | set(head_ids)
    changed, poms = _changed_modules(changed_files(tool_root, base, head), head_reactor, base_reactor)
    # A pom can move dependency versions for every module inheriting from or depending on it, so those rebuild
    # even under --rebuild changed; otherwise head mixes artifacts built against two dependency sets.
    rebuilt = (head_reactor.downstream(changed) if rebuild == "dependents" else changed) | head_reactor.downstream(poms)
    rebuilt = (rebuilt & closure) - instrument_ids
    outside = sorted(changed - closure - instrument_ids)
    manifest["head_rebuilt"] = sorted(rebuilt)
    if poms:
        manifest["caveats"].append("pom.xml changed in %s; every corpus-closure module inheriting from or depending on it was rebuilt" % _names(sorted(poms)))
    skipped_dependents = sorted((head_reactor.downstream(changed) & closure) - rebuilt - instrument_ids)
    if skipped_dependents:
        manifest["caveats"].append("%d module(s) depend on a changed module but were not rebuilt (--rebuild changed); a signature change would surface as linkage errors in head: %s" % (len(skipped_dependents), _names(skipped_dependents)))
    manifest["changed_outside_corpus_closure"] = outside
    manifest["changed_poms"] = sorted(poms)
    if outside:
        manifest["caveats"].append("%d changed module(s) are outside the corpus closure, so EMIT cannot observe them: %s" % (len(outside), _names(outside)))
    head_marker = os.path.join(head_repo, PRODUCT_MARKER)
    head_key = {"suites": plan.suite_modules, "sha": head, "base": base, "rebuilt": head_reactor.paths(rebuilt)}
    if head != WORKTREE and os.path.exists(head_marker) and json.load(open(head_marker)) == head_key:
        log("head product already built in " + head_repo)
        if not plan.in_reactor:
            manifest["head_framework"] = plan.instrument_step(head_maven, "head")
    else:
        for group in head_reactor.groups:
            shutil.rmtree(os.path.join(head_repo, *group.split(".")), ignore_errors=True)
        if not plan.in_reactor:
            manifest["head_framework"] = plan.instrument_step(head_maven, "head")
        if rebuilt:
            head_maven.run("head-product", ["clean", "install", "-DskipTests", "-pl", ",".join(head_reactor.paths(rebuilt))])
        else:
            log("no product module in the corpus closure changed; head reuses the base product")
        os.makedirs(head_repo, exist_ok=True)
        with open(head_marker, "w") as f:
            json.dump(head_key, f)
    if plan.in_reactor:
        plan.instrument_step(head_maven, "head")
    for side in ("base_framework", "head_framework"):
        version = manifest.get(side, {}).get("engine_version")
        if version:
            manifest.setdefault("engine_versions", {})[side.split("_")[0]] = version
    versions = manifest.get("engine_versions", {})
    if versions and versions.get("base") != versions.get("head"):
        manifest["caveats"].append("the pinned legend-engine moves %s -> %s; review that range with engine-review in legend-engine too" % (versions.get("base"), versions.get("head")))

    base_pass = {"sha": base, "engine": manifest.get("base_framework", {}).get("engine_version")}
    head_pass = {"sha": head, "base": base, "engine": manifest.get("head_framework", {}).get("engine_version")}
    passes = {"base-snapshot": plan.snapshot(base_maven, "base-snapshot", os.path.join(out, "base"), base_pass)}
    passes["head-snapshot"] = plan.snapshot(head_maven, "head-snapshot", os.path.join(out, "head"), head_pass, cross_read=os.path.join(out, "base"))
    if forward:
        passes["base-crossread"] = plan.snapshot(base_maven, "base-crossread", os.path.join(out, "base-xread"), base_pass, cross_read=os.path.join(out, "head"))
    for name, marker in passes.items():
        for gap in marker.get("gaps", []):
            manifest["caveats"].append("%s: %s finished but only %d of %d models under %s have a complete snapshot (model failed to load, "
                                       "or a corpus gap)" % (name, gap["runner"], gap["complete"], gap["expected"], gap["root"]))
        if marker.get("crashes"):
            manifest["caveats"].append("%s: fork crash or OOM lines in %s; the affected runner classes were rerun"
                                       % (name, ", ".join(c["log"] for c in marker["crashes"])))

    with open(os.path.join(out, "run.json"), "w") as f:
        json.dump(manifest, f, indent=2)
    return out, manifest


def snapshot_one(tool_root, rev, out, suites=None, repo_key=None, jobs=4, cross_read=None, config=None, engine_repo=None):
    config = config or resolve_config(tool_root)
    plan = Plan(tool_root, config, suites, engine_repo)
    sha = resolve(tool_root, rev)
    with resume.RunLock(out):
        return _snapshot_one(plan, tool_root, sha, out, repo_key, jobs, cross_read)


def _snapshot_one(plan, tool_root, sha, out, repo_key, jobs, cross_read):
    plan.preflight(out, 1, {"caveats": []})
    tree = plan.tree(sha)
    plan.overlay(tree)
    reactor = Reactor(tree)
    suite_ids = plan.suite_ids(reactor)
    key = "worktree" if sha == WORKTREE else sha[:SHORT]
    maven = plan.maven(tree, os.path.join(CACHE, "m2", repo_key or key), [M2], os.path.join(out, "logs"), jobs)
    plan.check_dependencies(maven, reactor, suite_ids, "snapshot")
    framework = plan.instrument_step(maven, "snapshot") if not plan.in_reactor else {}
    if not _product_built(maven.repo, plan.suite_modules):
        maven.run("product", ["clean", "install", "-DskipTests", "-pl", ",".join(plan.suite_modules), "-am"])
        with open(os.path.join(maven.repo, PRODUCT_MARKER), "w") as f:
            json.dump({"suites": plan.suite_modules, "sha": sha}, f)
    if plan.in_reactor:
        plan.instrument_step(maven, "snapshot")
    plan.snapshot(maven, "snapshot", os.path.join(out, "snapshot"), {"sha": sha, "engine": framework.get("engine_version")}, cross_read=cross_read)
    return os.path.join(out, "snapshot")
