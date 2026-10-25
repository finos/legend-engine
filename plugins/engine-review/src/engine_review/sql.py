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
"""SQL pulled out of execution plans, reduced to what a reviewer should compare.

Generated aliases ("root", "persontable_0", "..._ecq") are renumbered before comparing, because a
change that only renames them rewrites every query it touches and buries the queries whose shape
changed. Shape is then summarised as counts - joins by kind, tables by name, subqueries, set and
grouping operators - so that "17 queries each gained a LEFT OUTER JOIN to otherNamesTable" reads
as one finding rather than 17 diffs.
"""
import re
from collections import Counter

_TOKEN = re.compile(
    r"""(?P<string>'(?:[^']|'')*')
       |(?P<quoted>"(?:[^"]|"")*")
       |(?P<word>[A-Za-z_][A-Za-z0-9_$#]*)
       |(?P<number>\d+(?:\.\d+)?)
       |(?P<space>\s+)
       |(?P<punct><>|!=|<=|>=|\|\||::|.)""",
    re.VERBOSE | re.DOTALL)

_JOIN_KINDS = (
    (("left", "outer", "join"), "left_outer"),
    (("right", "outer", "join"), "right_outer"),
    (("full", "outer", "join"), "full_outer"),
    (("left", "join"), "left_outer"),
    (("right", "join"), "right_outer"),
    (("full", "join"), "full_outer"),
    (("inner", "join"), "inner"),
    (("cross", "join"), "cross"),
    (("join",), "inner"),
)

_PRETTY_BREAKS = ("select", "from", "where", "group by", "having", "order by", "union all", "union",
                  "left outer join", "right outer join", "full outer join", "inner join", "cross join",
                  "lateral", "limit", "offset", "with")


def tokenize(sql):
    tokens = []
    for match in _TOKEN.finditer(sql):
        kind = match.lastgroup
        if kind != "space":
            text = match.group()
            tokens.append((kind, text.lower() if kind == "word" else text))
    return tokens


def extract_sql(plan, path="$"):
    """Every SQL statement in a plan, in node order, as (json path, node _type, sql)."""
    found = []
    if isinstance(plan, dict):
        sql = plan.get("sqlQuery")
        if isinstance(sql, str):
            found.append((path, plan.get("_type"), sql))
        for key in sorted(plan):
            if key != "sqlQuery":
                found.extend(extract_sql(plan[key], "%s.%s" % (path, key)))
    elif isinstance(plan, list):
        for index, item in enumerate(plan):
            found.extend(extract_sql(item, "%s[%d]" % (path, index)))
    return found


def clause_positions(tokens):
    """Indexes of the clause keywords (from, where, joins, ...) that belong to a select at their own
    paren depth, as opposed to keywords inside a call: extract(x from y), trim(... from ...), the
    order by of an over(...) window, or the from of 'is [not] distinct from'."""
    positions, select_depths, depth = set(), [], 0
    for index, (kind, text) in enumerate(tokens):
        if text == "(":
            depth += 1
        elif text == ")":
            depth -= 1
            while select_depths and select_depths[-1] > depth:
                select_depths.pop()
        elif kind == "word":
            if text == "select":
                select_depths.append(depth)
            elif select_depths and select_depths[-1] == depth and not (text == "from" and index > 0 and tokens[index - 1] == ("word", "distinct")):
                positions.add(index)
    return positions


def table_aliases(tokens):
    """Quoted identifiers used as a qualifier ("alias".column), in order of first use."""
    aliases = []
    for current, following in zip(tokens, tokens[1:]):
        if current[0] == "quoted" and following == ("punct", ".") and current[1] not in aliases:
            aliases.append(current[1])
    return aliases


def normalize(sql):
    """Whitespace-insensitive, alias-insensitive rendering of a statement."""
    tokens = tokenize(sql)
    renames = {alias: '"t%d"' % (index + 1) for index, alias in enumerate(table_aliases(tokens))}
    return " ".join(renames.get(text, text) if kind == "quoted" else text for kind, text in tokens)


def metrics(sql):
    tokens = tokenize(sql)
    words = [text if kind == "word" else None for kind, text in tokens]
    clauses = clause_positions(tokens)
    counts = Counter()
    tables = Counter()
    depth = max_depth = 0
    index = 0
    while index < len(tokens):
        kind, text = tokens[index]
        if text == "(":
            depth += 1
            max_depth = max(max_depth, depth)
            if index + 1 < len(tokens) and tokens[index + 1] == ("word", "select"):
                counts["subqueries"] += 1
        elif text == ")":
            depth -= 1
        elif kind == "word":
            matched = False
            for sequence, join_kind in _JOIN_KINDS:
                if tuple(words[index:index + len(sequence)]) == sequence:
                    counts["join." + join_kind] += 1
                    index += len(sequence) - 1
                    matched = True
                    break
            if matched or (text == "from" and index in clauses):
                table = _table_after(tokens, index + 1)
                if table:
                    tables[table] += 1
            elif text == "select":
                counts["selects"] += 1
                if index + 1 < len(tokens) and tokens[index + 1] == ("word", "distinct"):
                    counts["distinct"] += 1
            elif text == "union":
                counts["union"] += 1
            elif text == "exists":
                counts["exists"] += 1
            elif text == "distinct" and index + 1 < len(tokens) and tokens[index + 1] == ("word", "from") and index > 0 and tokens[index - 1][1] in ("is", "not"):
                counts["null_safe_compare"] += 1
            elif text in ("group", "order") and index + 1 < len(tokens) and tokens[index + 1] == ("word", "by"):
                counts[text + "_by"] += 1
            elif text in ("having", "lateral", "limit", "offset", "top"):
                counts[text] += 1
            elif text == "over" and index + 1 < len(tokens) and tokens[index + 1] == ("punct", "("):
                counts["window"] += 1
            elif text == "case":
                counts["case"] += 1
            elif text in ("and", "or"):
                counts["boolean_ops"] += 1
            elif text == "in" and index + 2 < len(tokens) and tokens[index + 1] == ("punct", "(") and tokens[index + 2] == ("word", "select"):
                counts["in_subquery"] += 1
        index += 1
    counts["joins"] = sum(v for k, v in counts.items() if k.startswith("join."))
    counts["duplicate_joins"] = duplicate_joins(tokens)
    counts["max_paren_depth"] = max_depth
    result = dict(counts)
    result["tables"] = dict(tables)
    return result


def duplicate_joins(tokens):
    """Joins that repeat an earlier join exactly: same target, same ON condition, differing only in the aliases they
    introduce. The same exists() used twice in one query becoming two identical subquery joins is the shape of the
    #5266 join explosion; a new join for a different predicate is not counted."""
    aliases = set(table_aliases(tokens))
    words = [text if kind == "word" else None for kind, text in tokens]
    signatures = Counter()
    index = 0
    while index < len(tokens):
        length = next((len(seq) for seq, _ in _JOIN_KINDS if tuple(words[index:index + len(seq)]) == seq), 0)
        if not length:
            index += 1
            continue
        start = index + length
        end = _balanced_end(tokens, start) if (start < len(tokens) and tokens[start] == ("punct", "(")) else start + 1
        while end + 2 < len(tokens) and tokens[end] == ("punct", ".") and tokens[end + 1][0] in ("word", "quoted"):
            end += 2
        target = tokens[start:end]
        if end < len(tokens) and tokens[end] == ("word", "as"):
            end += 1
        own = tokens[end][1] if (end < len(tokens) and tokens[end][0] == "quoted") else None
        condition_start = end + 1 if own else end
        condition = []
        if condition_start < len(tokens) and tokens[condition_start] == ("word", "on"):
            condition_end = _balanced_end(tokens, condition_start + 1) if tokens[condition_start + 1:condition_start + 2] == [("punct", "(")] else condition_start + 2
            condition = tokens[condition_start + 1:condition_end]
        signature = (tuple('"?"' if (kind == "quoted" and text in aliases) else text for kind, text in target),
                     tuple('"SELF"' if text == own else text for _, text in condition))
        signatures[signature] += 1
        index = start
    return sum(count - 1 for count in signatures.values() if count > 1)


def _balanced_end(tokens, open_index):
    depth = 0
    for index in range(open_index, len(tokens)):
        if tokens[index] == ("punct", "("):
            depth += 1
        elif tokens[index] == ("punct", ")"):
            depth -= 1
            if depth == 0:
                return index + 1
    return len(tokens)


def _table_after(tokens, index):
    if index >= len(tokens) or tokens[index][0] not in ("word", "quoted"):
        return None
    parts = [tokens[index][1]]
    while index + 2 < len(tokens) and tokens[index + 1] == ("punct", ".") and tokens[index + 2][0] in ("word", "quoted"):
        parts.append(tokens[index + 2][1])
        index += 2
    name = ".".join(part.strip('"') for part in parts)
    return None if name in ("select", "lateral", "unnest", "table") else name


def metric_delta(before, after):
    """Changed metrics as {name: (before, after)}; tables are reported as table.<name>."""
    delta = {}
    keys = (set(before) | set(after)) - {"tables"}
    for key in sorted(keys):
        if before.get(key, 0) != after.get(key, 0):
            delta[key] = (before.get(key, 0), after.get(key, 0))
    before_tables, after_tables = before.get("tables", {}), after.get("tables", {})
    for table in sorted(set(before_tables) | set(after_tables)):
        if before_tables.get(table, 0) != after_tables.get(table, 0):
            delta["table." + table] = (before_tables.get(table, 0), after_tables.get(table, 0))
    return delta


def pretty(sql):
    """One clause per line, indented by subquery depth - stable input for a unified diff."""
    out, line, depth, line_depth = [], [], 0, 0
    tokens = tokenize(sql)
    words = [text if kind == "word" else None for kind, text in tokens]
    clauses = clause_positions(tokens)
    index = 0
    while index < len(tokens):
        kind, text = tokens[index]
        breaker = None
        if kind == "word" and (index in clauses or text == "select"):
            for phrase in _PRETTY_BREAKS:
                parts = tuple(phrase.split())
                if tuple(words[index:index + len(parts)]) == parts:
                    breaker = parts
                    break
        if breaker and line:
            out.append("  " * line_depth + _render(line))
            line = []
        if not line:
            line_depth = depth
        if breaker:
            line.extend(breaker)
            index += len(breaker)
            continue
        if text == "(":
            depth += 1
        elif text == ")":
            depth = max(0, depth - 1)
        line.append(text)
        index += 1
    if line:
        out.append("  " * line_depth + _render(line))
    return out


def _render(texts):
    rendered = ""
    for text in texts:
        if rendered and not (text in (".", ",", ")") or rendered.endswith(("(", "."))):
            rendered += " "
        rendered += text
    return rendered
