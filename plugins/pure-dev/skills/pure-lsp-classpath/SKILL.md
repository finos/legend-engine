---
name: pure-lsp-classpath
description: "Computes and caches the engine-scale Pure classpath (via Maven dependency:build-classpath) for any pure-bearing project registered in config/projects.json - legend-pure, legend-engine, or a project you've layered on top via PURE_DEV_EXTRA_PROJECTS_JSON - and resolves that project's dependency-ordered --repo-root set. Use before the first engine-scale LSP start in a repo, when the cached classpath is stale after pulling or rebuilding, when engine-scale boot fails on missing classpath entries, or when you need repo-roots for a project with dependencies. Provides pure-lsp-classpath and pure-lsp-roots."
---

# Compute the classpath + repo-roots for any Pure project

The engine-scale LSP daemon needs (a) the full **classpath** of jars for the project you're working
in plus everything it depends on, and (b) the right **`--repo-root`s** so your edits are read from
source. Both are machine- and project-specific, so nothing can be a shipped file. This skill computes
and caches them for **any** pure-bearing project, respecting the dependency graph between them.

## The project registry (dependency DAG)

`config/projects.json` declares the pure-bearing projects and their `dependsOn` edges. Current graph:

```
legend-pure  ←  legend-engine
```

- `legend-pure` (base, platform*), `legend-engine` (core*, → legend-pure)

`pure-lsp-classpath --list` / `pure-lsp-roots --list` prints the registry + your checkout paths.
Add or adjust projects by editing `config/projects.json` (`name`, `root`/`rootEnv`, `dependsOn`,
`module`), or layer extra projects on top without editing it via `PURE_DEV_EXTRA_PROJECTS_JSON`
(`os.pathsep`-separated list of files shaped like `config/projects.json`; an extra entry's
`dependsOn` may reference `legend-engine`/`legend-pure` by name).

## Two helpers

```bash
# Classpath (compute-if-stale, cached per project under ~/.cache/pure-dev/):
pure-lsp-classpath [project]            # print cache path, computing if missing/stale
pure-lsp-classpath [project] --force    # force recompute
pure-lsp-classpath [project] --path     # print cache path only (never compute)
pure-lsp-classpath [project] --module M # override the Maven module to build from

# Repo-roots (dependency-ordered):
pure-lsp-roots [project]                # DEFAULT: --repo-root for the FULL transitive dep chain
                                         #   (target + everything it depends on - edit anywhere)
pure-lsp-roots [project] --source X     # EXACT custom set instead of the default (repeatable;
                                         #   target is always included) - everything else in the
                                         #   chain then resolves from the classpath jars, e.g.
                                         #   `pure-lsp-roots legend-engine --source legend-engine`
                                         #   source-roots only legend-engine, leaving legend-pure to
                                         #   load from jars.
pure-lsp-roots [project] --all-source   # no-op alias for the default (kept for explicitness)
pure-lsp-roots [project] --json         # root paths as a JSON array
```

`project` defaults to `$PURE_DEV_PROJECT`, else the project inferred from your current directory,
else you pass it explicitly. Checkout paths come from each project's `rootEnv` (e.g.
`$LEGEND_ENGINE_ROOT`) or `$HOME/<root>`.

## How the classpath is chosen (respecting the DAG)

The classpath is built from the **highest project in the target's dependency set that has a classpath
module** — its `pure-ide-light` (for legend-engine, `legend-engine-pure-ide-light-http-server`). That
one module's runtime classpath already contains every dependency-repo jar below it, so it covers the
whole stack: `pure-lsp-classpath legend-engine` builds from legend-engine's `pure-ide-light` module
(covers legend-engine + legend-pure jars). A project layered in via `PURE_DEV_EXTRA_PROJECTS_JSON`
with no classpath module of its own falls back the same way, to the highest dependency that has one
(then you source-root the extra project for your edits).

Caching: per-project cache `~/.cache/pure-dev/classpath-<project>.txt` + a `.stamp` (project git
HEAD + module pom mtime). Reused until the checkout changes; recomputed on `--force` or staleness.
`-o` (offline) is tried first, retried online if needed. `dependency:build-classpath` output is the
`os.pathsep`-joined jar list `--classpath-file` expects.

## Launching the LSP for a project (the whole point)

```bash
# Work in legend-engine: DEFAULT already source-roots both legend-engine and legend-pure below it -
# no flag needed, edit anywhere in the stack:
pure-lsp-server $(pure-lsp-roots legend-engine) --classpath-file "$(pure-lsp-classpath legend-engine)" \
    --port 8991 --socket-port 9100 --jvm-arg=-Dlegend.test.server.* ...

# Narrow to JUST legend-engine (legend-pure resolves from the classpath jars instead):
pure-lsp-server $(pure-lsp-roots legend-engine --source legend-engine) \
    --classpath-file "$(pure-lsp-classpath legend-engine)" --port 8991 ...
```

`--repo-root` overrides which `.pure` is read from source; the `--classpath-file` still supplies the
JVM's Java + every dependency-repo jar you are NOT source-rooting. (See
`references/lsp-devloop-internals.md` for why both are needed.)

## When to recompute

- After `git pull` in the project or a dependency (the stamp catches most).
- After `mvn clean install` that bumps a `-SNAPSHOT` version (jar paths change).
- On a new machine (first call builds it; expect a Maven dependency-resolve pass).
- If boot fails with "classpath file contains a missing entry" → `--force`.

## Notes

- First compute needs the classpath module resolvable (a prior `mvn install` of the project, which
  the dev setup already does). It resolves the dependency classpath; it does not build code.
- Caches are per-machine and never committed. This replaces the deprecated shipped
  `pure-ide-classpath.txt`.
