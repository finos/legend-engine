---
name: release-train
description: Sequence a full Legend stack release across repos — shared, pure, engine, sdlc, depot — in dependency order, bumping each consumer's version properties as the previous one lands on Central. Use for "release the stack", "do the full release train", "release everything for 4.101", "what's next in the train".
---

# Legend stack release train

Releasing the whole stack in dependency order. Each repo consumes the versions
published by the ones before it, so the order is not negotiable and each hop
waits for the previous one to be **readable on Central** — not merely "published".

```
legend-shared → legend-pure → legend-engine → legend-sdlc → legend-depot
```

Checkouts live side by side under `/home/aziem/pure/`. Read
`.claude/skills/release-preflight/references/repo-matrix.md` — the five repos do
not share a release mechanism, and this skill drives all five.

`legend-stack-release.yml` exists in every repo and is believed stale. The train
is sequenced by hand, one `release.yml` dispatch per repo. Do not trigger the
stack workflow without asking.

## Per hop

For each repo in order:

1. **Bump the version properties** it pins to the versions just released
   upstream. From the matrix:
   - `legend-engine`: `legend.pure.version`, `legend.shared.version`
   - `legend-sdlc`: `legend.engine.version`, `legend.pure.version`, `legend.shared.version`
   - `legend-depot`: `legend.shared.version`, `legend.engine.version`, `legend.sdlc.version`
   ```bash
   mvn versions:set-property -Dproperty=<prop> -DnewVersion=<v> -DgenerateBackupPoms=false
   ```
   Commit and push this **before** dispatching — the release builds what is on
   the branch. Confirm the push, as with any mutation.
2. **Preflight** — run `release-preflight` for that repo and version.
3. **Dispatch** — run `release-dispatch`. Confirm each dispatch separately; do
   not batch-approve the whole train up front.
4. **Wait for readable on Central**, not just green CI. The next repo's build
   resolves the artifact, and Portal-published is not the same as readable on
   repo1:
   ```bash
   curl -s -o /dev/null -w '%{http_code}' \
     "https://repo1.maven.org/maven2/<group path>/<artifact>/<version>/<artifact>-<version>.pom"
   ```
   Poll until `200`. Only then start the next hop.
5. **Verify** — run `post-release-verify` before moving on. A half-published repo
   poisons every hop after it.

## Version numbering

Each repo has its **own independent artifact version** — engine `4.143.x`, sdlc
`0.232.x`, depot `2.96.x`, pure `5.97.x`, shared `0.37.x`. The release *branch*
name carries the Legend **stack** number (`release-legend-release-4.100`), which
is unrelated to any of them. Never derive one from the other; read each repo's
root pom for its current version and increment the patch unless told otherwise.

## Failure mid-train

Stop the train. A failed hop means every later repo would pin a version that does
not exist. Triage (`release-triage`), clean up if the release left a tag or
version commits (`release-cleanup`), fix, then resume from the failed repo — the
hops that already succeeded are published and immutable, so never re-run them
with the same version.

## Reporting

Keep a running summary: which repos are done and at what version, which is in
flight, which are pending. A stack release spans many hours and the user will
lose the thread otherwise.
