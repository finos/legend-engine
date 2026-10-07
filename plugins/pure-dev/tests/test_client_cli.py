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
import json
import urllib.error

import pytest

from pure_lsp_bridge import client_cli


def _args(**overrides):
    ns = argparse.Namespace(host="127.0.0.1", port=8991)
    for k, v in overrides.items():
        setattr(ns, k, v)
    return ns


def test_cmd_health_prints_get_result(monkeypatch, capsys):
    monkeypatch.setattr(client_cli, "_get", lambda args, path: {"alive": True})
    client_cli.cmd_health(_args())
    assert '"alive": true' in capsys.readouterr().out


def test_cmd_status_returns_0_when_ready(monkeypatch):
    monkeypatch.setattr(client_cli, "_get", lambda args, path: {"state": "ready"})
    assert client_cli.cmd_status(_args(wait=0)) == 0


def test_cmd_status_returns_1_when_not_ready(monkeypatch):
    monkeypatch.setattr(client_cli, "_get", lambda args, path: {"state": "degraded"})
    assert client_cli.cmd_status(_args(wait=0)) == 1


def test_cmd_check_returns_0_with_no_errors(monkeypatch, tmp_path):
    f = tmp_path / "Test.pure"
    f.write_text("let x = 1;")
    monkeypatch.setattr(client_cli, "_post", lambda args, path, payload: {
        "uri": "file://" + str(f), "diagnostics": [{"severity": 2}]  # warning, not an error
    })
    assert client_cli.cmd_check(_args(file=str(f), uri=None)) == 0


def test_cmd_check_returns_1_when_severity_1_present(monkeypatch, tmp_path):
    f = tmp_path / "Test.pure"
    f.write_text("let x = 1;")
    monkeypatch.setattr(client_cli, "_post", lambda args, path, payload: {
        "uri": "file://" + str(f), "diagnostics": [{"severity": 1, "message": "boom"}]
    })
    assert client_cli.cmd_check(_args(file=str(f), uri=None)) == 1


def test_cmd_check_reads_stdin_when_file_is_dash(monkeypatch):
    captured = {}

    def fake_post(args, path, payload):
        captured.update(payload)
        return {"diagnostics": []}

    monkeypatch.setattr(client_cli, "_post", fake_post)
    monkeypatch.setattr("sys.stdin", __import__("io").StringIO("content-from-stdin"))
    assert client_cli.cmd_check(_args(file="-", uri=None)) == 0
    assert captured["content"] == "content-from-stdin"
    assert captured["uri"] == "file:///stdin.pure"


def test_cmd_check_many_returns_0_on_success(monkeypatch, tmp_path):
    f1 = tmp_path / "A.pure"
    f1.write_text("1")
    f2 = tmp_path / "B.pure"
    f2.write_text("2")
    monkeypatch.setattr(client_cli, "_post", lambda args, path, payload, timeout=60: {"success": True})
    assert client_cli.cmd_check_many(_args(files=[str(f1), str(f2)])) == 0


def test_cmd_check_many_returns_1_on_failure(monkeypatch, tmp_path, capsys):
    f1 = tmp_path / "A.pure"
    f1.write_text("1")
    monkeypatch.setattr(client_cli, "_post", lambda args, path, payload, timeout=60: {
        "success": False, "errorUri": "file:///A.pure", "error": "boom"
    })
    assert client_cli.cmd_check_many(_args(files=[str(f1)])) == 1
    assert "file:///A.pure" in capsys.readouterr().err


def test_cmd_go_returns_0_on_success(monkeypatch):
    monkeypatch.setattr(client_cli, "_post", lambda args, path, payload, timeout=600: {"success": True})
    assert client_cli.cmd_go(_args(files=[])) == 0


def test_cmd_go_returns_1_on_failure(monkeypatch):
    monkeypatch.setattr(client_cli, "_post", lambda args, path, payload, timeout=600: {"success": False})
    assert client_cli.cmd_go(_args(files=[])) == 1


def test_main_dispatches_health_command(monkeypatch, capsys):
    monkeypatch.setattr(client_cli, "_get", lambda args, path: {"alive": True})
    rc = client_cli.main(["health"])
    assert rc == 0
    assert "alive" in capsys.readouterr().out


def test_main_returns_2_on_connection_error(monkeypatch, capsys):
    def boom(args, path):
        raise urllib.error.URLError("connection refused")

    monkeypatch.setattr(client_cli, "_get", boom)
    rc = client_cli.main(["health"])
    assert rc == 2
    assert "error contacting bridge" in capsys.readouterr().err


def test_main_requires_a_subcommand():
    with pytest.raises(SystemExit):
        client_cli.main([])


# --- execute-parallel ---------------------------------------------------------------------
# An explicit function list goes to /execute-tests too, not just a --package/--source scope. The
# older path fired one legend/execute per function and let the shared request pool throttle them,
# so a large batch could occupy every request thread and starve status/check/cancel. These pin the
# endpoint, since regressing to client-side fan-out would restore that starvation silently.

def test_execute_parallel_with_neither_functions_nor_scope_is_an_error(capsys):
    # The function list became optional when --package/--source landed, so argparse no longer
    # rejects this; the handler has to.
    assert client_cli.main(["--port", "8991", "execute-parallel"]) == 2
    assert "at least one function is required" in capsys.readouterr().err


def test_cmd_execute_prints_return_value_fields(monkeypatch, capsys):
    def fake_post(args, path, payload, timeout=60):
        return {"success": True, "output": "(no console output. See returnValue.)",
                "returnKind": "primitive", "returnType": "String", "returnValue": "select 1",
                "returnSize": 1, "returnTruncated": False}

    monkeypatch.setattr(client_cli, "_post", fake_post)
    assert client_cli.main(["--port", "8991", "execute", "a::b::f():String[1]"]) == 0
    printed = json.loads(capsys.readouterr().out)
    assert printed["returnValue"] == "select 1"
    assert printed["returnKind"] == "primitive"


def test_render_return_value_handles_each_kind():
    assert client_cli._render_return_value({"returnKind": "primitive", "returnValue": "x"}) == "'x'"
    assert client_cli._render_return_value({"returnKind": "empty"}) is None
    assert client_cli._render_return_value({}) is None
    assert "Firm" in client_cli._render_return_value(
        {"returnKind": "complex", "returnType": "my::Firm"})
    collection = client_cli._render_return_value(
        {"returnKind": "collection", "returnValue": ["a", "b"], "returnSize": 2})
    assert collection == "['a', 'b']"


def test_render_return_value_hides_passing_true_but_not_false():
    passing = {"returnKind": "primitive", "returnValue": True, "status": "passed"}
    assert client_cli._render_return_value(passing) is None
    returned_false = {"returnKind": "primitive", "returnValue": False, "status": "passed"}
    assert client_cli._render_return_value(returned_false) == "False"
    failing = {"returnKind": "primitive", "returnValue": True, "status": "failed"}
    assert client_cli._render_return_value(failing) == "True"


def test_render_return_value_flags_truncation():
    rendered = client_cli._render_return_value(
        {"returnKind": "collection", "returnValue": ["a"], "returnSize": 48211,
         "returnTruncated": True})
    assert "48211 total, truncated" in rendered


def test_truncation_marker_survives_the_length_cap():
    """The overflowing preview is exactly the truncated case, so the marker must outlive the cap."""
    rendered = client_cli._render_return_value(
        {"returnKind": "collection", "returnValue": list(range(1000)), "returnSize": 48211,
         "returnTruncated": True})
    assert rendered.endswith("(48211 total, truncated)")
    assert "..." in rendered


def test_render_return_value_caps_long_previews():
    rendered = client_cli._render_return_value(
        {"returnKind": "primitive", "returnValue": "x" * 500})
    assert len(rendered) <= client_cli.RETURN_VALUE_PREVIEW_CHARS + 3
    assert rendered.endswith("...")


def test_cmd_execute_sends_repeated_args_in_order(monkeypatch):
    seen = {}

    def fake_post(args, path, payload, timeout=60):
        seen.update(payload)
        return {"success": True}

    monkeypatch.setattr(client_cli, "_post", fake_post)
    assert client_cli.main(["--port", "8991", "execute", "a::b::t_String_1__String_1__Boolean_1_",
                            "--arg", "first", "--arg", "second"]) == 0
    assert seen["arguments"] == ["first", "second"]


def test_cmd_execute_omits_arguments_when_none_given(monkeypatch):
    seen = {}

    def fake_post(args, path, payload, timeout=60):
        seen.update(payload)
        return {"success": True}

    monkeypatch.setattr(client_cli, "_post", fake_post)
    client_cli.main(["--port", "8991", "execute", "a::b::t"])
    assert "arguments" not in seen


def test_cmd_execute_parallel_sends_functions_to_execute_tests(monkeypatch):
    seen = _capture_post(monkeypatch)
    assert client_cli.cmd_execute_parallel(_scope_args(functions=["a", "b"])) == 0
    assert seen["path"] == "/execute-tests"
    assert seen["payload"]["functions"] == ["a", "b"]
    assert seen["payload"]["parallel"] is True
    # No scope keys alongside the list - the server rejects more than one scope.
    assert "packagePath" not in seen["payload"]
    assert "uri" not in seen["payload"]


def test_cmd_execute_parallel_carries_a_run_id_so_ctrl_c_can_cancel(monkeypatch):
    seen = _capture_post(monkeypatch)
    client_cli.cmd_execute_parallel(_scope_args(functions=["a"]))
    assert seen["payload"]["runId"].startswith("cli-")


def test_cmd_execute_parallel_returns_1_when_the_run_fails(monkeypatch):
    seen = _capture_post(monkeypatch, result={
        "success": False, "tests": [], "passed": 0, "failed": 1, "skipped": 0, "durationMs": 0})
    assert client_cli.cmd_execute_parallel(_scope_args(functions=["a", "b"])) == 1
    assert seen["path"] == "/execute-tests"


def test_cmd_execute_parallel_forwards_the_pct_adapter(monkeypatch):
    # Previously dropped on this path: --pct-adapter was only honoured for a --package/--source
    # scope, so naming PCT tests explicitly silently ran them without an adapter.
    seen = _capture_post(monkeypatch)
    client_cli.cmd_execute_parallel(_scope_args(functions=["a"], pct_adapter="x::adapter"))
    assert seen["payload"]["pctAdapterPath"] == "x::adapter"


def test_cmd_execute_parallel_sends_compiled_files(monkeypatch, tmp_path):
    f = tmp_path / "A.pure"
    f.write_text("1")
    seen = _capture_post(monkeypatch)
    client_cli.cmd_execute_parallel(_scope_args(functions=["a"], files=[str(f)]))
    assert seen["payload"]["functions"] == ["a"]
    assert seen["payload"]["files"][0]["uri"].endswith("/A.pure")


# --- error bodies -------------------------------------------------------------------------
# The bridge answers a rejection with {"error": "..."} explaining exactly what was wrong.
# HTTPError subclasses URLError, so main()'s connection handler used to swallow it and print a
# bare "HTTP Error 400: Bad Request", discarding the only useful part.

def _http_error(code, body):
    import io
    return urllib.error.HTTPError("http://x/y", code, "Bad Request", {},
                                  io.BytesIO(body.encode("utf-8")))


def test_bridge_error_surfaces_the_json_error_body(monkeypatch):
    def boom(req, timeout=None):
        raise _http_error(400, '{"error": "missing \'uri\'"}')

    monkeypatch.setattr(client_cli.urllib.request, "urlopen", boom)
    with pytest.raises(client_cli.BridgeError) as excinfo:
        client_cli._post(_args(), "/delete", {})
    assert "missing 'uri'" in str(excinfo.value)


def test_bridge_error_falls_back_when_the_body_is_not_json(monkeypatch):
    def boom(req, timeout=None):
        raise _http_error(500, "<html>nope</html>")

    monkeypatch.setattr(client_cli.urllib.request, "urlopen", boom)
    with pytest.raises(client_cli.BridgeError) as excinfo:
        client_cli._get(_args(), "/status")
    assert "HTTP 500" in str(excinfo.value)


def test_main_reports_bridge_errors_with_exit_2(monkeypatch, capsys):
    monkeypatch.setattr(client_cli, "_get",
                        lambda args, path: (_ for _ in ()).throw(client_cli.BridgeError("nope")))
    assert client_cli.main(["--port", "8991", "health"]) == 2
    assert "nope" in capsys.readouterr().err


# --- package/file scoped test runs --------------------------------------------------------
# `execute` and `execute-parallel` route to /execute-tests when given a scope. Which subcommand
# was used is what decides serial vs concurrent - there is no --parallel flag - so these pin that
# mapping, since getting it backwards would silently run a shared-H2 suite concurrently.

def _scope_args(**overrides):
    base = dict(files=[], package=None, source=None, no_recursive=False, vanilla_only=False,
                pct_only=False, pct_adapter=None, json=False, function=None, functions=[])
    base.update(overrides)
    return _args(**base)


def _capture_post(monkeypatch, result=None):
    seen = {}

    def fake_post(args, path, payload, timeout=600):
        seen["path"] = path
        seen["payload"] = payload
        return result if result is not None else {"success": True, "tests": [], "passed": 0,
                                                  "failed": 0, "skipped": 0, "durationMs": 0}

    monkeypatch.setattr(client_cli, "_post", fake_post)
    return seen


def test_execute_with_package_runs_serially_against_execute_tests(monkeypatch):
    seen = _capture_post(monkeypatch)
    assert client_cli.cmd_execute(_scope_args(package="a::b")) == 0
    assert seen["path"] == "/execute-tests"
    assert seen["payload"]["packagePath"] == "a::b"
    assert seen["payload"]["parallel"] is False
    assert seen["payload"]["recursive"] is True


def test_execute_parallel_with_package_sets_parallel(monkeypatch):
    seen = _capture_post(monkeypatch)
    assert client_cli.cmd_execute_parallel(_scope_args(package="a::b")) == 0
    assert seen["path"] == "/execute-tests"
    assert seen["payload"]["parallel"] is True


def test_scope_flags_map_onto_the_payload(monkeypatch):
    seen = _capture_post(monkeypatch)
    client_cli.cmd_execute(_scope_args(package="a::b", no_recursive=True, vanilla_only=True,
                                       pct_adapter="x::adapter"))
    assert seen["payload"]["recursive"] is False
    assert seen["payload"]["includePct"] is False
    assert seen["payload"]["pctAdapterPath"] == "x::adapter"


def test_source_scope_becomes_a_file_uri_when_the_path_exists(monkeypatch, tmp_path):
    f = tmp_path / "Tests.pure"
    f.write_text("// tests")
    seen = _capture_post(monkeypatch)
    client_cli.cmd_execute(_scope_args(source=str(f)))
    assert seen["payload"]["uri"] == "file://" + str(f)


def test_source_scope_passes_through_a_non_path_unchanged(monkeypatch):
    seen = _capture_post(monkeypatch)
    client_cli.cmd_execute(_scope_args(source="platform/pure/x.pure"))
    assert seen["payload"]["uri"] == "platform/pure/x.pure"


@pytest.mark.parametrize("handler, overrides", [
    (client_cli.cmd_execute, dict(package="a::b", function="a::b::t")),
    (client_cli.cmd_execute_parallel, dict(package="a::b", functions=["a::b::t"])),
])
def test_scope_and_explicit_functions_together_is_rejected(handler, overrides, capsys):
    assert handler(_scope_args(**overrides)) == 2
    assert "not both" in capsys.readouterr().err


def test_scoped_run_returns_1_when_a_test_failed(monkeypatch):
    _capture_post(monkeypatch, result={"success": False, "passed": 1, "failed": 1, "skipped": 0,
                                       "durationMs": 10, "tests": [
                                           {"name": "t1", "suitePath": "a::b", "status": "passed"},
                                           {"name": "t2", "suitePath": "a::b", "status": "failed",
                                            "message": "boom"}]})
    assert client_cli.cmd_execute(_scope_args(package="a::b")) == 1


def test_scoped_run_renders_nested_suites_indented(monkeypatch, capsys):
    _capture_post(monkeypatch, result={"success": True, "passed": 2, "failed": 0, "skipped": 0,
                                       "durationMs": 1200, "scope": "a::b", "tests": [
                                           {"name": "setUp", "suitePath": "a::b", "status": "passed",
                                            "hook": True, "hookKind": "before", "durationMs": 1},
                                           {"name": "deepTest", "suitePath": "a::b::c::d",
                                            "status": "passed", "durationMs": 2}]})
    client_cli.cmd_execute(_scope_args(package="a::b"))
    out = capsys.readouterr().out.splitlines()
    # Indentation is relative to the shallowest suite in the run, so the root sits flush left and
    # a two-level-deeper package is indented by two steps.
    assert out[0] == "a::b"
    assert "setUp (before)" in out[1]
    assert out[2].startswith("    a::b::c::d")
    assert "2 passed, 0 failed, 0 skipped in 1.2s" in "\n".join(out)


def test_scoped_run_with_json_flag_prints_raw_payload(monkeypatch, capsys):
    _capture_post(monkeypatch, result={"success": True, "tests": [], "passed": 0, "failed": 0,
                                       "skipped": 0, "durationMs": 0})
    client_cli.cmd_execute(_scope_args(package="a::b", json=True))
    assert '"success": true' in capsys.readouterr().out


def test_scoped_run_error_is_reported_without_the_empty_tree_line(monkeypatch, capsys):
    _capture_post(monkeypatch, result={"success": False, "error": "Cannot find package 'a::b'",
                                       "tests": []})
    assert client_cli.cmd_execute(_scope_args(package="a::b")) == 1
    captured = capsys.readouterr()
    assert "Cannot find package" in captured.err
    assert "no tests found" not in captured.out


def test_scoped_run_with_no_tests_says_so(monkeypatch, capsys):
    _capture_post(monkeypatch, result={"success": True, "scope": "a::b", "tests": [],
                                       "passed": 0, "failed": 0, "skipped": 0, "durationMs": 0})
    assert client_cli.cmd_execute(_scope_args(package="a::b")) == 0
    assert "no tests found in a::b" in capsys.readouterr().out


# --- cancelling a run ----------------------------------------------------------------------
# A scoped run keeps executing on the daemon (holding the graph read lock) unless it is explicitly
# cancelled, so abandoning the client is not enough - these pin the paths that do the cancelling.

def test_scoped_run_always_sends_a_run_id(monkeypatch):
    seen = _capture_post(monkeypatch)
    client_cli.cmd_execute(_scope_args(package="a::b"))
    assert seen["payload"]["runId"], "without a runId there is no handle to cancel the run with"


def test_ctrl_c_cancels_the_run_on_the_daemon(monkeypatch, capsys):
    posts = []

    def fake_post(args, path, payload, timeout=600):
        posts.append((path, payload))
        if path == "/execute-tests":
            raise KeyboardInterrupt()
        return {"cancelledRunIds": [payload.get("runId")]}

    monkeypatch.setattr(client_cli, "_post", fake_post)
    # 130 is the conventional shell exit code for SIGINT.
    assert client_cli.cmd_execute(_scope_args(package="a::b")) == 130


def test_cancel_failure_is_warned_about_not_raised(monkeypatch, capsys):
    def fake_post(args, path, payload, timeout=30):
        raise client_cli.BridgeError("bridge gone")

    monkeypatch.setattr(client_cli, "_post", fake_post)
    client_cli._cancel_run(_args(), "run-9")
    err = capsys.readouterr().err
    assert "could not cancel" in err
    assert "--all" in err, "should point at the recovery command"


def test_cmd_cancel_tests_by_run_id(monkeypatch):
    seen = _capture_post(monkeypatch, result={"cancelledRunIds": ["r1"]})
    assert client_cli.cmd_cancel_tests(_args(run_id="r1", all=False)) == 0
    assert seen["path"] == "/cancel-tests"
    assert seen["payload"] == {"runId": "r1"}


def test_cmd_cancel_tests_all(monkeypatch):
    seen = _capture_post(monkeypatch, result={"cancelledRunIds": []})
    # Nothing in flight is not a failure - the run had most likely already finished.
    assert client_cli.cmd_cancel_tests(_args(run_id=None, all=True)) == 0
    assert seen["payload"] == {"all": True}


def test_cancel_tests_requires_a_target():
    with pytest.raises(SystemExit):
        client_cli.main(["--port", "8991", "cancel-tests"])


def test_cancelled_run_shows_partial_results_and_says_so(monkeypatch, capsys):
    _capture_post(monkeypatch, result={
        "success": False, "cancelled": True, "error": "Run cancelled after 2 of 40 entries",
        "passed": 2, "failed": 0, "skipped": 0, "durationMs": 500,
        "tests": [{"name": "t1", "suitePath": "a::b", "status": "passed"},
                  {"name": "t2", "suitePath": "a::b", "status": "passed"}]})
    assert client_cli.cmd_execute(_scope_args(package="a::b")) == 1
    captured = capsys.readouterr()
    assert "t1" in captured.out, "the entries that did complete are the useful part"
    assert "run cancelled" in captured.err


# --- timeouts -----------------------------------------------------------------------------

def test_client_timeout_exceeds_the_bridges_own_ceiling():
    """They used to be identical (max(600, 3*n) both sides), so on a real timeout the client gave
    up at the same instant as the bridge and the caller saw a socket timeout instead of the
    bridge's actual error."""
    for files in (None, [], ["a"] * 10, ["a"] * 500):
        bridge_ceiling = max(600, 3 * len(files or []))
        assert client_cli._exec_timeout(files) > bridge_ceiling


def test_execute_parallel_refuses_more_functions_than_the_limit(monkeypatch, capsys):
    seen = _capture_post(monkeypatch)
    over = ["a::f%d" % i for i in range(client_cli.MAX_TESTS_PER_RUN + 1)]
    assert client_cli.cmd_execute_parallel(_scope_args(functions=over)) == 2
    err = capsys.readouterr().err
    assert "limit is %d" % client_cli.MAX_TESTS_PER_RUN in err
    # Refused before any request - a run this size is what wedges the daemon.
    assert seen == {}


def test_execute_parallel_allows_exactly_the_limit(monkeypatch):
    seen = _capture_post(monkeypatch)
    at_limit = ["a::f%d" % i for i in range(client_cli.MAX_TESTS_PER_RUN)]
    assert client_cli.cmd_execute_parallel(_scope_args(functions=at_limit)) == 0
    assert len(seen["payload"]["functions"]) == client_cli.MAX_TESTS_PER_RUN


def test_scope_payload_asks_the_daemon_to_cap_the_run(monkeypatch):
    seen = _capture_post(monkeypatch)
    client_cli.cmd_execute_parallel(_scope_args(package="a::b::tests"))
    assert seen["payload"]["maxTests"] == client_cli.MAX_TESTS_PER_RUN


def test_oversized_scope_run_warns_after_the_fact(monkeypatch, capsys):
    # A scope is sized by the daemon, so the client can only report it once the run is back.
    tests = [{"suitePath": "a::b", "name": "t%d" % i, "status": "pass", "durationMs": 1}
             for i in range(client_cli.MAX_TESTS_PER_RUN + 5)]
    _capture_post(monkeypatch, result={
        "success": True, "tests": tests, "passed": len(tests), "failed": 0, "skipped": 0,
        "durationMs": 10})
    client_cli.cmd_execute_parallel(_scope_args(package="a::b"))
    assert "over the %d limit" % client_cli.MAX_TESTS_PER_RUN in capsys.readouterr().err


def test_cmd_check_returns_1_and_explains_when_diagnostics_time_out(monkeypatch, tmp_path, capsys):
    src = tmp_path / "a.pure"
    src.write_text("function a::b():Boolean[1]{true}")
    monkeypatch.setattr(client_cli, "_post", lambda args, path, payload: {
        "uri": "file://x", "diagnostics": [], "timedOut": True, "error": "timed out after 15s"})
    args = argparse.Namespace(host="h", port=1, file=str(src), uri=None)
    assert client_cli.cmd_check(args) == 1
    assert "timed out" in capsys.readouterr().err
