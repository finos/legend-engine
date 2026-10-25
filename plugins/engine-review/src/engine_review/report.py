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
"""Render a compare() result as Markdown for a reviewer."""

STATUS_ORDER = ("identical", "alias-only", "changed", "error-new", "error-fixed", "error-changed", "error-unchanged", "added", "removed")
MAX_MEMBERS = 12
MAX_DIFF_LINES = 80


def _item(member):
    return "%s/%s: %s" % (member["suite"], member["model"], member["item"])


def _fence(lines, language=""):
    clipped = lines[:MAX_DIFF_LINES]
    if len(lines) > MAX_DIFF_LINES:
        clipped.append("... %d more lines" % (len(lines) - MAX_DIFF_LINES))
    return ["```" + language] + clipped + ["```"]


def _plans(result, out):
    counts = result["plan_counts"]
    total = sum(counts.values())
    out.append("## SQL and execution plans")
    out.append("")
    out.append("%d plans compared across %d models (base) / %d models (head)." % (total, result["models"]["base"], result["models"]["head"]))
    out.append("")
    out.append("| outcome | plans |")
    out.append("|---|---|")
    for status in STATUS_ORDER:
        if counts.get(status):
            out.append("| %s | %d |" % (status, counts[status]))
    out.append("")
    clusters = [c for c in result["plans"]["clusters"] if c["signature"] not in ("added", "removed")]
    if not clusters:
        out.append("No plan in the corpus changed.")
        out.append("")
    for index, cluster in enumerate(clusters, 1):
        out.append("### %d. [%s] %s (%d plan%s)" % (index, cluster["severity"], cluster["signature"], cluster["count"], "" if cluster["count"] == 1 else "s"))
        out.append("")
        for member in cluster["members"][:MAX_MEMBERS]:
            out.append("- " + _item(member))
        if cluster["count"] > MAX_MEMBERS:
            out.append("- ... and %d more" % (cluster["count"] - MAX_MEMBERS))
        out.append("")
        first = cluster["members"][0]
        if first.get("head_error") or first.get("base_error"):
            out.append("Representative error (%s):" % _item(first))
            out.extend(_fence(["base: " + (first.get("base_error") or "ok"), "head: " + (first.get("head_error") or "ok")]))
        if first.get("sql_diff"):
            out.append("Representative SQL diff (%s, aliases as generated):" % _item(first))
            out.extend(_fence(first["sql_diff"], "diff"))
        if first.get("plan_changes"):
            out.append("Non-SQL plan changes (%d, first shown):" % first["plan_change_count"])
            out.extend(_fence(["%s %s: %s -> %s" % (k, p, b, a) for k, p, b, a in first["plan_changes"][:15]]))
        out.append("")
    for status, title in (("added", "Plans only in head"), ("removed", "Plans only in base")):
        members = [i for i in result["plans"]["items"] if i["status"] == status]
        if members:
            out.append("### %s (%d)" % (title, len(members)))
            out.extend("- " + _item(m) for m in members[:MAX_MEMBERS])
            out.append("")


def _tests(result, out):
    tests = result["tests"]
    out.append("## EMIT test outcomes")
    out.append("")
    out.append("%d test tasks in base, %d in head (%d only in base, %d only in head; surefire names dynamic tests by position, so those are not matched)."
               % (tests["base_total"], tests["head_total"], tests.get("only_base", 0), tests.get("only_head", 0)))
    out.append("")
    if not tests["changed"]:
        out.append("No test task changed outcome.")
    else:
        out.append("| test | base | head |")
        out.append("|---|---|---|")
        for row in tests["changed"][:60]:
            out.append("| %s | %s | %s |" % (row["test"].replace("|", "/"), row["base"].replace("|", "/"), row["head"].replace("|", "/")))
    out.append("")


def _protocol(result, out):
    protocol = result["protocol"]
    out.append("## Protocol JSON")
    out.append("")
    broken = []
    for row in protocol["models"]:
        label = "%s/%s" % (row["suite"], row["model"])
        for key in ("parse", "compile", "roundTrip"):
            if row.get("base_" + key) != row.get("head_" + key):
                broken.append("- %s %s: base `%s` -> head `%s`" % (label, key, row.get("base_" + key), row.get("head_" + key)))
        for direction in ("backward", "forward"):
            for step in ("deserialize", "compile"):
                value = row.get("%s_%s" % (direction, step))
                if value not in (None, "ok"):
                    broken.append("- %s %s %s: `%s`" % (label, direction, step, value))
    out.append("Backward = head engine reading base JSON (what SDLC already stores). Forward = base engine reading head JSON (an older engine fed by a newer Studio or SDLC).")
    out.append("")
    if broken:
        out.append("### Compatibility failures")
        out.extend(broken[:80])
        out.append("")
    else:
        out.append("No parse, compile or cross-read failure in either direction.")
        out.append("")
    subtypes = protocol.get("subtypes", {})
    if subtypes.get("changes") or subtypes.get("new_collisions"):
        out.append("### `_type` registry changes")
        out.append("")
        out.append("removed/retargeted: JSON already stored with that `_type` no longer deserializes as before. added: engines older than head reject JSON that uses it.")
        out.append("")
        out.append("| supertype | _type | change | base | head |")
        out.append("|---|---|---|---|---|")
        for row in subtypes.get("changes", [])[:60]:
            out.append("| %s | `%s` | %s | %s | %s |" % (row["supertype"].rsplit(".", 1)[-1], row["type"], row["change"],
                                                  ", ".join(c.rsplit(".", 1)[-1] for c in row["base"] or []), ", ".join(c.rsplit(".", 1)[-1] for c in row["head"] or [])))
        for row in subtypes.get("new_collisions", []):
            out.append("")
            out.append("- collision: `%s` under %s now resolves to %s" % (row["type"], row["supertype"], ", ".join(row["classes"])))
        out.append("")
    if protocol["patterns"]:
        out.append("### JSON shape changes (same grammar, different JSON)")
        out.append("")
        out.append("| change | elements | examples |")
        out.append("|---|---|---|")
        for row in protocol["patterns"][:40]:
            out.append("| `%s` | %d | %s |" % (row["pattern"], row["elements"], ", ".join(row["examples"][:3])))
        out.append("")
    else:
        out.append("No element serializes differently.")
        out.append("")
    drift = [(row, element, patterns) for row in protocol["models"] for element, patterns in (row.get("backward_drift") or {}).items()]
    if drift:
        out.append("### Backward drift (base JSON read by head differs from head JSON read by head)")
        for row, element, patterns in drift[:40]:
            out.append("- %s/%s %s: %s" % (row["suite"], row["model"], element, "; ".join(patterns[:4])))
        out.append("")


def render(result, manifest=None):
    out = ["# Engine impact report", ""]
    out.append("- base: `%s`" % (manifest or {}).get("base", result["base"]))
    out.append("- head: `%s`" % (manifest or {}).get("head", result["head"]))
    if manifest:
        out.append("- suites: %s" % ", ".join(manifest.get("suites", [])))
        out.append("- head rebuilt: %s" % (", ".join(manifest.get("head_rebuilt", [])) or "nothing (no product module in the corpus closure changed)"))
        for caveat in manifest.get("caveats", []):
            out.append("- caveat: " + caveat)
    out.append("")
    _plans(result, out)
    _tests(result, out)
    _protocol(result, out)
    return "\n".join(out) + "\n"
