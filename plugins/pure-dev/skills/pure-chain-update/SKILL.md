---
name: pure-chain-update
description: "Procedure for git-syncing and rebuilding a local legend-pure + legend-engine checkout, in dependency order, with pinned dependency versions resynced. Run by hand, one project at a time, fixing problems as they surface. Use for 'update my project chain', 'pull latest and rebuild <project>', 'sync legend-pure/legend-engine', 'refresh my local checkouts', or 'propagate my engine snapshot downstream'."
---

# Update a local legend-pure + legend-engine checkout

Run this by hand, one project at a time. There is no script: the chain breaks in a different place
every time — a conflict, a version pin that didn't take, a SNAPSHOT nobody published — and each
needs a judgment call. Automating it produced a run that reported success while leaving the real
problem buried in a `build.log`.

Work the steps below per project, in dependency order, and **stop at the first thing that looks
wrong** rather than pressing on to the next project.

## The chain

```
legend-pure  ←  legend-engine
```

Same DAG as `config/projects.json` (`pure-lsp-roots --list` prints it with your checkout paths).
Build upstream first.

**Which projects to rebuild.** If you changed `legend-pure`, you must rebuild it *and*
`legend-engine` afterwards — each layer copies Pure sources and generated Java into its own jars, so
an upstream change stays invisible downstream until the dependent is rebuilt. A change to
`legend-engine` only requires rebuilding `legend-engine` itself.

Checkout paths resolve as everywhere else in this plugin: `$<rootEnv>` (`$LEGEND_PURE_ROOT`,
`$LEGEND_ENGINE_ROOT`) if set, else `$HOME/<root>` from the registry.

## Per project, in order

### 1. Sync git

Check for a rebase or merge already in progress first (`git -C <path> status`) — one left over from
a previous attempt means an earlier conflict was never resolved; finish or abort it before
continuing.

If the tree is dirty, commit a checkpoint rather than stashing — it survives a rebase cleanly and
is trivial to back out with `git reset --soft HEAD~1`:

```bash
git -C <path> add -A && git -C <path> commit -m "WIP: temp commit before chain sync"
```

**Resolve the default branch; do not assume `master`.** legend-pure and legend-engine
use `finos-master`, while `origin` *also* carries an unrelated stale `master` — rebasing onto the
wrong one is silently destructive:

```bash
git -C <path> symbolic-ref --quiet --short refs/remotes/origin/HEAD   # e.g. origin/finos-master
```

Then, on that branch: `git -C <path> pull --rebase` (not a bare `git pull`, which errors on
divergent branches unless `pull.rebase` is configured). On any other branch:
`git -C <path> fetch origin <default>:<default> && git -C <path> rebase <default>`.

### 2. Resync the version pin

`legend-engine` pins its `legend-pure` dependency as a fixed `pom.xml` property
(`legend.pure.version`), so a plain `git pull` only picks up whatever was last pinned *upstream* —
which lags your local snapshot. Read the actual current version out of your `legend-pure` checkout's
own `pom.xml` and rewrite `legend-engine`'s pin to match. `legend-pure` is the root of the chain and
pins nothing.

Most root poms here use Maven's CI-friendly `<version>${revision}</version>` with a `<revision>`
property, so resolve that rather than taking the literal placeholder.

A pin only works if `legend-pure` is already `mvn install`ed into `~/.m2` — i.e. you built it
earlier in this same pass. Otherwise the build fails resolving a SNAPSHOT no repository has.

### 3. Build

One project at a time. **Never two Maven reactor builds at once** — this box cannot take it.

```bash
cd <path> && mvn -T <threads> clean install -DskipTests
```

`clean install`, not plain `install`: these Pure-source modules fail with
`IllegalStateException: The code repository <name> already exists!` against a stale `target/classes`.

**JDK**: JDK 11 for both projects. Don't rely on ambient `$JAVA_HOME` being right; a non-login shell
often has not run the JDK setup.

**Resources** (tuned for an 8-core/64GB box, one build at a time). Heap goes in the project's
`.mvn/jvm.config` as `-Xmx<n>G`, or pass `MAVEN_OPTS` instead to avoid touching a tracked file:

| Project         | `-T` | Heap |
|-----------------|-----:|-----:|
| `legend-pure`   |    4 | 12G  |
| `legend-engine` |    4 | 32G  |

Watch the build to a real `BUILD SUCCESS`/`BUILD FAILURE` before starting the next project. If you
background it, tail the log — don't infer success from the process exiting.

## Conflicts: the judgment call

This is the step that can't be mechanised. The working rule for **trivial** (resolve inline and
continue) vs **concerning** (abort and hand back to the user):

> Both sides changed the same *location* but not the same *behaviour* — whitespace or formatting
> drift, a version-property bump colliding with an unrelated edit two lines away, one side adding
> an adjacent function git couldn't 3-way-merge. That's trivial: resolve it and continue.
>
> A concrete "concerning" case hit in practice: upstream `master` had reworked a function's
> signature (dropped a parameter, added a sibling function using the old one) while the local
> branch's own commit message said it was *also* restructuring that function's parameters as part
> of a larger refactor. Both sides intentionally changed the same behaviour in different,
> not-obviously-reconcilable ways. Resolving that requires knowing which intent should win — the
> user's call, not a guess from the diff.

Trivial: fix the files, `git add`, `git rebase --continue`, carry on. Concerning:
`git rebase --abort`, stop, and tell the user what conflicted and why it looked like a real
disagreement.

## When the rebuild doesn't take

Failures in this chain are runtime and misleading, not compile-time. The two shapes:

- `Invalid URLs for '/<path>.pure' - different content: [jar:a, jar:b]` — two jars carry the same
  Pure file with different content.
- `AbstractMethodError: Root_meta_..._Impl does not define ..._someProperty(...)` — stale generated
  Java against a fresh interface, typical after a Pure property/type rename.

Both name the culprit. Don't rebuild blindly — intersect the real classpath with the jars holding
that class and compare mtimes:

```bash
mvn -q dependency:build-classpath -pl <module> -Dmdep.outputFile=/tmp/cp.txt -DincludeScope=test
tr ':' '\n' < /tmp/cp.txt | while read -r j; do
  [ -f "$j" ] && unzip -l "$j" 2>/dev/null | grep -q "<Class>.class" && echo "$(stat -c %y "$j" | cut -c1-16) $j"
done | sort
```

A commonly-missed rebuild target after a **legend-pure** `platform`/`platform_store_relational`
change — the Java-generation mirrors carry the same `Root_meta_*_Impl` classes — is legend-engine's
`legend-engine-pure-platform-java` and `legend-engine-pure-platform-store-relational-java` (under
`legend-engine-core-pure/legend-engine-pure-platform-modular-generation/`).

After rebuilding, a running LSP daemon still holds the old jars in memory — restart it
(`pure-lsp-restart`, or relaunch via `pure-lsp-launch`) and refresh the cached classpath with
`pure-lsp-classpath <project> --force`.
