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
import json

from engine_review import compare, report

BASE_SQL = 'select "root".NAME as "name" from firmTable as "root" left outer join (select distinct "p_1".FIRMID from personTable as "p_1" where "p_1".FIRSTNAME = \'Peter\') as "p_0" on ("p_0".FIRMID = "root".ID) where "p_0".FIRMID is not null'
RENAMED_SQL = BASE_SQL.replace('"p_0"', '"p_d_ecq"')
EXTRA_JOIN_SQL = BASE_SQL.replace(
    ' where "p_0".FIRMID is not null',
    ' left outer join (select distinct "p_3".FIRMID from personTable as "p_3" where "p_3".LASTNAME = \'Smith\') as "p_2" on ("p_2".FIRMID = "root".ID)'
    ' where "p_0".FIRMID is not null and "p_2".FIRMID is not null')


def plan(sql, columns=("name",)):
    return {"status": "ok", "plan": {"_type": "single", "rootExecutionNode": {
        "_type": "relationalTdsInstantiation",
        "resultType": {"_type": "tds", "tdsColumns": [{"name": c, "type": "String"} for c in columns]},
        "executionNodes": [{"_type": "sql", "sqlQuery": sql, "implementation": {"noise": 1}}]}}}


def write_side(root, plans=None, pmcd=None, subtypes=None, model_json=None, crossread=None):
    model = root / "relational-emit-models" / "m"
    for item, entry in (plans or {}).items():
        path = model / "plans" / (item + ".json")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(entry))
    model.mkdir(parents=True, exist_ok=True)
    (model / "model.json").write_text(json.dumps(model_json or {"parse": {"status": "ok"}, "compile": {"status": "ok"}}))
    if pmcd is not None:
        (model / "pmcd.json").write_text(json.dumps(pmcd))
    if crossread is not None:
        (model / "crossread.json").write_text(json.dumps(crossread))
    if subtypes is not None:
        (root / "relational-emit-models" / "_subtypes.json").write_text(json.dumps(subtypes))
    return str(root)


def test_plan_buckets():
    assert compare.compare_plan_item(plan(BASE_SQL), plan(BASE_SQL))["status"] == "identical"
    assert compare.compare_plan_item(plan(BASE_SQL), plan(RENAMED_SQL))["status"] == "alias-only"
    changed = compare.compare_plan_item(plan(BASE_SQL), plan(EXTRA_JOIN_SQL))
    assert changed["status"] == "changed"
    assert changed["severity"] == "high"
    assert changed["sql_delta"]["join.left_outer"] == [1, 2]
    assert compare.compare_plan_item(plan(BASE_SQL), {"status": "error", "message": "boom"})["status"] == "error-new"
    assert compare.compare_plan_item({"status": "error", "message": "boom"}, plan(BASE_SQL))["status"] == "error-fixed"


def test_java_implementation_is_not_a_plan_change():
    noisy = plan(BASE_SQL)
    noisy["plan"]["rootExecutionNode"]["executionNodes"][0]["implementation"] = {"noise": 2}
    assert compare.compare_plan_item(plan(BASE_SQL), noisy)["status"] == "identical"


def test_result_type_change_is_high_even_with_same_sql():
    result = compare.compare_plan_item(plan(BASE_SQL), plan(BASE_SQL, columns=("name", "extra")))
    assert (result["status"], result["severity"]) == ("changed", "high")


def test_error_messages_ignore_object_hashes():
    a = {"status": "error", "message": "x Root_meta_Foo@1a2b3c4d failed"}
    b = {"status": "error", "message": "x Root_meta_Foo@9f8e7d6c failed"}
    assert compare.compare_plan_item(a, b)["status"] == "error-unchanged"


def test_clusters_group_items_with_the_same_delta(tmp_path):
    plans_base = {"test/M/s%d/t" % i: plan(BASE_SQL) for i in range(3)}
    plans_head = {"test/M/s%d/t" % i: plan(EXTRA_JOIN_SQL) for i in range(3)}
    plans_head["test/M/s0/t"] = plan(RENAMED_SQL)
    result = compare.compare(write_side(tmp_path / "b", plans_base), write_side(tmp_path / "h", plans_head))
    signatures = {c["signature"]: c["count"] for c in result["plans"]["clusters"]}
    assert signatures["alias-only"] == 1
    assert [count for sig, count in signatures.items() if "join.left_outer +1" in sig] == [2]
    assert "join.left_outer +1" in report.render(result)


def test_modules_sharing_a_suite_root_name_stay_apart(tmp_path):
    for module in ("service-emit", "persistence-emit"):
        write_side(tmp_path / "b" / module, {"service/S": plan(BASE_SQL)})
    side = compare.Side(str(tmp_path / "b"))
    assert sorted(side.models) == [("persistence-emit/relational-emit-models", "m"), ("service-emit/relational-emit-models", "m")]
    assert len(side.plans) == 2


def test_pmcd_patterns_ignore_source_information():
    element = {"_type": "class", "package": "demo", "name": "Firm", "properties": [{"name": "a", "sourceInformation": {"startLine": 1}}]}
    moved = json.loads(json.dumps(element))
    moved["properties"][0]["sourceInformation"]["startLine"] = 9
    assert compare.compare_pmcd({"elements": [element]}, {"elements": [moved]}) == {}
    grown = json.loads(json.dumps(element))
    grown["properties"][0]["defaultValue"] = {"value": 1}
    change = compare.compare_pmcd({"elements": [element]}, {"elements": [grown]})["demo::Firm (class)"]
    assert change["patterns"] == ["added class.properties[].defaultValue.value"]


def test_protocol_reports_crossread_failures_and_subtype_changes(tmp_path):
    pmcd = {"elements": []}
    base = write_side(tmp_path / "b", pmcd=pmcd, subtypes={"Base": {"old": ["a.Old"], "kept": ["a.Kept"]}})
    head = write_side(tmp_path / "h", pmcd=pmcd, subtypes={"Base": {"kept": ["a.Kept"], "new": ["a.New", "a.Other"]}},
                      crossread={"deserialize": {"status": "error", "message": "Unrecognized field"}})
    result = compare.compare(base, head)
    row = result["protocol"]["models"][0]
    assert row["backward_deserialize"].startswith("error: Unrecognized field")
    changes = {(c["type"], c["change"]) for c in result["protocol"]["subtypes"]["changes"]}
    assert changes == {("old", "removed"), ("new", "added")}
    assert result["protocol"]["subtypes"]["new_collisions"][0]["type"] == "new"
    rendered = report.render(result)
    assert "backward deserialize" in rendered and "collision" in rendered


def test_statement_entries_compare_like_plans():
    base = {"status": "ok", "statements": ['INSERT INTO "MAIN" (SELECT "s"."A" FROM "STAGING" as "s")']}
    same_aliases_renamed = {"status": "ok", "statements": ['INSERT INTO "MAIN" (SELECT "t"."A" FROM "STAGING" as "t")']}
    more = {"status": "ok", "statements": base["statements"] + ['DELETE FROM "STAGING"']}
    assert compare.compare_plan_item(base, base)["status"] == "identical"
    assert compare.compare_plan_item(base, same_aliases_renamed)["status"] == "alias-only"
    assert compare.compare_plan_item(base, more)["sql_delta"]["statements"] == [1, 2]
