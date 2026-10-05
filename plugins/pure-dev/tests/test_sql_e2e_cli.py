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
import argparse

import pytest

from pure_lsp_bridge import sql_e2e_cli


def _args(**overrides):
    ns = argparse.Namespace(host="127.0.0.1", port=8991, mode="both", serial=False, json=False)
    for k, v in overrides.items():
        setattr(ns, k, v)
    return ns


def _index_output(ids, paths=("TDS", "Relation")):
    return "\n".join("%s|%s" % (i, p) for i in ids for p in paths) + "\n"


def _stub_execute(monkeypatch, output, captured=None):
    def fake_post(args, path, payload, timeout=60):
        if captured is not None:
            captured.append((path, payload))
        return {"success": True, "output": output}

    monkeypatch.setattr(sql_e2e_cli, "_post", fake_post)


def test_case_index_parses_pairs_and_ignores_other_console_lines(monkeypatch):
    _stub_execute(monkeypatch, "some preamble\nabs__big|TDS\nabs__big|Relation\n5 entries\n")
    assert sql_e2e_cli._case_index(_args(), ["abs__big"]) == [
        ("abs__big", "TDS"), ("abs__big", "Relation")]


def test_case_index_strips_the_quotes_pure_println_wraps_the_payload_in(monkeypatch):
    _stub_execute(monkeypatch, "'a|TDS\na|Relation\nb|TDS'\n")
    assert sql_e2e_cli._case_index(_args(), ["x"]) == [
        ("a", "TDS"), ("a", "Relation"), ("b", "TDS")]


def test_case_index_joins_filters_into_one_call(monkeypatch):
    captured = []
    _stub_execute(monkeypatch, _index_output(["a"]), captured)
    sql_e2e_cli._case_index(_args(), ["a", "structural/joins"])
    assert captured[0][1]["arguments"] == ["a,structural/joins"]


def test_case_index_deduplicates_overlapping_filters(monkeypatch):
    _stub_execute(monkeypatch, _index_output(["a"]) + _index_output(["a"]))
    assert sql_e2e_cli._case_index(_args(), ["a", "a*"]) == [("a", "TDS"), ("a", "Relation")]


@pytest.mark.parametrize("mode,expected", [
    ("tds", [("a", "TDS")]),
    ("relation", [("a", "Relation")]),
    ("both", [("a", "TDS"), ("a", "Relation")]),
])
def test_for_mode_selects_the_requested_paths(mode, expected):
    assert sql_e2e_cli._for_mode([("a", "TDS"), ("a", "Relation")], mode) == expected


def test_run_sends_one_invocation_per_entry_with_id_and_path(monkeypatch):
    _stub_execute(monkeypatch, _index_output(["a", "b"]))
    sent = {}
    monkeypatch.setattr(sql_e2e_cli, "cmd_execute_tests",
                        lambda args, parallel, payload: sent.update(payload) or 0)

    assert sql_e2e_cli.cmd_run(_args(filters=["cat"], mode="tds")) == 0
    assert sent["invocations"] == [
        {"path": sql_e2e_cli.CHECK_CASE, "label": "a|TDS", "arguments": ["a", "TDS"]},
        {"path": sql_e2e_cli.CHECK_CASE, "label": "b|TDS", "arguments": ["b", "TDS"]},
    ]
    assert sent["parallel"] is True


def test_run_at_the_cap_still_runs(monkeypatch):
    ids = ["c%d" % n for n in range(sql_e2e_cli.MAX_ENTRIES)]
    _stub_execute(monkeypatch, _index_output(ids, paths=("TDS",)))
    monkeypatch.setattr(sql_e2e_cli, "cmd_execute_tests", lambda args, parallel, payload: 0)
    assert sql_e2e_cli.cmd_run(_args(filters=["cat"], mode="tds")) == 0


def test_run_over_the_cap_refuses_without_executing(monkeypatch, capsys):
    ids = ["c%d" % n for n in range(sql_e2e_cli.MAX_ENTRIES + 1)]
    _stub_execute(monkeypatch, _index_output(ids, paths=("TDS",)))

    def fail(*a, **kw):
        pytest.fail("no run should be issued past the cap")

    monkeypatch.setattr(sql_e2e_cli, "cmd_execute_tests", fail)

    assert sql_e2e_cli.cmd_run(_args(filters=["functions/math_functions"], mode="tds")) == 2
    err = capsys.readouterr().err
    assert "capped at %d" % sql_e2e_cli.MAX_ENTRIES in err
    assert "-Dtest.filter=functions/math_functions" in err


def test_run_counts_both_modes_against_the_cap(monkeypatch, capsys):
    ids = ["c%d" % n for n in range(sql_e2e_cli.MAX_ENTRIES)]
    _stub_execute(monkeypatch, _index_output(ids))

    def fail(*a, **kw):
        pytest.fail("no run should be issued past the cap")

    monkeypatch.setattr(sql_e2e_cli, "cmd_execute_tests", fail)
    assert sql_e2e_cli.cmd_run(_args(filters=["cat"], mode="both")) == 2
    assert "%d entries" % (2 * sql_e2e_cli.MAX_ENTRIES) in capsys.readouterr().err


def test_run_with_no_match_reports_rather_than_running_nothing(monkeypatch, capsys):
    _stub_execute(monkeypatch, "")
    assert sql_e2e_cli.cmd_run(_args(filters=["nope"], mode="tds")) == 2
    assert "no corpus case matched" in capsys.readouterr().err


def test_case_asks_for_each_requested_path(monkeypatch, capsys):
    captured = []
    _stub_execute(monkeypatch, "STATUS=PASS x", captured)
    assert sql_e2e_cli.cmd_case(_args(case_id="x", mode="both")) == 0
    assert [c[1]["arguments"] for c in captured] == [["x", "TDS"], ["x", "Relation"]]


def test_adhoc_passes_both_as_a_single_path_value(monkeypatch):
    captured = []
    _stub_execute(monkeypatch, "PATH=TDS", captured)
    sql_e2e_cli.cmd_adhoc(_args(sql="SELECT 1", mode="both"))
    assert captured[0][1] == {"function": sql_e2e_cli.DIAG_ADHOC,
                              "arguments": ["SELECT 1", "Both"]}


def test_failed_execution_surfaces_the_daemon_error(monkeypatch):
    monkeypatch.setattr(sql_e2e_cli, "_post",
                        lambda args, path, payload, timeout=60: {"success": False, "error": "boom"})
    with pytest.raises(sql_e2e_cli.BridgeError, match="boom"):
        sql_e2e_cli._case_index(_args(), ["a"])


def test_jvm_level_failure_surfaces_its_data_not_a_bare_no_error_message(monkeypatch):
    # A missing class on the daemon classpath comes back as raw JSON-RPC with no "error" key.
    monkeypatch.setattr(sql_e2e_cli, "_post", lambda args, path, payload, timeout=60: {
        "code": -32603, "message": "Internal error.",
        "data": "java.lang.NoClassDefFoundError: some/Missing\n\tat ...\n"})
    with pytest.raises(sql_e2e_cli.BridgeError, match="NoClassDefFoundError"):
        sql_e2e_cli._case_index(_args(), ["a"])
