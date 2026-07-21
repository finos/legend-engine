# Relation Mapping EMIT Integration Test Plan

This document is the implementation plan for **EMIT (Engine Model Integration Test) models**
covering the relation (function) class mapping form
`ClassName: Relation { ~func f():Relation<Any>[1] | … }`.

The plan covers all twelve `TEST_ONLY` scenarios identified in
[`relation-mapping-test-gaps.md`](relation-mapping-test-gaps.md) (Section 6). Every
entry cross-references the Pure unit tests that should be used as the authoring reference.

EMIT exercises the full six-phase pipeline — INITIALIZATION → PARSE → COMPILE →
MODEL_GENERATION → FILE_GENERATION → TEST_EXECUTION → PLAN_GENERATION — from raw `.pure`
source, the same path a Studio or HTTP client takes. It is intentionally a subset of the
unit-test matrix: unit tests prove correctness breadth; **EMIT proves the pipeline works
end-to-end for representative scenarios**.

---

## Prerequisites

Read first:

- [`relation-mapping-test-gaps.md`](relation-mapping-test-gaps.md) — gap analysis, feature
  matrix, and all known `<<test.ToFix>>` limitations.
- [`testing-strategy.md`](testing-strategy.md) — test pyramid and EMIT framework overview.

EMIT runner: `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/java/.../RelationalEMITTests.java`
Model root: `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/`

---

## Authoring Rules for Relation Mapping EMIT Models

These apply to every model below. They are derived from the unit-test gotchas documented in
the gap analysis (Section 2 of `relation-mapping-test-gaps.md`) and from audit of
`MappingTestRunner` and `RelationalConnectionFactory`.

### 0a. YAML `dependencies:` is nested under `modelSources:`, not at top level

The EMIT loader (`EMITModelDescriptor`) deserialises `dependencies` as a field of the
inner `ModelSources` class, not as a top-level key. A top-level `dependencies:` entry is
**silently ignored** because the descriptor is annotated `@JsonIgnoreProperties(ignoreUnknown = true)`,
causing all declared model dependencies to be missing at runtime.

**Correct structure:**

```yaml
modelSources:
  model:
    root: relation-simple
    files:
      - mapping/employeeMapping.pure
      - data/testData.pure
  dependencies:              # <-- nested inside modelSources, not at top level
    - source: relation-shared-domain.emit.yaml
    - source: relation-shared-db.emit.yaml
```

**Wrong structure (silently ignored):**

```yaml
modelSources:
  model:
    root: relation-simple
    files:
      - mapping/employeeMapping.pure
dependencies:                # <-- top-level: IGNORED by EMIT loader
  - source: relation-shared-domain.emit.yaml
```

Verify against working examples: `relational-simple.emit.yaml` and `relational-service.emit.yaml`
both place `dependencies:` inside `modelSources:`.

### 0b. Service testSuite connection key is the runtime connection *id*, not the element name

In a `Service` testSuite `data: [ connections: [ KEY: ... ] ]` block, `KEY` must match the
**id** used in the Runtime's `connections:` map — not the `Connection` package path or element
name.

**Correct:**

```pure
// runtime/employeeRuntime.pure — connections map id is 'h2'
connections:
[
  demo::relation::db::EmployeeDB:
  [
    h2: demo::relation::connection::EmployeeH2Connection
  ]
];
```

```pure
// service testSuite data block — key must be 'h2'
connections:
[
  h2:
    Relation #{ ... }#
]
```

**Wrong:**

```pure
connections:
[
  EmployeeH2Connection:   // element name, not the runtime id — will not match
    Relation #{ ... }#
]
```

Reference: `relational-service/service/personService.pure` uses `h2:` as the key, and
`relational-service/runtime/personRuntime.pure` maps `h2: demo::connection::PersonH2Connection`.

### 0c. Relation functions live in the mapping file, referenced by mangled name

All relation functions (`~func`) are defined in a `###Pure` section at the **top of the
same `.pure` file** as the mapping that uses them. No separate `func/` directory is used.

The `~func` reference uses the **mangled name** form (no parentheses or type annotation)
paired with a wildcard import over the function's package:

```pure
###Pure
import meta::pure::metamodel::relation::*;

function demo::relation::func::employees(): meta::pure::metamodel::relation::Relation<Any>[1]
{
  #>{demo::relation::db::EmployeeDB.default.EmployeeTable}#
}

###Mapping
import demo::relation::func::*;   // wildcard import makes the mangled name resolvable

Mapping demo::relation::mapping::EmployeeSimpleMapping
(
  *demo::relation::domain::Employee: Relation
  {
    ~func demo::relation::func::employees__Relation_1_   // mangled name — no signature
    ...
  }
```

The mangled suffix for a `Relation<Any>[1]` return type is `__Relation_1_`. Functions
returning typed column variants (`Relation<(COL:Type,…)>[1]`) use the same suffix because
the column spec is erased in the mangled form. The EMIT model YAML `files:` list for each
model therefore lists only `mapping/<name>.pure` and `data/testData.pure` — no `func/` entry.

### 1. Multiplicity — `[1]` property requires a `NOT NULL` column

The `#4941` multiplicity validator rejects a `[1]` property mapping if the backing column or
expression infers `[0..1]`. Rule of thumb:

| Column declaration | Pure property multiplicity | Requires |
|---|---|---|
| `COL TYPE NOT NULL` | `[1]` | Direct mapping OK |
| `COL TYPE` (nullable) | `[0..1]` | Direct mapping OK |
| `COL TYPE` (nullable) | `[1]` | Expression RHS with `->toOne()` |
| `BIT/BOOLEAN` | `Boolean[1]` | Expression RHS `$src.COL == 1` |

All shared database columns that back `[1]` properties **must** carry `NOT NULL`.

### 2. `Boolean` properties require an expression RHS

A `BIT` column infers as `TinyInt` in the relation type system. Direct mapping to
`Boolean[1]` fails with a type mismatch. Always use:

```pure
active: $src.ACTIVE == 1
```

### 3. `StrictDate` vs `DateTime` in test assertions

The `TestTDS` parser only recognises full datetimes (`YYYY-MM-DDT00:00:00.000+0000`).
Projecting a `StrictDate` (`DATE`) column materialises a date-only value and causes
`"Not supported data type: 'STRING' for Pure type: 'StrictDate'"`. Options:

- Project `->toString()` (String column) when asserting date-only values; or
- Use full `TIMESTAMP` columns for date assertions (carry a time component, project directly).

`DateTime`/`TIMESTAMP` columns project without this issue.

### 4. `~src` inline source must be wrapped in a zero-arg helper function

The runtime parser stack-overflows if a raw `#>{store.table}#` literal appears directly
under `~src`. Always wrap in a named Pure function:

```pure
// WRONG — parser overflow
~src #>{demo::relation::db::EmployeeDB.default.EmployeeTable}#;

// CORRECT — zero-arg wrapper
function demo::relation::func::employeeSource():Relation<(ID:Integer,FIRST_NAME:String)>[1]
{ #>{demo::relation::db::EmployeeDB.default.EmployeeTable}# }

// in mapping:
~src demo::relation::func::employeeSource();
```

### 5. Test data format in `testSuites:` data blocks

Reference the **underlying Database store** (not the function) in the `data:` block. This
routes through `RelationalConnectionFactory`, which converts `Relation #{ … }#` rows to an
H2 CSV connection. The stub `RelationAccessorTestConnectionFactory` is NOT needed.

```pure
data:
[
  demo::relation::db::EmployeeDB:   // the Database, not the function
    Relation
    #{
      default.EmployeeTable: ID, FIRST_NAME, LAST_NAME, FIRM_ID, EMP_TYPE, ACTIVE
        1, 'Alice', 'Green', 1, 'FULL_TIME', 1
        2, 'Bob', 'Smith', 2, 'CONTRACT', 1
    }#
];
```

### 6. `.rows` / `.getString` require a `->cast(@TabularDataSet)` first

If the test query uses `execute(|...).values->at(0)`, the result is a `Relation`, not a TDS.
Append `->cast(@meta::pure::tds::TabularDataSet)` before accessing rows. EMIT assertions use
`EqualToJson` / `EqualTo` against projected TDS results, so this does not apply to EMIT
mapping `testSuites:`.

---

## Shared Fixtures

Two shared models serve as building blocks for all twelve relation EMIT models, parallel
to `relational-shared-domain` and `relational-shared-firm-db` used by the relational models.

---

### Fixture A — `relation-shared-domain`

**Purpose:** Pure domain types (classes, enumeration, association) used across all relation
EMIT models. Store-agnostic — no databases or functions.

**File:** `relation-shared-domain.emit.yaml`

```yaml
name: relation-shared-domain
title: "Shared Domain for Relation Mapping EMIT Models"
description: |
  Employee, Firm, EmployeeType, and their Association — the common domain types
  reused across all relation-mapping EMIT scenarios.

modelSources:
  model:
    root: relation-shared-domain
    files:
      - model/employeeType.pure
      - model/firm.pure
      - model/employee.pure
      - model/employment.pure
      - model/position.pure

features:
  - scaffolding:class
  - grammar:enumeration
  - grammar:association
  - grammar:derived-property
stores: []
complexity: basic
tags:
  - shared-domain
  - relation
```

**`model/employeeType.pure`** — `EmployeeType` enumeration:

```pure
###Pure
Enum demo::relation::domain::EmployeeType
{
  FULL_TIME,
  CONTRACT
}
```

**`model/firm.pure`** — `Firm` class:

```pure
###Pure
Class demo::relation::domain::Firm
{
  id: Integer[1];
  legalName: String[1];
}
```

**`model/employee.pure`** — `Employee` class:

```pure
###Pure
Class demo::relation::domain::Employee
{
  id: Integer[1];
  firstName: String[1];
  lastName: String[1];
  employeeType: demo::relation::domain::EmployeeType[1];
  active: Boolean[1];
  hireDate: StrictDate[1];
  salary: Float[0..1];
  fullName() { $this.firstName + ' ' + $this.lastName }: String[1];
}
```

`salary` is deliberately `[0..1]` — it will be backed by a nullable column and tested in
`relation-expression-rhs`. `active` requires the `$src.ACTIVE == 1` coercion (see rule 2
above). `hireDate` requires `->toString()` assertion care (see rule 3).

**`model/employment.pure`** — `Employee ↔ Firm` Association:

```pure
###Pure
Association demo::relation::domain::Employment
{
  employer: demo::relation::domain::Firm[1];
  employees: demo::relation::domain::Employee[*];
}
```

**`model/position.pure`** — `Position` class (used by `relation-groupBy`):

```pure
###Pure
Class demo::relation::domain::Position
{
  gsn: String[1];
  productId: Integer[1];
  totalQty: Integer[1];
}
```

---

### Fixture B — `relation-shared-db`

**Purpose:** Shared relational database (H2) with the tables used across multiple EMIT
models. Includes a `Position`/`Product` sub-schema for the `groupBy` model.

**File:** `relation-shared-db.emit.yaml`

```yaml
name: relation-shared-db
title: "Shared H2 Database for Relation Mapping EMIT Models"
description: |
  EmployeeDB — the backing H2 store for Employee and Firm relation functions.
  Includes EmployeeTable, FirmTable, and Position/Product tables for aggregation tests.

modelSources:
  model:
    root: relation-shared-db
    files:
      - store/employeeDb.pure
  dependencies:
    - source: relation-shared-domain.emit.yaml

features:
  - scaffolding:relational-store
stores:
  - relational
complexity: basic
tags:
  - shared-store
  - relation
  - h2
```

**`store/employeeDb.pure`** — schema:

```pure
###Relational
Database demo::relation::db::EmployeeDB
(
  Table EmployeeTable
  (
    ID          INTEGER PRIMARY KEY,
    FIRST_NAME  VARCHAR(100) NOT NULL,
    LAST_NAME   VARCHAR(100) NOT NULL,
    FIRM_ID     INTEGER NOT NULL,
    EMP_TYPE    VARCHAR(20) NOT NULL,
    ACTIVE      BIT NOT NULL,
    HIRE_DATE   TIMESTAMP NOT NULL,
    SALARY      FLOAT
  )

  Table FirmTable
  (
    ID          INTEGER PRIMARY KEY,
    LEGAL_NAME  VARCHAR(100) NOT NULL
  )

  Join EmployeeFirmJoin(EmployeeTable.FIRM_ID = FirmTable.ID)
)
```

Notes:
- `ACTIVE` is `BIT NOT NULL` — `Boolean[1]` property requires expression RHS coercion.
- `HIRE_DATE` is `TIMESTAMP NOT NULL` (not `DATE`) to avoid the StrictDate projection bug
  (see rule 3). Properties map to `DateTime[1]` and assert with full timestamps.
- `SALARY` is nullable `FLOAT` — maps to `Float[0..1]`. `relation-expression-rhs` tests the
  `->toOne()` coercion path.
- `Join EmployeeFirmJoin` is used by `relation-join` for in-body join tests, and by the
  `relational` side of `relation-relational-union`.

For `relation-groupBy`, the shared DB also defines a `Position`/`Product` sub-schema.
Add these tables to `employeeDb.pure` (or a second store file if the shared DB grows too
large):

```pure
  Table PositionTable
  (
    ID          INTEGER PRIMARY KEY,
    ACC_NUM     INTEGER NOT NULL,
    GSN         VARCHAR(20) NOT NULL,
    PRODUCT_ID  INTEGER NOT NULL,
    QTY         INTEGER NOT NULL
  )

  Table ProductTable
  (
    ID          INTEGER PRIMARY KEY,
    DESCRIPTION VARCHAR(100) NOT NULL
  )

  Join PositionProductJoin(PositionTable.PRODUCT_ID = ProductTable.ID)
```

---

## Implementation Status

| Component | Status |
|:---|:---|
| Shared fixtures (`relation-shared-domain`, `relation-shared-db`) | ✅ Complete |
| Wave 1: `relation-simple` | ✅ Complete — PARSE, COMPILE, TEST_EXECUTION pass |
| Wave 1: `relation-service` | ✅ Complete — service test + PLAN_GENERATION pass |
| Wave 2: `relation-join`, `relation-filter`, `relation-enumeration`, `relation-groupBy` | Implemented — fixing issues found during test runs |
| Wave 3: `relation-src`, `relation-expression-rhs`, `relation-embedded` | Implemented — fixing issues found during test runs |
| Wave 4: `relation-union`, `relation-relational-union`, `relation-milestoning` | Implemented — fixing issues found during test runs |
| Extra: `relation-modelJoin` | Implemented — ModelJoin alternative to relation-join's XStore |

---

## Implementation Waves

Models are ordered from least to most complexity. Each model depends on Fixture A and
Fixture B unless otherwise noted.

---

## Wave 1 — Foundational Pipeline (2 models)

These two models validate the end-to-end pipeline before any feature-specific coverage.
`relation-simple` is the prerequisite for all subsequent models.

---

### Model 1: `relation-simple`

**Mirrors:** `relational-simple`  
**Exercises:** `~func` returning `Relation<(…)>[1]`, plain property mapping, mapping
`testSuites:` with `Relation #{ … }#` data, `EqualToJson` assertion.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Unit test references:**
- `core_relational/relational/tests/mapping/relation/tests.pure` →
  `testSimpleMappingQuery` (basic `all()->project()`)
- Same file → `testNullableSalaryMapping` (demonstrates `[0..1]` salary property)

**File structure:**

```
relation-simple/
  mapping/employeeMapping.pure  # ###Pure section (employees func) + ###Mapping section
  data/testData.pure            # Data element with Relation rows
```

**`relation-simple.emit.yaml`** key sections:

```yaml
name: relation-simple

modelSources:
  model:
    root: relation-simple
    files:
      - mapping/employeeMapping.pure   # ###Pure (employees func) + ###Mapping
      - data/testData.pure
  dependencies:
    - source: relation-shared-domain.emit.yaml
    - source: relation-shared-db.emit.yaml

features:
  - scaffolding:class
  - scaffolding:relation-function
  - scaffolding:relation-mapping
  - execution:data-element
  - execution:test-data
stores:
  - relational
tags:
  - h2
  - mapping-test
  - relation
```

**`mapping/employeeMapping.pure`** — `###Pure` section (function) followed by `###Mapping`:

The file opens with a `###Pure` section defining the relation function, then a `###Mapping`
section that references it by mangled name (see authoring rule 0c). The function return type
is `Relation<Any>[1]` — column types are enforced by the compiler against the database schema,
not stated explicitly in the function signature.

**`mapping/employeeMapping.pure`** — the `~func` mapping:

```pure
###Mapping
Mapping demo::relation::mapping::EmployeeSimpleMapping
(
  *demo::relation::domain::Employee: Relation
  {
    ~func demo::relation::func::employees():Relation<Any>[1];
    id: ID,
    firstName: FIRST_NAME,
    lastName: LAST_NAME,
    employeeType: EMP_TYPE,
    active: $src.ACTIVE == 1,
    salary: SALARY
  }

  EnumerationMapping EmployeeTypeMapping
  {
    FULL_TIME: ['FULL_TIME'],
    CONTRACT: ['CONTRACT']
  }

  testSuites:
  [
    simpleSuite:
    {
      function: |demo::relation::domain::Employee.all()
                  ->sortBy(x|$x.firstName)
                  ->project([x|$x.firstName, x|$x.lastName], ['First Name', 'Last Name']);
      tests:
      [
        allEmployees:
        {
          data:
          [
            demo::relation::db::EmployeeDB:
              Reference
              #{
                demo::relation::data::EmployeeData
              }#
          ];
          asserts:
          [
            shouldMatch:
              EqualToJson
              #{
                expected:
                  ExternalFormat
                  #{
                    contentType: 'application/json';
                    data: '[{"First Name":"Alice","Last Name":"Green"},{"First Name":"Bob","Last Name":"Smith"}]';
                  }#;
              }#
          ];
        }
      ];
    }
  ]
)
```

**`data/testData.pure`** — the `Data` element:

```pure
###Data
Data demo::relation::data::EmployeeData
{
  Relation
  #{
    default.EmployeeTable: ID, FIRST_NAME, LAST_NAME, FIRM_ID, EMP_TYPE, ACTIVE, HIRE_DATE, SALARY
      1, 'Alice', 'Green', 1, 'FULL_TIME', 1, '2020-01-15 00:00:00', 90000.0
      2, 'Bob', 'Smith', 2, 'CONTRACT', 1, '2021-06-01 00:00:00', 75000.0
      3, 'Carol', 'White', 1, 'FULL_TIME', 0, '2019-03-10 00:00:00', %null
  }#
}
```

**Authoring notes:**
- `active: $src.ACTIVE == 1` is required (BIT→Boolean coercion, rule 2).
- `salary: SALARY` maps `Float[0..1]` directly — no `->toOne()` needed because property is already `[0..1]`.
- The `EnumerationMapping` block is included here even though enumeration-specific assertions
  belong in `relation-enumeration`; it is needed for the `employeeType` property to compile.
- Verify: does `MappingTestRunner`'s store-test-data path correctly wire the H2 connection
  for a `~func` mapping via `RelationalConnectionFactory`? This is the first end-to-end
  validation of that path. If TEST_EXECUTION fails, check whether matrix #32 reclassifies
  from `NEW_DEV` to `TEST_ONLY`.

---

### Model 2: `relation-service`

**Mirrors:** `relational-service`  
**Exercises:** `PureSingleExecution` `Service` over a relation mapping, service `testSuites:`
with connection-keyed data, PLAN_GENERATION phase.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION (service), PLAN_GENERATION.

**Unit test references:**
- Same mapping as `relation-simple`; the service wraps it.
- Pattern mirrors `testSimpleMappingQuery` from `tests.pure`.

**Additional files beyond `relation-simple`:**

```
relation-service/
  connection/h2Connection.pure
  runtime/employeeRuntime.pure
  service/employeeService.pure
```

**`relation-service.emit.yaml`** key additions:

```yaml
name: relation-service

modelSources:
  model:
    root: relation-service
    files:
      - connection/h2Connection.pure
      - runtime/employeeRuntime.pure
      - service/employeeService.pure
  dependencies:
    - source: relation-simple.emit.yaml

features:
  - scaffolding:relation-function
  - scaffolding:relation-mapping
  - scaffolding:relational-connection
  - scaffolding:runtime
  - execution:service
  - execution:service-test
tags:
  - h2
  - service-test
  - plan-generation
  - relation
```

**`connection/h2Connection.pure`:**

```pure
###Connection
RelationalDatabaseConnection demo::relation::connection::EmployeeH2Connection
{
  store: demo::relation::db::EmployeeDB;
  type: H2;
  specification: LocalH2 {};
  auth: DefaultH2;
}
```

**`runtime/employeeRuntime.pure`:**

```pure
###Runtime
Runtime demo::relation::runtime::EmployeeRuntime
{
  mappings:
  [
    demo::relation::mapping::EmployeeSimpleMapping
  ];
  connections:
  [
    demo::relation::db::EmployeeDB:
    [
      h2: demo::relation::connection::EmployeeH2Connection
    ]
  ];
}
```

**`service/employeeService.pure`** — service with testSuites:

```pure
###Service
Service demo::relation::service::EmployeeService
{
  pattern: '/api/relation/employees';
  documentation: 'List all employees via relation mapping.';
  autoActivateUpdates: true;
  execution: Single
  {
    query: |demo::relation::domain::Employee.all()
              ->sortBy(x|$x.firstName)
              ->project([x|$x.firstName, x|$x.lastName], ['First Name', 'Last Name']);
    mapping: demo::relation::mapping::EmployeeSimpleMapping;
    runtime: demo::relation::runtime::EmployeeRuntime;
  }
  testSuites:
  [
    serviceSuite:
    {
      data:
      [
        connections:
        [
          h2:                    // must match the id in the Runtime connections map
            Relation
            #{
              default.EmployeeTable: ID, FIRST_NAME, LAST_NAME, FIRM_ID, EMP_TYPE, ACTIVE, HIRE_DATE, SALARY
                1, 'Alice', 'Green', 1, 'FULL_TIME', 1, '2020-01-15 00:00:00', 90000.0
                2, 'Bob', 'Smith', 2, 'CONTRACT', 1, '2021-06-01 00:00:00', 75000.0
            }#
        ]
      ]
      tests:
      [
        allEmployees:
        {
          serializationFormat: PURE_TDSOBJECT;
          asserts:
          [
            shouldMatch:
              EqualToJson
              #{
                expected:
                  ExternalFormat
                  #{
                    contentType: 'application/json';
                    data: '[{"First Name":"Alice","Last Name":"Green"},{"First Name":"Bob","Last Name":"Smith"}]';
                  }#;
              }#
          ]
        }
      ]
    }
  ]
}
```

---

## Wave 2 — Core Mapping Features (4 models)

These models cover the feature combinations most commonly used in production relation
mappings.

---

### Model 3: `relation-join`

**Mirrors:** `relational-joins`  
**Exercises:** ModelJoin association (`Employee → Firm` traversal via a cross-relation join
condition), asserting navigation through the association in a query.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Unit test references:**
- `mapping/modelJoin/testModelJoinSimple.pure` → `testSimpleEquality` (equality join
  condition: `employees.FIRM_ID == firm.ID`), `testDerivedPropertyInCondition` (derived
  `fullName` in join condition).
- `mapping/relation/tests.pure` → `testSimpleMappingWithAssociation` (association
  traversal pattern).

**Key design:** Two relation functions — `employees()` and `firms()` — each backed by their
respective table. The association between `Employee` and `Firm` is realised via a ModelJoin
on `FIRM_ID == ID`. The test asserts `Employee.all()->project([x|$x.firstName, x|$x.employer.legalName], [...])`.

**Additional files:**

```
relation-join/
  mapping/joinMapping.pure    # ###Pure (employees + firms funcs) + ###Mapping with ModelJoin + testSuites
  data/testData.pure          # Relation rows for both EmployeeTable and FirmTable
```

**`relation-join.emit.yaml`** key sections:

```yaml
name: relation-join

modelSources:
  model:
    root: relation-join
    files:
      - mapping/joinMapping.pure   # ###Pure (employees + firms funcs) + ###Mapping
      - data/testData.pure
  dependencies:
    - source: relation-shared-domain.emit.yaml
    - source: relation-shared-db.emit.yaml
```

**`mapping/joinMapping.pure`** key structure (the `###Pure` func section precedes this `###Mapping` block):

```pure
###Pure
import meta::pure::metamodel::relation::*;

function demo::relation::func::employees(): meta::pure::metamodel::relation::Relation<Any>[1]
{ #>{demo::relation::db::EmployeeDB.default.EmployeeTable}# }

function demo::relation::func::firms(): meta::pure::metamodel::relation::Relation<Any>[1]
{ #>{demo::relation::db::EmployeeDB.default.FirmTable}# }

###Mapping
import demo::relation::func::*;

Mapping demo::relation::mapping::EmployeeJoinMapping
(
  *demo::relation::domain::Employee: Relation
  {
    ~func demo::relation::func::employees__Relation_1_
    id: ID,
    firstName: FIRST_NAME,
    lastName: LAST_NAME,
    employeeType: EMP_TYPE,
    active: $src.ACTIVE == 1
  }

  *demo::relation::domain::Firm: Relation
  {
    ~func demo::relation::func::firms__Relation_1_
    id: ID,
    legalName: LEGAL_NAME
  }

  demo::relation::domain::Employment: XStore
  {
    employer[demo_relation_domain_Employee, demo_relation_domain_Firm]:
      $this.FIRM_ID == $that.ID,
    employees[demo_relation_domain_Firm, demo_relation_domain_Employee]:
      $this.ID == $that.FIRM_ID
  }

  testSuites:
  [
    joinSuite:
    {
      function: |demo::relation::domain::Employee.all()
                  ->sortBy(x|$x.firstName)
                  ->project([x|$x.firstName, x|$x.employer.legalName], ['Name', 'Firm']);
      tests:
      [
        withFirm:
        {
          data:
          [
            demo::relation::db::EmployeeDB:
              Reference #{ demo::relation::data::EmployeeFirmData }#
          ];
          asserts:
          [
            shouldJoin: EqualToJson #{ expected: ExternalFormat
              #{ contentType: 'application/json';
                 data: '[{"Name":"Alice","Firm":"Acme"},{"Name":"Bob","Firm":"Beta"}]'; }#; }#
          ];
        }
      ];
    }
  ]
)
```

**`data/testData.pure`:** `Relation #{ … }#` rows for both tables in one `Data` element.

---

### Model 3b: `relation-modelJoin` (extra — not in original wave numbering)

**Mirrors:** `relation-join`, but swaps the association mechanism.  
**Exercises:** `ModelJoin` (instead of `XStore`) as the association wiring between two
`Relation`-mapped classes.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Why this model exists:** `relation-join` demonstrates the XStore form, which is what
every reference unit test uses for a plain Relation-to-Relation association. `ModelJoin`
is a viable alternative — `HelperMappingBuilder.processModelJoinAssociationMapping`
(legend-engine compiler) is generic over `InstanceSetImplementation` and pairs every
(source-set, target-set) combination regardless of whether the sets are `Relational` or
`Relation`-mapped, so it works for two `Relation`-mapped classes exactly as it does for
two `Relational` ones. This model is a dedicated, isolated demonstration of that path so
a regression here is attributable to ModelJoin specifically, not conflated with the core
join model.

**Key design:** Identical domain/DB/data shape to `relation-join` (two relation functions,
`+firmId` local property on `Employee`, real `id` property on `Firm`), but the association
is declared as:

```pure
demo::relation::domain::Employment: ModelJoin
{
  {employer: demo::relation::domain::Firm[1], employees: demo::relation::domain::Employee[1] | $employer.id == $employees.firmId}
}
```

**Authoring note:** ModelJoin pairs lambda parameters to association ends by **type**, not
by name — name matching is only enforced for self-associations (where both ends share a
type and type-based pairing is ambiguous). The lambda parameter multiplicities are always
`[1]` regardless of the association's actual declared end multiplicities (e.g. `employees:
Employee[*]` on the `Employment` association itself) — the lambda is a pointwise join
predicate evaluated per-instance-pair, not over the collection.

**File structure:**

```
relation-modelJoin/
  mapping/modelJoinMapping.pure   # ###Pure (modelJoinEmployees + modelJoinFirms funcs) + ###Mapping with ModelJoin + testSuites
  data/testData.pure              # Relation rows for both EmployeeTable and FirmTable
```

---

### Model 4: `relation-filter`

**Mirrors:** `relational-filter`  
**Exercises:** `->filter()` in the `~func` body; rows excluded by the filter never surface
in queries.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Unit test references:**
- `mapping/relation/relationCoverageFilter.pure` → `testFilterMapping` (simple numeric
  filter `filterVal <= 4`), `testFilterMappingWithJoin` (filter referencing a joined table),
  `testFilterMappingWithJoinNotEmpty` (`->isNotEmpty()` filter on joined column).
- `mapping/relation/tests.pure` → `testSimpleMappingQueryWithPreFilter`.

**Key design:** The relation function body applies `->filter(r | $r.ACTIVE == 1)` so that
inactive employees are never returned. The `testSuites:` data contains both active and
inactive rows; the assertion verifies only active rows appear.

**`mapping/filterMapping.pure`** — `###Pure` section (filter function, defined at the top of the file):

```pure
function demo::relation::func::activeEmployees(): meta::pure::metamodel::relation::Relation<Any>[1]
{
  #>{demo::relation::db::EmployeeDB.default.EmployeeTable}#
    ->filter(r | $r.ACTIVE == 1)
}
```

The `###Mapping` section follows in the same file, referencing `activeEmployees__Relation_1_`.

**`testSuites:` assertion:** Supply 3 employees (2 active, 1 inactive); assert result
contains only the 2 active ones. A second test case (optional) applies a query-time filter
on top (`->filter(e | $e.firstName == 'Alice')`) to verify that filter stacking works.

---

### Model 5: `relation-groupBy`

**Mirrors:** (new — no classic relational equivalent with groupBy at mapping level)  
**Exercises:** `->groupBy()` in the `~func` body with aggregate columns; the class represents
the grouped/aggregated result.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Unit test references:**
- `mapping/relation/relationCoverageGroupBy.pure` → `testGroupByMapping` (basic
  `~groupBy` with `sum`), `testGroupByMappingMultipleAggregates` (5 aggregates: sum,
  count, min, max, average), `testGroupByMappingWithFilter` (filter on dimension),
  `testGroupByMappingWithFilterOnAggregate` (filter on aggregate column).
- `mapping/relation/tests.pure` → `testMappingWithGroupBy` (groupBy at class mapping level).

**Key design:** Define a `Position` class (gsn, productId, totalQty) backed by a grouped
relation function over `PositionTable` (part of the shared DB). The function applies:

```pure
#>{demo::relation::db::EmployeeDB.default.PositionTable}#
  ->groupBy(~[GSN, PRODUCT_ID], ~[TOTAL_QTY: x | $x.QTY : y | $y->sum()])
```

The `testSuites:` assertion loads a few `PositionTable` rows and verifies the grouped
aggregate values.

**Domain:** `demo::relation::domain::Position` is defined in `relation-shared-domain/model/position.pure`
(part of the shared fixture — no local class needed). The `groupBy` model depends on `relation-shared-domain`
transitively via `relation-shared-db`.

---

### Model 6: `relation-enumeration`

**Mirrors:** `relational-enumeration`  
**Exercises:** `EnumerationMapping` transformer on a relation property; `testSuites:` asserts
decoded enum labels.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Unit test references:**
- `mapping/relation/tests.pure` → `testEnumerationMapping` (project enum property),
  `testEnumerationMappingWithFilter` (filter on enum equality),
  `testEnumerationMappingWithIn` (`->in()` filter with enum list),
  `testMultipleEnumerationMappings` (two enum fields in one mapping).
- `mapping/relation/tests.pure` → `testEnumerationMappingProjection` — NOTE: this test
  is currently a **shared-suite regression** (quoted-column SQL-gen aliasing bug in a
  nested subquery). The EMIT model should NOT reproduce that exact query shape until the
  regression is fixed.

**Key design:** The `employeeType` property is mapped via `EnumerationMapping` from the
VARCHAR column `EMP_TYPE`. Two assertion tests: project enum labels (verify `"FULL_TIME"`
and `"CONTRACT"` decode correctly); filter by enum equality (verify only matching rows
are returned).

**`mapping/enumMapping.pure`** key section:

```pure
*demo::relation::domain::Employee: Relation
{
  ~func demo::relation::func::employees():Relation<Any>[1];
  employeeType: EnumerationMapping EmployeeTypeMapping: EMP_TYPE,
  // ... other properties
}

EnumerationMapping EmployeeTypeMapping
{
  FULL_TIME: ['FULL_TIME'],
  CONTRACT: ['CONTRACT']
}
```

The `testSuites:` query projects `[x|$x.employeeType->toString(), x|$x.firstName]` and
asserts the decoded string values.

---

## Wave 3 — Advanced Grammar Forms (3 models)

These models demonstrate the grammar features added by PR #4941 (`~src` and expression RHS)
and the embedded property mapping form.

---

### Model 7: `relation-src`

**Mirrors:** (new)  
**Exercises:** `~src <expr>` inline source form through the full pipeline — grammar composer
validates round-trip, compiler validates the source lambda, executor runs it.  
**Pipeline phases:** PARSE (composer round-trip), COMPILE, TEST_EXECUTION.

**Unit test references:**
- `mapping/relation/relationCoverageSrc.pure` → `testSrcPlain` (plain `~src` mapping),
  `testSrcWithFilterInSource` (filter applied inside the `~src` lambda), `testSrcSubset`
  (only a column subset exposed via `~src`).
- `mapping/relation/tests.pure` → `testExplicitSrcMappingQuery`, `testExplicitSrcMappingWithAssociation`.

**Key design:** Two mapping variants in one model file:

1. **Plain `~src`:** `~src demo::relation::func::activeEmployeeSource();` — the helper
   function selects all columns. Properties map via bare column names.

2. **Filter-in-source `~src`:** The helper function applies `->filter(r | $r.ACTIVE == 1)`.
   The mapping's query has no explicit filter; the source supplies only active rows.

See authoring rule 4 above — the `~src` helper function must be a named zero-arg Pure
function, not an inline expression.

**`mapping/srcMapping.pure`** — `###Pure` section (two source helper functions, at the top of the file):

```pure
function demo::relation::func::activeEmployeeSource(): meta::pure::metamodel::relation::Relation<Any>[1]
{
  #>{demo::relation::db::EmployeeDB.default.EmployeeTable}#
    ->select(~[ID, FIRST_NAME, LAST_NAME, ACTIVE])
}

function demo::relation::func::filteredEmployeeSource(): meta::pure::metamodel::relation::Relation<Any>[1]
{
  #>{demo::relation::db::EmployeeDB.default.EmployeeTable}#
    ->filter(r | $r.ACTIVE == 1)
    ->select(~[ID, FIRST_NAME, LAST_NAME])
}
```

The `###Mapping` section follows, with two mapping variants referencing
`activeEmployeeSource__Relation_1_` and `filteredEmployeeSource__Relation_1_` respectively.

---

### Model 8: `relation-expression-rhs`

**Mirrors:** (new)  
**Exercises:** Expression-RHS property mapping (`$src.COL * 2`, string concat, `->toOne()`
coercion); asserts computed values in projection.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Unit test references:**
- `mapping/relation/relationCoverageExpression.pure` → `testExpressionArithmetic`
  (`ageDoubled: $src.AGE * 2`), `testExpressionConcat` (`greeting: 'Hi ' + $src.FIRSTNAME`),
  `testExpressionToOneCoercion` (`salary: $src.SALARY->toOne()`).
- `mapping/relation/tests.pure` → `testExpressionMappingArithmetic`,
  `testRfpmLiftMixedConcat`.

**Key design:** Extend `Employee` with computed properties (or use a separate class
`ComputedEmployee`) that have:

- `salaryDoubled: Float[1]` ← `$src.SALARY->toOne() * 2.0` (combines toOne coercion and
  arithmetic; `SALARY` is nullable so `->toOne()` is required for `[1]` multiplicity).
- `fullGreeting: String[1]` ← `'Hello, ' + $src.FIRST_NAME + ' ' + $src.LAST_NAME`.

The `testSuites:` data contains rows where `SALARY` is non-null; the assertion verifies
the doubled salary and the greeting string. Optionally add a second test with a `->filter()`
on the mapped expression property.

**Authoring note:** The `->toOne()` coercion in an expression RHS is the only sanctioned way
to map a nullable column to a `[1]` property without changing the schema.

---

### Model 9: `relation-embedded`

**Mirrors:** (new)  
**Exercises:** Embedded property mapping (inline `address(street: …, city: …)`) and inline
embedded (`() Inline [set]`) variants. Both compile and execute end-to-end.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Unit test references:**
- `mapping/relation/tests.pure` → `testEmbeddedRelationMapping`,
  `testEmbeddedRelationMappingWithFilter`, `testEmbeddedRelationMappingWithAssociation`,
  `testInlineEmbeddedRelationMapping`, `testInlineEmbeddedRelationMappingWithFilter`,
  `testInlineEmbeddedRelationMappingWithAssociation`.

**Key design:** Introduce an `Address` class and embed it in `Employee`:

```pure
Class demo::relation::domain::Address
{
  street: String[1];
  city: String[1];
}
Class demo::relation::domain::Employee
{
  // ... existing properties
  address: demo::relation::domain::Address[1];
}
```

Add `STREET` and `CITY` columns to `EmployeeTable` (NOT NULL). The mapping embeds
`Address` inline:

```pure
*demo::relation::domain::Employee: Relation
{
  ~func demo::relation::func::employees():Relation<Any>[1];
  // ...
  address
  (
    street: STREET,
    city: CITY
  )
}
```

Include two `testSuites:` tests: one projecting `$x.address.city`; one filtering on
`$x.address.city == 'London'`.

---

## Wave 4 — Complex Scenarios (3 models)

These models combine multiple features and represent the more advanced use cases
documented in the unit tests.

---

### Model 10: `relation-union`

**Mirrors:** (new)  
**Exercises:** `Operation { union(…) }` over two `Relation` set implementations; rows from
both sets appear in query results; filter applied across both legs.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Unit test references:**
- `mapping/union/relation/testRelationUnion.pure` → `testUnionTwoRelationMappings_SimpleProject`
  (project lastName across union), `testUnionOfTwoRelationMappings_FilterAcrossSets` (filter
  across both legs), `testUnionTwoRelationMappings_FilterInRelationFunc` (filter baked into
  union source functions), `testUnionTwoRelationMappings_ConstantInMappingProject` (constant
  literal as a property value).
- Same file → `testUnionTwoRelationMappings_EmbeddedFirmProject` (union with embedded access)
  — use as a reference but keep the EMIT model simple.

**Known limitation:** Do NOT include the `importDataFlow=true` / U_TYPE discriminator test
(`testUnionTwoRelationMappings_PksWithImportDataFlow`) — this is a `<<test.ToFix>>` engine
gap (matrix #28).

**Known limitation (found during implementation):** a `->filter()` on a property that is
**not** part of the outer `->project()` list, where that property is mapped via an
expression (`active: $src.ACTIVE == 1`, not a passthrough column), fails against a
Relation-mapped `Operation` union. The generated SQL references the filtered column
against the union-scoped derived table without pulling it through the intermediate
projection ("Column unionBase.ACTIVE not found") and separately produces an invalid
`= 1 = true` fragment by concatenating the mapping-level coercion with the query-level
comparison. Every reference union test that filters across sets
(`testUnionOfTwoRelationMappings_FilterAcrossSets` et al.) filters on a property that is
*also* the projected property (`lastName`), so this specific combination is untested
upstream. Implemented as a single `allUnionedSuite` test (plain project, no filter); the
filter-across-legs variant was removed rather than worked around — see "Out of EMIT scope"
below.

**Key design:** Two `EmployeeTable`-backed functions (e.g. `contractEmployees()` filtering
`EMP_TYPE == 'CONTRACT'` and `fullTimeEmployees()` filtering `EMP_TYPE == 'FULL_TIME'`),
unioned into one `Employee` set. The `testSuites:` data contains both types; the assertion
verifies all employees from both legs appear.

```pure
*demo::relation::domain::Employee: Operation
{
  union(contractEmployees, fullTimeEmployees)
}

contractEmployees: Relation
{
  ~func demo::relation::func::contractEmployees():Relation<Any>[1];
  // ...
}

fullTimeEmployees: Relation
{
  ~func demo::relation::func::fullTimeEmployees():Relation<Any>[1];
  // ...
}
```

---

### Model 11: `relation-relational-union`

**Mirrors:** (new)  
**Exercises:** Mixed union — one `Relation` set (`~func`) and one classic `Relational` set
under a single `Operation { union(…) }`.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Unit test references:**
- `mapping/union/relation/testRelationUnion.pure` →
  `testUnionOfRelationAndRelational_SimpleProject` (basic project across mixed union),
  `testUnionOfRelationAndRelational_FilterAcrossSets` (filter across mixed union).

**Key design:** Use the shared `EmployeeDB` — one leg is a `~func` returning a relation
over `EmployeeTable`; the other is a classic `Relational` mapping over the same (or a
parallel) table. Both legs map `Employee`.

The `testSuites:` data feeds the same underlying `EmployeeDB`; assertion verifies that
rows from both legs appear in the result and union semantics hold.

**Authoring note:** Mixed unions require that both set implementations share the same
primary-key column structure. Confirm that the column quoting / aliasing regression
(`testEnumerationMappingProjection` regression from shared-suite) does not affect this
model by keeping column names simple (no spaces).

---

### Model 12: `relation-milestoning`

**Mirrors:** (new)  
**Exercises:** A `<<temporal.processingtemporal>>` class backed by a milestoned relation
function; `testSuites:` with a processing-date point-in-time query; verifies that milestoned
rows outside the query date are excluded.  
**Pipeline phases:** PARSE, COMPILE, TEST_EXECUTION.

**Unit test references:**
- `mapping/relation/tests.pure` → `testMappingWithProcessingTemporalMilestoning`,
  `testMappingWithProcessingTemporalMilestoningAllVersionsInRange`.
- `mapping/relation/relationCoverageAllVersions.pure` → `testMilestoningAllVersions`
  (unbounded `allVersions()` query), `testMilestoningProcessingDate` (point-in-time
  `all(%date)` query).
- `mapping/relation/relationCoverageDates.pure` → reference for date column handling.

**Key design:**

Domain:

```pure
Class <<temporal.processingtemporal>>
  demo::relation::domain::TemporalEmployee
{
  id: Integer[1];
  firstName: String[1];
  salary: Float[1];
}
```

Database — add a milestoned table to `EmployeeDB`:

```
Table TemporalEmployeeTable
(
  ID        INTEGER PRIMARY KEY,
  FIRST_NAME VARCHAR(100) NOT NULL,
  SALARY    FLOAT NOT NULL,
  IN_Z      TIMESTAMP NOT NULL,
  OUT_Z     TIMESTAMP NOT NULL
)
```

Relation function — the function body selects from the temporal table; the milestoning
filter (on `IN_Z`/`OUT_Z`) is applied by the plan generator:

```pure
function demo::relation::func::temporalEmployees():Relation<(…)>[1]
{
  #>{demo::relation::db::EmployeeDB.default.TemporalEmployeeTable}#
}
```

Two `testSuites:` tests:

1. **Point-in-time query** `TemporalEmployee.all(%2024-01-01)` — data has two rows: one
   with `IN_Z < 2024-01-01 < OUT_Z` (should appear) and one expired before the query date
   (should not appear).

2. **`allVersions()` query** — same data; assert both rows appear (no date filter).

**Authoring note:** `IN_Z`/`OUT_Z` in the relation function column signature must be
`DateTime` (not `StrictDate`). Because milestoning columns are temporal sentinels, their
values must include a time component in the `Relation #{ … }#` data rows (e.g.
`'2023-06-01 00:00:00'`).

---

## File Layout Summary

The complete directory tree for all twelve models plus shared fixtures:

```
emit-models/
  relation-shared-domain.emit.yaml
  relation-shared-domain/
    model/
      employeeType.pure
      firm.pure
      employee.pure
      employment.pure
      position.pure                 # Position class (used by relation-groupBy)

  relation-shared-db.emit.yaml
  relation-shared-db/
    store/
      employeeDb.pure               # EmployeeTable, FirmTable, PositionTable, ProductTable,
                                    # TemporalEmployeeTable (add for relation-milestoning), plus joins

  relation-simple.emit.yaml         # ✅ Complete
  relation-simple/
    mapping/employeeMapping.pure    # ###Pure (employees func) + ###Mapping + testSuites
    data/testData.pure

  relation-service.emit.yaml        # ✅ Complete (depends on relation-simple.emit.yaml)
  relation-service/
    connection/h2Connection.pure
    runtime/employeeRuntime.pure
    service/employeeService.pure    # inline Relation test data; no data/ directory

  relation-join.emit.yaml
  relation-join/
    mapping/joinMapping.pure        # ###Pure (employees + firms funcs) + ###Mapping (XStore) + testSuites
    data/testData.pure

  relation-modelJoin.emit.yaml
  relation-modelJoin/
    mapping/modelJoinMapping.pure   # ###Pure (modelJoinEmployees + modelJoinFirms funcs) + ###Mapping (ModelJoin) + testSuites
    data/testData.pure

  relation-filter.emit.yaml
  relation-filter/
    mapping/filterMapping.pure      # ###Pure (activeEmployees func) + ###Mapping + testSuites
    data/testData.pure

  relation-groupBy.emit.yaml
  relation-groupBy/
    mapping/groupByMapping.pure     # ###Pure (positions func) + ###Mapping + testSuites
    data/testData.pure

  relation-enumeration.emit.yaml
  relation-enumeration/
    mapping/enumMapping.pure        # ###Pure (employees func) + ###Mapping + testSuites
    data/testData.pure

  relation-src.emit.yaml
  relation-src/
    mapping/srcMapping.pure         # ###Pure (activeEmployeeSource + filteredEmployeeSource) + ###Mapping
    data/testData.pure

  relation-expression-rhs.emit.yaml
  relation-expression-rhs/
    mapping/exprMapping.pure        # ###Pure (employees func) + ###Mapping + testSuites
    data/testData.pure

  relation-embedded.emit.yaml
  relation-embedded/
    mapping/embeddedMapping.pure    # ###Pure (employees func) + ###Mapping + testSuites
    data/testData.pure

  relation-union.emit.yaml
  relation-union/
    mapping/unionMapping.pure       # ###Pure (contractEmployees + fullTimeEmployees) + ###Mapping
    data/testData.pure

  relation-relational-union.emit.yaml
  relation-relational-union/
    mapping/mixedUnionMapping.pure  # ###Pure (relation func) + ###Mapping (mixed Relation/Relational)
    data/testData.pure

  relation-milestoning.emit.yaml
  relation-milestoning/
    mapping/milestoningMapping.pure # ###Pure (temporalEmployees func) + ###Mapping + testSuites
    data/testData.pure
```

**Dependency note:** `relation-service` reuses the mapping from `relation-simple` by declaring
`relation-simple.emit.yaml` as a dependency (option 1 — implemented). `relation-service/`
therefore contains only `connection/`, `runtime/`, and `service/` — no mapping or data files.
The EMIT runner resolves `dependencies:` transitively.

---

## EMIT YAML Feature Tag Conventions

New tags introduced for relation models (extend existing `scaffolding:`, `execution:`,
`grammar:`, `store:` namespaces):

| Tag | Meaning |
|:---|:---|
| `scaffolding:relation-function` | Model includes a Pure function returning `Relation<…>[1]` |
| `scaffolding:relation-mapping` | Model includes a `ClassName: Relation { ~func … }` mapping |
| `grammar:enumeration-mapping` | Model exercises an `EnumerationMapping` block |
| `grammar:embedded-relation` | Model exercises embedded / inline-embedded property mapping |
| `grammar:relation-union` | Model exercises `Operation { union(…) }` over relation sets |
| `grammar:relation-expression-rhs` | Model exercises expression-RHS property (`$src.col * …`) |
| `grammar:relation-src` | Model exercises `~src <expr>` inline source form |
| `grammar:milestoning` | Model exercises `<<temporal.*>>` milestoned class |
| `store:relation-model-join` | Model exercises XStore association (ModelJoin) between relation sets |

---

## Cross-Cutting Notes

### Reusing unit-test data shapes

The coverage databases in the unit tests (`testDB`, `coverageDB` in `relationMappingSetup.pure`)
are H2 in-memory schemas. The EMIT `Relation #{ … }#` rows must cover the same logical
scenarios (same column values, same edge cases) that the unit tests verify. Use the unit test
expected TDS results directly as the `data:` in EMIT JSON assertions.

### Stereotype reminder — `<<test.AlloyOnly>>`

The Pure unit tests for relation mappings carry `<<test.Test, test.AlloyOnly>>`. They are
**not** executed by `Test_Pure_Relational` (the compiled H2 runner) — see
[`reference_relation_test_runner_alloyonly`](../../..) memory entry. EMIT is therefore the
primary end-to-end execution path that validates these features.

### Out of EMIT scope (for now)

The following are excluded from this plan — blocked at the engine layer:

| Feature | Reason | Gap doc reference |
|:---|:---|:---|
| Advanced union / sub-aggregation-in-union | 12 `<<test.ToFix>>` engine gaps | Section 5, `testRelationUnionAdvanced.pure` |
| `importDataFlow=true` / U_TYPE discriminator | Engine gap (matrix #28) | Section 5 |
| Binding transformer | Execution path not available | Matrix #10 |
| `extends [setId]` mapping inheritance | Second-pass compiler unimplemented | Matrix #27 |
| Subtype / superclass polymorphic dispatch | `findMappingForType` unimplemented | Matrix #26, Section 5 |
| Store substitution / model chain | UNCERTAIN — probe G-A first | Section 4, G-A |
| Sub-aggregation SQL edge cases | 2 `<<test.ToFix>>` | Section 5 |
| Filter (across Operation-union legs) on a non-projected, expression-derived property | SQL-gen bug: missing column pull-through + invalid `= 1 = true` fragment; found while implementing `relation-union` | This doc, Model 10 |

---

## Recommended Authoring Order

```
1.  relation-shared-domain  (no deps; write first)                  ✅ DONE
2.  relation-shared-db      (depends on shared-domain)              ✅ DONE
3.  relation-simple         (Wave 1; validates testSuites path)     ✅ DONE — passes
4.  relation-service        (Wave 1; validates plan-generation path) ✅ DONE — passes
5.  relation-filter         (Wave 2; simplest function-body feature)
6.  relation-enumeration    (Wave 2; EnumerationMapping)
7.  relation-join           (Wave 2; XStore association)
7b. relation-modelJoin      (extra; ModelJoin as an alternative association wiring)
8.  relation-groupBy        (Wave 2; groupBy + aggregates)
9.  relation-src            (Wave 3; ~src inline form)
10. relation-expression-rhs (Wave 3; expression RHS + toOne)
11. relation-embedded       (Wave 3; embedded property)
12. relation-union          (Wave 4; union of two Relation sets)
13. relation-relational-union (Wave 4; mixed union)
14. relation-milestoning    (Wave 4; temporal milestoning)
```

**Gate:** If `relation-simple` (step 3) fails at TEST_EXECUTION, diagnose whether the
`MappingTestRunner` store-test-data path needs additional wiring for relation function
mappings before proceeding. This also determines whether matrix #32
(`testSuites / Testables`) reclassifies from `NEW_DEV` to `TEST_ONLY`.
