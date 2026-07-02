# Relation Mappings (`~func`)

> **Audience.** Engine developers working on class-to-relation mappings
> (`RelationFunctionInstanceSetImplementation`): grammar, compiler, SQL generator,
> and routing layer. This document covers the full feature set — from the simplest
> primitive-column mapping through primary keys, local properties, binding
> (semi-structured), enumeration, embedded, and union variants.
>
> **Authoritative sources** (referenced throughout):
>
> | Stage | File |
> |-------|------|
> | Lexer grammar | `legend-engine-language-pure-grammar/.../antlr4/mapping/relationFunctionMapping/RelationFunctionMappingLexerGrammar.g4` |
> | Parser grammar | `legend-engine-language-pure-grammar/.../antlr4/mapping/relationFunctionMapping/RelationFunctionMappingParserGrammar.g4` |
> | Parse-tree walker | `legend-engine-language-pure-grammar/.../mapping/RelationFunctionMappingParseTreeWalker.java` |
> | Grammar entry point | `legend-engine-language-pure-grammar/.../CorePureGrammarParser.java` (`parseRelationFunctionClassMapping`) |
> | Protocol — class mapping POJO | `legend-engine-protocol-pure/.../mapping/relationFunction/RelationFunctionClassMapping.java` |
> | Protocol — property mapping POJO | `legend-engine-protocol-pure/.../mapping/relationFunction/RelationFunctionPropertyMapping.java` |
> | Protocol — embedded POJO | `legend-engine-protocol-pure/.../mapping/relationFunction/RelationFunctionEmbeddedPropertyMapping.java` |
> | Compiler — first pass | `legend-engine-language-pure-compiler/.../toPureGraph/ClassMappingFirstPassBuilder.java` |
> | Compiler — second pass | `legend-engine-language-pure-compiler/.../toPureGraph/ClassMappingSecondPassBuilder.java` |
> | Compiler — third pass | `legend-engine-language-pure-compiler/.../toPureGraph/ClassMappingThirdPassBuilder.java` |
> | Compiler — property mappings | `legend-engine-language-pure-compiler/.../toPureGraph/PropertyMappingBuilder.java` |
> | Compiler — validation | `legend-engine-language-pure-compiler/.../toPureGraph/validator/MappingValidator.java` |
> | Helper functions (Pure) | `core_relational/relational/helperFunctions/helperFunctions.pure` |
> | SQL generation — main | `core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure` (`processRelationFunctionClassMapping`) |
> | SQL generation — union | `core_relational/relational/pureToSQLQuery/pureToSQLQuery_union.pure` (`buildUnion`) |
> | Router — store contract | `core/pure/router/store/cluster.pure` (`storeContractForSetImplementation`) |
> | Router — set routing | `core/pure/router/store/routing.pure` (`potentiallyRouteSetImplementations`) |

---

## 0. What is a Relation Mapping?

A **Relation mapping** (`Relation` keyword in mapping grammar, `~func` inside the
block) lets you map a Pure class to the output of an arbitrary Pure
`Relation<Any>` expression — most commonly a function that builds a typed tabular
result backed by a relational store, a `#>{db.table}#` relation accessor, or any
other Pure expression that yields a `Relation<T>`.

Compared with the classic `Relational` mapping (which is tightly coupled to a
physical schema via `~mainTable`, join graphs, and `[db]Table.Column` paths), a
Relation mapping:

- uses a **Pure function** as its data source — the function is compiled and
  type-checked like any other Pure code;
- binds properties to **column names** by string label, not by table path;
- feeds SQL generation through `processRelationFunctionClassMapping`, which
  evaluates the function expression and wraps it in a sub-select rather than
  referencing a physical table directly.

The primary key can be declared explicitly with `~primaryKey`, or inferred at
runtime from the relation function's output via
`resolveRelationFunctionPrimaryKey`.

---

## 1. Complete Grammar Reference

### 1.1 Lexer tokens

```antlr
RELATION_FUNC:        '~func' ;
RELATION_PRIMARY_KEY: '~primaryKey' ;
BINDING:              'Binding' ;
ENUMERATION_MAPPING:  'EnumerationMapping' ;
INLINE:               'Inline' ;
```

All five tokens are introduced by `RelationFunctionMappingLexerGrammar.g4` and are
imported into the main M3 lexer hierarchy.

### 1.2 Parser rules

```antlr
relationFunctionMapping:
    RELATION_FUNC functionIdentifier
    primaryKey?
    (singlePropertyMapping (COMMA singlePropertyMapping)*)?
    EOF
;

primaryKey:
    RELATION_PRIMARY_KEY COLON
    (identifier | BRACKET_OPEN identifier (COMMA identifier)* BRACKET_CLOSE)
;

singlePropertyMapping:
    singleLocalPropertyMapping | singleNonLocalPropertyMapping
;

// Local (derived) property — adds a new property to the class in the mapping scope
singleLocalPropertyMapping:
    PLUS qualifiedName COLON type multiplicity relationFunctionPropertyMapping
;

// Standard property mapping
singleNonLocalPropertyMapping:
    qualifiedName
    (
        relationFunctionPropertyMapping
      | relationFunctionEmbeddedPropertyMapping
      | inlineRelationFunctionEmbeddedPropertyMapping
    )
;

// Column binding for a single property
relationFunctionPropertyMapping:
    COLON (transformer)? identifier
;

transformer:
    bindingTransformer | enumTransformer
;

bindingTransformer:
    BINDING qualifiedName COLON
;

enumTransformer:
    ENUMERATION_MAPPING identifier COLON
;

// Normal embedded — child columns in the same relation
relationFunctionEmbeddedPropertyMapping:
    PAREN_OPEN
    (singlePropertyMapping (COMMA singlePropertyMapping)*)?
    PAREN_CLOSE
;

// Inline embedded — delegates to a separately-declared class mapping
inlineRelationFunctionEmbeddedPropertyMapping:
    PAREN_OPEN PAREN_CLOSE INLINE BRACKET_OPEN identifier BRACKET_CLOSE
;
```

The block-type keyword (`Relation`) is registered in `CorePureGrammarParser` as
`RELATION_EXPRESSION` and dispatched to `parseRelationFunctionClassMapping`.

### 1.3 Full grammar skeleton

```
###Mapping
Mapping myPkg::MyMapping
(
  // Root class mapping — asterisk makes this the default mapping for the class
  *MyClass[optionalId]: Relation
  {
    ~func      myPkg::myFunction():Relation<Any>[1]
    ~primaryKey: [colA, colB]          // optional; inferred if omitted

    // Primitive or enum property
    primitiveProperty  : COLUMN_NAME
    enumProperty       : EnumerationMapping myEnumMapping : ENUM_COLUMN

    // Semi-structured (binary / JSON) property backed by a binding
    complexProperty    : Binding myPkg::MyBinding : SEMI_STRUCT_COLUMN

    // Normal embedded sub-object (columns in same relation)
    subObject
    (
      childProp1: CHILD_COL_1,
      childProp2: CHILD_COL_2
    )

    // Inline embedded — delegates to separately-declared addressSet mapping
    subObject2 () Inline [addressSet]

    // Local property (adds a transient property to the class in this mapping scope)
    +localProp: String[1] : LOCAL_COL
  }
)
```

---

## 2. Examples

### 2.1 Primitive columns (minimal)

```
###Pure
Class myPkg::Person
{
  firstName: String[1];
  age:       Integer[1];
}

function myPkg::personFunc(): Relation<(FIRSTNAME:String, AGE:Integer)>[1]
{
  #>{myDb.PERSON}#->select(~[FIRSTNAME, AGE])
}

###Mapping
Mapping myPkg::PersonMapping
(
  *Person: Relation
  {
    ~func myPkg::personFunc():Relation<Any>[1]
    firstName: FIRSTNAME,
    age:       AGE
  }
)
```

A query `Person.all()->filter(x | $x.age > 30)` routes through
`processRelationFunctionClassMapping`, evaluates the function body to get a
`SelectSQLQuery`, wraps it in a sub-select, then applies the filter on top.

### 2.2 Explicit primary key

```
*Person: Relation
{
  ~func      myPkg::personFunc():Relation<Any>[1]
  ~primaryKey: ID
  firstName: FIRSTNAME,
  age:       AGE
}
```

Multiple PK columns:

```
~primaryKey: [FIRST_NAME, LAST_NAME]
```

If `~primaryKey` is absent, the runtime calls
`resolveRelationFunctionPrimaryKey([])` which attempts to infer PK columns from
the relation function output via the `RelationElementAccessorExtension`
(e.g., table primary-key metadata for `#>{db.table}#` accessors).

### 2.3 Local (derived) property

Local properties extend the class within the mapping scope without modifying the
canonical Pure class definition:

```
*Person: Relation
{
  ~func myPkg::personFunc():Relation<Any>[1]
  firstName:  FIRSTNAME,
  +displayAge: String[1]: AGE_DISPLAY   // adds displayAge: String[1] to Person in this scope
}
```

### 2.4 Semi-structured column (binding transformer)

Complex JSON / binary columns are mapped via a `Binding`:

```
*Person: Relation
{
  ~func myPkg::personFunc():Relation<Any>[1]
  firstName: FIRSTNAME,
  address:   Binding myPkg::AddressBinding : ADDRESS_JSON
}
```

The compiler checks that the binding's model unit includes the property's return
type. At SQL generation time the column is treated as a `Variant` (semi-structured)
type and the binding transformer is carried through as
`SemiStructuredEmbeddedRelationalInstanceSetImplementation`.

### 2.5 Enumeration mapping

Map an enum-typed property to a relation column, converting raw string values:

```
###Pure
Enum myPkg::EmployeeType { CONTRACT; FULL_TIME; }

Class myPkg::Employee
{
  name:         String[1];
  employeeType: myPkg::EmployeeType[1];
}

###Mapping
Mapping myPkg::EmployeeMapping
(
  *Employee: Relation
  {
    ~func myPkg::employeeFunc():Relation<Any>[1]
    name:         NAME,
    employeeType: EnumerationMapping empTypeMap : EMP_TYPE
  }

  EmployeeType: EnumerationMapping empTypeMap
  {
    CONTRACT:  'CONTRACT',
    FULL_TIME: ['SALARY', 'FULL_TIME']
  }
)
```

### 2.6 Normal embedded mapping

Map a sub-object whose columns come from the same relation:

```
###Pure
Class myPkg::Address     { street: String[1]; city: String[1]; }
Class myPkg::PersonWithAddress
{
  firstName: String[1];
  address:   myPkg::Address[1];
}

###Mapping
Mapping myPkg::EmbeddedMapping
(
  *PersonWithAddress: Relation
  {
    ~func myPkg::personFunc():Relation<Any>[1]
    firstName: FIRSTNAME,
    address
    (
      street: STREET,
      city:   CITY
    )
  }
)
```

### 2.7 Inline embedded mapping

Delegate the sub-object mapping to a separately-declared class mapping (which may
use a different relation function):

```
*PersonWithAddress[personSet]: Relation
{
  ~func myPkg::personFunc():Relation<Any>[1]
  firstName: FIRSTNAME,
  address () Inline [addressSet]
}

*Address[addressSet]: Relation
{
  ~func myPkg::personFunc():Relation<Any>[1]
  street: STREET,
  city:   CITY
}
```

### 2.8 Union mapping

Two (or more) Relation class mappings unioned together:

```
*Person: Operation
{
  meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(
    personFT, personCT
  )
}

*Person[personFT]: Relation
{
  ~func myPkg::fullTimeFunc():Relation<Any>[1]
  firstName: FNAME,
  firmId:    FIRMID
}

*Person[personCT]: Relation
{
  ~func myPkg::contractFunc():Relation<Any>[1]
  firstName: FNAME,
  firmId:    FIRMID
}
```

Mixed union (one Relation, one Relational):

```
*Person: Operation
{
  meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(
    personRelation, personTable
  )
}
*Person[personRelation]: Relation  { ~func myPkg::f():Relation<Any>[1] ... }
*Person[personTable]:    Relational { ~mainTable [db]PERSON ... }
```

All leaves must still resolve to the **same store**.

---

## 3. Pipeline at a Glance

```
   Mapping grammar text
         │
         ▼  (1) Parser  [RelationFunctionMappingParseTreeWalker]
   Protocol POJOs
     ├── RelationFunctionClassMapping
     │     ├── relationFunction: PackageableElementPointer
     │     ├── primaryKey: List<String>
     │     └── propertyMappings: List<PropertyMapping>
     │           ├── RelationFunctionPropertyMapping   (column + optional transformer)
     │           └── RelationFunctionEmbeddedPropertyMapping (normal or inline)
         │
         ▼  (2) Compiler — 3 passes  [ClassMappingFirstPassBuilder,
         │                            ClassMappingSecondPassBuilder,
         │                            ClassMappingThirdPassBuilder]
   Pure graph objects
     └── RelationFunctionInstanceSetImplementation
           ├── class, id, root, parent
           ├── relationFunction: FunctionDefinition<?>   (resolved in 2nd pass)
           ├── primaryKey: Column[*]                     (resolved in 3rd pass)
           └── propertyMappings
                 ├── RelationFunctionPropertyMapping    (column: Column[1], transformer?)
                 └── EmbeddedRelationFunctionSetImplementation
         │
         ▼  (3) Transformation  [helperFunctions.pure]
   transformRelationFunctionClassMapping converts:
     ├── RelationFunctionPropertyMapping  → RelationalPropertyMapping (TableAliasColumn)
     └── EmbeddedSetImplementation        → EmbeddedRelationFunctionSetImplementation
         │
         ▼  (4) Routing  [cluster.pure, routing.pure]
   Store contract resolved per set:
     ├── RelationFunctionInstanceSetImplementation → from relationFunction's store
     ├── EmbeddedSetImplementation → inherits from owning RF set
     └── OperationSetImplementation (union) → resolved per leaf
         │
         ▼  (5) SQL Generation  [pureToSQLQuery.pure, pureToSQLQuery_union.pure]
   processRelationFunctionClassMapping
     ├── evaluates the function body → inner SelectSQLQuery
     ├── wraps in sub-select (moveSelectQueryToSubSelect)
     └── property resolution: findPropertyMapping → RelationalPropertyMapping
         ├── Enum: processPropertyMapping sets pushDownEnumTransformations=true
         ├── Embedded: EmbeddedRelationFunctionSetImplementation arm
         └── Union: buildUnion dispatches per leaf type
```

---

## 4. Parser

### 4.1 Entry point

`CorePureGrammarParser` registers `"Relation"` as a mapping-block keyword and
dispatches to `parseRelationFunctionClassMapping`. This method:

1. Reads the block header (`*ClassName[id]`, `root`, `extendsClassMappingId`).
2. Invokes `RelationFunctionMappingParseTreeWalker.visitRelationFunctionClassMapping`.

### 4.2 Walker: class-level fields

```java
// RelationFunctionMappingParseTreeWalker.visitRelationFunctionClassMapping
relationFunctionClassMapping.relationFunction =
    new PackageableElementPointer(FUNCTION, ctx.functionIdentifier().getText(), ...);

if (ctx.primaryKey() != null)
    relationFunctionClassMapping.primaryKey =
        ctx.primaryKey().identifier().stream()
           .map(PureGrammarParserUtility::fromIdentifier)
           .collect(Collectors.toList());
else
    relationFunctionClassMapping.primaryKey = Collections.emptyList();

relationFunctionClassMapping.propertyMappings =
    ctx.singlePropertyMapping().stream()
       .map(c -> this.visitPropertyMapping(c, ...))
       .collect(Collectors.toList());
```

### 4.3 Walker: property dispatch

`visitPropertyMapping` branches on which child rule is present:

| Branch | Result |
|--------|--------|
| `singleLocalPropertyMapping` | `RelationFunctionPropertyMapping` with `localMappingProperty` set |
| `relationFunctionEmbeddedPropertyMapping` | `RelationFunctionEmbeddedPropertyMapping` (normal) |
| `inlineRelationFunctionEmbeddedPropertyMapping` | `RelationFunctionEmbeddedPropertyMapping` (inline, `id` set, `propertyMappings = []`) |
| plain `relationFunctionPropertyMapping` | `RelationFunctionPropertyMapping` (column + optional transformer) |

For a **binding transformer**, `bindingTransformer.binding` is set from the
`qualifiedName` after `Binding`. For an **enum transformer**, `enumMappingId` is
set from the identifier after `EnumerationMapping`.

### 4.4 Protocol shapes after parsing

```
// Primitive / enum / binding property mapping
RelationFunctionPropertyMapping {
  property        : PropertyPointer("firstName")
  column          : "FIRSTNAME"           // raw string column name
  enumMappingId   : null | "empTypeMap"   // set for enum properties
  bindingTransformer: null | BindingTransformer{ binding: "myPkg::MyBinding" }
  localMappingProperty: null | LocalMappingPropertyInfo{ type, multiplicity }
}

// Normal embedded
RelationFunctionEmbeddedPropertyMapping {
  property        : PropertyPointer("address")
  id              : null
  propertyMappings: [
    RelationFunctionPropertyMapping { property: "street", column: "STREET" },
    RelationFunctionPropertyMapping { property: "city",   column: "CITY"   }
  ]
}

// Inline embedded
RelationFunctionEmbeddedPropertyMapping {
  property        : PropertyPointer("address")
  id              : "addressSet"
  propertyMappings: []
}
```

---

## 5. Compiler

The compiler operates in three sequential passes per class mapping.

### 5.1 First pass — `ClassMappingFirstPassBuilder`

Creates the `RelationFunctionInstanceSetImplementation` Pure graph node and
compiles property mappings (but cannot yet resolve the relation function, which
may be declared later in the same compilation unit):

```java
// ClassMappingFirstPassBuilder.visit(RelationFunctionClassMapping)
final RelationFunctionInstanceSetImplementation setImpl =
    new Root_meta_pure_mapping_relation_RelationFunctionInstanceSetImplementation_Impl(id, ...)
        ._class(pureClass)
        ._id(id)
        ._root(classMapping.root)
        ._parent(parentMapping)
        ._propertyMappings(
            ListIterate.collect(classMapping.propertyMappings,
                p -> p.accept(new PropertyMappingBuilder(context, baseSetImpl,
                                                         allEnumerationMappings)))
        );
```

Any `EmbeddedSetImplementation` nodes found in `propertyMappings` are returned as
the second element of the pair so the mapping compiler can register them.

### 5.2 Second pass — `ClassMappingSecondPassBuilder`

Resolves the relation function by its path/descriptor and attaches it to the set
implementation. Also propagates the `relationFunction` reference down into every
`EmbeddedRelationFunctionSetImplementation` (so embedded mappings share the same
backing function):

```java
// ClassMappingSecondPassBuilder.visit(RelationFunctionClassMapping)
FunctionDefinition<?> relationFunction =
    (FunctionDefinition<?>) context.resolvePackageableElement(functionId, ...);
setImpl._relationFunction(relationFunction);
propagateRelationFunctionToEmbedded(setImpl, relationFunction);
```

### 5.3 Third pass — `ClassMappingThirdPassBuilder`

Resolves explicit `~primaryKey` column names against the columns returned by the
relation function's typed `RelationType`:

```java
// ClassMappingThirdPassBuilder.visit(RelationFunctionClassMapping)
RichIterable<? extends Column<?, ?>> relationColumns = getRelationFunctionColumns(setImpl);

for (String pkName : classMapping.primaryKey)
{
    Column<?, ?> col = relationColumns.detect(c -> pkName.equals(c._name()));
    if (col == null)
        throw new EngineException("Primary key column '" + pkName + "' not found ...", ...);
    resolvedPK.add(col);
}
setImpl._primaryKey(resolvedPK);
```

If `primaryKey` is empty (omitted in grammar), the set's `primaryKey` list stays
empty; primary keys are then auto-inferred at SQL generation time by
`resolveRelationFunctionPrimaryKey`.

### 5.4 Property mapping: `PropertyMappingBuilder.visit(RelationFunctionPropertyMapping)`

**Multiplicity check** — only `[1]` or `[0..1]` are supported (no collection
properties in a flat tabular context).

**Type check** — the property's return type must be one of:
- A Pure primitive → column type is the primitive name.
- An Enum (with `enumMappingId` set) → column type is resolved from the relation
  function's `RelationType` via `resolveRelationColumnTypeName`.
- A complex class (with `bindingTransformer` set) → column type is `Variant`
  (semi-structured fallback).
- Anything else without a transformer → compilation error.

```java
String propertyTypeName =
    processorSupport.type_isPrimitiveType(propertyType)
        ? propertyType._name()
        : propertyMapping.enumMappingId != null
            ? resolveRelationColumnTypeName(this.immediateParent, propertyMapping.column)
            : M3Paths.Variant;   // binding fallback

RelationType<?> newRelationType = _RelationType.build(
    Lists.mutable.with(
        _Column.getColumnInstance(propertyMapping.column, false, propertyTypeName, ...)),
    sourceInfo, processorSupport);

RelationFunctionPropertyMapping pm = new Root_meta_pure_mapping_relation_RelationFunctionPropertyMapping_Impl(...)
    ._property(property)
    ._column(newRelationType._columns().toList().get(0))
    ._owner(immediateParent);
```

**Enum transformer attachment:**

```java
if (propertyMapping.enumMappingId != null)
{
    EnumerationMapping<Object> eMap =
        allEnumerationMappings.select(e -> e._name().equals(propertyMapping.enumMappingId))
                              .toList().getFirst();
    Assert.assertTrue(eMap != null, () -> "Can't find enumeration mapping '...'", ...);
    pm._transformer(eMap);
}
```

**Binding transformer attachment:**

```java
if (propertyMapping.bindingTransformer != null)
{
    Root_meta_external_format_shared_binding_Binding binding =
        (Root_meta_external_format_shared_binding_Binding) context.resolvePackageableElement(...);
    // Validates property type is in binding's modelUnit
    pm._transformer(new Root_meta_external_format_shared_binding_BindingTransformer_Impl<>(...));
    pm._targetSetImplementationId("");
}
```

#### `resolveRelationColumnTypeName` helper

Walks up through any chain of `EmbeddedSetImplementation` parents to find the
outermost `RelationFunctionInstanceSetImplementation`, then inspects that
function's last expression's `RelationType` to find the column's actual type:

```java
private static String resolveRelationColumnTypeName(
        PropertyMappingsImplementation parent, String columnName)
{
    // Walk up through embedded parents
    PropertyMappingsImplementation currentImpl = parent;
    while (currentImpl instanceof EmbeddedSetImplementation)
    {
        SetImplementation owner = (SetImplementation)
            ((EmbeddedSetImplementation) currentImpl)._owner();
        if (owner == null) break;
        currentImpl = (PropertyMappingsImplementation) owner;
    }
    if (currentImpl instanceof RelationFunctionInstanceSetImplementation)
    {
        // Read RelationType from function's return type
        // ... find column by name, return colRawType._name()
    }
    return "String"; // fallback
}
```

### 5.5 Property mapping: `PropertyMappingBuilder.visit(RelationFunctionEmbeddedPropertyMapping)`

Creates an `EmbeddedRelationFunctionSetImplementation` with:

```java
boolean isInline = propertyMapping.propertyMappings == null
                || propertyMapping.propertyMappings.isEmpty();
String inlineTargetId = propertyMapping.id; // null for normal embedded

String selfId, targetId;
if (isInline && inlineTargetId != null)
{
    selfId   = sourceId + "_" + propertyMapping.property.property;
    targetId = inlineTargetId;   // points to the separately-declared set
}
else
{
    String embeddedId = inlineTargetId != null ? inlineTargetId
                                               : sourceId + "_" + property.property;
    selfId   = embeddedId;
    targetId = embeddedId;       // self-referential (normal embedded)
}
```

The inner `propertyMappings` are compiled recursively with a fresh
`PropertyMappingBuilder` whose `immediateParent` is the new embedded set.

### 5.6 Validation — `MappingValidator`

After compilation the validator iterates over all
`RelationFunctionInstanceSetImplementation` class mappings and checks:

- Every mapped property exists on the target class (or is declared as a local
  property).
- Column names referenced in `~primaryKey` are present in the typed `RelationType`
  (enforced in the third pass above).
- The `EnumerationMapping` referenced by `enumMappingId` exists in the same
  parent `Mapping`.

---

## 6. Transformation Layer (`helperFunctions.pure`)

Before the SQL generator can process a `RelationFunctionInstanceSetImplementation`,
its `RelationFunctionPropertyMapping` nodes must be converted to
`RelationalPropertyMapping` (which carries a `TableAliasColumn` the SQL generator
works with). This conversion is performed by helper functions in
`core_relational/relational/helperFunctions/helperFunctions.pure`.

### 6.1 `transformRelationFunctionClassMapping`

Top-level entry point. Dispatches on the type of each property mapping:

```
transformRelationFunctionClassMapping(classMapping)
    classMapping.propertyMappings->map(pm | pm->match([
        r: RelationFunctionPropertyMapping[1]
            → transformRelationPropertyMappingsToRelational(r, classMapping),
        e: EmbeddedSetImplementation[1]
            → transformRelationFunctionEmbeddedPropertyMapping(e, classMapping),
        p: PropertyMapping[1]
            → p   // pass-through (already relational / binding)
    ]))
```

### 6.2 `transformRelationPropertyMappingsToRelational`

Converts one (or many) `RelationFunctionPropertyMapping` to a
`RelationalPropertyMapping`:

```
transformRelationPropertyMappingsToRelational(rfpm, classMapping)
    ^RelationalPropertyMapping(
        owner                        = rfpm.owner,
        sourceSetImplementationId    = rfpm.sourceSetImplementationId,
        targetSetImplementationId    = rfpm.targetSetImplementationId,
        property                     = rfpm.property,
        localMappingProperty         = rfpm.localMappingProperty,
        localMappingPropertyType     = rfpm.localMappingPropertyType,
        localMappingPropertyMultiplicity = rfpm.localMappingPropertyMultiplicity,
        store                        = rfpm.store,
        transformer                  = rfpm.transformer,    // carries EnumerationMapping
        relationalOperationElement   = getTransformedRelationFunctionRelOp(classMapping, rfpm)
    )
```

The `transformer` field is explicitly copied so that any `EnumerationMapping`
attached by the compiler is preserved across the conversion.

### 6.3 `getTransformedRelationFunctionRelOp`

Produces the `TableAliasColumn` that represents the relation-function column in
the relational SQL world:

```
getTransformedRelationFunctionRelOp(classMapping, rfpm)
    let relationColumnType = rfpm.column.classifierGenericType.typeArguments->at(1).rawType->toOne();
    ^TableAliasColumn(
        alias  = ^TableAlias(name = classMapping.id,
                              relationalElement = ^RelationFunction(owner = classMapping)),
        column = ^RelationFunctionColumn(
                     column = rfpm.column,
                     name   = rfpm.column.name->toOne(),
                     type   = pureTypeToDataType(relationColumnType)->toOne()
                 )
    )
```

### 6.4 `transformRelationFunctionEmbeddedPropertyMapping`

Converts an `EmbeddedSetImplementation` owned by a
`RelationFunctionInstanceSetImplementation` to an
`EmbeddedRelationFunctionSetImplementation`. Recursively transforms nested
property mappings. Inherits `relationFunction` from the **parent** class mapping
(embedded columns share the same backing relation):

```
transformRelationFunctionEmbeddedPropertyMapping(embedded, classMapping)
    let transformedPms = embedded.propertyMappings->map(pm | pm->match([
        r: RelationFunctionPropertyMapping[1]
            → transformRelationPropertyMappingsToRelational(r, classMapping),
        nested: EmbeddedSetImplementation[1]
            → transformRelationFunctionEmbeddedPropertyMapping(nested, classMapping),
        p: PropertyMapping[1] → p
    ]));
    ^EmbeddedRelationFunctionSetImplementation(
        id              = embedded.id,
        root            = false,
        class           = embedded.class,
        parent          = classMapping.parent,
        relationFunction= classMapping.relationFunction,   // inherited
        owner           = embedded.owner,
        property        = embedded.property,
        propertyMappings= transformedPms
    )
```

### 6.5 `normalizeRelationFunctionEmbeddedMapping`

A bridge used inside `findPropertyMapping` to lazily convert any
`EmbeddedSetImplementation` whose owner is a `RelationFunctionInstanceSetImplementation`
that has not yet been transformed:

```
normalizeRelationFunctionEmbeddedMapping(pm)
    pm->match([
        e: EmbeddedSetImplementation[1]
            if (!e->instanceOf(EmbeddedRelationalInstanceSetImplementation)
                && !e.owner->isEmpty()
                && e.owner->toOne()->instanceOf(RelationFunctionInstanceSetImplementation),
               | transformRelationFunctionEmbeddedPropertyMapping(e, ...),
               | $e),
        p: PropertyMapping[1] → $p
    ])
```

### 6.6 `findPropertyMapping` — extensions for RF sets

`findPropertyMapping` is the central dispatch for property resolution at execution
time. Two arms added / extended:

1. **`EmbeddedRelationalInstanceSetImplementation` arm** — now calls
   `normalizeRelationFunctionEmbeddedMapping` first so RF-backed embedded sets are
   normalised before the existing dispatch logic runs.

2. **`EmbeddedRelationFunctionSetImplementation` arm (new):**

   - **Normal embedded** — looks up the property directly in the embedded set's
     own `propertyMappings` (these are already `RelationalPropertyMapping` after
     transformation).

   - **Inline embedded** — follows `targetSetImplementationId` to the independently
     declared set, retrieves its `RelationFunctionPropertyMapping` nodes, and
     transforms them to `RelationalPropertyMapping` at resolution time:

     ```
     let result = $mapping->_classMappingByIdRecursive($inlineTargetIds)
         ->map(c | c->_propertyMappingsByPropertyName($propertyName))
         ->map(pm | pm->match([
             rfpm: RelationFunctionPropertyMapping[1]
                 → rfpm->transformRelationPropertyMappingsToRelational(
                       rfpm.owner->cast(@RelationFunctionInstanceSetImplementation)),
             p: PropertyMapping[1] → p
         ]));
     ```

### 6.7 Primary key helpers

```
// Returns TableAliasColumn instances for the set's primaryKey columns
getRelationFunctionPkAsTableAliasColumns(classMapping)
    let alias = ^TableAlias(name = classMapping.id,
                             relationalElement = ^RelationFunction(owner = classMapping));
    classMapping.primaryKey->map(c |
        ^TableAliasColumn(alias = alias,
                           column = ^RelationFunctionColumn(...)))

resolvePrimaryKey(rfi: RelationFunctionInstanceSetImplementation[1])
    rfi->getRelationFunctionPkAsTableAliasColumns()
```

---

## 7. Routing (`cluster.pure` / `routing.pure`)

### 7.1 `storeContractForSetImplementation`

Determines which `StoreContract` and `Store` handle a given set implementation.
Key dispatch arms:

| Set type | Resolution |
|----------|-----------|
| `RelationFunctionInstanceSetImplementation` | reads the store from the relation function's expression |
| `EmbeddedSetImplementation` | delegates to `owner` (new arm — embedded sets inherit from their RF parent) |
| `OperationSetImplementation` (union) | resolves each leaf recursively; all must share the same store |

**Union arm:**

```
o: OperationSetImplementation[1]
    let resolvedPairs = roots->map(r |
        r->cast(@SetImplementation)->storeContractForSetImplementation($mapping, $extensions));
    let storeContract = resolvedPairs.first->removeDuplicatesBy(x | x.id)->toOne();
    let store         = resolvedPairs.second->removeDuplicatesBy(x | x->elementToPath())->toOne();
    pair(storeContract, store)
```

The `->toOne()` on deduplication enforces that all union leaves resolve to the
same store.

### 7.2 `potentiallyRouteRelationFunctionSet`

Wires a `RelationFunctionInstanceSetImplementation` into the relational routing
infrastructure — attaches an alias and `SelectSQLQuery` stub so the set can
participate in SQL plan building.

### 7.3 `potentiallyRouteSetImplementations` — union extension

When the routing cache (`classMappingsByClass`) is built for a union, the function
now also routes `OperationSetImplementation` wrappers through
`potentiallyRouteRelationFunctionSet` so their RF leaves are correctly wired:

```
let routedNonRelFuncMappings = nonRelFuncMappings->map(cm | cm->match([
    o: OperationSetImplementation[1]
        mappingsFromCache->filter(mc | mc.id == o.id)
            ->defaultIfEmpty(o)->toOne()
            ->potentiallyRouteRelationFunctionSet($mapping, $runtime, $extensions),
    a: SetImplementation[1] → $a
]));
```

---

## 8. SQL Generation

### 8.1 `processRelationFunctionClassMapping`

The central SQL generation function for relation mappings:

```
processRelationFunctionClassMapping(r, vars, state, ...)
    // 1. Auto-infer PK if not already set
    let resolved = if(r.primaryKey->isEmpty(),
        | let pkCols = r->resolveRelationFunctionPrimaryKey([], $extensions);
          ^$r(primaryKey = pkCols),
        | r);

    // 2. Route the relation function to get an aliased expression
    let routedRelationFunction =
        resolved->potentiallyRouteRelationFunctionSet($state.mapping, $extensions)
                ->cast(@RelationFunctionInstanceSetImplementation).relationFunction;

    // 3. Evaluate the function body (Pure expression → SelectSQLQuery)
    let relationExpression =
        routedRelationFunction.expressionSequence->evaluateAndDeactivate()->at(0)
            ->cast(@ClusteredValueSpecification).val;

    // 4. Process the relation expression into a SelectWithCursor
    let cursor = relationExpression->processValueSpecification(...);

    // 5. Wrap in a sub-select
    let newSelect = cursor.select->moveSelectQueryToSubSelect(...);
    ^cursor(select = newSelect, currentTreeNode = [])
```

The result is a `SelectWithCursor` whose `select` is a sub-select wrapping the
entire relation function output. All subsequent filter / project / sort operations
are applied on top of this sub-select.

### 8.2 `processGetAll` dispatch

`processGetAll` handles the initial `getAll()` call for any set implementation
type. For RF sets, the `OperationSetImplementation` arm now uses
`InstanceSetImplementation` (the common supertype) and dispatches per leaf:

```
let processSingleSetImpl = {r: InstanceSetImplementation[1] |
    r->match([
        rr: RootRelationalInstanceSetImplementation[1]
            → processGetAll($rr, ...),
        rf: RelationFunctionInstanceSetImplementation[1]
            → processRelationFunctionClassMapping($rf, ...)
    ])
};

o: OperationSetImplementation[1]
    let setImpls = o->resolveOperation($state.mapping)->cast(@InstanceSetImplementation);
    if(setImpls->size() == 1,
        processSingleSetImpl->eval(setImpls->at(0)),
        buildUnion(setImpls, ...) // > 1 leaf → union
    )
```

### 8.3 `processPropertyMapping` — enumeration push-down

When any property mapping in scope carries an `EnumerationMapping` transformer,
the state flag `pushDownEnumTransformations = true` is set before dispatch.

`RelationFunctionPropertyMapping` nodes are normalised to `RelationalPropertyMapping`
first:

```
let normalizedPM = propertyMapping->map(pm | pm->match([
    rf: RelationFunctionPropertyMapping[1]
        → rf->transformRelationPropertyMappingsToRelational(
              $state->getClassMappingById(rf.sourceSetImplementationId)
                    ->cast(@RelationFunctionInstanceSetImplementation))
           ->cast(@PropertyMapping),
    pm: PropertyMapping[1] → pm
]));

let normalizedState = if(propertyMapping->exists(pm | pm->match([
    rf: RelationFunctionPropertyMapping[1] → rf.transformer->isNotEmpty(),
    pm: PropertyMapping[1]                 → false])),
    ^$state(pushDownEnumTransformations=true),
    $state);
```

`getEnumPropMappingTransformer` extracts the transformer from either
`RelationalPropertyMapping` or `RelationFunctionPropertyMapping`:

```
getEnumPropMappingTransformer(pm)
    pm->toOne()->match([
        rpm:  RelationalPropertyMapping[1]         → rpm.transformer,
        rfpm: RelationFunctionPropertyMapping[1]   → rfpm.transformer
    ])
```

### 8.4 Embedded property resolution in SQL

When a query traverses `$x.address.city`, the router calls `findPropertyMapping`
which dispatches through the `EmbeddedRelationFunctionSetImplementation` arm (see
§6.6). The result is always a `RelationalPropertyMapping` with a `TableAliasColumn`
referencing a `RelationFunctionColumn` on the sub-select alias.

No special SQL generation path is needed for embedded properties: once
`findPropertyMapping` returns a `RelationalPropertyMapping`, the standard
`processRelationalPropertyMapping` path handles it.

### 8.5 Union SQL generation (`pureToSQLQuery_union.pure`)

**`buildUnion` signature relaxed:**

```
// OLD
buildUnion(setImpls: RootRelationalInstanceSetImplementation[*], ...)

// NEW
buildUnion(setImpls: InstanceSetImplementation[*], ...)
```

**Per-leaf query construction:**

```
let simpleAllQueries = setImpls->map(r | r->match([
    rr: RootRelationalInstanceSetImplementation[1]
        → processGetAll($rr, $rr.class, ...),
    rf: RelationFunctionInstanceSetImplementation[1]
        → let swc = processRelationFunctionClassMapping($rf, newMap([]), ...);
           ^$swc(currentTreeNode = swc.select.data)
]));
```

**Milestoning columns** — RF leaves return empty milestoning column lists (no
physical table):

```
let milestoningColumns = setImpls->map(s | s->match([
    rr: RootRelationalInstanceSetImplementation[1]
        → rr.mainTableAlias.relationalElement->findMainNamedRelation()->match([
              t: Table[1]         → t.milestoning->getAllTemporalColumns(),
              r: NamedRelation[1] → ^List<Column>(values=[])
          ]),
    rf: RelationFunctionInstanceSetImplementation[1]
        → ^List<Column>(values=[])
]));
```

**Column expansion** for non-merge-compatible unions (when joins cannot be simply
merged). RF leaves synthesise columns from the `SelectSQLQuery`'s existing
`Alias` list:

```
let allColumns = setImpl->match([
    rr: RootRelationalInstanceSetImplementation[1]
        → rr->mainRelation().columns->cast(@Column),
    rf: RelationFunctionInstanceSetImplementation[1]
        → q.data->toOne().alias.relationalElement->match([
              s: SelectSQLQuery[1]
                  → s.columns->cast(@Alias)->map(a |
                      ^Column(name=a.name,
                               type=^meta::relational::metamodel::datatype::Integer())),
              o: RelationalOperationElement[1] → []->cast(@Column)
          ])
]);
```

**`managePrimaryKeys` signature relaxed:**

```
// OLD: managePrimaryKeys(allQueries, setImpls: RootRelationalInstanceSetImplementation[*], ...)
// NEW: managePrimaryKeys(allQueries, setImpls: InstanceSetImplementation[*], ...)
```

**`findUnionPropertyMapping`** — extended with an RF arm:

```
rf: RelationFunctionInstanceSetImplementation[1] → rf.id
```

**`buildUniqueName`** — extended with an RF arm:

```
rf: RelationFunction[1] → 'rf(' + rf.owner.id + ')'
```

---

## 9. Decision Cheat-sheet

| Question | Answer | Where |
|----------|--------|-------|
| What type of expression can `~func` reference? | Any Pure function returning `Relation<Any>[1]` — table accessors (`#>{db.t}#`), Pure functions, SQL-generating expressions | Grammar §1.3 |
| When should I omit `~primaryKey`? | When the function returns a typed relation from a single table — the runtime infers PK from table metadata. Use `~primaryKey` for multi-table functions or when inference fails. | Compiler §5.3, SQL §8.1 |
| Can I map multiple PK columns? | Yes: `~primaryKey: [COL1, COL2]` | Grammar §1.2 |
| What property types are supported? | Primitives, Enums (with `EnumerationMapping`), complex types (with `Binding`). Collection properties (`[*]`) are not supported. | Compiler §5.4 |
| How do I map an enum property? | Add `EnumerationMapping <id> : COLUMN_NAME` and declare the `EnumerationMapping` in the same `Mapping`. | Example §2.5, Compiler §5.4 |
| Why does the compiler resolve the column type for enum properties? | `resolveRelationColumnTypeName` walks up to the owning RF set and reads the `RelationType` columns. This allows the compiler to verify type compatibility. | Compiler §5.4 |
| Why is `transformer` explicitly copied in `transformRelationPropertyMappingsToRelational`? | Without it, the `EnumerationMapping` attached by the compiler would be silently lost during the RF→Relational conversion. | Transformation §6.2 |
| Normal vs inline embedded — when to use which? | Normal: all sub-object columns come from the same relation function. Inline: sub-object has its own independently-declared class mapping (possibly a different function). | Examples §2.6, §2.7 |
| Can inline embedded use a different relation function? | Yes. The inline target set is fully independent and can declare its own `~func`. | Example §2.7 |
| How does `$x.address.city` resolve in SQL? | Via `findPropertyMapping`'s `EmbeddedRelationFunctionSetImplementation` arm — direct child lookup for normal embedded, or `targetSetImplementationId` follow for inline. | Transformation §6.6 |
| Can a `Relation` mapping participate in a `union`? | Yes. All leaves must resolve to the same store. The union infrastructure dispatches per-leaf to `processRelationFunctionClassMapping` or `processGetAll`. | Example §2.8, Router §7.1, SQL §8.5 |
| Can I mix Relation and Relational leaves in a union? | Yes, as long as they share the same store. | Example §2.8 |
| What happens to milestoning in a union with an RF leaf? | The RF leaf returns empty milestoning columns; temporal filtering is not applied to that branch. | SQL §8.5 |
| Does embedded support `->isEmpty()` on a sub-property? | Yes — once `findPropertyMapping` resolves to a `RelationalPropertyMapping`, the standard relational `processIsEmpty` path handles it. | (standard relational path) |
| Cross-store union? | Not supported. `->toOne()` on store deduplication in `storeContractForSetImplementation` enforces single-store constraint. | Router §7.1 |
| How do local properties differ from class properties? | Local properties are declared with `+name: Type[mult]` in the mapping and exist only in the mapping scope — they do not modify the canonical Pure class. | Grammar §1.2, Example §2.3 |

---

## 10. Authoritative File Map

| Concern | Key files |
|---------|-----------|
| Lexer / Parser | `RelationFunctionMappingLexerGrammar.g4`, `RelationFunctionMappingParserGrammar.g4` |
| Parse-tree walker | `RelationFunctionMappingParseTreeWalker.java` |
| Protocol POJOs | `RelationFunctionClassMapping.java`, `RelationFunctionPropertyMapping.java`, `RelationFunctionEmbeddedPropertyMapping.java` |
| Compiler — class mapping | `ClassMappingFirstPassBuilder.java`, `ClassMappingSecondPassBuilder.java`, `ClassMappingThirdPassBuilder.java` |
| Compiler — property mappings | `PropertyMappingBuilder.java` |
| Compiler — validation | `MappingValidator.java` |
| Transformation (Pure) | `core_relational/relational/helperFunctions/helperFunctions.pure` |
| SQL generation | `core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure` |
| Union SQL generation | `core_relational/relational/pureToSQLQuery/pureToSQLQuery_union.pure` |
| Routing | `core/pure/router/store/cluster.pure`, `core/pure/router/store/routing.pure` |

---

## 11. Quick Reference: Call Graph

```
PARSE
  CorePureGrammarParser.parseRelationFunctionClassMapping
  └── RelationFunctionMappingParseTreeWalker.visitRelationFunctionClassMapping
        ├── visitPropertyMapping
        │     ├── visitRelationFunctionPropertyMapping     (column + optional transformer)
        │     ├── visitRelationFunctionEmbeddedPropertyMapping    (normal, recursive)
        │     └── visitInlineRelationFunctionEmbeddedPropertyMapping  (inline, id only)
        └── primaryKey[] parsed from context

COMPILE  (3 passes)
  ClassMappingFirstPassBuilder.visit(RelationFunctionClassMapping)
  └── creates RelationFunctionInstanceSetImplementation
      └── PropertyMappingBuilder.visit(RelationFunctionPropertyMapping)
            ├── multiplicity check
            ├── type check → primitive / enum / binding
            ├── resolveRelationColumnTypeName   (for enum: walk up to RF set, read RelationType)
            └── attach EnumerationMapping / BindingTransformer as transformer
      └── PropertyMappingBuilder.visit(RelationFunctionEmbeddedPropertyMapping)
            ├── creates EmbeddedRelationFunctionSetImplementation
            └── recursively compiles inner propertyMappings

  ClassMappingSecondPassBuilder.visit(RelationFunctionClassMapping)
  └── resolves relationFunction by path
      └── propagateRelationFunctionToEmbedded   (cascades to all embedded sets)

  ClassMappingThirdPassBuilder.visit(RelationFunctionClassMapping)
  └── resolves ~primaryKey column names against RelationType
      └── getRelationFunctionColumns            (reads last expression's type args)

TRANSFORM  (helperFunctions.pure)
  transformRelationFunctionClassMapping
    ├── RelationFunctionPropertyMapping  → transformRelationPropertyMappingsToRelational
    │     └── getTransformedRelationFunctionRelOp → TableAliasColumn (RelationFunctionColumn)
    ├── EmbeddedSetImplementation        → transformRelationFunctionEmbeddedPropertyMapping
    │     └── recursively transforms; inherits relationFunction from parent
    └── other                            → pass-through

  normalizeRelationFunctionEmbeddedMapping   (called in findPropertyMapping)
  └── EmbeddedSetImplementation owned by RF set → transformRelationFunctionEmbeddedPropertyMapping

  findPropertyMapping
    ├── EmbeddedRelationalInstanceSetImplementation arm  (normalizes RF embedded first)
    └── EmbeddedRelationFunctionSetImplementation arm   (direct lookup or inline target)

ROUTE  (cluster.pure, routing.pure)
  storeContractForSetImplementation
    ├── RelationFunctionInstanceSetImplementation → reads store from relation function
    ├── EmbeddedSetImplementation               → delegate to owner (new arm)
    └── OperationSetImplementation (union)      → recursively resolve per-leaf

  potentiallyRouteSetImplementations
    └── OperationSetImplementation              → also routes RF leaves via
                                                  potentiallyRouteRelationFunctionSet

SQL GENERATION  (pureToSQLQuery.pure, pureToSQLQuery_union.pure)
  processRelationFunctionClassMapping
    ├── auto-infer primaryKey if empty
    ├── potentiallyRouteRelationFunctionSet
    ├── evaluateAndDeactivate → ClusteredValueSpecification
    ├── processValueSpecification → SelectWithCursor
    └── moveSelectQueryToSubSelect → wrapped sub-select

  processGetAll → dispatches RF sets to processRelationFunctionClassMapping

  processPropertyMapping
    ├── normalize RelationFunctionPropertyMapping → RelationalPropertyMapping
    ├── set pushDownEnumTransformations=true if transformer present
    └── getEnumPropMappingTransformer (works for both RPM and RFPM)

  buildUnion  (pureToSQLQuery_union.pure)
    ├── per-leaf dispatch: processGetAll (relational) OR processRelationFunctionClassMapping (RF)
    ├── milestoningColumns → [] for RF leaves
    └── allColumns → synthesised from SelectSQLQuery.columns for RF leaves

  managePrimaryKeys      → signature relaxed to InstanceSetImplementation[*]
  findUnionPropertyMapping → new arm for RelationFunctionInstanceSetImplementation
  buildUniqueName        → new arm for RelationFunction
```

