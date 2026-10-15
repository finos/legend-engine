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
"""Tests for bridge.py's HTTP layer (Handler/run). Bridge itself (which owns a real LSP JVM
subprocess) is not instantiated here - instead a small in-memory FakeBridge double stands in for
it, so these tests exercise the real do_GET/do_POST routing and JSON (de)serialization over a real
ThreadingHTTPServer/socket, without needing a JVM.
"""
import json
import os
import threading
from http.client import HTTPConnection
from http.server import ThreadingHTTPServer

import pytest

from pure_lsp_bridge.bridge import Handler


@pytest.mark.parametrize("files, expected", [
    (None, None),
    ([], None),
    ([{"uri": "file:///a.pure"}], None),
    ("not-a-list", "'files' must be a list"),
    ([{"no_uri": "x"}], "every entry in 'files' needs a 'uri'"),
    ([{"uri": ""}], "every entry in 'files' needs a 'uri'"),
    (["not-a-dict"], "every entry in 'files' needs a 'uri'"),
])
def test_validate_files(files, expected):
    assert Handler._validate_files(files) == expected


class FakeLspClientHandle:
    """Stands in for Bridge.client for the /health route, which reads bridge.client.proc/.alive()."""
    proc = None

    def alive(self):
        return True


class FakeBridge:
    def __init__(self):
        self.client = FakeLspClientHandle()
        self.calls = []

    def status(self):
        return {"state": "ready"}

    def check(self, uri, content):
        self.calls.append(("check", uri, content))
        return {"uri": uri, "diagnostics": []}

    def check_many(self, files):
        self.calls.append(("check_many", files))
        return {"success": True, "modifiedFiles": [f["uri"] for f in files]}

    def execute_go(self, files=None):
        self.calls.append(("execute_go", files))
        return {"success": True, "result": 42}

    def execute_function(self, function, files=None, pct_adapter_path=None,
                         before_function_path=None, after_function_path=None, arguments=None):
        # Keyword names must track Bridge.execute_function: the Handler forwards all of them, so a
        # stale signature here surfaces as an opaque HTTP 500 from the generic except-handler
        # rather than a TypeError naming the mismatch. New fields go on the END of the recorded
        # tuple so the index-based assertions below keep meaning what they did.
        self.calls.append(("execute_function", function, files, pct_adapter_path,
                           before_function_path, after_function_path, arguments))
        return {"success": True, "result": function, "returnKind": "primitive",
                "returnType": "String", "returnValue": "hello", "returnSize": 1,
                "returnTruncated": False}

    def execute_functions_parallel(self, functions, files=None):
        self.calls.append(("execute_functions_parallel", functions, files))
        return [{"function": fn, "result": {"success": True}} for fn in functions]

    def execute_tests(self, package_path=None, uri=None, functions=None, invocations=None,
                      recursive=True, parallel=False, include_vanilla=True, include_pct=True,
                      pct_adapter_path=None, files=None, run_id=None, timeout=None):
        # Keyword names must track Bridge.execute_tests - the Handler forwards all of them by
        # keyword, so a stale signature here shows up as an opaque HTTP 500 instead of a naming
        # TypeError. New fields go on the END of the recorded tuple so the index-based assertions
        # below keep meaning what they did.
        self.calls.append(("execute_tests", package_path, uri, functions, recursive, parallel,
                           include_vanilla, include_pct, pct_adapter_path, files, run_id, timeout,
                           invocations))
        return {"success": True,
                "scope": package_path or uri or ("%d function(s)" % len(functions or invocations or [])),
                "passed": 1, "failed": 0,
                "skipped": 0, "tests": [{"name": "t", "suitePath": "a::b", "status": "passed"}]}

    def cancel_tests(self, run_id=None, all_runs=False):
        self.calls.append(("cancel_tests", run_id, all_runs))
        return {"cancelledRunIds": [run_id] if run_id else ["a", "b"], "message": "ok"}

    def set_option(self, name, value):
        self.calls.append(("set_option", name, value))
        return {"name": name, "value": value}

    def delete_file(self, uri):
        self.calls.append(("delete_file", uri))
        return {"deleted": uri}


@pytest.fixture(scope="module")
def _running_server():
    httpd = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    thread.start()
    try:
        yield httpd
    finally:
        httpd.shutdown()
        httpd.server_close()


@pytest.fixture
def http_server(_running_server):
    """One shared HTTP server per test module (thread start/stop is the expensive part); each test
    still gets a fresh FakeBridge so call recordings don't leak across tests. Handler.port/
    socket_port are also reset per test since /health reads them as class attributes (set by
    bridge.run() in production, which no test here calls)."""
    Handler.bridge = FakeBridge()
    Handler.port = None
    Handler.socket_port = None
    try:
        yield _running_server
    finally:
        Handler.bridge = None
        Handler.port = None
        Handler.socket_port = None


def _request(httpd, method, path, body=None):
    conn = HTTPConnection("127.0.0.1", httpd.server_address[1], timeout=5)
    try:
        headers = {}
        payload = None
        if body is not None:
            payload = json.dumps(body).encode("utf-8") if not isinstance(body, bytes) else body
            headers["Content-Type"] = "application/json"
        conn.request(method, path, body=payload, headers=headers)
        resp = conn.getresponse()
        raw = resp.read()
        data = json.loads(raw.decode("utf-8")) if raw else None
        return resp.status, data
    finally:
        conn.close()


def test_health(http_server):
    status, data = _request(http_server, "GET", "/health")
    assert status == 200
    assert data == {"alive": True, "pid": None, "transport": "socket", "port": None, "socket_port": None}


def test_health_reports_configured_ports(http_server):
    Handler.port = 18991
    Handler.socket_port = 19099
    status, data = _request(http_server, "GET", "/health")
    assert status == 200
    assert data["port"] == 18991
    assert data["socket_port"] == 19099


def test_health_merges_metadata_sidecar_without_clobbering_live_fields(http_server):
    Handler.port = http_server.server_address[1]
    Handler.socket_port = 19099
    sidecar_path = "/tmp/pure_lsp_server_%d.json" % Handler.port
    # A sidecar deliberately claiming a different port/pid than the live-computed values, to prove
    # those live values win and only the sidecar-only keys (repo_roots, jvm_args, java, started_at)
    # get merged in.
    with open(sidecar_path, "w") as f:
        json.dump({"pid": 99999, "port": 1, "socket_port": 2, "repo_roots": ["/x", "/y"],
                   "jvm_args": ["-Dfoo=bar"], "java": "java", "started_at": 123.0}, f)
    try:
        status, data = _request(http_server, "GET", "/health")
        assert status == 200
        assert data["pid"] is None  # live-computed (FakeLspClientHandle.proc is None), not the sidecar's 99999
        assert data["port"] == Handler.port
        assert data["socket_port"] == 19099
        assert data["repo_roots"] == ["/x", "/y"]
        assert data["jvm_args"] == ["-Dfoo=bar"]
        assert data["java"] == "java"
        assert data["started_at"] == 123.0
    finally:
        os.remove(sidecar_path)


def test_health_ignores_missing_metadata_sidecar(http_server):
    Handler.port = 65000  # picked so no sidecar file exists for it
    assert not os.path.exists("/tmp/pure_lsp_server_65000.json")
    status, data = _request(http_server, "GET", "/health")
    assert status == 200
    assert data == {"alive": True, "pid": None, "transport": "socket", "port": 65000, "socket_port": None}


def test_status(http_server):
    status, data = _request(http_server, "GET", "/status")
    assert status == 200
    assert data == {"state": "ready"}


def test_unknown_get_path_is_404(http_server):
    status, data = _request(http_server, "GET", "/nope")
    assert status == 404


def test_check_requires_uri(http_server):
    status, data = _request(http_server, "POST", "/check", {"content": "x"})
    assert status == 400
    assert "uri" in data["error"]


def test_check_happy_path(http_server):
    status, data = _request(http_server, "POST", "/check",
                            {"uri": "file:///a.pure", "content": "let x = 1;"})
    assert status == 200
    assert data == {"uri": "file:///a.pure", "diagnostics": []}
    assert Handler.bridge.calls == [("check", "file:///a.pure", "let x = 1;")]


def test_check_batch_requires_nonempty_files(http_server):
    status, data = _request(http_server, "POST", "/check-batch", {"files": []})
    assert status == 400


def test_check_batch_rejects_malformed_files(http_server):
    status, data = _request(http_server, "POST", "/check-batch", {"files": ["bad"]})
    assert status == 400


def test_check_batch_happy_path(http_server):
    files = [{"uri": "file:///a.pure", "content": "1"}, {"uri": "file:///b.pure", "content": "2"}]
    status, data = _request(http_server, "POST", "/check-batch", {"files": files})
    assert status == 200
    assert data["success"] is True
    assert data["modifiedFiles"] == ["file:///a.pure", "file:///b.pure"]


def test_go_with_no_files(http_server):
    status, data = _request(http_server, "POST", "/go", {})
    assert status == 200
    assert data == {"success": True, "result": 42}
    assert Handler.bridge.calls == [("execute_go", None)]


def test_go_rejects_malformed_files(http_server):
    status, data = _request(http_server, "POST", "/go", {"files": "nope"})
    assert status == 400


def test_execute_requires_function(http_server):
    status, data = _request(http_server, "POST", "/execute", {})
    assert status == 400


def test_execute_happy_path(http_server):
    status, data = _request(http_server, "POST", "/execute", {"function": "a::b::t():Boolean[1]"})
    assert status == 200
    assert data["result"] == "a::b::t():Boolean[1]"


def test_execute_passes_return_value_fields_through(http_server):
    """The bridge must not whitelist keys: a function's return value only reaches the caller
    because every field the daemon sends is forwarded verbatim."""
    status, data = _request(http_server, "POST", "/execute", {"function": "a::b::t():String[1]"})
    assert status == 200
    assert data["returnKind"] == "primitive"
    assert data["returnType"] == "String"
    assert data["returnValue"] == "hello"
    assert data["returnSize"] == 1
    assert data["returnTruncated"] is False


def test_execute_forwards_string_arguments(http_server):
    status, data = _request(http_server, "POST", "/execute",
                            {"function": "a::b::t_String_1__String_1__Boolean_1_",
                             "arguments": ["one", "two"]})
    assert status == 200
    assert Handler.bridge.calls[-1][-1] == ["one", "two"]


def test_execute_parallel_requires_functions_list(http_server):
    status, data = _request(http_server, "POST", "/execute-parallel", {})
    assert status == 400


def test_execute_parallel_happy_path(http_server):
    status, data = _request(http_server, "POST", "/execute-parallel", {"functions": ["a", "b"]})
    assert status == 200
    assert [r["function"] for r in data["results"]] == ["a", "b"]


def test_set_option_requires_name(http_server):
    status, data = _request(http_server, "POST", "/set-option", {})
    assert status == 400


def test_set_option_defaults_value_from_path(http_server):
    status, data = _request(http_server, "POST", "/set-option", {"name": "Foo"})
    assert status == 200
    assert data == {"name": "Foo", "value": True}

    status, data = _request(http_server, "POST", "/unset-option", {"name": "Foo"})
    assert data == {"name": "Foo", "value": False}


def test_delete_requires_uri(http_server):
    status, data = _request(http_server, "POST", "/delete", {})
    assert status == 400


def test_delete_happy_path(http_server):
    status, data = _request(http_server, "POST", "/delete", {"uri": "file:///a.pure"})
    assert status == 200
    assert data == {"deleted": "file:///a.pure"}


def test_execute_tests_package_scope_forwards_defaults(http_server):
    status, data = _request(http_server, "POST", "/execute-tests", {"packagePath": "a::b"})
    assert status == 200
    assert data["success"] is True
    call = Handler.bridge.calls[-1]
    # (package_path, uri, functions, recursive, parallel, include_vanilla, include_pct, ...)
    assert call[:8] == ("execute_tests", "a::b", None, None, True, False, True, True)


def test_execute_tests_forwards_every_option(http_server):
    status, data = _request(http_server, "POST", "/execute-tests", {
        "uri": "file:///a.pure", "recursive": False, "parallel": True,
        "includeVanilla": False, "includePct": True, "pctAdapterPath": "x::adapter",
        "runId": "run-7"})
    assert status == 200
    call = Handler.bridge.calls[-1]
    assert call[1:9] == (None, "file:///a.pure", None, False, True, False, True, "x::adapter")
    assert call[10] == "run-7"


def test_execute_tests_accepts_an_explicit_function_list(http_server):
    status, data = _request(http_server, "POST", "/execute-tests",
                            {"functions": ["a::b::t1", "a::b::t2"], "parallel": True})
    assert status == 200
    assert data["success"] is True
    call = Handler.bridge.calls[-1]
    assert call[1] is None          # no packagePath
    assert call[2] is None          # no uri
    assert call[3] == ["a::b::t1", "a::b::t2"]


def test_execute_tests_accepts_invocations_with_arguments_and_labels(http_server):
    invocations = [{"path": "a::b::check", "label": "case|TDS", "arguments": ["case", "TDS"]},
                   {"path": "a::b::check", "label": "case|Relation", "arguments": ["case", "Relation"]}]
    status, data = _request(http_server, "POST", "/execute-tests",
                            {"invocations": invocations, "parallel": True})
    assert status == 200
    call = Handler.bridge.calls[-1]
    assert call[3] is None          # no plain function list
    assert call[-1] == invocations


@pytest.mark.parametrize("payload", [
    {},
    {"packagePath": "a::b", "uri": "file:///a.pure"},
    {"packagePath": "a::b", "functions": ["a::b::t"]},
    {"uri": "file:///a.pure", "functions": ["a::b::t"]},
    {"functions": ["a::b::t"], "invocations": [{"path": "a::b::t"}]},
])
def test_execute_tests_requires_exactly_one_scope(http_server, payload):
    # Both-or-neither is a caller bug worth naming: silently preferring one would run a different
    # scope than the caller asked for.
    status, data = _request(http_server, "POST", "/execute-tests", payload)
    assert status == 400
    assert "exactly one" in data["error"]


def test_execute_tests_validates_files(http_server):
    status, data = _request(http_server, "POST", "/execute-tests",
                            {"packagePath": "a::b", "files": [{"no_uri": "x"}]})
    assert status == 400
    assert "files" in data["error"]


def test_cancel_tests_by_run_id(http_server):
    status, data = _request(http_server, "POST", "/cancel-tests", {"runId": "run-7"})
    assert status == 200
    assert data["cancelledRunIds"] == ["run-7"]
    assert Handler.bridge.calls[-1] == ("cancel_tests", "run-7", False)


def test_cancel_tests_all(http_server):
    status, data = _request(http_server, "POST", "/cancel-tests", {"all": True})
    assert status == 200
    assert Handler.bridge.calls[-1] == ("cancel_tests", None, True)


def test_cancel_tests_requires_a_target(http_server):
    status, data = _request(http_server, "POST", "/cancel-tests", {})
    assert status == 400
    assert "runId" in data["error"]


def test_unknown_post_path_is_404(http_server):
    status, data = _request(http_server, "POST", "/nope", {})
    assert status == 404


def test_malformed_json_body_treated_as_empty_payload(http_server):
    status, data = _request(http_server, "POST", "/check", b"{not-json")
    assert status == 400
    assert "uri" in data["error"]
