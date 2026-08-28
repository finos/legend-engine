# Debugging Pure Code with the Pure IDE Delta Compiler (HTTP Driver Guide)

> **Audience**: developers and coding agents that need to iterate on `.pure` code without
> paying for a Maven rebuild.
> **Related reading**: [Getting Started](./getting-started.md), [Build & CI](./build-and-ci.md),
> [PCT how-tos](../../pct/)

---

## Table of Contents

1. [Why this exists](#1-why-this-exists)
2. [Starting the IDE and finding its port](#2-starting-the-ide-and-finding-its-port)
3. [The delta-compile loop in one call](#3-the-delta-compile-loop-in-one-call)
4. [Warning: `openFiles` overwrites the file on disk](#4-warning-openfiles-overwrites-the-file-on-disk)
5. [Reading the console output](#5-reading-the-console-output)
6. [Running tests](#6-running-tests)
7. [Navigating the graph](#7-navigating-the-graph)
8. [Endpoint reference](#8-endpoint-reference)
9. [Recommended agent protocol](#9-recommended-agent-protocol)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Why this exists

A full `mvn clean install` of legend-engine takes 15–25 minutes. Most of that cost is
irrelevant when the change is a handful of lines of `.pure` code.

The **Pure IDE Light** server (`org.finos.legend.engine.ide.PureIDELight`) holds a fully
compiled Pure graph in memory. When you hand it a modified source file, it does **not**
recompile the world — it invalidates only the affected sources and their dependents and
recompiles that delta. In practice a round trip is **seconds**, not minutes.

The IDE ships a browser UI at `/ide`, but everything the UI does is a plain HTTP call.
That makes the same delta compiler drivable from `curl`, a script, or an agent. The loop is:

```
submit modified .pure source  ──▶  server recompiles the delta  ──▶  runs go()  ──▶  returns console text
```

You then **scan the returned console text** to decide whether the change worked.

> **Execution mode caveat.** The IDE runs Pure in **interpreted** mode. Production runs
> **compiled** mode (Pure pre-translated to Java bytecode at build time). The two agree on
> the overwhelming majority of behaviour, but a green result here is not a substitute for the
> real test suite — see [§10](#10-troubleshooting).

---

## 2. Starting the IDE and finding its port

Main class `org.finos.legend.engine.ide.PureIDELight`, with program arguments:

```
server legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json
```

> **Two details that older notes get wrong** — the root `CLAUDE.md` carried both until this
> guide was written, so expect to meet them in stale docs and prior conversations:
>
> - The module lives under `legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/…`,
>   **not** `legend-engine-pure/legend-engine-pure-ide/…`.
> - The port is whatever `ideLightConfig.json` declares — as checked in, **`9010`**
>   (`server.connector.port`). The frequently-quoted **9200 is wrong**.

**Never assume the port — read it, then confirm the server answers:**

```bash
# what the config declares
grep -A2 '"connector"' legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/\
legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json

# what is actually listening (the IDE is usually launched from IntelliJ)
lsof -nP -iTCP -sTCP:LISTEN | grep java
```

The UI then lives at `http://127.0.0.1:9010/ide` and Swagger at `http://127.0.0.1:9010/swagger`.

`ideLightConfig.json` also sets `"initPureRuntimeBeforeStart": true`, so the first startup
compiles the whole graph (a few minutes). Until that finishes, calls will block or report
that the system is not initialised. Start it once and leave it running.

> ### Do not run Maven while the IDE is up
>
> The IDE is launched from IntelliJ with module `target/classes` directories on its
> classpath, and `mvn clean` deletes the generated Pure-runtime resources it serves from.
> **The running process keeps answering** — its classes are already loaded — so nothing
> appears broken until the next restart, which then fails with `ClassNotFoundException` for
> generated classes. Same failure as the IntelliJ *Clear output directory on rebuild*
> gotcha in the root `CLAUDE.md`.
>
> You cannot avoid it by dropping `clean`: the Pure Maven plugins fail with
> `The code repository <repo> already exists!` when building over an existing `target/`,
> which forces `clean` back in. Even `mvn test -pl <one-module>` is enough to do the damage.
>
> Stop the IDE first, or verify through the IDE itself — which is what this guide is for.
> If a module's `target/` has already been wiped, rebuild it with:
>
> ```bash
> mvn -o install -DskipTests -pl <module-path>
> ```

---

## 3. The delta-compile loop in one call

`POST /executeGo` does all four steps — save, delta-compile, run `go()`, return console — in
a single request.

**Request body:**

```json
{
  "openFiles": [
    { "path": "/welcome.pure", "code": "<full new text of the file>" }
  ],
  "extraParams": {}
}
```

- `openFiles` — the files to overwrite before compiling. `code` is the **entire** file
  content, not a patch. Pass `[]` to just re-run the current `go()` untouched.

- `path` — the IDE's virtual source id, always leading-slash (`/welcome.pure`,
  `/core_external_query_sql/…/foo.pure`). It is *not* a filesystem path.

> ### Rule: always submit a delta of the existing file version
>
> `code` replaces the file wholesale, but that does **not** mean you author it from scratch.
> Every submission must be *the current version of the file with your change applied to it*:
>
> 1. `GET /fileAsJson/<path>` to fetch the current content, **in full**.
> 2. Apply your edit to that exact text.
> 3. Submit the complete result as `code`.
>
> Never hand `openFiles` a file you composed yourself. A four-line `go()` written from
> memory is a valid request and the server will accept it happily — it simply deletes
> everything else that was in the file: imports, helper functions, other definitions, and
> any parked scratch work. That is a silent, unrecoverable data loss, not an error.
>
> The same rule applies to the `path` field: it must name an existing source you have just
> read. Use `/newFile/{path}` to create something new.

The server requires a function with the exact signature `go():Any[*]`. Out of the box it
lives in **`/welcome.pure`** at the repository root — a gitignored scratch file that exists
purely as the IDE's entry point.

**Verified example:**

```bash
python3 - <<'EOF'
import json, urllib.request

code = '''function go():Any[*]
{
  println('hello from the delta compiler');
  println([1,2,3]->map(x | $x * 2));
}
'''

body = json.dumps({"openFiles": [{"path": "/welcome.pure", "code": code}],
                   "extraParams": {}}).encode()
req = urllib.request.Request("http://127.0.0.1:9010/executeGo", data=body,
                             headers={"Content-Type": "application/json"})
print(urllib.request.urlopen(req, timeout=180).read().decode())
EOF
```

**Actual response:**

```json
{"text":"'hello from the delta compiler'\n[\n   2\n   4\n   6\n]\n",
 "modifiedFiles":[],"compiler":"pureSession.getCompilerLogs()","cached":false}
```

`text` is the console capture — that is the field to scan. `println` output lands here;
so does any error (see [§5](#5-reading-the-console-output)).

Prefer a real HTTP client over `curl -d` for this: the payload is a JSON string containing a
whole Pure file, and shell quoting will bite you.

---

## 4. Warning: `openFiles` overwrites the file on disk

**This is the single most important thing in this document.**

Despite the read-only feel of "submit a buffer and run it", `POST /executeGo` (and
`/executeTests`) **persists every entry in `openFiles` to the real file on disk**, replacing
its contents. There is no dry-run flag and no undo. `welcome.pure` is gitignored, so `git`
will not save you — `git status` stays clean while the file is destroyed.

There is a second, independent trap: **there are two copies of every source, and they can
diverge.**

| Copy | Read by | Written by |
|------|---------|------------|
| the file on disk (code storage) | `GET /fileAsJson/<path>` | `openFiles` |
| the in-memory source registry / compiled graph | `go()` execution | `openFiles` |

`openFiles` updates both. Editing the file on disk by any other means — your editor, `sed`,
the `Write` tool — updates **only the first**. This is confirmed behaviour: after restoring
`welcome.pure` on disk, `fileAsJson` returned the restored text while `POST /executeGo` with
`{"openFiles":[]}` kept executing the *previous* submission, because the compiled graph had
not been touched.

So: **the only way to fully roll back is to resubmit the original text through `openFiles`.**
Repairing the file out-of-band leaves the runtime stale and makes `fileAsJson` actively
misleading — it will report a clean file while the IDE runs something else.

**Always snapshot before the first write:**

```bash
# 1. read the current content off disk via the IDE (before any openFiles write, disk == runtime)
curl -s "http://127.0.0.1:9010/fileAsJson//welcome.pure" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['content'])" > /tmp/welcome.pure.bak

# 2. ... iterate with /executeGo ...

# 3. restore both disk AND runtime by submitting the backup as openFiles
python3 - <<'EOF'
import json, urllib.request
code = open("/tmp/welcome.pure.bak").read()
body = json.dumps({"openFiles":[{"path":"/welcome.pure","code":code}],"extraParams":{}}).encode()
req = urllib.request.Request("http://127.0.0.1:9010/executeGo", data=body,
                             headers={"Content-Type":"application/json"})
print(urllib.request.urlopen(req, timeout=180).read().decode()[:400])
EOF
```

Note the doubled slash in `fileAsJson//welcome.pure` — the route is
`fileAsJson/{filePath:.+}` and `filePath` must itself begin with `/`.

Additional rules:

- **Read the whole file before overwriting it.** Do not truncate the snapshot; a partial
  backup is an unrecoverable partial restore.
- **Never overwrite a file you did not first snapshot**, especially `welcome.pure`, which
  typically holds the developer's own scratch experiments and has no git history to fall
  back on.
- **Always submit a delta of the current version** — read it with `fileAsJson`, edit *that*
  text, submit the whole result. See the rule box in [§3](#3-the-delta-compile-loop-in-one-call).
  Authoring a replacement file from scratch silently deletes everything you did not retype.
- **Prefer additive edits**: keep the existing `go()` body, comment it out, and append your
  probe below it, rather than replacing the file wholesale.
- If you only need to *read* code, use `GET /fileAsJson/...` — never round-trip through
  `openFiles`.

---

## 5. Reading the console output

Every `/executeGo` response is a single JSON object. The console lives in its `text` field,
JSON-escaped. Which *other* keys are present tells you the outcome — that is what an agent
should branch on.

**Step one is always: decode the JSON, then print `text`.** The escaped form is unreadable
and must never be regexed directly.

```python
import json, urllib.request

def run_go(code=None, host="http://127.0.0.1:9010"):
    files = [{"path": "/welcome.pure", "code": code}] if code else []
    body = json.dumps({"openFiles": files, "extraParams": {}}).encode()
    req = urllib.request.Request(host + "/executeGo", data=body,
                                 headers={"Content-Type": "application/json"})
    r = json.loads(urllib.request.urlopen(req, timeout=180).read())

    print(r["text"])                       # <- the console, as a human would see it

    if not r.get("error"):
        return "PASS"
    return "COMPILE_ERROR" if "exceptionType" in r else "RUNTIME_ERROR"
```

### Success

Raw response:

```json
{"text":"'hello from the delta compiler'\n[\n   2\n   4\n   6\n]\n",
 "modifiedFiles":[],"compiler":"…","cached":false}
```

`text` **decoded** — this is what `println('hello…')` and `println([1,2,3]->map(x|$x*2))`
actually produce:

```
'hello from the delta compiler'
[
   2
   4
   6
]
```

No `error` key means it passed. Note both quirks visible here: strings come back
**single-quoted**, and collections are **pretty-printed across multiple lines** with
three-space indent — never match on exact equality.

### Compile error (delta compilation failed)

```json
{"text":"\nCompilation error at (resource:/welcome.pure line:3 column:12), \"The variable 'undefinedVar' is unknown!\"\n    1: resource:/welcome.pure line:3 column:12\n",
 "exceptionType":"Compilation error","error":true,
 "source":"/welcome.pure","line":3,"column":12,
 "compiler":null,"RO":false}
```

Note `"compiler": null` and the presence of `exceptionType` — the code never ran. The
`source`/`line`/`column` triple is machine-readable; use it instead of regexing `text`.

### Runtime failure (compiled fine, blew up executing)

```json
{"text":"\n\nAssert failure at (resource:/welcome.pure line:3 column:3), \"my assertion blew up\"\n1: resource:/welcome.pure line:3 column:3\n\nFull Stack:\n    assert(Boolean[1], Function<{->String[1]}>[1]):Boolean[1]     <-     resource:/welcome.pure line:3 column:3",
 "error":true,"source":"/welcome.pure","line":3,"column":3,"RO":false,
 "modifiedFiles":[],"compiler":"…","cached":false}
```

`error:true` but **no** `exceptionType`, and `modifiedFiles`/`cached` are present — the
delta compile succeeded and execution started.

`text` **decoded**:

```
Assert failure at (resource:/welcome.pure line:3 column:3), "my assertion blew up"
1: resource:/welcome.pure line:3 column:3

Full Stack:
    assert(Boolean[1], Function<{->String[1]}>[1]):Boolean[1]     <-     resource:/welcome.pure line:3 column:3
```

The `Full Stack:` block is where the useful diagnosis lives — read it bottom-up, each
`<-` step naming the call site.

> **Console output printed before the failure is preserved.** The server opens `{"text":"`,
> streams the live console into it, and only then appends the error
> ([`GoRun.java`](../../../legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/java/org/finos/legend/engine/ide/api/execution/go/GoRun.java)).
> So `text` reads as *everything your `println`s emitted, then the stack trace*. That is what
> makes `println`-tracing work: scatter markers through `go()`, and the last one to appear
> tells you how far execution got before it blew up. In the example above nothing precedes
> the error because `go()` printed nothing — hence the leading blank lines.

### Decision table

| Condition | Meaning | Next move |
|-----------|---------|-----------|
| no `error` key | passed | read `text` for your `println` output |
| `error:true` **and** `exceptionType` present | delta compile failed | fix at `source:line:column`; nothing executed |
| `error:true`, no `exceptionType` | runtime/assertion failure | read `Full Stack:` in `text` |
| response is a raw Java stack trace string | you hit a malformed request, not a Pure error | check params/route |

**Gotchas when parsing `text`:**

- An empty `text` with no `error` means `go()` ran and printed nothing — a silent pass,
  indistinguishable from a `go()` you accidentally emptied. Always end with a
  `println('PASSED')` sentinel and assert on *that*, not on the absence of an error.
- Use `println` (appends a newline), not `print`, or your markers run together on one line.
- The `source`/`line`/`column` keys are the machine-readable form of the location already
  embedded in `text` — branch on those rather than parsing the message.
- Console output from **tests** does not appear here. `/executeTests` returns it per-test in
  a `console` field on each result — see [§6b](#6b-run-a-whole-package-with-the-test-runner).
- `"compiler":"pureSession.getCompilerLogs()"` is a literal placeholder string in the server
  source, not real compiler logs. Ignore it.
- `cached` reflects the runtime's graph cache state, not whether your code was cached.

---

## 6. Running tests

### 6a. Call one test directly from `go()`

For a specific test, this is far better than the test runner: one call, full console, real
stack traces. Pure test functions are ordinary functions, so just invoke one.

```pure
function go():Any[*]
{
  meta::pure::functions::math::tests::divide::testDecimalDivide(
    meta::relational::tests::pct::h2::testAdapterForRelationalWithH2Execution_Function_1__X_o_
  );
  println('PASSED');
}
```

PCT tests take an **adapter** parameter identifying the store to run against; a non-PCT test
takes none. Because a failing assertion throws, the trailing `println('PASSED')` is what
proves it got to the end — check for that marker rather than for absence of output.

Useful adapter references:

| Store | Adapter function reference |
|-------|---------------------------|
| H2 | `meta::relational::tests::pct::h2::testAdapterForRelationalWithH2Execution_Function_1__X_o_` |
| DuckDB | `meta::relational::tests::pct::duckDB::testAdapterForRelationalWithDuckDBExecution_Function_1__X_o_` |
| Snowflake | `meta::relational::tests::pct::snowflake::testAdapterForRelationalWithSnowflakeExecution_Function_1__X_o_` |

(Cloud adapters need credentials; H2 and DuckDB run locally.)

### 6b. Run a whole package with the test runner

Two steps: start a run, then poll for results.

```bash
curl -s -X POST http://127.0.0.1:9010/executeTests \
  -H 'Content-Type: application/json' \
  -d '{"openFiles":[],"extraParams":{"path":"meta::pure::functions::string::tests::plus","relevantTestsOnly":false}}'
```

`extraParams` accepts:

| Key | Type | Meaning |
|-----|------|---------|
| `path` | string | package to run; defaults to `::` (**everything** — avoid) |
| `filterPaths` | string[] | restrict to specific element paths |
| `relevantTestsOnly` | boolean | only tests in repos affected by modified files |
| `pctAdapter` | string | run only PCT tests, against this adapter |

The response includes `runnerId`, a `count`, and the discovered test tree:

```json
{"runnerId":1,"path":"meta::pure::functions::string::tests::plus","filterPaths":[],
 "relevantTestsOnly":false,"pctAdapter":"null","count":6,"tests":[ … ]}
```

Then poll with that id:

```bash
curl -s "http://127.0.0.1:9010/testRunnerCheck?testRunnerId=1"
```

```json
{"finished":true,"tests":[
  {"test":["meta","pure","functions","string","tests","plus","testPlus_Function_1__Boolean_1_"],
   "console":"","status":"Success"}, … ]}
```

**Reading these results.** Each element gives three things:

| Field | Shape | Notes |
|-------|-------|-------|
| `test` | array of path segments | join with `::` to get the FQN |
| `status` | `"Success"` or a failure status | the only field to branch on |
| `console` | JSON-escaped string | that test's captured output; **empty for passing tests** |

`console` is per-test and independent of the `text` field in [§5](#5-reading-the-console-output)
— a test run never populates `text`. It is empty on success, so an empty `console` is not a
signal of anything; check `status`. On failure it carries the assertion message and Pure
stack, in the same format as a runtime failure.

```python
failures = [t for t in results if t["status"] != "Success"]
for t in failures:
    print("::".join(t["test"]))
    print(t["console"])          # already decoded by json.loads — print, don't regex
```

> **`testRunnerCheck` is a draining read.** Each call returns only results *new since the
> last call*, and once it reports `finished:true` the runner is **discarded**. Polling the
> same id again returns `{"error":true,"text":"Unknown test runner: 1"}`. Accumulate results
> across polls yourself, and never re-poll after `finished`.

Poll on a modest interval (~1s) and accumulate until `finished:true`. `GET /testRunnerCancel?testRunnerId=N` stops a run.

Because `/executeTests` shares `saveFilesAndExecute`, its `openFiles` **also writes to
disk** — [§4](#4-warning-openfiles-overwrites-the-file-on-disk) applies equally.

---

## 7. Navigating the graph

These are all read-only and safe.

**Search source text** (the fastest way to locate a function):

```bash
curl -s "http://127.0.0.1:9010/findInSources?string=function%20go()&regex=false&max=20"
```

```json
[{"sourceId":"/welcome.pure","coordinates":[
  {"startLine":45,"startColumn":1,"endLine":45,"endColumn":13,
   "preview":{"before":"","found":"function go()","after":":Any[*]"}}]}]
```

Params: `string` (required), `regex`, `caseSensitive`, `sourceRegex` (limit to matching
source ids), `max`.

**Browse repositories** — `parameters` is a bare path, not a JSON array:

```bash
curl -s "http://127.0.0.1:9010/dir?parameters=/"            # ✅ repository roots
curl -s "http://127.0.0.1:9010/dir?parameters=/core_relational"
curl -s "http://127.0.0.1:9010/dir?parameters=%5B%22/%22%5D"  # ❌ 400 + Java stack trace
```

**Read a file** (remember the doubled slash):

```bash
curl -s "http://127.0.0.1:9010/fileAsJson//welcome.pure"
```

**Inspect an element:** `GET /getConceptInfo`, `POST /getConcept`.

---

## 8. Endpoint reference

Base URL `http://127.0.0.1:9010`. "Writes disk" flags the endpoints that mutate your working tree.

| Method | Path | Purpose | Writes disk |
|--------|------|---------|-------------|
| `POST` | `/executeGo` | save + delta-compile + run `go()` | **yes** (via `openFiles`) |
| `POST` | `/executeTests` | save + delta-compile + start a test run | **yes** (via `openFiles`) |
| `GET` | `/testRunnerCheck?testRunnerId=N` | drain new results for a run | no |
| `GET` | `/testRunnerCancel?testRunnerId=N` | cancel a run | no |
| `GET` | `/execute` | execute a named function by URL | no |
| `PUT` | `/updateSource` | line-level add/remove edits | **yes** |
| `GET` | `/fileAsJson/{path}` | read a source file | no |
| `GET` | `/dir?parameters={path}` | list a directory / repo roots | no |
| `POST` | `/newFile/{path}`, `/newFolder/{path}` | create | **yes** |
| `DELETE`| `/deleteFile/{path}` | delete | **yes** |
| `GET` | `/findInSources` | text/regex search across sources | no |
| `GET` | `/findPureFiles` | find files by name | no |
| `GET` | `/getConceptInfo`, `POST /getConcept` | element metadata | no |
| `POST` | `/suggestion/*` | completions (path, identifier, attribute, class, variable) | no |
| `GET` | `/initialize` | (re)initialise the runtime | no |
| `POST` | `/executeSaveAndReset` | save and reset the session | **yes** |
| `GET` | `/pureRuntimeOptions/getAllPureRuntimeOptions` | list runtime options | no |
| `GET` | `/pureRuntimeOptions/setPureRuntimeOption/{name}/{value}` | toggle a runtime option | no |
| `GET` | `/conceptsActivity`, `/executionActivity`, `/initializationActivity` | server activity | no |
| `POST` | `/debugging` | step-debugger control | no |

`PUT /updateSource` takes a list of `{path, line, column, message, add}` records and edits
by line number. Its remove path has a known bug (an unconditional throw), so **use
`/executeGo` with a full `code` payload instead**.

`POST /executeSaveAndReset` is heavier than its name suggests: it calls `pureRuntime.reset()`
**and `getCache().deleteCache()`** before saving `openFiles`. That discards the compiled
graph *and* the on-disk cache, so the next compile is a full one — minutes, not seconds.
Reach for it only when the runtime is genuinely wedged.

---

## 9. Recommended agent protocol

1. **Locate the IDE.** Read the port from `ideLightConfig.json`, confirm with `lsof`, and
   check `POST /executeGo` with `{"openFiles":[],"extraParams":{}}` returns JSON. Do not
   assume 9200.
2. **Snapshot every file you intend to touch** via `GET /fileAsJson/...`, in full, to a
   scratch location outside the repo. This is mandatory, not optional — see
   [§4](#4-warning-openfiles-overwrites-the-file-on-disk).
3. **Locate the code** with `findInSources` / `dir` / `fileAsJson`.
4. **Build the payload as a delta of the current file.** Fetch the live content with
   `fileAsJson`, insert a minimal probe into `go()` — smallest expression that exercises the
   behaviour, with a `println('PASSED')` sentinel at the end — and submit the *whole edited
   file*. Never compose the file from scratch; `code` replaces everything you omit.
5. **Submit** via `POST /executeGo` and parse the JSON response.
6. **Branch on the response shape** using the table in
   [§5](#decision-table): compile error → fix at `source:line:column`; runtime error → read
   `Full Stack:`; success → check `text` for your sentinel.
7. **Iterate.** Each round trip is seconds; prefer many small probes over one large one.
8. **Restore.** Push every snapshot back through `openFiles` so *both* disk and the
   in-memory runtime return to their original state. Verify by re-reading with
   `fileAsJson`.
9. **Confirm for real.** The IDE is interpreted mode. Before claiming a fix works, run the
   corresponding JUnit/PCT module with Maven.

---

## 10. Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Connection refused on 9200 | wrong port | read `ideLightConfig.json`; it is 9010 |
| `Please write a go function. Example: function go():Any[*]{print('ok');}` | no `go()` in the graph | add one to `/welcome.pure` |
| `Cannot get files for /scratch` + Java stack | `dir` path is not a real repository | call `/dir?parameters=/` to list valid roots |
| `Cannot get files for ["/"]` | you JSON-encoded `parameters` | pass a bare path: `parameters=/` |
| `System not initialized…` | startup compile still running | wait for initial compile to finish |
| `Unknown test runner: N` | results already drained, or run finished | start a new run; do not re-poll after `finished:true` |
| Fix works in IDE, fails in Maven | interpreted vs compiled mode divergence | reproduce with the real PCT/JUnit module |
| Restored the file but the error persists | you fixed disk only; the compiled graph is stale | resubmit the restored text via `openFiles` — see [§4](#4-warning-openfiles-overwrites-the-file-on-disk) |
| `fileAsJson` shows correct code but `go()` runs something else | disk and in-memory registry have diverged | resubmit via `openFiles`, or restart the IDE |
| `welcome.pure` clobbered, `git status` clean | it is gitignored; `openFiles` overwrote it | recover from your snapshot — git cannot help |
| imports / helper functions vanished after a run | you submitted a from-scratch `code` instead of a delta | always edit the text returned by `fileAsJson`; see the rule box in [§3](#3-the-delta-compile-loop-in-one-call) |
| `The variable 'x' is unknown!` / unresolved paths right after an edit | same cause — your rewrite dropped the file's `import` block | resubmit the full file with imports intact |

### If there is no snapshot

`welcome.pure` is gitignored and has no history. Recovery options, in order:

1. **The IDE browser tab.** If the file is still open in the UI, its editor buffer holds the
   pre-overwrite text. Copy it out *before* touching the tab.
2. **IntelliJ Local History** (`~/Library/Caches/JetBrains/<product>/LocalHistory`) — only if
   the file was opened in the IDE; content is not plain-text greppable.
3. **Time Machine / filesystem snapshots**, if enabled.

If all three fail, the content is gone. This is precisely why step 2 of
[§9](#9-recommended-agent-protocol) is non-negotiable.
