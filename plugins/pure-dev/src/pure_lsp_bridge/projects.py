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
"""Pure-project registry: the layered set of pure-bearing repos and helpers to resolve, per target
project, the repo-roots and the classpath-anchor module while respecting the dependency ordering.

The base registry lives in config/projects.json inside the plugin; a consumer can layer its own
additional project entries on top via PURE_DEV_EXTRA_PROJECTS_JSON (see load_layers). Layers are
ordered bottom (base) -> top; a project depends on every layer below it.
"""
import json
import os


def _plugin_root():
    # this file: <plugin>/src/pure_lsp_bridge/projects.py -> plugin root is 3 dirs up
    return os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))


def registry_path():
    return os.environ.get("PURE_DEV_PROJECTS_JSON",
                          os.path.join(_plugin_root(), "config", "projects.json"))


def _read_registry_file(path):
    """Parse one registry file into a list of project dicts (unresolved 'path').

    Accepts either "projects" (DAG form) or legacy "layers"; entries missing a 'name', or not a
    dict, are silently skipped - this tolerance predates the extra-file mechanism below and applies
    equally to it.
    """
    with open(path) as f:
        data = json.load(f)
    return [l for l in data.get("projects", data.get("layers", []))
            if isinstance(l, dict) and l.get("name")]


def _extra_registry_paths():
    """Supplementary registry files layered on top of the base one, via PURE_DEV_EXTRA_PROJECTS_JSON
    (os.pathsep-separated list of file paths, each shaped like config/projects.json). Lets a
    downstream consumer add its own project entries - including ones 'dependsOn' a base entry by
    name - without forking the base registry file."""
    raw = os.environ.get("PURE_DEV_EXTRA_PROJECTS_JSON", "")
    return [p for p in raw.split(os.pathsep) if p]


def load_layers():
    """Return all project dicts, each with resolved absolute 'path'. (Order is DAG, not linear;
    use resolve_plan/deps_in_order for dependency-aware ordering.)

    Starts from the base registry (registry_path()), then appends any extra registry files named by
    PURE_DEV_EXTRA_PROJECTS_JSON, in order. A project name defined more than once - within the
    extras, or colliding with the base registry - is an error: the two sides must agree on exactly
    one definition per name, same as a single file would.
    """
    layers = _read_registry_file(registry_path())
    for extra_path in _extra_registry_paths():
        for l in _read_registry_file(extra_path):
            if any(existing["name"] == l["name"] for existing in layers):
                raise ValueError(
                    "project '%s' from %s is already defined (base registry or an earlier extra "
                    "file) - project names must be unique across the base registry and every "
                    "PURE_DEV_EXTRA_PROJECTS_JSON file" % (l["name"], extra_path))
            layers.append(l)
    for l in layers:
        l["path"] = resolve_root(l)
    return layers


def _enclosing_checkout(layer):
    marker = layer.get("marker") or layer.get("module")
    if not marker:
        return None
    path = os.getcwd()
    while True:
        if os.path.isdir(os.path.join(path, marker)):
            return path
        parent = os.path.dirname(path)
        if parent == path:
            return None
        path = parent


def _home_candidates(layer):
    roots = layer.get("root", "")
    if isinstance(roots, str):
        roots = [roots]
    return [r if os.path.isabs(r) else os.path.join(os.path.expanduser("~"), r) for r in roots]


def resolve_root(layer):
    """Absolute checkout path for a layer: $<rootEnv> if set, else the checkout enclosing the
    current directory, else the first existing entry of 'root' (a path or a list of candidate
    paths, relative to $HOME unless absolute), else the first entry."""
    env = layer.get("rootEnv")
    if env and os.environ.get(env):
        return os.path.abspath(os.environ[env])
    enclosing = _enclosing_checkout(layer)
    if enclosing:
        return enclosing
    candidates = _home_candidates(layer) or [""]
    return next((c for c in candidates if os.path.isdir(c)), candidates[0])


def find_layer(layers, name):
    for l in layers:
        if l["name"] == name:
            return l
    return None


def layer_for_path(layers, path):
    """Which layer a given filesystem path belongs to (longest matching root wins)."""
    path = os.path.abspath(path)
    best = None
    for l in layers:
        lp = l["path"]
        if path == lp or path.startswith(lp + os.sep):
            if best is None or len(l["path"]) > len(best["path"]):
                best = l
    return best


def deps_in_order(layers, target_name):
    """Target + its transitive dependencies, in dependency (topological) order: a project appears
    AFTER everything it depends on (bottom->top). Walks the explicit dependsOn DAG, not list order,
    so sibling projects that both depend on a common base are NOT treated as dependencies of each
    other."""
    by_name = {l["name"]: l for l in layers}
    out = []
    seen = set()

    def visit(name):
        if name in seen:
            return
        node = by_name.get(name)
        if node is None:
            raise KeyError("unknown project '%s' referenced in dependsOn/target" % name)
        seen.add(name)
        for dep in node.get("dependsOn", []):
            visit(dep)
        out.append(node)

    visit(target_name)
    return out


def resolve_plan(target_name, source_root_names=None):
    """Compute a working plan for a target project.

    Returns dict:
      target:        the target layer
      needed:        target + all layers it depends on (bottom->top)
      source_roots:  layers to pass as --repo-root
      classpath_layer: the highest 'needed' layer with a Maven module - always the full classpath
                       anchor, regardless of what's source-rooted, since --repo-root only changes
                       which .pure is READ, not which jars are on the JVM classpath (Java-side
                       platform/connector code always needs to be loadable).

    source_root_names: explicit, EXACT set of layer names to source-back (the target is always
      force-included). None (the default) means the full transitive dependency chain - i.e. every
      layer 'target' depends on, plus itself - is source-rooted. Pass a smaller explicit set (e.g.
      {'legend-pure'} when target is an extra project several layers up) to source-back only that
      subset and let everything else resolve from the classpath jars instead.
    """
    layers = load_layers()
    target = find_layer(layers, target_name)
    if target is None:
        raise KeyError("unknown project '%s'; known: %s" % (target_name, ", ".join(l["name"] for l in layers)))
    needed = deps_in_order(layers, target_name)
    if source_root_names is None:
        # Default: source-root the full transitive chain, not just the target.
        source_root_names = {l["name"] for l in needed}
    else:
        source_root_names = set(source_root_names) | {target_name}
    source_roots = [l for l in needed if l["name"] in source_root_names]
    classpath_layer = None
    for l in needed:  # bottom->top; keep last (highest) with a module
        if l.get("module"):
            classpath_layer = l
    return {
        "target": target,
        "needed": needed,
        "source_roots": source_roots,
        "classpath_layer": classpath_layer,
    }
