# Legend release repo matrix

Five repos, **four different release mechanisms**. Never assume the shape of a
release from one repo when working in another — look the repo up here first.

Checkouts live side by side under `/home/aziem/pure/`.

## Release order

Releases flow along the dependency graph, each hop consuming the previous one's
published version:

```
legend-shared → legend-pure → legend-engine → legend-sdlc → legend-depot
```

Version properties carrying those versions forward (in the consumer's root `pom.xml`):

| Repo | Properties it pins |
|---|---|
| `legend-shared` | none |
| `legend-pure` | none in root pom |
| `legend-engine` | `legend.pure.version`, `legend.shared.version`, `legend.pylegend.version` |
| `legend-sdlc` | `legend.engine.version`, `legend.pure.version`, `legend.shared.version` |
| `legend-depot` | `legend.shared.version`, `legend.engine.version`, `legend.sdlc.version` |

Bump with `mvn versions:set-property -Dproperty=<prop> -DnewVersion=<v>`.

## Per-repo facts

| Repo | groupId:artifactId | Release mechanism | `dryRun`? | Other inputs |
|---|---|---|---|---|
| `legend-shared` | `org.finos.legend.shared:legend-shared` | **No maven-release-plugin.** `-Drevision=<v>` build, manual `git tag`, then `sed` the next `<revision>` snapshot into `pom.xml` | ✗ | — |
| `legend-pure` | `org.finos.legend.pure:legend-pure` | `release:prepare` against a **pinned commit SHA**; separate `prepare` job validates and re-creates the release branch at the tested commit; has a concurrency group | ✓ | — |
| `legend-engine` | `org.finos.legend.engine:legend-engine` | `validate` gate → build (`release:prepare`) → full test matrix → **two-wave** Central bundle publish | ✓ | — |
| `legend-sdlc` | `org.finos.legend.sdlc:legend-sdlc` | Plain `release:prepare` + `release:perform`, no validation gate | ✗ | `logLevel` (choice) |
| `legend-depot` | `org.finos.legend.depot:legend-depot` | Plain `release:prepare` + `release:perform`, checks out `github.ref` | ✗ | — |

### Consequences that actually matter

- **`legend-shared` has no release-plugin commits.** A failed shared release
  leaves ONE commit (the `<revision>` bump) plus a tag — not the two commits
  every other repo leaves. `clean-after-failed-release.yml`'s `reset --hard HEAD~2`
  is wrong there.
- **`legend-pure` validates `releaseVersion >= current branch version`** and
  pins a SHA, so a re-run after a branch push behaves differently from the others.
- **`legend-engine` is the only one with a Central pre-check** (`validate` job:
  tag-free + not-on-Central). The other four fail at upload instead — so run
  `release-preflight` by hand there, it is the only gate they have.
- **`legend-sdlc` and `legend-depot` have no dry run.** Dispatch is for real.

## Naming conventions

- **Tag:** `<repo-name>-<version>`, e.g. `legend-engine-4.143.1`, `legend-shared-0.37.1`.
  Built in the workflows as `${{ github.event.repository.name }}-${{ inputs.releaseVersion }}`.
- **Release branch:** `release-legend-release-<STACK-VERSION>` — the *Legend stack*
  number, NOT the repo's own artifact version. `legend-engine` on
  `release-legend-release-4.100` carries engine version `4.143.x`; `legend-sdlc`
  on `release-legend-release-4.99` carries sdlc version `0.232.x`. Never derive
  one from the other.
- **Backport label:** `backport release-legend-release-<STACK-VERSION>`, created by
  `create-release-branch.yml`. Applying it to a merged master PR opens the backport PR.
- **Central coordinate URL:**
  `https://repo1.maven.org/maven2/<groupId with dots as slashes>/<artifactId>/<version>/<artifactId>-<version>.pom`
  A `200` means the version is published and **immutable** — that version number is burned.

## Dead workflows — do not use

- `legend-engine/.github/workflows/release-large-runner.yml` — last success
  2026-02-12, nothing but failures since; superseded by `release.yml`.
- `legend-stack-release.yml` (present in every repo, `repository_dispatch`-driven)
  — believed stale; the stack is sequenced by hand. Confirm with the release
  manager before ever triggering it.

## `clean-after-failed-release.yml` — know what it does before suggesting it

Present in all five repos. It deletes `git describe`'s **globally newest tag**
and runs `git reset --hard HEAD~2 && git push --force` on whatever ref it was
dispatched on. That is right only when the last two commits really are the
release-plugin pair and the newest tag really is this release's. It is wrong when:

- a backport squashed the release-plugin commits together with a fix (both land
  in one commit — resetting 2 deletes real work);
- the repo is `legend-shared` (one commit, not two);
- a newer tag exists anywhere in the repo.

Prefer the `release-cleanup` skill, which inspects before it acts.
