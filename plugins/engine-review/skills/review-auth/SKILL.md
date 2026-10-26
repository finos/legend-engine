---
name: review-auth
description: "Authentication and connectivity lens of a legend-engine impact review: static analysis of changes to pac4j/identity derivation, credential providers and vaults, relational JDBC connectivity (ConnectionManagerSelector, DataSourceSpecification keys, ConnectionStateManager pool naming, AuthenticationStrategy implementations, authentication flows), other identity-bearing stores and server/session config, producing severity-ranked findings with file:line evidence. Use when a change touches Identity, Subject/doAs, credentials, vault lookups, connection pools, authentication strategies or flows, pac4j config, or new HTTP endpoints, or when asked whether a change is safe from an auth or security standpoint."
---

# Auth and connectivity lens

This lens is static. EMIT runs against H2 with `DefaultH2` auth, so it cannot observe identity
behaviour. The output is a list of findings, each with evidence.

## Procedure

1. **Collect the hunks.** `engine-review scope --base <base> --head <head>` lists the files in the
   auth lens. A file matched on content ("changed lines mention Credential, doAs") counts too. Read
   each hunk with enough surrounding code to know which identity is in scope: the request's, the
   pool's, or the service's.
2. **Read `references/auth-checklist.md`** in full the first time. It explains how identity flows
   from pac4j to the JDBC pool, and why the pool name is the isolation boundary.
3. **Apply checklist sections A–H** to every hunk. Answer each applicable question with evidence
   (`file:line` and the code), not with an impression. Mark sections that do not apply as N/A in the
   review so readers can see they were considered.
4. **Trace beyond the hunk.** For a changed method, find its callers. A harmless-looking change to
   `Identity.getName()`, a key's `equals`, or a credential's `id()` propagates into pool naming,
   caches and `authId`. Follow it to where the value is consumed.
5. **Separate new from existing.** If the risky pattern already exists on base, say so. The PR may
   extend it, or merely sit next to it. Pre-existing issues go in a separate "existing risk"
   section, not in the PR's findings.
6. **Ask for tests where the checklist names one** (pool separation per user, fail-closed on a
   credential error, no secrets in logs, endpoint authorization) and none exists.
7. **Severity** follows the checklist: blocker, major or minor.

Never paste a secret, token or real credential value into the review. Describe where it would leak,
not what it is.
