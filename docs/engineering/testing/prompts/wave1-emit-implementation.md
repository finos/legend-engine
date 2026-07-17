# Wave 1 EMIT Implementation Prompt

Use this prompt verbatim (or paste into a new Claude Code session) to implement Wave 1 of
the relation mapping EMIT integration tests.

---

## Prompt

You are implementing **Wave 1** of the relation-mapping EMIT integration test suite for the
legend-engine repository at `/home/developer/projects/finos-legend-engine`.

EMIT (Engine Model Integration Test) exercises the full six-phase pipeline —
INITIALIZATION → PARSE → COMPILE → MODEL_GENERATION → FILE_GENERATION → TEST_EXECUTION →
PLAN_GENERATION — from raw `.pure` source, exactly as Studio/HTTP clients do. You are
creating four models that together cover the foundational pipeline for relation (`~func`)
mappings.

**Do not run Maven builds or execute tests.** Write files only.

---

### Background: What you are building

Relation mappings use the form:

```pure
*MyClass: Relation
{
  ~func myPackage::myFunc():Relation<Any>[1];
  prop1: COLUMN_NAME,
  boolProp: $src.FLAG_COL == 1
}
```

The `~func` points to a named Pure function that returns a `Relation`. The mapping compiler
validates that property multiplicities match column nullability. This is distinct from the
classic `Relational` mapping form.

You are adding four models to the EMIT model directory. Existing relational EMIT models
(e.g. `relational-simple`, `relational-service`) are your structural reference for YAML
format, copyright headers, and `###Section` grammar layout. Read them before starting.

---

### Key files to read first

Before writing any files, read these to understand the patterns you must follow:

1. **Existing EMIT model structure:**
   - `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/relational-simple.emit.yaml`
   - `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/relational-simple/mapping/personMapping.pure`
   - `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/relational-simple/data/testData.pure`
   - `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/relational-service.emit.yaml`
   - `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/relational-service/connection/h2Connection.pure`
   - `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/relational-service/runtime/personRuntime.pure`
   - `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/relational-service/service/personService.pure`
   - `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/relational-shared-domain.emit.yaml`

2. **Relation-specific data format reference** (shows the `Relation #{ ... }#` grammar):
   - `legend-engine-core/legend-engine-core-testable/legend-engine-test-runner-function/src/test/resources/testable/legend-testable-function-test-relation-relationDatabaseAccessor.pure`

3. **Unit tests to use as authoring reference** (read these to understand what behaviour
   each EMIT model is testing):
   - `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/tests/mapping/relation/tests.pure`
     → functions `testSimpleMappingQuery`, `testNullableSalaryMapping`
   - `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/tests/mapping/relation/relationMappingSetup.pure`
     → the `testDB` schema and `setUp()` function

---

### Critical authoring rules

Violating any of these will cause a compile or runtime failure.

**Rule 1 — Multiplicity: `[1]` property requires a `NOT NULL` column.**
The multiplicity validator (introduced in PR #4941) rejects a `[1]` property if the backing
column is nullable. Every column that backs a `[1]` property must declare `NOT NULL` in the
`###Relational Database` block. A nullable column (no `NOT NULL`) infers as `[0..1]`.

**Rule 2 — `Boolean[1]` properties require an expression RHS.**
A `BIT` (or `BOOLEAN`) SQL column is typed as `TinyInt` in the relation type system. Direct
column mapping to a `Boolean[1]` property fails with a type mismatch. Always write:

```pure
active: $src.ACTIVE == 1
```

**Rule 3 — `StrictDate` columns cause assertion failures; use `TIMESTAMP` instead.**
The TDS result parser only recognises full datetimes (`YYYY-MM-DDT00:00:00.000+0000`). A
`DATE` column materialises as a date-only string and causes
`"Not supported data type: 'STRING' for Pure type: 'StrictDate'"` at assertion time.
Use `TIMESTAMP NOT NULL` for any date-like column in the shared DB, and map it to
`DateTime[1]` in the domain class. Values in test data rows must include a time component
(`'2020-01-15 00:00:00'`).

**Rule 4 — Test data format for mapping `testSuites:`.**
Reference the **underlying Database** (not the function) in the `data:` block. Use the
`Relation #{ … }#` format. The exact grammar is:

```
Relation
#{
  schemaName.TableName:
    col1,col2,col3
    val1,val2,val3
    val4,val5,val6;
}#
```

- Column names comma-separated on one line, no quotes.
- One data row per line, comma-separated. String values containing commas must be quoted
  with `"..."`.
- A semicolon `;` terminates the last data row.
- The schema name is `default` for H2 unless the database definition uses a named schema.

**Rule 5 — Copyright header.**
Every `.pure` and `.yaml` file must begin with the Apache 2.0 header. Copy the exact header
from any existing file in the `emit-models/` directory.

**Rule 6 — `EnumerationMapping` is required for enum properties.**
Even if a specific EMIT model does not assert on the enum property, if the domain class has
an `Enum[1]` property, the mapping must include an `EnumerationMapping` block and reference
it for that property, or the compiler will reject the mapping. Include it in `relation-simple`.

**Rule 7 — YAML `dependencies:` is nested under `modelSources:`, not at top level.**
The EMIT loader (`EMITModelDescriptor`) deserialises `dependencies` as a field of its inner
`ModelSources` class. A top-level `dependencies:` key is **silently ignored** due to
`@JsonIgnoreProperties(ignoreUnknown = true)`, causing all dependency models to be absent
at runtime with no error.

Correct structure — `dependencies:` indented inside `modelSources:`:

```yaml
modelSources:
  model:
    root: relation-simple
    files:
      - func/employeeFunc.pure
  dependencies:
    - source: relation-shared-domain.emit.yaml
    - source: relation-shared-db.emit.yaml
```

Wrong structure — `dependencies:` at top level (silently ignored):

```yaml
modelSources:
  model:
    root: relation-simple
    files:
      - func/employeeFunc.pure
dependencies:               # WRONG — ignored by the EMIT loader
  - source: relation-shared-domain.emit.yaml
```

Verify by reading `relational-simple.emit.yaml` and `relational-service.emit.yaml` — both
nest `dependencies:` inside `modelSources:`.

**Rule 8 — Service testSuite connection key is the runtime connection *id*, not the element name.**
In a `Service` testSuite `data: [ connections: [ KEY: ... ] ]` block, `KEY` must be the
**id** used in the Runtime's `connections:` map (e.g. `h2`), not the `Connection` element's
package path or name.

```pure
// Runtime — the map id is 'h2'
connections:
[
  demo::relation::db::EmployeeDB:
  [
    h2: demo::relation::connection::EmployeeH2Connection
  ]
];

// Service testSuite data — use the id 'h2', not 'EmployeeH2Connection'
connections:
[
  h2:
    Relation #{ ... }#
]
```

Reference: `relational-service/service/personService.pure` uses `h2:` as the key;
`relational-service/runtime/personRuntime.pure` maps `h2: demo::connection::PersonH2Connection`.

---

### What to create

All files go under:
```
legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-emit/src/test/resources/emit-models/
```

Create four models in this order (each depends on the previous shared fixtures):

1. `relation-shared-domain` (shared fixture — domain types only)
2. `relation-shared-db` (shared fixture — database schema)
3. `relation-simple` (Wave 1, model 1)
4. `relation-service` (Wave 1, model 2)

---

### Model 1: `relation-shared-domain` (shared fixture)

This is a pure-domain model: no stores, no mappings, no functions. Its own EMIT run
only exercises parse and compile. Downstream models list it as a dependency.

**`relation-shared-domain.emit.yaml`:**

```yaml
name: relation-shared-domain
title: "Shared Domain Types for Relation Mapping EMIT Models"
description: |
  Employee, Firm, EmployeeType enumeration, and their Employment association —
  the common domain types reused across all relation-mapping EMIT scenarios.
  No store, mapping or runtime is declared here; those are layered on by
  each consuming model via the EMIT dependencies mechanism.

modelSources:
  model:
    root: relation-shared-domain
    files:
      - model/employeeType.pure
      - model/firm.pure
      - model/employee.pure
      - model/employment.pure

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

**`relation-shared-domain/model/employeeType.pure`:**

```pure
###Pure
Enum demo::relation::domain::EmployeeType
{
  FULL_TIME,
  CONTRACT
}
```

**`relation-shared-domain/model/firm.pure`:**

```pure
###Pure
Class demo::relation::domain::Firm
{
  id: Integer[1];
  legalName: String[1];
}
```

**`relation-shared-domain/model/employee.pure`:**

```pure
###Pure
Class demo::relation::domain::Employee
{
  id: Integer[1];
  firstName: String[1];
  lastName: String[1];
  employeeType: demo::relation::domain::EmployeeType[1];
  active: Boolean[1];
  hireDate: DateTime[1];
  salary: Float[0..1];
  fullName() { $this.firstName + ' ' + $this.lastName }: String[1];
}
```

Notes:
- `active` is `Boolean[1]` — backed by a `BIT NOT NULL` column; requires `$src.ACTIVE == 1`
  expression RHS (Rule 2).
- `hireDate` is `DateTime[1]` (not `StrictDate[1]`) — backed by a `TIMESTAMP NOT NULL`
  column (Rule 3).
- `salary` is `Float[0..1]` — backed by a nullable `FLOAT` column; no `->toOne()` needed.

**`relation-shared-domain/model/employment.pure`:**

```pure
###Pure
Association demo::relation::domain::Employment
{
  employer: demo::relation::domain::Firm[1];
  employees: demo::relation::domain::Employee[*];
}
```

---

### Model 2: `relation-shared-db` (shared fixture)

This model declares only the H2 database schema. No functions, mappings, or connections.

**`relation-shared-db.emit.yaml`:**

```yaml
name: relation-shared-db
title: "Shared H2 Database for Relation Mapping EMIT Models"
description: |
  EmployeeDB — the H2-backed relational store for Employee and Firm relation functions.
  Contains EmployeeTable and FirmTable with a join, plus PositionTable and ProductTable
  for aggregation scenarios used by later EMIT models.

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

**`relation-shared-db/store/employeeDb.pure`:**

```pure
###Relational
Database demo::relation::db::EmployeeDB
(
  Table EmployeeTable
  (
    ID         INTEGER PRIMARY KEY,
    FIRST_NAME VARCHAR(100) NOT NULL,
    LAST_NAME  VARCHAR(100) NOT NULL,
    FIRM_ID    INTEGER NOT NULL,
    EMP_TYPE   VARCHAR(20) NOT NULL,
    ACTIVE     BIT NOT NULL,
    HIRE_DATE  TIMESTAMP NOT NULL,
    SALARY     FLOAT
  )

  Table FirmTable
  (
    ID         INTEGER PRIMARY KEY,
    LEGAL_NAME VARCHAR(100) NOT NULL
  )

  Table PositionTable
  (
    ID         INTEGER PRIMARY KEY,
    ACC_NUM    INTEGER NOT NULL,
    GSN        VARCHAR(20) NOT NULL,
    PRODUCT_ID INTEGER NOT NULL,
    QTY        INTEGER NOT NULL
  )

  Table ProductTable
  (
    ID          INTEGER PRIMARY KEY,
    DESCRIPTION VARCHAR(100) NOT NULL
  )

  Join EmployeeFirmJoin(EmployeeTable.FIRM_ID = FirmTable.ID)
  Join PositionProductJoin(PositionTable.PRODUCT_ID = ProductTable.ID)
)
```

Notes:
- All `[1]`-mapped columns are `NOT NULL` (Rule 1).
- `ACTIVE` is `BIT NOT NULL` — matches `active: Boolean[1]` via expression RHS (Rule 2).
- `HIRE_DATE` is `TIMESTAMP NOT NULL` — matches `hireDate: DateTime[1]` (Rule 3).
- `SALARY` is nullable — matches `salary: Float[0..1]`.
- `PositionTable` and `ProductTable` are placeholders for Wave 2's `relation-groupBy` model.

---

### Model 3: `relation-simple`

This is the foundational model. It validates that the `~func` relation mapping form
compiles and that `testSuites:` executes end-to-end via `MappingTestRunner`.

**This model is a gate:** if TEST_EXECUTION fails, diagnose the failure before creating
`relation-service` or any Wave 2 models. A failure here indicates either a
`MappingTestRunner` wiring gap for relation function mappings or a grammar issue, and
must be fixed at the engine level before proceeding.

**`relation-simple.emit.yaml`:**

```yaml
name: relation-simple
title: "Relation Function Class Mapping — Simple Property Mapping with Test Suite"
description: |
  The Employee class from relation-shared-domain is mapped to an H2 table via a
  ~func relation function. A mapping testSuite loads row data via a Data element
  and asserts that all()->project() returns the expected JSON. This is the
  foundational end-to-end check for the relation mapping pipeline: grammar parse,
  compile (including the #4941 multiplicity validator), and TEST_EXECUTION via
  MappingTestRunner.

modelSources:
  model:
    root: relation-simple
    files:
      - func/employeeFunc.pure
      - mapping/employeeMapping.pure
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
complexity: basic
tags:
  - h2
  - mapping-test
  - relation
```

**`relation-simple/func/employeeFunc.pure`:**

The function returns all rows from `EmployeeTable`. The return type signature must name
every column that the mapping references.

```pure
###Pure
import meta::pure::precisePrimitives::*;

function demo::relation::func::employees(): meta::pure::metamodel::relation::Relation<(ID: Integer, FIRST_NAME: Varchar(100), LAST_NAME: Varchar(100), FIRM_ID: Integer, EMP_TYPE: Varchar(20), ACTIVE: TinyInt, HIRE_DATE: DateTime, SALARY: Float)>[1]
{
  #>{demo::relation::db::EmployeeDB.default.EmployeeTable}#
}
```

**Important:** Look at the unit test reference to get the exact column type syntax used in
the relation type signature. Read `testSimpleMappingQuery` in `tests.pure` and the
`testDB` definition in `relationMappingSetup.pure`. The column types in the `Relation<(…)>`
signature must match what H2 reports for those SQL types:
- `INTEGER` → `Integer`
- `VARCHAR(n)` → `Varchar(n)` (from `meta::pure::precisePrimitives`)
- `BIT` → `TinyInt`
- `TIMESTAMP` → `DateTime`
- `FLOAT` → `Float`

**`relation-simple/mapping/employeeMapping.pure`:**

```pure
###Mapping
Mapping demo::relation::mapping::EmployeeSimpleMapping
(
  *demo::relation::domain::Employee: Relation
  {
    ~func demo::relation::func::employees():meta::pure::metamodel::relation::Relation<Any>[1];
    id: ID,
    firstName: FIRST_NAME,
    lastName: LAST_NAME,
    employeeType: EnumerationMapping EmployeeTypeMapping: EMP_TYPE,
    active: $src.ACTIVE == 1,
    hireDate: HIRE_DATE,
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
                  ->sortBy(x | $x.firstName)
                  ->project(
                    [x | $x.firstName, x | $x.lastName],
                    ['First Name', 'Last Name']
                  );
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
                    data: '[{"First Name":"Alice","Last Name":"Green"},{"First Name":"Bob","Last Name":"Smith"},{"First Name":"Carol","Last Name":"White"}]';
                  }#;
              }#
          ];
        }
      ];
    }
  ]
)
```

Notes:
- `id: ID` — maps `Integer[1]` to `ID INTEGER PRIMARY KEY` (NOT NULL implied by PK). ✓
- `firstName: FIRST_NAME` — maps `String[1]` to `FIRST_NAME VARCHAR(100) NOT NULL`. ✓
- `employeeType: EnumerationMapping EmployeeTypeMapping: EMP_TYPE` — enum transformer. ✓
- `active: $src.ACTIVE == 1` — Boolean coercion from BIT (Rule 2). ✓
- `hireDate: HIRE_DATE` — maps `DateTime[1]` to `TIMESTAMP NOT NULL`. ✓
- `salary: SALARY` — maps `Float[0..1]` to nullable `FLOAT`. ✓ No toOne() needed.
- The `data:` block keys to `demo::relation::db::EmployeeDB` (the **Database**, not the
  function). This routes through `RelationalConnectionFactory.tryBuildTestConnectionsForStore`
  which converts `RelationElementsData` to H2 CSV. See Rule 4.

**`relation-simple/data/testData.pure`:**

```pure
###Data
Data demo::relation::data::EmployeeData
{
  Relation
  #{
    default.EmployeeTable:
      ID,FIRST_NAME,LAST_NAME,FIRM_ID,EMP_TYPE,ACTIVE,HIRE_DATE,SALARY
      1,Alice,Green,1,FULL_TIME,1,2020-01-15 00:00:00,90000.0
      2,Bob,Smith,2,CONTRACT,1,2021-06-01 00:00:00,75000.0
      3,Carol,White,1,FULL_TIME,0,2019-03-10 00:00:00,;
  }#
}
```

Notes on the `Relation #{ … }#` format (Rule 4):
- Column headers are on a single line, comma-separated, no spaces or quotes.
- One row per line. String values do not need quoting unless they contain a comma.
- `SALARY` for row 3 (Carol) is `null` — leave an empty value before the `;`.
- The last data row ends with `;`.
- The schema prefix is `default` (H2's default schema).
- `HIRE_DATE` values use `YYYY-MM-DD HH:MM:SS` format (TIMESTAMP).
- All three employees sort to `[Alice, Bob, Carol]` alphabetically, matching the assertion.

**Verify the assertion:** The `testSuites:` projects only `firstName` and `lastName`,
sorted by `firstName`. Carol is `ACTIVE = 0` but there is no filter — all three rows
should appear. If you want the test to be more interesting, add a second test case that
filters `->filter(e | $e.active)` and asserts only Alice and Bob appear.

---

### Model 4: `relation-service`

This model wraps the same mapping as `relation-simple` in a `PureSingleExecution` Service
and validates PLAN_GENERATION (Phase 6) in addition to TEST_EXECUTION.

**Depends on `relation-simple`** via the `dependencies:` mechanism — do not duplicate the
mapping or func files; reference them through the EMIT dependency on `relation-simple.emit.yaml`.

**`relation-service.emit.yaml`:**

```yaml
name: relation-service
title: "Relation Function Mapping — Service with Test Suite and Plan Generation"
description: |
  A PureSingleExecution Service whose query targets EmployeeSimpleMapping (from
  relation-simple) backed by a relation function over H2. Exercises EMIT Phase 5
  (Service test execution with connection-keyed Relation data) and Phase 6 (plan
  generation). Demonstrates the full plumbing: RelationalDatabaseConnection,
  Runtime referencing the connection, Service, and a service testSuite that primes
  test data per connection.

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
  - scaffolding:class
  - scaffolding:relation-function
  - scaffolding:relation-mapping
  - scaffolding:relational-connection
  - scaffolding:runtime
  - execution:service
  - execution:service-test
stores:
  - relational
complexity: basic
tags:
  - h2
  - service-test
  - plan-generation
  - relation
```

**`relation-service/connection/h2Connection.pure`:**

```pure
###Connection
RelationalDatabaseConnection demo::relation::connection::EmployeeH2Connection
{
  store: demo::relation::db::EmployeeDB;
  type: H2;
  specification: LocalH2
  {
  };
  auth: DefaultH2;
}
```

**`relation-service/runtime/employeeRuntime.pure`:**

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

**`relation-service/service/employeeService.pure`:**

The service testSuite uses connection-keyed data (not store-keyed). The connection key
in the `data: [ connections: [ … ] ]` block must be the **id** from the Runtime's
`connections:` map — in this case `h2` (not the Connection element name
`EmployeeH2Connection`). See Rule 8.

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
              ->sortBy(x | $x.firstName)
              ->project(
                [x | $x.firstName, x | $x.lastName],
                ['First Name', 'Last Name']
              );
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
          h2:
            Relation
            #{
              default.EmployeeTable:
                ID,FIRST_NAME,LAST_NAME,FIRM_ID,EMP_TYPE,ACTIVE,HIRE_DATE,SALARY
                1,Alice,Green,1,FULL_TIME,1,2020-01-15 00:00:00,90000.0
                2,Bob,Smith,2,CONTRACT,1,2021-06-01 00:00:00,75000.0;
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

Notes:
- The service testSuite `data:` block is under `connections:`, not `stores:`. The
  connection key (`h2`) must exactly match the **id** used in the Runtime's `connections:`
  map — not the Connection element name. Look at `relational-service/service/personService.pure`
  to see the same pattern with `h2` as the key name (Rule 8).
- Two rows of data (no null salary) so the assertion is clean.
- `serializationFormat: PURE_TDSOBJECT` — required for service tests that project a TDS.

---

### Self-check before finishing

After creating all files, verify:

1. **File count:**
   - `relation-shared-domain/`: 1 YAML + 4 `.pure` files = 5 files
   - `relation-shared-db/`: 1 YAML + 1 `.pure` file = 2 files
   - `relation-simple/`: 1 YAML + 3 `.pure` files = 4 files
   - `relation-service/`: 1 YAML + 3 `.pure` files = 4 files
   - Total: 15 files

2. **Copyright headers** on every file (Apache 2.0, same format as existing models).

3. **`EnumerationMapping`** present in `employeeMapping.pure` and referenced by the
   `employeeType` property.

4. **`active: $src.ACTIVE == 1`** (not `active: ACTIVE`) in the mapping.

5. **`HIRE_DATE TIMESTAMP NOT NULL`** in the database (not `DATE`).

6. **Data rows end with `;`** inside `Relation #{ … }#` blocks.

7. **The `data:` store key in the mapping testSuite** is
   `demo::relation::db::EmployeeDB` (the Database packageable element path, not a
   function path).

8. **The `data:` connection key in the service testSuite** is `h2` — the **id** from the
   Runtime's `connections:` map, not the Connection element name (`EmployeeH2Connection`).
   Verify by reading `relational-service/service/personService.pure` and
   `relational-service/runtime/personRuntime.pure`.

9. **`relation-service.emit.yaml`** lists `relation-simple.emit.yaml` as a dependency
   (so the mapping and func files are available to it).

10. **No `###` section in a file that doesn't need it** — for example, the YAML files have
    no Pure grammar sections; `.pure` files with only `###Pure` content don't include
    `###Relational` or `###Mapping` sections.

11. **`dependencies:` is nested under `modelSources:`** in every YAML file that has
    dependencies — not at top level. A top-level `dependencies:` is silently ignored by the
    EMIT loader (Rule 7).

---

### What the EMIT runner discovers

The `RelationalEMITTests.java` runner calls `EMITTestSuiteBuilder.testContainers("emit-models/")`
which scans the classpath for all `*.emit.yaml` files and dynamically generates JUnit 5
test containers. Each YAML file's `modelSources:` files and `dependencies:` are assembled
into a `PureModelContextData`, then each EMIT phase runs as a distinct dynamic test. Your
new models will be automatically discovered by the existing runner without any Java changes.
