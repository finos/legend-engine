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
"""Compare two EMIT snapshot trees (written by EMITSnapshot) into a structured impact report.

Layout of one side: <side>/<suite>/<model>/{model.json, pmcd.json, crossread.json,
pmcd.crossread.json, plans/<kind>/...json} and <side>/_surefire/<module>/TEST-*.xml.
"""
import difflib
import json
import os
import re
import xml.etree.ElementTree as ET
from collections import OrderedDict, defaultdict

from engine_review import sql as sqllib

PLAN_NOISE_KEYS = {"implementation", "globalImplementationSupport", "sqlQuery"}
HIGH_SQL_METRICS = ("duplicate_joins", "null_safe_compare", "joins", "join.", "table.", "distinct", "union", "group_by", "exists", "in_subquery", "having", "lateral", "window", "limit", "offset", "top")
_HASH = re.compile(r"@[0-9a-f]{5,}")


def _load(path):
    with open(path) as f:
        return json.load(f)


def _strip(value, keys):
    if isinstance(value, dict):
        return {k: _strip(v, keys) for k, v in value.items() if k not in keys}
    if isinstance(value, list):
        return [_strip(v, keys) for v in value]
    return value


def _flatten(value, path="", out=None):
    out = OrderedDict() if out is None else out
    if isinstance(value, dict):
        if not value:
            out[path or "$"] = {}
        for key in sorted(value):
            _flatten(value[key], "%s.%s" % (path, key) if path else key, out)
    elif isinstance(value, list):
        if not value:
            out[path or "$"] = []
        for index, item in enumerate(value):
            _flatten(item, "%s[%d]" % (path, index), out)
    else:
        out[path or "$"] = value
    return out


def json_changes(before, after, limit=None):
    """Leaf-level differences as (kind, path, before, after) with kind in added/removed/changed."""
    flat_before, flat_after = _flatten(before), _flatten(after)
    changes = []
    for path in flat_before:
        if path not in flat_after:
            changes.append(("removed", path, flat_before[path], None))
        elif flat_before[path] != flat_after[path]:
            changes.append(("changed", path, flat_before[path], flat_after[path]))
    for path in flat_after:
        if path not in flat_before:
            changes.append(("added", path, None, flat_after[path]))
    return changes if limit is None else changes[:limit]


def _message(entry):
    return _HASH.sub("@<hash>", entry.get("message") or "")


class Side:
    """Every model under a snapshot root, wherever it sits: a model is a directory holding model.json, and its suite
    is the path from the root to its parent (<suite> for one JVM, <module>/<suite> for engine-review runs)."""

    def __init__(self, root):
        self.root = root
        self.models = OrderedDict()
        self.plans = OrderedDict()
        self.subtype_files = OrderedDict()
        for dirpath, dirnames, filenames in os.walk(root):
            dirnames[:] = sorted(d for d in dirnames if not d.startswith("_") and d != "plans")
            relative = os.path.relpath(dirpath, root)
            if "_subtypes.json" in filenames:
                self.subtype_files[relative] = os.path.join(dirpath, "_subtypes.json")
            if "model.json" in filenames:
                key = (os.path.dirname(relative), os.path.basename(relative))
                self.models[key] = dirpath
                plans_dir = os.path.join(dirpath, "plans")
                for plan_dir, _, plan_files in os.walk(plans_dir):
                    for filename in sorted(plan_files):
                        if filename.endswith(".json"):
                            full = os.path.join(plan_dir, filename)
                            self.plans[key + (os.path.relpath(full, plans_dir)[:-len(".json")],)] = full

    def model_file(self, key, name):
        path = os.path.join(self.models[key], name) if key in self.models else None
        return path if path and os.path.exists(path) else None


# ----- SQL / plans -----

def _statements(plan):
    return [s for _, _, s in sqllib.extract_sql(plan)]


def _entry_statements(entry):
    """A plan's SQL, or for a test that issues statements without a plan (persistence), those statements."""
    return _statements(entry["plan"]) if "plan" in entry else list(entry.get("statements", []))


def _sql_metrics(statements):
    total = {"tables": {}}
    for statement in statements:
        for key, value in sqllib.metrics(statement).items():
            if key == "tables":
                for table, count in value.items():
                    total["tables"][table] = total["tables"].get(table, 0) + count
            elif key == "max_paren_depth":
                total[key] = max(total.get(key, 0), value)
            else:
                total[key] = total.get(key, 0) + value
    total["statements"] = len(statements)
    return total


def _sql_diff(before, after):
    lines_before = [line for s in before for line in sqllib.pretty(s) + [";"]]
    lines_after = [line for s in after for line in sqllib.pretty(s) + [";"]]
    return list(difflib.unified_diff(lines_before, lines_after, "base", "head", lineterm="", n=2))


def _severity(delta, plan_changes):
    if any(path.startswith("rootExecutionNode.resultType") or ".resultType" in path for _, path, _, _ in plan_changes):
        return "high"
    if any(key == m or key.startswith(m) for key in delta for m in HIGH_SQL_METRICS):
        return "high"
    return "medium"


def compare_plan_item(base_entry, head_entry):
    base_ok, head_ok = base_entry.get("status") == "ok", head_entry.get("status") == "ok"
    if not base_ok and not head_ok:
        same = _message(base_entry) == _message(head_entry)
        return {"status": "error-unchanged" if same else "error-changed", "severity": "low" if same else "medium",
                "base_error": _message(base_entry), "head_error": _message(head_entry)}
    if not head_ok:
        return {"status": "error-new", "severity": "high", "head_error": _message(head_entry)}
    if not base_ok:
        return {"status": "error-fixed", "severity": "medium", "base_error": _message(base_entry)}
    base_sql, head_sql = _entry_statements(base_entry), _entry_statements(head_entry)
    plan_changes = json_changes(_strip(base_entry.get("plan"), PLAN_NOISE_KEYS), _strip(head_entry.get("plan"), PLAN_NOISE_KEYS))
    if base_sql == head_sql and not plan_changes:
        return {"status": "identical", "severity": "none"}
    if [sqllib.normalize(s) for s in base_sql] == [sqllib.normalize(s) for s in head_sql] and not plan_changes:
        return {"status": "alias-only", "severity": "low", "sql_diff": _sql_diff(base_sql, head_sql)}
    delta = sqllib.metric_delta(_sql_metrics(base_sql), _sql_metrics(head_sql))
    return {
        "status": "changed",
        "severity": _severity(delta, plan_changes),
        "sql_delta": {k: list(v) for k, v in delta.items()},
        "sql_diff": _sql_diff(base_sql, head_sql),
        "plan_changes": [list(c) for c in plan_changes[:25]],
        "plan_change_count": len(plan_changes),
    }


def _signature(result):
    if result["status"] != "changed":
        return result["status"]
    parts = ["%s %+d" % (k, v[1] - v[0]) for k, v in sorted(result.get("sql_delta", {}).items())]
    if result.get("plan_change_count"):
        parts.append("plan:%s" % ",".join(sorted({re.sub(r"\[\d+\]", "[]", p).split(".")[-1] for _, p, _, _ in result["plan_changes"]})))
    return "; ".join(parts) or "same shape, different SQL text"


def compare_plans(base, head):
    items = []
    for key in sorted(set(base.plans) | set(head.plans)):
        suite, model, item = key
        if key not in head.plans:
            result = {"status": "removed", "severity": "medium"}
        elif key not in base.plans:
            result = {"status": "added", "severity": "low"}
        else:
            result = compare_plan_item(_load(base.plans[key]), _load(head.plans[key]))
        result.update({"suite": suite, "model": model, "item": item})
        items.append(result)
    clusters = defaultdict(list)
    for result in items:
        if result["status"] not in ("identical", "error-unchanged"):
            clusters[_signature(result)].append(result)
    ordered = sorted(clusters.items(), key=lambda kv: (_RANK[kv[1][0]["severity"]], "duplicate_joins +" not in kv[0], -len(kv[1])))
    return {"items": items, "clusters": [{"signature": sig, "severity": members[0]["severity"], "count": len(members), "members": members} for sig, members in ordered]}


_RANK = {"high": 0, "medium": 1, "low": 2, "none": 3}


# ----- protocol -----

def _elements(pmcd):
    out = OrderedDict()
    for element in pmcd.get("elements", []):
        path = "%s::%s" % (element.get("package", ""), element.get("name", "")) if element.get("package") else element.get("name", "")
        out["%s (%s)" % (path, element.get("_type"))] = _strip(element, {"sourceInformation"})
    return out


def _change_pattern(element_key, change):
    kind, path, _, _ = change
    element_type = element_key.rsplit("(", 1)[-1].rstrip(")")
    return "%s %s.%s" % (kind, element_type, re.sub(r"\[\d+\]", "[]", path))


def compare_pmcd(base_pmcd, head_pmcd):
    base_elements, head_elements = _elements(base_pmcd), _elements(head_pmcd)
    changes = OrderedDict()
    for key in sorted(set(base_elements) | set(head_elements)):
        if key not in head_elements:
            changes[key] = {"status": "removed"}
        elif key not in base_elements:
            changes[key] = {"status": "added"}
        elif base_elements[key] != head_elements[key]:
            diff = json_changes(base_elements[key], head_elements[key])
            changes[key] = {"status": "changed", "changes": [list(c) for c in diff[:25]], "change_count": len(diff),
                            "patterns": sorted({_change_pattern(key, c) for c in diff})}
    return changes


def _outcome(path, step):
    if not path:
        return None
    entry = _load(path).get(step)
    return None if entry is None else ("ok" if entry.get("status") == "ok" else "%s: %s" % (entry.get("status"), _message(entry)))


def compare_subtypes(base, head):
    """_type registry differences per suite: removed or retargeted names break stored JSON, added names break older readers."""
    changes, collisions = [], []
    for suite in sorted(set(base.subtype_files) | set(head.subtype_files)):
        before = _load_optional(base.subtype_files.get(suite))
        after = _load_optional(head.subtype_files.get(suite))
        for supertype in sorted(set(before) | set(after)):
            names_before, names_after = before.get(supertype, {}), after.get(supertype, {})
            for name in sorted(set(names_before) | set(names_after)):
                classes_before, classes_after = names_before.get(name), names_after.get(name)
                if classes_before != classes_after:
                    kind = "added" if classes_before is None else "removed" if classes_after is None else "retargeted"
                    changes.append({"suite": suite, "supertype": supertype, "type": name, "change": kind, "base": classes_before, "head": classes_after})
                if classes_after and len(classes_after) > 1 and classes_after != classes_before:
                    collisions.append({"suite": suite, "supertype": supertype, "type": name, "classes": classes_after})
    unique = {(c["supertype"], c["type"], c["change"], json.dumps(c["head"])): c for c in changes}
    return {"changes": list(unique.values()), "new_collisions": collisions}


def _load_optional(path):
    return _load(path) if path and os.path.exists(path) else {}


def compare_protocol(base, head, forward=None):
    models, patterns, element_index = [], defaultdict(set), {}
    for key in sorted(set(base.models) | set(head.models)):
        suite, model = key
        row = {"suite": suite, "model": model}
        base_model, head_model = base.model_file(key, "model.json"), head.model_file(key, "model.json")
        for side, path in (("base", base_model), ("head", head_model)):
            if path:
                data = _load(path)
                row[side + "_parse"] = "ok" if data["parse"]["status"] == "ok" else _message(data["parse"])
                for step in ("compile", "roundTrip"):
                    if step in data:
                        row[side + "_" + step] = "ok" if data[step]["status"] == "ok" else _message(data[step])
        base_pmcd, head_pmcd = base.model_file(key, "pmcd.json"), head.model_file(key, "pmcd.json")
        if base_pmcd and head_pmcd:
            changes = compare_pmcd(_load(base_pmcd), _load(head_pmcd))
            row["element_changes"] = len(changes)
            for element, change in changes.items():
                signature = (element, json.dumps(change, sort_keys=True))
                element_index.setdefault(signature, {"element": element, "change": change, "models": []})["models"].append("%s/%s" % key)
                for pattern in change.get("patterns", [change["status"] + " element"]):
                    patterns[pattern].add(element)
        row["backward_deserialize"] = _outcome(head.model_file(key, "crossread.json"), "deserialize")
        row["backward_compile"] = _outcome(head.model_file(key, "crossread.json"), "compile")
        backward_pmcd = head.model_file(key, "pmcd.crossread.json")
        head_roundtrip = head.model_file(key, "pmcd.roundtrip.json") or head_pmcd
        if backward_pmcd and head_roundtrip:
            drift = compare_pmcd(_load(backward_pmcd), _load(head_roundtrip))
            row["backward_drift"] = {element: change.get("patterns", [change["status"]]) for element, change in drift.items()}
        if forward is not None:
            row["forward_deserialize"] = _outcome(forward.model_file(key, "crossread.json"), "deserialize")
            row["forward_compile"] = _outcome(forward.model_file(key, "crossread.json"), "compile")
        models.append(row)
    return {
        "models": models,
        "subtypes": compare_subtypes(base, head),
        "patterns": sorted(({"pattern": p, "elements": len(e), "examples": sorted(e)[:5]} for p, e in patterns.items()), key=lambda r: -r["elements"]),
        "element_changes": list(element_index.values()),
    }


# ----- test outcomes -----

def _surefire_cases(root):
    cases = {}
    base_dir = os.path.join(root, "_surefire")
    for dirpath, _, filenames in os.walk(base_dir):
        for filename in filenames:
            if filename.startswith("TEST-") and filename.endswith(".xml"):
                for case in ET.parse(os.path.join(dirpath, filename)).getroot().iter("testcase"):
                    outcome = "pass"
                    for tag in ("failure", "error", "skipped"):
                        node = case.find(tag)
                        if node is not None:
                            outcome = tag
                            message = (node.get("message") or "").splitlines()
                            outcome += (": " + _HASH.sub("@<hash>", message[0])[:300]) if message else ""
                            break
                    cases["%s :: %s" % (case.get("classname"), case.get("name"))] = outcome
    return cases


def compare_tests(base_root, head_root):
    """Outcome flips of tests present on both sides. Surefire names dynamic tests by position, so a test that exists on
    one side only (for example the cross-read task, which only cross-reading passes have) is counted, not matched."""
    base_cases, head_cases = _surefire_cases(base_root), _surefire_cases(head_root)
    changed = []
    for name in sorted(set(base_cases) & set(head_cases)):
        before, after = base_cases[name], head_cases[name]
        if before.split(":")[0] != after.split(":")[0]:
            changed.append({"test": name, "base": before, "head": after})
    return {"base_total": len(base_cases), "head_total": len(head_cases), "changed": changed,
            "only_base": len(set(base_cases) - set(head_cases)), "only_head": len(set(head_cases) - set(base_cases))}


def compare(base_root, head_root, forward_root=None):
    base, head = Side(base_root), Side(head_root)
    forward = Side(forward_root) if forward_root else None
    plans = compare_plans(base, head)
    counts = defaultdict(int)
    for item in plans["items"]:
        counts[item["status"]] += 1
    return {
        "base": os.path.abspath(base_root),
        "head": os.path.abspath(head_root),
        "models": {"base": len(base.models), "head": len(head.models)},
        "plan_counts": dict(counts),
        "plans": plans,
        "protocol": compare_protocol(base, head, forward),
        "tests": compare_tests(base_root, head_root),
    }
