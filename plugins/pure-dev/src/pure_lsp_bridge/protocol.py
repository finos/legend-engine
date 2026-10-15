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
"""Minimal LSP-style JSON-RPC client.

Frames are `Content-Length`-delimited JSON, identical in both directions. The server can send
unsolicited notifications (e.g. publishDiagnostics) at any time, so a background reader thread
continuously drains the input stream and dispatches by message shape while the caller's thread is
free to send requests whenever it wants.

Two transports:
  * stdio (legacy): spawn the JVM as a child and talk over its stdin/stdout pipes. The JVM's
    lifecycle is bound to this process (pipe EOF kills it).
  * socket (durable): connect to a JVM that is already listening on a TCP socket (started as a
    detached daemon). Nothing owns the JVM via a pipe, so it survives this process and a fresh
    client can reconnect to the same warm session.
"""
import json
import socket
import subprocess
import sys
import threading


class LspClient:
    """JSON-RPC framing over a pair of binary streams, plus the transport that produces them.

    Construct via one of the classmethods:
      * LspClient.spawn(command, stderr_log_path) - stdio transport (spawns + pipes the JVM).
      * LspClient.connect(host, port)             - socket transport (connects to a daemon JVM).
    """

    def __init__(self, reader_stream, writer_stream, proc=None, sock=None):
        self._in = reader_stream   # binary stream we READ server output from
        self._out = writer_stream  # binary stream we WRITE requests to
        self.proc = proc           # Popen or None (socket transport has no owned child)
        self._sock = sock          # socket or None
        self._next_id = 1
        self._write_lock = threading.Lock()
        self._pending = {}  # id -> {"event": Event, "msg": None}
        self.notification_handlers = {}  # method -> callable(params)
        # Set when the reader thread returns, i.e. the transport is gone. See alive().
        self._closed = threading.Event()
        self._reader = threading.Thread(target=self._read_loop, daemon=True)
        self._reader.start()

    # ---------- construction ----------

    @classmethod
    def spawn(cls, command, stderr_log_path):
        with open(stderr_log_path, "wb") as stderr_log:
            proc = subprocess.Popen(
                command,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=stderr_log,
                bufsize=0,
                # Own session so an incidental process-group signal (parent shell/tool-call teardown)
                # doesn't propagate to the JVM. NOTE: in stdio mode the JVM still dies when THIS process
                # exits (its stdin pipe closes -> EOF); use socket mode for true durability.
                start_new_session=True,
            )
        # Popen already dup'd stderr_log's fd into the child before returning; our copy in this
        # process is now redundant and would otherwise stay open (leaking one fd) for the life of
        # this process, since nothing else ever closes it.
        return cls(proc.stdout, proc.stdin, proc=proc)

    @classmethod
    def connect(cls, host, port, timeout=10):
        sock = socket.create_connection((host, port), timeout=timeout)
        sock.settimeout(None)
        sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        # makefile gives us buffered binary streams with the same read/readline/write API the
        # framing code used for the pipes.
        reader = sock.makefile("rb", buffering=0)
        writer = sock.makefile("wb", buffering=0)
        return cls(reader, writer, sock=sock)

    def on_notification(self, method, handler):
        self.notification_handlers[method] = handler

    def alive(self):
        # The reader thread returning is the authoritative death signal for BOTH transports, and
        # the only one that works in socket mode: when the detached daemon JVM dies, our local
        # socket fd stays perfectly valid, so the fileno() check below kept returning True and
        # /health reported "alive": true for a daemon that was gone. Check this first.
        if self._closed.is_set():
            return False
        if self.proc is not None:
            return self.proc.poll() is None
        if self._sock is not None:
            try:
                return self._sock.fileno() != -1
            except Exception:
                return False
        return False

    # ---------- framing ----------

    def _write(self, obj):
        body = json.dumps(obj).encode("utf-8")
        header = ("Content-Length: %d\r\n\r\n" % len(body)).encode("ascii")
        with self._write_lock:
            self._out.write(header)
            self._out.write(body)
            self._out.flush()

    def _read_loop(self):
        out = self._in
        try:
            while True:
                headers = {}
                while True:
                    line = out.readline()
                    if not line:
                        return  # stream closed (child stdout closed, or socket closed)
                    line = line.strip()
                    if line == b"":
                        break
                    if b":" in line:
                        k, v = line.split(b":", 1)
                        headers[k.strip().lower()] = v.strip()
                length = int(headers.get(b"content-length", b"0"))
                body = b""
                while len(body) < length:
                    chunk = out.read(length - len(body))
                    if not chunk:
                        return
                    body += chunk
                self._dispatch(json.loads(body.decode("utf-8")))
        except Exception as e:
            print("[protocol] reader thread died:", e, file=sys.stderr)
        finally:
            # Reached on clean EOF and on error alike - either way the transport is finished.
            self._closed.set()
            # Nothing will ever answer the in-flight requests now; wake their waiters instead of
            # making each one burn its full timeout.
            with self._write_lock:
                pending, self._pending = self._pending, {}
            for entry in pending.values():
                entry["event"].set()

    def _dispatch(self, msg):
        if "id" in msg and ("result" in msg or "error" in msg):
            with self._write_lock:
                entry = self._pending.pop(msg["id"], None)
            if entry is not None:
                entry["msg"] = msg
                entry["event"].set()
            return

        method = msg.get("method")
        if method is not None and "id" in msg:
            # a request from the server we don't implement; reply null so it never hangs
            self._write({"jsonrpc": "2.0", "id": msg["id"], "result": None})
            return

        if method is not None:
            handler = self.notification_handlers.get(method)
            if handler is not None:
                handler(msg.get("params"))

    # ---------- JSON-RPC calls ----------

    def request(self, method, params=None, timeout=30):
        msg_id, entry = self.request_async(method, params)
        if not entry["event"].wait(timeout):
            with self._write_lock:
                self._pending.pop(msg_id, None)
            raise TimeoutError("timed out waiting for response to %s" % method)
        if entry["msg"] is None:
            # Woken by _read_loop's shutdown rather than by a response.
            raise ConnectionError("connection to the LSP closed while awaiting %s" % method)
        return entry["msg"]

    def request_async(self, method, params=None):
        """Send a request WITHOUT blocking for the response. Returns (msg_id, entry) where entry is a
        {"event": Event, "msg": None} that the reader thread fills + sets when the response arrives.
        Lets a caller fire many requests concurrently (the server dispatches each on its own thread)
        and then wait on all of them - the basis for parallel executeFunction. Use await_response()
        to collect. Note: response correlation is by JSON-RPC id, so out-of-order completion is fine."""
        with self._write_lock:
            msg_id = self._next_id
            self._next_id += 1
            entry = {"event": threading.Event(), "msg": None}
            self._pending[msg_id] = entry
        self._write({"jsonrpc": "2.0", "id": msg_id, "method": method, "params": params})
        return msg_id, entry

    def await_response(self, msg_id, entry, timeout=600):
        if not entry["event"].wait(timeout):
            with self._write_lock:
                self._pending.pop(msg_id, None)
            raise TimeoutError("timed out waiting for response (id=%s)" % msg_id)
        if entry["msg"] is None:
            raise ConnectionError("connection to the LSP closed while awaiting id=%s" % msg_id)
        return entry["msg"]

    def notify(self, method, params=None):
        self._write({"jsonrpc": "2.0", "method": method, "params": params})
