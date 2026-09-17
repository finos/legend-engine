---
name: post-release-verify
description: Verify a Legend release actually completed — artifacts readable on Central, tag correct, branch left at the right next SNAPSHOT, docker images pushed. Use after a release run goes green, or for "did the release work", "check the release landed", "verify 4.143.1".
---

# Post-release verification

A green pipeline is not proof the release landed. Publishing to the Central
Portal is not the same as being readable on repo1, and the branch can be left in
a state that breaks the *next* release even when this one succeeded.

Read `.claude/skills/release-preflight/references/repo-matrix.md` for coordinates
and naming.

## Checks

1. **Artifacts readable on Central** — the one that actually matters, because
   downstream repos resolve against repo1:
   ```bash
   curl -s -o /dev/null -w '%{http_code}' \
     "https://repo1.maven.org/maven2/<group path>/<artifact>/<version>/<artifact>-<version>.pom"
   ```
   `200` on the root coordinate. For legend-engine, which publishes in two waves,
   spot-check a coordinate from each wave — wave 1 landing while wave 2 failed is
   a real and previously-seen outcome. If the root is `404` while CI was green,
   the publish job may still be propagating; re-check before raising it.
2. **Tag exists and points where it should:**
   ```bash
   git fetch --tags
   git rev-list -n1 <repo>-<version>
   ```
   It should be the release-plugin's "prepare release" commit (or, for
   `legend-shared`, the released commit itself).
3. **Branch left at the next SNAPSHOT** — after a successful release the branch
   should sit one patch above what was released, e.g. `4.143.2-SNAPSHOT` after
   releasing `4.143.1`. If it does not, the next release will start from the
   wrong version. For `legend-shared`, check `<revision>`.
4. **Local checkout in sync** — `git fetch && git status -sb`. The release pushed
   commits and a tag; a stale local checkout is how the *next* release goes wrong.
5. **Docker images** — repos building under the `docker` profile
   (`legend-engine`, `legend-sdlc`, `legend-depot`, `legend-shared`) push images
   to the `finos` org. Confirm the tag for this version exists if the user cares
   about the image, and say plainly that you checked only the ones you could see.
6. **The run itself** — `gh run view <run-id>`: confirm every job passed, and
   that the publish job actually ran rather than being skipped (a dry run skips
   it while still going green).

## Report

State what passed and what did not, with the observed values. If everything
landed, say so plainly and give the released version and tag. If the release is
part of a stack train, confirm it is safe for the next hop to start — that is
the question `release-train` is waiting on.
