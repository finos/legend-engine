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
from engine_review import sql

EXISTS_AS_JOIN = (
    'select "root".FIRSTNAME as "firstName" from personTable as "root" '
    'left outer join (select distinct "othernamestable_1".PERSON_ID from otherNamesTable as "othernamestable_1" '
    'where "othernamestable_1".OTHER_NAME = \'abc\') as "othernamestable_0" on ("root".ID = "othernamestable_0".PERSON_ID) '
    'where "othernamestable_0".PERSON_ID is not null')


def test_alias_renames_normalize_equal():
    renamed = EXISTS_AS_JOIN.replace('"othernamestable_0"', '"othernamestable_d_ecq"').replace('"root"', '"r"')
    assert sql.normalize(renamed) == sql.normalize(EXISTS_AS_JOIN)


def test_column_alias_changes_are_not_normalized_away():
    renamed = EXISTS_AS_JOIN.replace('"firstName"', '"first"')
    assert sql.normalize(renamed) != sql.normalize(EXISTS_AS_JOIN)


def test_literals_are_not_normalized_away():
    assert sql.normalize(EXISTS_AS_JOIN.replace("'abc'", "'abd'")) != sql.normalize(EXISTS_AS_JOIN)


def test_whitespace_and_keyword_case_are_ignored():
    reformatted = EXISTS_AS_JOIN.replace(" from ", "\n  FROM ").replace(" where ", "\n WHERE ")
    assert sql.normalize(reformatted) == sql.normalize(EXISTS_AS_JOIN)


def test_metrics_of_exists_as_join():
    m = sql.metrics(EXISTS_AS_JOIN)
    assert m["join.left_outer"] == 1
    assert m["joins"] == 1
    assert m["subqueries"] == 1
    assert m["selects"] == 2
    assert m["distinct"] == 1
    assert m["tables"] == {"persontable": 1, "othernamestable": 1}


def test_duplicated_join_shows_as_delta():
    exploded = EXISTS_AS_JOIN.replace(
        ' where "othernamestable_0".PERSON_ID is not null',
        ' left outer join (select distinct "othernamestable_3".PERSON_ID from otherNamesTable as "othernamestable_3" '
        'where "othernamestable_3".OTHER_NAME = \'abc\') as "othernamestable_2" on ("root".ID = "othernamestable_2".PERSON_ID) '
        'where "othernamestable_0".PERSON_ID is not null')
    delta = sql.metric_delta(sql.metrics(EXISTS_AS_JOIN), sql.metrics(exploded))
    assert delta["join.left_outer"] == (1, 2)
    assert delta["table.othernamestable"] == (1, 2)
    assert delta["subqueries"] == (1, 2)


def test_join_kinds_and_schema_qualified_tables():
    m = sql.metrics('select * from s.a as "a" inner join s.b as "b" on 1=1 cross join c full outer join d on 1=1')
    assert (m["join.inner"], m["join.cross"], m["join.full_outer"]) == (1, 1, 1)
    assert m["tables"] == {"s.a": 1, "s.b": 1, "c": 1, "d": 1}


def test_extract_sql_walks_nested_nodes_in_order():
    plan = {"rootExecutionNode": {"_type": "relationalTdsInstantiation", "executionNodes": [
        {"_type": "sql", "sqlQuery": "select 1"},
        {"_type": "sql", "sqlQuery": "select 2"}]}}
    assert [s for _, _, s in sql.extract_sql(plan)] == ["select 1", "select 2"]


def test_pretty_breaks_clauses_and_indents_subqueries():
    lines = sql.pretty(EXISTS_AS_JOIN)
    assert lines[0] == 'select "root".firstname as "firstName"'
    assert any(line.startswith("left outer join (") for line in lines)
    assert any(line.startswith("  select distinct") for line in lines)


SAME_EXISTS_TWICE_BASE = (
    'select "root".firstName as "first", "persontable_1".firmId is not null as "colleagueSmith" from PersonTable as "root" '
    'left outer join FirmTable as "firmtable_0" on ("root".firmId = "firmtable_0".id) '
    'left outer join (select distinct "persontable_2".firmId from PersonTable as "persontable_2" where "persontable_2".lastName = \'Smith\') as "persontable_1" on ("persontable_1".firmId = "firmtable_0".id) '
    'where "persontable_1".firmId is not null')
SAME_EXISTS_TWICE_HEAD = SAME_EXISTS_TWICE_BASE.replace(
    ' where "persontable_1".firmId is not null',
    ' left outer join (select distinct "persontable_4".firmId from PersonTable as "persontable_4" where "persontable_4".lastName = \'Smith\') as "persontable_3" on ("persontable_3".firmId = "firmtable_0".id)'
    ' where "persontable_1".firmId is not null').replace('"persontable_1".firmId is not null as', '"persontable_3".firmId is not null as')


def test_the_same_exists_joined_twice_is_a_duplicate_join():
    assert sql.metrics(SAME_EXISTS_TWICE_BASE)["duplicate_joins"] == 0
    assert sql.metrics(SAME_EXISTS_TWICE_HEAD)["duplicate_joins"] == 1


def test_a_join_for_a_different_predicate_is_not_a_duplicate():
    different = SAME_EXISTS_TWICE_HEAD.replace("lastName = 'Smith') as \"persontable_3\"", "lastName = 'Brown') as \"persontable_3\"")
    different = different.replace("\"persontable_4\".lastName = 'Smith'", "\"persontable_4\".lastName = 'Brown'")
    assert sql.metrics(different)["duplicate_joins"] == 0


def test_self_joins_on_different_columns_are_not_duplicates():
    two_paths = ('select * from T as "root" left outer join P as "p_0" on ("p_0".a = "root".id) '
                 'left outer join P as "p_1" on ("p_1".b = "root".id)')
    assert sql.metrics(two_paths)["duplicate_joins"] == 0
    assert sql.metrics(two_paths.replace('("p_1".b', '("p_1".a'))["duplicate_joins"] == 1


def test_from_inside_a_comparison_or_call_is_not_a_table():
    m = sql.metrics('select "r".A from T as "r" where "x".B is not distinct from "r"."ID" and extract(year from "r".D) = 2020')
    assert m["tables"] == {"t": 1}
    assert m["null_safe_compare"] == 1


def test_null_safe_to_plain_equality_is_its_own_delta():
    before = sql.metrics('select 1 from T as "t" where "t".A is not distinct from "t".B')
    after = sql.metrics('select 1 from T as "t" where "t".A = "t".B')
    assert sql.metric_delta(before, after) == {"null_safe_compare": (1, 0)}


def test_pretty_keeps_window_order_by_and_call_keywords_on_their_line():
    lines = sql.pretty('select rank() over (partition by "t".A order by "t".B) as "r", trim(both from "t".C) as "c" from T as "t" order by "r"')
    assert lines == ['select rank () over (partition by "t".a order by "t".b) as "r", trim (both from "t".c) as "c"', 'from t as "t"', 'order by "r"']
