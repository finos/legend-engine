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
"""CLI backing pure-lsp-classpath and pure-lsp-roots. Resolves, for a target Pure project, the
dependency-ordered repo-roots and the Maven classpath-anchor module - so users can spin up the LSP
for ANY pure-bearing repo registered in config/projects.json (legend-pure, legend-engine, or a
project layered on top via PURE_DEV_EXTRA_PROJECTS_JSON), not just legend-engine. See
config/projects.json for the layering.
"""
import argparse
import os
import subprocess
import sys

from . import projects as reg


def _detect_target(explicit):
    """Resolve the target project name: explicit arg, else $PURE_DEV_PROJECT, else infer from CWD."""
    if explicit:
        return explicit
    if os.environ.get("PURE_DEV_PROJECT"):
        return os.environ["PURE_DEV_PROJECT"]
    layers = reg.load_layers()
    hit = reg.layer_for_path(layers, os.getcwd())
    if hit:
        return hit["name"]
    return None


def cmd_list(args):
    layers = reg.load_layers()
    print("layer            checkout                                   has-classpath-module  exists")
    for l in layers:
        exists = "yes" if os.path.isdir(l["path"]) else "no"
        mod = "yes" if l.get("module") else "no"
        print("%-16s %-42s %-20s %s" % (l["name"], l["path"], mod, exists))
    return 0


def cmd_roots(args):
    """Print --repo-root args for the target project, in dep order.

    Default (no --source): the full transitive dependency chain is source-rooted. Pass --source to
    instead specify the EXACT set of layers to source-back (target is always included); everything
    else in the chain then resolves from the classpath jars. --all-source is a no-op alias for the
    default, kept for explicitness in scripts/docs.
    """
    target = _detect_target(args.project)
    if not target:
        print("ERROR: could not determine target project. Pass one (see `pure-lsp-roots --list`), "
              "set $PURE_DEV_PROJECT, or run from inside a project checkout.", file=sys.stderr)
        return 2
    exact = None if (args.all_source or not args.source) else set(args.source)
    plan = reg.resolve_plan(target, exact)
    parts = []
    for l in plan["source_roots"]:
        if not os.path.isdir(l["path"]):
            print("WARNING: source-root layer '%s' checkout not found at %s (skipping)"
                  % (l["name"], l["path"]), file=sys.stderr)
            continue
        parts.append("--repo-root")
        parts.append(l["path"])
    if args.json:
        import json
        print(json.dumps([p for p in parts if p != "--repo-root"]))
    else:
        print(" ".join(parts))
    return 0


DEFAULT_BRIDGE_PORT = 8991
DEFAULT_SOCKET_PORT = 9100


def cmd_launch_config(args):
    """Emit pure-lsp-launch's per-project defaults as one tab-separated line.

    Tab-separated rather than JSON because the only consumer is bash, which would otherwise need jq
    (not a dependency of this plugin) or an eval of generated shell.

    Fields, in order: project, root, bridgePort, socketPort, welcomeFile, sqlE2eCorpusDir.
    Absent optional fields are the empty string.
    """
    target = _detect_target(args.project)
    if not target:
        print("ERROR: could not determine target project. Pass one (see `pure-lsp-roots --list`), "
              "set $PURE_DEV_PROJECT, or run from inside a project checkout.", file=sys.stderr)
        return 2
    layers = reg.load_layers()
    layer = reg.find_layer(layers, target)
    if layer is None:
        print("ERROR: unknown project '%s'; known: %s"
              % (target, ", ".join(l["name"] for l in layers)), file=sys.stderr)
        return 2

    launch = layer.get("launch") or {}
    root = layer["path"]
    corpus = launch.get("sqlE2eCorpus")
    corpus_dir = os.path.join(root, corpus) if corpus else ""
    print("\t".join([
        target,
        root,
        str(launch.get("bridgePort") or DEFAULT_BRIDGE_PORT),
        str(launch.get("socketPort") or DEFAULT_SOCKET_PORT),
        os.path.join(root, "welcome.pure"),
        corpus_dir,
    ]))
    return 0


def _cache_paths(target):
    cache_dir = os.path.join(os.environ.get("XDG_CACHE_HOME", os.path.expanduser("~/.cache")), "pure-dev")
    os.makedirs(cache_dir, exist_ok=True)
    return (os.path.join(cache_dir, "classpath-%s.txt" % target),
            os.path.join(cache_dir, "classpath-%s.stamp" % target))


def _stamp(layer):
    root = layer["path"]
    try:
        head = subprocess.check_output(["git", "-C", root, "rev-parse", "HEAD"],
                                       stderr=subprocess.DEVNULL).decode().strip()
    except Exception:
        head = "nohead"
    pom = os.path.join(root, layer.get("module") or "", "pom.xml")
    try:
        mtime = str(int(os.path.getmtime(pom)))
    except Exception:
        mtime = "0"
    return "%s:%s" % (head, mtime)


def cmd_classpath(args):
    target = _detect_target(args.project)
    if not target:
        print("ERROR: could not determine target project. Pass one (see `pure-lsp-classpath --list`), "
              "set $PURE_DEV_PROJECT, or run from inside a project checkout.", file=sys.stderr)
        return 2
    plan = reg.resolve_plan(target)
    cp_layer = plan["classpath_layer"]
    cache_file, stamp_file = _cache_paths(target)

    if args.path:
        print(cache_file)
        return 0

    if cp_layer is None:
        print("ERROR: project '%s' has no classpath-anchor module and no lower layer supplies one. "
              "Pass --module <maven-module> to build a classpath explicitly." % target, file=sys.stderr)
        return 1

    module = args.module or cp_layer.get("module")
    build_root = cp_layer["path"]
    if not os.path.isdir(build_root):
        print("ERROR: classpath source project '%s' checkout not found at %s"
              % (cp_layer["name"], build_root), file=sys.stderr)
        return 1

    # staleness: recompute if forced, or cache missing, or stamp changed.
    need = args.force or not (os.path.isfile(cache_file) and os.path.getsize(cache_file) > 0
                              and os.path.isfile(stamp_file))
    cur = _stamp({"path": build_root, "module": module})
    if not need:
        with open(stamp_file) as f:
            need = f.read().strip() != cur
    if need:
        print("[pure-lsp-classpath] computing classpath for '%s' (module %s under %s)..."
              % (target, module, cp_layer["name"]), file=sys.stderr)
        tmp = cache_file + ".tmp"
        base = ["mvn", "-q", "-pl", module]
        # -am rebuilds the whole upstream reactor just to resolve a classpath; projects whose
        # siblings are already installed to .m2 opt out of it via classpathAlsoMake: false (large
        # reactors can cost minutes).
        if cp_layer.get("classpathAlsoMake", True):
            base.append("-am")
        base += ["dependency:build-classpath", "-Dmdep.outputFile=" + tmp]
        if cp_layer.get("classpathScope"):
            base.append("-Dmdep.includeScope=" + cp_layer["classpathScope"])

        env = dict(os.environ)
        # a project whose Maven enforcer pins a JDK the session isn't running under
        java_home_env = cp_layer.get("javaHomeEnv")
        if java_home_env and os.environ.get(java_home_env):
            env["JAVA_HOME"] = os.environ[java_home_env]

        rc = subprocess.call(["mvn"] + ["-o"] + base[1:], cwd=build_root, env=env,
                             stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if rc != 0:
            print("[pure-lsp-classpath] offline resolve failed; retrying online...", file=sys.stderr)
            rc = subprocess.call(base, cwd=build_root, env=env)
        if rc != 0 or not (os.path.isfile(tmp) and os.path.getsize(tmp) > 0):
            print("[pure-lsp-classpath] ERROR: Maven failed to produce a classpath.", file=sys.stderr)
            return 1
        # validate at least one real jar
        with open(tmp) as f:
            entries = f.read().replace("\n", os.pathsep).split(os.pathsep)
        jars = [e for e in entries if e.strip().endswith(".jar")]
        if not jars or not os.path.exists(jars[0].strip()):
            print("[pure-lsp-classpath] ERROR: computed classpath has no resolvable jars.", file=sys.stderr)
            return 1
        os.replace(tmp, cache_file)
        with open(stamp_file, "w") as f:
            f.write(cur)
        print("[pure-lsp-classpath] cached %d jars -> %s" % (len(jars), cache_file), file=sys.stderr)

    print(cache_file)
    return 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="command", required=True)

    p_list = sub.add_parser("list", help="list known Pure projects (layers) and their checkouts")

    p_roots = sub.add_parser("roots", help="print --repo-root args for a target project (dep-ordered)")
    p_roots.add_argument("project", nargs="?", help="project name (default: infer from CWD/$PURE_DEV_PROJECT)")
    p_roots.add_argument("--source", action="append",
                         help="EXACT set of layers to source-root (repeatable; target is always "
                              "included). Overrides the default of source-rooting the full chain - "
                              "everything not listed resolves from the classpath jars instead.")
    p_roots.add_argument("--all-source", action="store_true",
                         help="source-root every dependency layer (this is already the default; "
                              "kept as an explicit no-op alias)")
    p_roots.add_argument("--json", action="store_true", help="print root paths as a JSON array")

    p_cp = sub.add_parser("classpath", help="compute+cache the classpath for a target project")
    p_cp.add_argument("project", nargs="?", help="project name (default: infer from CWD/$PURE_DEV_PROJECT)")
    p_cp.add_argument("--force", action="store_true", help="force recompute")
    p_cp.add_argument("--path", action="store_true", help="print cache path only (never compute)")
    p_cp.add_argument("--module", help="override the Maven module to build the classpath from")

    p_launch = sub.add_parser("launch-config",
                              help="print pure-lsp-launch's per-project defaults (tab-separated: "
                                   "project, root, bridgePort, socketPort, welcomeFile, "
                                   "sqlE2eCorpusDir)")
    p_launch.add_argument("project", nargs="?",
                          help="project name (default: infer from CWD/$PURE_DEV_PROJECT)")

    args = ap.parse_args(argv)
    return {"list": cmd_list, "roots": cmd_roots, "classpath": cmd_classpath,
            "launch-config": cmd_launch_config}[args.command](args)


if __name__ == "__main__":
    sys.exit(main())
