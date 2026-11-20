#!/usr/bin/env python3
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
"""Groups a suite results file into sweeps (same shape, one size varied) and reports how each phase
and plan-generation span grows with size.

Usage: sweep_slopes.py <results.json>   (written by PipelineBench --suite <name> --out <results.json>)

The slope is d log(time) / d log(size) between the two largest sizes of a sweep: about 1 is linear,
about 2 quadratic. It is taken at the top end because every plan carries several milliseconds of
fixed cost, which flattens a fit over the whole sweep.
"""
import json
import math
import re
import sys
from collections import defaultdict

COLS = ["planPure", "plan: route", "route: enrich", "route: strategy enrichment", "route: cluster",
        "plan: cluster nodes", "relational: toSQLQuery", "relational: postProcess", "relational: toSqlString",
        "relational: sqlQueryToString", "javaBind"]
SHORT = ["plan", "route", "enrich", "strat", "clust", "nodes", "toSQL", "post", "toSqlStr", "legacy", "bind"]


def sweep_key(w):
    cfg = w["config"]
    q = cfg["query"]
    m = re.match(r"([a-z]+)(\d+)$", q)
    flags = "".join([
        "+mult1" if cfg.get("nextMultiplicity") == "1" else "",
        "+inc" if cfg.get("includes") else "",
        "+union" if cfg.get("unionSets") else "",
    ])
    if cfg.get("includes"):
        return q + "+includes", cfg["includes"]
    if cfg.get("unionSets"):
        return q + "+union", cfg["unionSets"]
    if q == "join4" and cfg["scale"] != 30:
        return "join4+scale", cfg["scale"]
    return (m.group(1) + flags, int(m.group(2))) if m else (q, 0)


def value(w, col):
    if col in w["warmMedianMs"]:
        return w["warmMedianMs"][col]
    return w.get("warmMedianSpansMs", {}).get(col)


def slope(points):
    pts = [(x, y) for x, y in points if x > 0 and y and y > 0.05]
    if len(pts) < 2:
        return None
    (x1, y1), (x2, y2) = pts[-2], pts[-1]
    return math.log(y2 / y1) / math.log(x2 / x1) if x2 != x1 else None


def main(path):
    results = json.load(open(path))
    sweeps = defaultdict(list)
    for w in results["workloads"]:
        key, size = sweep_key(w)
        sweeps[key].append((size, w))
    if "join4+scale" in sweeps:
        sweeps["join4+scale"] += [(30, w) for s, w in sweeps.get("join", []) if w["config"]["query"] == "join4"][:1]
    print(f"{'sweep':16}{'size':>6} " + "".join(f"{c:>9}" for c in SHORT))
    for key, items in sweeps.items():
        items.sort(key=lambda p: p[0])
        for size, w in items:
            row = [value(w, c) for c in COLS]
            print(f"{key:16}{size:>6} " + "".join(f"{('-' if v is None else round(v, 1)):>9}" for v in row))
        slopes = [slope([(s, value(w, c)) for s, w in items]) for c in COLS]
        print(f"{'  top-end slope':22} " + "".join(f"{('' if v is None else f'{v:.2f}'):>9}" for v in slopes))
        print()


if __name__ == "__main__":
    main(sys.argv[1])
