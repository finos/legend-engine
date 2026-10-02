# Isolating a Local Build from the Shared `~/.m2` Cache

All checkouts share one local repository, and every branch builds the same
`<version>-SNAPSHOT` coordinates. If two working copies are built at different times, whichever
installed last wins, and the other silently runs against the wrong jars. This is especially
dangerous for Pure modules, whose `.pure` sources are compiled to bytecode inside the installed
jar — a stale jar produces failures with no connection to anything you changed.

## Symptoms of a collision

- Tests failing in modules you never touched.
- A test count that changes between identical runs.
- A failure that disappears after rebuilding an unrelated module.

Suspect the cache before you suspect your change.

## Stamp the build with `-Drevision`

The reactor uses Maven CI-friendly versions — a single `<revision>` property in the root
`pom.xml`, which every module inherits — so overriding it on the command line gives the whole
build its own coordinates without editing a single file:

```bash
mvn clean install -DskipTests -Drevision=$(git rev-parse --abbrev-ref HEAD)-SNAPSHOT
mvn clean test -pl <module>  -Drevision=$(git rev-parse --abbrev-ref HEAD)-SNAPSHOT
```

Installs land under those coordinates and cannot collide with another checkout. Pass the same
`-Drevision` on **every** command in the session, tests included — one that omits it silently
builds and resolves the shared `4.x.y-SNAPSHOT` coordinates again. Never commit a changed
`<revision>`.

Do **not** isolate with `versions:set` — it rewrites every `pom.xml` in the reactor, which buries
the real diff and can be committed by accident. `-Drevision` achieves the same thing with no
edits.

## When you must not write to `~/.m2` at all

Someone else's build is running concurrently, or the shared repository already holds jars from
another branch you do not want to read: add a chained local repository as well. Maven 3.9+ writes
to the **head** and reads through to a read-only **tail**:

```bash
HEAD=/tmp/m2-$(git rev-parse --abbrev-ref HEAD)
mvn clean install -DskipTests -pl <changed-modules> \
    -Dmaven.repo.local=$HEAD -Dmaven.repo.local.tail=$HOME/.m2/repository
```

Nothing is written to `~/.m2`, and anything you did not build resolves from it as usual, so there
is nothing to re-download and `-o` still works. Naming only the changed modules is enough when the
tail's engine jars match your branch; when they may not, add `-am` so every engine-internal
artifact is built from source and only third-party dependencies come from the tail. Delete `$HEAD`
when done.
