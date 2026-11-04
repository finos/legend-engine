# GraphQL Store

GraphQL Store lets a single GraphQL endpoint act as a Legend Store: a Pure `Class` model is generated from the endpoint's own introspection schema, `graphFetch` queries against that model are translated into outbound GraphQL requests, and the JSON response is deserialized back into typed Pure objects.

## Where does GraphQL Store fit in Legend/PURE modeling?

GraphQL Store is a *type* of store that fits in the store layer, alongside a relational (database) store and Service Store. Legend Engine is always the GraphQL *client* here, issuing outbound requests to a third-party endpoint — it is not a GraphQL server.

## Use cases

GraphQL Store should be used when users want to query an existing GraphQL API and model it as a Store in Legend. GraphQL Store allows users to:

- Query data from a GraphQL API without hand-authoring a parallel data model — the model, mapping, and binding are generated from the endpoint's own introspection schema.
- Enrich data from a GraphQL API by merging it with data fetched from another API/store (e.g. enriching with relational database data) using cross-store joins.

## Restrictions

- GraphQL Store only works with graph fetch queries (`graphFetch()->serialize()`); there is no push-down filtering support.
- `filter()` on a GraphQL-routed fetch is rejected at plan-generation time with an explicit error, not silently ignored — apply filtering via the GraphQL field's own arguments (server-side) instead.
- Only one top-level query is supported per `graphFetch`. Two independent top-level fields that each bind a *literal* argument to a schema argument of the same name (e.g. both declare `limit`) can collide on the generated GraphQL variable name — fails loudly if the literal values differ, silently succeeds if they happen to match. Bind both calls to the same `$variable` instead of two independent literals to avoid this; a shared `$variable` used by multiple properties, or multiple `subType()` branches on one field, is correctly deduped to a single GraphQL variable.
- A polymorphic field's argument must be the same `$variable` in every `subType()` branch, not a repeated literal — two independently-parsed literals (e.g. two separate `7`s) are not recognized as "the same call" and fail plan generation.
- Every declared argument must be supplied at every call site; there is no "nullable arguments may be omitted" shorthand. Pass `[]` (or an empty literal) to exclude an optional argument from the outgoing request. The one exception: a *required* (non-null) list argument's `[]` is still sent as an explicit, empty value, not dropped.
- Binding a whole list argument to one top-level `$variable` (e.g. `employeesByBaseFilter($filters)`) fails at execution time, even though the equivalent literal list syntax works. Root cause is a Java-codegen collision in the generated input-object class, not specific to this store. Use the literal (or per-field `$variable`-embedded instance-creation) syntax instead.
- A `$variable`-bound Enum argument fails at execution time, both at the query root and nested inside an inline instance-creation argument. A *literal* Enum value always works in both positions — use literals for Enum arguments until this is fixed.
- No cross-field query batching: a cross-instance join (`~joinAccessor`) issues one HTTP call per parent object. Fetching N related objects costs N round trips to the GraphQL endpoint.
- A bare, unaliased qualified-property call with no nested selection (e.g. `greet($key)` where `greet` returns a plain scalar, not an object) renders a malformed result key and, for a scalar return type specifically, comes back `null`, even though the same call with a nested selection (`greet($key){message}`) returns correct data. Not triggered by any realistic query shape in practice — known gap, tracked.
- Comments and `"""docstrings"""` on GraphQL types/enums don't survive a grammar round-trip (the schema is a structured object, not a raw string). Field names and types are unaffected — cosmetic/documentation loss only.
- Only GraphQL `query` operations are supported. Mutations, directives, and fragments are out of scope — none are generated or supported in hand-authored queries.
- **Overloaded qualified properties (same name, different argument count/signature) can't arise from generation** — a GraphQL schema can't declare two fields with the same name in one type, so every generated QP name is already unique. Hand-adding an overload on top of a generated class (eg. a shorthand that omits some of the real field's own nullable arguments) is safe for independent call sites: each is resolved by Pure's own overload resolution to the specific signature before GraphQL Store's code ever sees it, so argument rendering is correct per call site with no shared, name-based lookup to conflate them. Two restrictions remain: (1) requesting two different overloads of the same property as siblings in one `graphFetch` (there's no single wire field shape to render for both at once) fails loudly at plan-generation time naming the conflict, rather than silently picking one; (2) `~joinAccessor` has no syntax to disambiguate an overloaded property by argument count/type, so naming an overloaded property there fails loudly at compile time — rename one of the overloads if you need it as a join accessor.

## GraphQL Store metamodel and concepts

A single `GraphQLStore` definition points at one GraphQL endpoint and carries that endpoint's schema (as introspected SDL). GraphQL APIs hosted by different servers should be modeled as separate `GraphQLStore`s.

### Structure

- **GraphQLStore**: modeled with one property:
    - `schema`: a structured SDL document (`type`/`input`/`enum`/`interface` declarations), not a free-form string — round-trips through the grammar composer. You rarely hand-write it; see [Generating the model from an introspection](#generating-the-model-from-an-introspection) below.
- **GraphQLConnection**: one-to-one with a `GraphQLStore`. Modeled with:
    - `store`: reference to the `GraphQLStore`.
    - `url`: the endpoint's URL.
    - `authentication`: a generic, store-agnostic authentication-spec island — same mechanism every other store uses, nothing GraphQL-specific. GraphQL Store's own outbound connection only supports OAuth bearer-token passthrough (an `OAuthCredential` is sent as `Authorization: Bearer <token>`); other authentication specifications compile but fail at execution time with "AuthenticationSpecification not supported".

#### Example

```java
###GraphQL
GraphQLStore demo::store::EmployeeDirectoryStore
{
  schema: { <generated - see "Generating the model from an introspection"> };
}

###Connection
GraphQLConnection demo::connection::EmployeeDirectoryConnection
{
  store: demo::store::EmployeeDirectoryStore;
  url: 'https://api.example.com/graphql';
  authentication: #OAuth { ... }#;
}

###Runtime
Runtime demo::runtime::EmployeeDirectoryRuntime
{
  mappings: [ demo::mapping::EmployeeDirectoryMapping ];
  connections: [
    demo::store::EmployeeDirectoryStore: [ connection_1: demo::connection::EmployeeDirectoryConnection ]
  ];
}
```

### Proxy properties and qualified properties

A GraphQL field can take arguments (`employee(id: Int): Employee`), but an ordinary Pure property can't — it's just a name, type, and multiplicity, with nowhere to attach a parameter list. Only a Pure **qualified property** (a property with a body and parameters) can take arguments. The catch: the JSON-response-binding layer, which deserializes the server's HTTP response into a Pure object, only knows how to populate plain stored properties — it has no way to "write into" a qualified property, since a QP's value comes from evaluating its body expression, not from deserialized data. One property can't do both jobs.

The generator splits the two concerns into two properties:

- A hidden, plain **proxy property** (`employeeProxy: Employee[0..1]`) — the real landing spot for the server's JSON, backed by the JSON Schema binding.
- A **qualified property** (`employee(id: Integer[0..1]) { $this.employeeProxy }: Employee[0..1]`) that exists purely to declare the argument signature and give you something to call in a query. Its body is a pure forward to the proxy — it does no filtering itself; the actual filtering happened server-side, driven by the argument you passed over the wire.

You always call the qualified property in a query (`employee(42) { ... }`), never the proxy directly — the proxy is an internal implementation detail, though it is a real, queryable Pure property if you need to inspect it while debugging.

Each generated proxy also carries a `{meta::external::store::graphQL::generation::GeneratedProxyProfile.generatedProxyForQualifiedProperty = 'employee'}` tagged value naming the qualified property it backs. If your schema happens to declare its own field whose name collides with the generator's proxy-naming convention (eg. a real `employeeProxy` field alongside `employee(id)`), the generator renames its own proxy (`employeeProxy2`) to avoid overwriting yours; the tag is what lets query planning tell a generated proxy apart from a genuinely-named field of the same shape, rather than guessing from the name.

## Generating the model from an introspection

Hand-writing the `###Pure`, `###ExternalFormat`, `###GraphQL schema:`, and `###Mapping` sections for a non-trivial schema is impractical — `GraphQLStoreModelGenerator` does it for you from a live introspection query.

**REST API:**

```
POST /api/pure/v1/graphQL/generateModelsFromIntrospection
```

Request body (`GraphQLStoreModelGenerationInput`):

```json
{
  "url": "https://api.example.com/graphql",
  "authenticationSpecification": { ... },
  "storePath": "demo::store::EmployeeDirectoryStore",
  "targetPackage": "demo",
  "targetBindingPath": "demo::EmployeeDirectoryBinding",
  "targetMappingPath": "demo::mapping::EmployeeDirectoryMapping"
}
```

The generator runs a standard GraphQL introspection query (`__schema`) against `url`, then produces:

- **`###Pure`** — one Pure `Class` per GraphQL `OBJECT`/`INPUT_OBJECT`/`INTERFACE` type (interfaces become a Pure superclass, implementing types become real Pure subclasses), one `Enum` per GraphQL `enum`, and a proxy property + qualified property pair for every field that takes arguments.
- **`###ExternalFormat`** — a JSON Schema `SchemaSet` + `Binding` describing the shape of the server's JSON responses. Polymorphic fields are represented as a discriminated `oneOf`, keyed by the response's `__typename`.
- **`###GraphQL`**'s `schema:` block — the introspected SDL, re-rendered.
- **`###Mapping`** — the root `Query` class mapping, plus one minimal root `ClassMapping` per concrete subtype reachable anywhere in the schema (every Pure class that can be the target of a routed fetch needs its own root `ClassMapping` entry, even if it is never fetched as the top-level root itself).

Programmatically, the same logic is available via `GraphQLStoreModelGenerator.generate(GraphQLStoreModelGenerationInput, Identity)` (`legend-engine-xt-store-graphQL-generation`), which returns a `GraphQLStoreModelGenerationResult` carrying the generated grammar text plus any types the generator had to skip — check `skippedTypes` after generation.

**Typical workflow:**

1. Hand-author a minimal `GraphQLStore` (empty `schema: {}`), `Connection`, and `Runtime` — just enough to point generation at a live, reachable endpoint.
2. Call `generateModelsFromIntrospection` (or the Java API directly) with that store's coordinates.
3. Splice the generated `###Pure` / `###ExternalFormat` / `###GraphQL` / `###Mapping` sections into your model (the `###Connection`/`###Runtime` sections you hand-wrote are untouched by generation).
4. Re-run generation whenever the upstream schema changes; regenerated output replaces the generated sections wholesale — don't hand-edit generated classes, since the next regeneration will silently overwrite your changes.

## GraphQL Store mapping

### Structure

The root `Query` class mapping points a Pure class at the GraphQL type backing it:

- `~typeName [Store]`: the GraphQL type in the schema that backs this Pure class.
- `~binding`: the `Binding` (from generation) used to deserialize this class's JSON shape.

#### Example

```java
###Mapping
Mapping demo::mapping::EmployeeDirectoryMapping
(
  *demo::Query: GraphQL
  {
    ~typeName [demo::store::EmployeeDirectoryStore] 'Query'
    ~binding demo::EmployeeDirectoryBinding
  }
)
```

## Examples

All examples below assume a model generated from a schema exposing:

```graphql
type Query {
  employees: [Employee]
  employee(id: Int): Employee
  employeesByStatus(status: Status!): [Employee]
  employeesByTags(requiredTags: [String!]!, optionalTags: [String]): [Employee]
  currentStaffMember(id: Int): StaffMember
}
type Employee {
  firstName: String!
  status: Status
  department: Department
  assignedTo: StaffMember
}
type Department { name: String! }
enum Status { ACTIVE INACTIVE ON_LEAVE }
interface StaffMember
type Worker implements StaffMember { employeeId: String! }
type Requisition implements StaffMember { requisitionId: String! }
```

### Example 1 — simple query, nested object, enum field

```pure
demo::Query.all()->graphFetch(#{
  demo::Query {
    employee(42) {
      firstName,
      status,
      department { name }
    }
  }
}#)->serialize(#{
  demo::Query {
    employee(42) { firstName, status, department { name } }
  }
}#)
```

### Example 2 — argument bound to a `$variable` instead of a literal

Any qualified-property argument can be a literal (as above) or a `$variable`, resolved from `ExecutionState` at execution time rather than baked into the query text:

```pure
{employeeId: Integer[1] |
  demo::Query.all()->graphFetch(#{
    demo::Query { employee($employeeId) { firstName } }
  }#)->serialize(#{
    demo::Query { employee($employeeId) { firstName } }
  }#)
}
```

A `$variable`-bound argument renders as a declared GraphQL operation variable (`query($employeeId: Int) { employeeProxy: employee(id: $employeeId) { firstName } }`), not interpolated inline.

### Example 3 — enum-typed and required-list arguments

```pure
demo::Query.all()->graphFetch(#{
  demo::Query { employeesByStatus(Status.ACTIVE) { firstName } }
}#)->serialize(#{
  demo::Query { employeesByStatus(Status.ACTIVE) { firstName } }
}#)
```

A literal Enum argument works directly. For a required (non-null) list argument you want to leave empty at the call site, pass `[]` explicitly — the generator tags such arguments so the translator renders them as an explicit, empty GraphQL variable rather than omitting the argument:

```pure
employeesByTags([], ['vip']) { firstName }   // requiredTags sent as [], optionalTags as ['vip']
```

### Example 4 — polymorphic (interface-typed) field

Use `->subType(@ConcreteClass)` once per concrete subtype you want to request; only the branch matching the server's actual `__typename` for a given row contributes to the result:

```pure
demo::Query.all()->graphFetch(#{
  demo::Query {
    employee(42) {
      firstName,
      assignedTo->subType(@demo::Worker) { employeeId },
      assignedTo->subType(@demo::Requisition) { requisitionId }
    }
  }
}#)->serialize(#{
  demo::Query {
    employee(42) {
      firstName,
      assignedTo->subType(@demo::Worker) { employeeId },
      assignedTo->subType(@demo::Requisition) { requisitionId }
    }
  }
}#)
```

This works identically when the polymorphic field is itself a root-level `Query` field (e.g. `currentStaffMember(id): StaffMember`), not just a nested property — subject to the argument-identity restriction noted above.
