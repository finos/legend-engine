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
"""End-to-end tests for LspClient's JSON-RPC framing, driven over a real loopback TCP connection
with a tiny hand-rolled fake peer (playing the role of the LSP server) running in a background
thread. This exercises the real read/write/threading code, not a mocked substitute.
"""
import json
import socket
import threading
import time

import pytest

from pure_lsp_bridge.protocol import LspClient


def _write_frame(sock, obj):
    body = json.dumps(obj).encode("utf-8")
    header = ("Content-Length: %d\r\n\r\n" % len(body)).encode("ascii")
    sock.sendall(header + body)


def _read_frame(rfile):
    headers = {}
    while True:
        line = rfile.readline()
        if not line:
            return None
        line = line.strip()
        if line == b"":
            break
        if b":" in line:
            k, v = line.split(b":", 1)
            headers[k.strip().lower()] = v.strip()
    length = int(headers.get(b"content-length", b"0"))
    body = b""
    while len(body) < length:
        chunk = rfile.read(length - len(body))
        if not chunk:
            return None
        body += chunk
    return json.loads(body.decode("utf-8"))


class FakePeer:
    """A minimal fake LSP peer: listens on loopback, accepts one connection, and lets the test
    script canned responses/notifications while also recording what it received."""

    def __init__(self):
        self._server_sock = socket.socket()
        self._server_sock.bind(("127.0.0.1", 0))
        self._server_sock.listen(1)
        self.port = self._server_sock.getsockname()[1]
        self.received = []
        self._conn = None
        self._rfile = None
        self._ready = threading.Event()
        self._thread = threading.Thread(target=self._accept_loop, daemon=True)
        self._thread.start()

    def _accept_loop(self):
        conn, _ = self._server_sock.accept()
        self._conn = conn
        self._rfile = conn.makefile("rb", buffering=0)
        self._ready.set()
        try:
            while True:
                msg = _read_frame(self._rfile)
                if msg is None:
                    return
                self.received.append(msg)
        except (OSError, ValueError):
            return  # close() pulled the socket out from under this thread; that's the exit path

    def wait_connected(self, timeout=5):
        assert self._ready.wait(timeout), "fake peer never accepted a connection"

    def send(self, obj):
        self.wait_connected()
        _write_frame(self._conn, obj)

    def close(self):
        # shutdown() BEFORE close(), because close() alone does not reliably disconnect here:
        # this peer's own _accept_loop thread is blocked in readline() on the same fd, and a
        # close() while a read is in flight leaves the descriptor open (the makefile stream holds
        # its own reference too - the CPython detail test_alive_reflects_socket_state calls out).
        # No FIN reaches the client, so a test that needs the peer to actually die just hangs.
        # shutdown(SHUT_RDWR) forces the FIN regardless of blocked readers.
        try:
            if self._conn is not None:
                try:
                    self._conn.shutdown(socket.SHUT_RDWR)
                except OSError:
                    pass  # already disconnected
            if self._rfile is not None:
                self._rfile.close()
            if self._conn is not None:
                self._conn.close()
            self._server_sock.close()
        except Exception:
            pass


@pytest.fixture
def peer():
    p = FakePeer()
    yield p
    p.close()


@pytest.fixture
def client(peer):
    c = LspClient.connect("127.0.0.1", peer.port)
    yield c
    peer.close()


def _wait_until(predicate, timeout=5):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if predicate():
            return True
        time.sleep(0.02)
    return False


def test_request_round_trip(peer, client):
    def responder():
        _wait_until(lambda: len(peer.received) == 1)
        req = peer.received[0]
        peer.send({"jsonrpc": "2.0", "id": req["id"], "result": {"ok": True}})

    threading.Thread(target=responder, daemon=True).start()
    resp = client.request("legend/status", {"foo": "bar"}, timeout=5)
    assert resp["result"] == {"ok": True}
    assert peer.received[0]["method"] == "legend/status"
    assert peer.received[0]["params"] == {"foo": "bar"}


def test_request_times_out_without_response(peer, client):
    with pytest.raises(TimeoutError):
        client.request("legend/never-answered", timeout=0.2)


def test_request_async_await_response_correlates_out_of_order(peer, client):
    id1, entry1 = client.request_async("m1")
    id2, entry2 = client.request_async("m2")

    def responder():
        _wait_until(lambda: len(peer.received) == 2)
        # Reply to the SECOND request first, to prove correlation is by id, not send order.
        peer.send({"jsonrpc": "2.0", "id": id2, "result": "second"})
        peer.send({"jsonrpc": "2.0", "id": id1, "result": "first"})

    threading.Thread(target=responder, daemon=True).start()
    resp2 = client.await_response(id2, entry2, timeout=5)
    resp1 = client.await_response(id1, entry1, timeout=5)
    assert resp1["result"] == "first"
    assert resp2["result"] == "second"


def test_notification_dispatches_to_registered_handler(peer, client):
    received_params = []
    done = threading.Event()

    def handler(params):
        received_params.append(params)
        done.set()

    client.on_notification("textDocument/publishDiagnostics", handler)
    peer.wait_connected()
    peer.send({"jsonrpc": "2.0", "method": "textDocument/publishDiagnostics",
              "params": {"uri": "file:///a.pure", "diagnostics": []}})

    assert done.wait(5)
    assert received_params[0]["uri"] == "file:///a.pure"


def test_unimplemented_incoming_request_gets_null_reply(peer, client):
    peer.wait_connected()
    peer.send({"jsonrpc": "2.0", "id": "server-req-1", "method": "window/workDoneProgress/create"})

    assert _wait_until(lambda: len(peer.received) == 1)
    reply = peer.received[0]
    assert reply["id"] == "server-req-1"
    assert reply["result"] is None


def test_notify_sends_without_id(peer, client):
    client.notify("initialized", {})
    assert _wait_until(lambda: len(peer.received) == 1)
    assert "id" not in peer.received[0]
    assert peer.received[0]["method"] == "initialized"


def test_alive_reflects_socket_state(peer, client):
    assert client.alive() is True
    # The socket's fd stays open as long as any makefile()-derived stream still references it
    # (a CPython socket-module detail), so close every handle to actually release the fd.
    client._in.close()
    client._out.close()
    client._sock.close()
    assert client.alive() is False


def test_alive_is_false_once_the_remote_peer_dies(peer, client):
    """/health used to report "alive": true for a dead socket daemon.

    In socket mode the JVM is a detached daemon, so when it dies our local socket fd stays
    perfectly valid and the old fileno() != -1 check kept returning True. The reader thread
    returning on EOF is the only real signal; alive() now consults it.
    """
    peer.wait_connected()
    assert client.alive() is True

    peer.close()  # the "daemon" goes away; we do NOT touch our own socket object
    assert _wait_until(lambda: not client.alive()), "alive() never noticed the peer died"


def test_inflight_request_fails_fast_when_the_peer_dies(peer, client):
    """A caller blocked on a request shouldn't burn its whole timeout after the transport dies."""
    peer.wait_connected()
    msg_id, entry = client.request_async("legend/status")
    peer.close()
    with pytest.raises(ConnectionError):
        client.await_response(msg_id, entry, timeout=5)
