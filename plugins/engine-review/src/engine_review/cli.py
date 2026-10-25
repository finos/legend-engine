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
"""engine-review: scope a change, snapshot the EMIT corpus on two revisions, and report the difference."""
import argparse
import json
import os
import subprocess
import sys

from engine_review import compare as comparelib
from engine_review import report as reportlib
from engine_review import run as runlib
from engine_review import scope as scopelib


def _repo_root(path):
    return subprocess.run(["git", "-C", path, "rev-parse", "--show-toplevel"], check=True, capture_output=True, text=True).stdout.strip()


def _default_base(repo):
    for candidate in ("origin/master", "master"):
        try:
            return scopelib.git(repo, "merge-base", candidate, "HEAD").strip()
        except subprocess.CalledProcessError:
            continue
    raise SystemExit("cannot find master or origin/master; pass --base")


def _write_report(out, result, manifest):
    with open(os.path.join(out, "report.json"), "w") as f:
        json.dump(result, f, indent=2)
    with open(os.path.join(out, "report.md"), "w") as f:
        f.write(reportlib.render(result, manifest))
    print(os.path.join(out, "report.md"))


def cmd_scope(args):
    repo = _repo_root(args.repo)
    base = scopelib.resolve(repo, args.base or _default_base(repo))
    head = scopelib.resolve(repo, args.head)
    result = scopelib.classify(repo, base, head)
    if args.json:
        print(json.dumps(result, indent=2))
        return
    print("base %s  head %s  (%d files, %d modules)" % (base[:11], head if head == scopelib.WORKTREE else head[:11], len(result["files"]), len(result["modules"])))
    for lens, hits in result["lenses"].items():
        print("\n[%s] %d file(s)" % (lens, len(hits)))
        for hit in hits[:25]:
            print("  %s  - %s" % (hit["path"], hit["why"]))
        if len(hits) > 25:
            print("  ... %d more" % (len(hits) - 25))
    for spot in result["blind_spots"]:
        print("\n[blind spot] %s\n  %s" % (spot["note"], "\n  ".join(spot["paths"][:10])))
    print("\nEMIT suites: %s" % (", ".join(result["suites"]) or "none"))


def cmd_run(args):
    repo = _repo_root(args.repo)
    base = args.base or _default_base(repo)
    out, manifest = runlib.run(repo, base, args.head, suites=args.suites, rebuild=args.rebuild, jobs=args.jobs, out=args.out, forward=not args.no_forward)
    forward = os.path.join(out, "base-xread")
    result = comparelib.compare(os.path.join(out, "base"), os.path.join(out, "head"), forward if os.path.isdir(forward) else None)
    _write_report(out, result, manifest)


def cmd_snapshot(args):
    repo = _repo_root(args.repo)
    config = scopelib.load_config()
    suites = args.suites or [n for n, s in config["suites"].items() if s["lenses"]]
    modules = [config["suites"][n]["module"] for n in suites]
    sha = scopelib.resolve(repo, args.rev)
    tree = repo if sha == scopelib.WORKTREE else runlib.prepare_tree(repo, sha)
    runlib.overlay(repo, tree, modules, config["instrument_files"])
    key = "worktree" if sha == scopelib.WORKTREE else sha[:runlib.SHORT]
    maven = runlib.Maven(tree, os.path.join(runlib.CACHE, "m2", args.repo_key or key), [runlib.M2], os.path.join(args.out, "logs"), args.jobs)
    if not runlib._product_built(maven.repo, modules):
        maven.run("product", ["clean", "install", "-DskipTests", "-pl", ",".join(modules), "-am"])
    maven.run("instrument", ["clean", "install", "-DskipTests", "-pl", ",".join(config["framework"])], parallel=False)
    runlib.snapshot(maven, "snapshot", [(config["suites"][n]["module"], config["suites"][n]["tests"]) for n in suites], os.path.join(args.out, "snapshot"), cross_read=args.cross_read)
    print(os.path.join(args.out, "snapshot"))


def cmd_compare(args):
    result = comparelib.compare(args.base_dir, args.head_dir, args.forward_dir)
    os.makedirs(args.out, exist_ok=True)
    manifest = None
    run_json = os.path.join(os.path.dirname(os.path.abspath(args.base_dir)), "run.json")
    if os.path.exists(run_json):
        with open(run_json) as f:
            manifest = json.load(f)
    _write_report(args.out, result, manifest)


def main(argv=None):
    parser = argparse.ArgumentParser(prog="engine-review", description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    scope = sub.add_parser("scope", help="classify the changes between two revisions into review lenses")
    scope.add_argument("--repo", default=".")
    scope.add_argument("--base", help="base revision (default: merge-base with origin/master)")
    scope.add_argument("--head", default="HEAD", help="head revision, or WORKTREE for uncommitted changes")
    scope.add_argument("--json", action="store_true")
    scope.set_defaults(func=cmd_scope)

    run = sub.add_parser("run", help="build base and head, snapshot the EMIT corpus on both, write report.md")
    run.add_argument("--repo", default=".")
    run.add_argument("--base")
    run.add_argument("--head", default="HEAD")
    run.add_argument("--suites", nargs="*", help="EMIT suites from config/lenses.json (default: every suite with a lens)")
    run.add_argument("--rebuild", choices=("changed", "dependents"), default="changed",
                     help="head rebuilds only changed modules, or also everything in the corpus closure that depends on them")
    run.add_argument("--jobs", type=int, default=4)
    run.add_argument("--out")
    run.add_argument("--no-forward", action="store_true", help="skip the base-engine-reads-head-JSON pass")
    run.set_defaults(func=cmd_run)

    snap = sub.add_parser("snapshot", help="snapshot the EMIT corpus on one revision (e.g. to check a new corpus model)")
    snap.add_argument("--repo", default=".")
    snap.add_argument("--rev", default=scopelib.WORKTREE)
    snap.add_argument("--repo-key", help="local repository to build into, under the cache (default: the revision)")
    snap.add_argument("--suites", nargs="*")
    snap.add_argument("--jobs", type=int, default=4)
    snap.add_argument("--cross-read")
    snap.add_argument("--out", required=True)
    snap.set_defaults(func=cmd_snapshot)

    cmp_ = sub.add_parser("compare", help="compare two existing snapshot directories")
    cmp_.add_argument("base_dir")
    cmp_.add_argument("head_dir")
    cmp_.add_argument("--forward-dir")
    cmp_.add_argument("--out", required=True)
    cmp_.set_defaults(func=cmd_compare)

    args = parser.parse_args(argv)
    try:
        args.func(args)
    except RuntimeError as e:
        print("[engine-review] " + str(e), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
