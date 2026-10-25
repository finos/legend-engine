# Auth and connectivity review checklist

Static review of changes that decide **who a query runs as** and **which credential reaches a
store**. Anchors are class and method names, not line numbers. Read the current code before you
cite it.

## How identity flows (read this first)

1. **pac4j → profiles.** `LegendPac4jBundle` (legend-shared, external jar) authenticates the
   request. HTTP APIs take `@Pac4JProfileManager` and call `ProfileManagerHelper.extractProfiles`,
   which fails closed on non-`CommonProfile` profiles.
2. **Profiles → `Identity`.** `Identity.makeIdentity(profiles)` runs every ServiceLoader
   `IdentityFactory` (registered only in `legend-engine-xts-identity/*-kerberos` and `*-pac4j`).
   - Only Kerberos profiles get a real name.
   - Every other profile (OIDC, Anonymous) becomes `_UNKNOWN_`.
   - `CurrentUser` and `ApplicationQuery` read the pac4j profile id instead. So the user the UI
     shows and the user execution runs as can differ.
3. **`Identity` → execution.** `PlanExecutor.execute(..., identity, ...)` builds the
   `ExecutionNodeExecutor`.
   - Overloads without an identity default to the anonymous identity.
   - `PlanExecutor.setUpState` sets `authId` from the plan's `kerberos` field when `authDependent`
     is true. The plan may have been posted by the client (`/executePlan`), and `authId` feeds
     `currentUserId()`.
4. **`Identity` → JDBC connection.**
   - `ConnectionManagerSelector.getDatabaseConnection` → `RelationalConnectionManager.getCredential`
     (flow lookup in `DatabaseAuthenticationFlowProviderSelector`) → `DataSourceSpecification.getConnectionUsingIdentity`.
   - **Pool key.** `ConnectionStateManager` names pools
     `DBPool_<ConnectionKey.shortId()>_<identity.getName()>_<credential class simple names>`. The
     credential *value* (`Credential.id()`) is included only when
     `org.finos.legend.engine.execution.connectionPoolNameIncludesCredentialId` is set.
   - `ConnectionKey` = datasource key + auth-strategy key, with no identity in `equals`/`hashCode`.
   - Each pool keeps an `IdentityState`. Strategies such as `DelegatedKerberosAuthenticationStrategy`
     take the subject from the **pool's** state, not from the request.
5. **Credentials.**
   - `CredentialProviderProvider` → `CredentialProvider.makeCredential` short-circuits when the
     identity already holds a credential of the output type. Otherwise it applies the first matching
     `IntermediationRule`.
   - Vault lookups (`CredentialVault`, legacy `Vault.INSTANCE`) resolve **secret names chosen by
     the model**.
   - The server wires only the private-key provider by default (`Server.java`).

## Checklist: apply to every changed auth-relevant hunk

### A. Identity derivation and isolation
- [ ] Does the change alter `Identity.getName()` for any population: realm stripping in
      `KerberosSubjectIdentityFactory`, `_UNKNOWN_`, the shared `ANONYMOUS_IDENTITY`, `setName`, or
      the Postgres wire server's `AnonymousIdentityProvider` (client-supplied name)? The name is
      the pool-isolation key, so two users who share a name share pools.
- [ ] A new `Credential` type: does it override `id()`? Is it a distinct class? If neither, two
      different secrets for the same user name share a pool.
- [ ] A new or changed `AuthenticationStrategy.handleConnection` / `getConnectionUsingKerberos`:
      does it read the subject or credential from the request identity, or from the pool's
      `IdentityState`? Hikari refill threads connect with whatever the pool holds.
- [ ] New identity-bearing input from the client (plan fields, headers, query params) that feeds
      `authId`, `USER_ID`/`currentUserId()`, pool names or credential lookup?

### B. Keys, caches and equality
- [ ] A new or changed `*Key` (`DataSourceSpecificationKey`, `AuthenticationStrategyKey`
      subclasses): are `equals`/`hashCode`/`shortId` complete and deterministic? Missing fields
      merge pools. Non-deterministic ones, like `System.nanoTime()` in
      `GCPApplicationDefaultCredentialsAuthenticationStrategyKey`, leak a pool per call.
- [ ] A static cache (`ModelManager`, plan caches, `GraphQLPlanCache`, `SubjectCache`,
      `*.INSTANCE` singletons): is anything identity-dependent stored under a key that has no
      identity?
- [ ] Does `shortId()` or the pool name now carry a secret (vault value rather than reference)?
      It ends up in logs, metrics and `/executorInfo`.

### C. Secrets in logs, errors and serialization
- [ ] `LOGGER.*` / `LogInfo` / span logs / exception messages that include a `Credential`,
      `Subject`, `DataSourceSpecification`, JDBC URL, `Properties`, token, password or
      vault-resolved value? `Subject.toString()` can include private credentials.
- [ ] A new public getter on a credential or strategy, or a POJO served by an API
      (`ConnectionStateManagerPOJO`, `/executorInfo`, `/sessionInfo`): would Jackson now serialize
      a secret?
- [ ] `toString()` added or changed on a credential, spec or key.

### D. Fallbacks and swallowed failures (fail open vs fail closed)
- [ ] A `catch` that turns a credential or authorization failure into a degraded success. The
      pattern to look for is `ServiceStoreConnectionProvider`, which logs to the span and returns
      an unauthenticated client. New code should throw (`EngineException`, `USER_CREDENTIALS_ERROR`).
- [ ] A new fallback to a service or engine identity: `AWSDefaultCredentials`, GCP application
      default, H2 `sa`, legacy `Vault.INSTANCE`, `systemIdentitySupplier`, anonymous identity
      defaults in `PlanExecutor`, or `Subject.doAs(null, …)` via `KerberosUtils.doAs` when the
      identity has no Kerberos credential.
- [ ] `ExecSubject.exec` ignores the requested subject if one is already on the thread. Does the
      change nest `doAs` calls or run user work on a thread that already carries the service
      subject?
- [ ] A missing flow or provider that is only `warn`-logged (`RelationalConnectionManager.getCredential`).

### E. Model-controlled secret resolution (confused deputy)
- [ ] A new `AuthenticationSpecification`, `CredentialVaultSecret`, `IntermediationRule` or
      `DatabaseAuthenticationFlow` that resolves a secret, role, service account or host **named by
      the user's model**. Is there an allowlist or a binding to the caller's identity? The pattern to
      follow is `PureServerLoader`'s `allowedOverrideUrls`.
- [ ] Does `EnvironmentCredentialVault` / `SystemPropertiesCredentialVault` become reachable from
      a server path (they are in `CredentialProviderProvider.defaultProviderProvider()`)?

### F. Test and demo code reachable in production
- [ ] New code in `src/main` named `*ForTest*`, `Test*`, `Demo*`, `InMemory*Vault*`, or H2 test
      flows registered in a default provider (`LegendDefaultDatabaseAuthenticationFlowProvider`).
- [ ] Config changes: `AnonymousClient`, `bypassPaths`, `deployment.mode`, CORS
      `allowedOrigins`/credentials, `sessionCookie`, `trustedPackages`.
- [ ] Stubs that always succeed (e.g. a MAC or signature verification that returns without
      checking).

### G. Authorization of new endpoints
- [ ] A new JAX-RS resource or method: does it take `@Pac4JProfileManager` and act **as the
      caller**? Does it expose other users' data (sessions, pools, queries) or mutate shared state
      (pool eviction, cache flush) without an authorization check?
- [ ] Do `ExecutePlan*` / middle-tier paths still reach `PlanExecutionAuthorizer`? Note that
      `Server` registers them with no authorizer by default.

### H. Other identity-bearing stores
Service store (`ServiceStoreConnectionProvider`, `ServiceStoreExecutionExtension`), MongoDB
(`MongoDBStoreConnectionProvider`), Elasticsearch (`ElasticsearchHttpContextUtil`, `AuthScope.ANY`),
function activators (`*DeploymentManager.publishToSandbox/deploy`), hosted service, and the shared
SPNEGO `HttpClientBuilder` (`AuthScope.ANY_HOST` + credential delegation). Apply A–E to them.

## Tests a change like this should come with

- Different users must get different pools for delegated Kerberos. No test asserts this today; ask
  for one when a change touches pool naming or `IdentityState`.
- A credential failure must fail closed rather than degrade.
- No secret appears in log output. Capture with a test appender.
- A new endpoint rejects or scopes cross-user access.

Existing anchors: `TestConnectionStateManager`, `TestConnectionStateManagerRace`, `TestPoolName`,
`DelegatedKerberosAuthenticationStrategyTest`, `TestCredentialProviderEdgeCases`,
`TestRelationalMiddleTierPlanExecutionAuthorizer`, `TestProfileManagerHelper`.

## Severity

- **blocker**: cross-user credential or pool sharing; client-controlled identity; secret in a log
  or response; fail-open on authentication or authorization.
- **major**: new silent fallback to a service identity; model-controlled secret resolution without
  an allowlist; test code wired into a default provider.
- **minor**: missing tests for the above; non-deterministic keys; logging of non-secret
  connection metadata.
