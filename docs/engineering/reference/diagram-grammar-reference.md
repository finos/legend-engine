# Diagram Grammar Reference

A `Diagram` is a **visual layout description** for a subset of your model. It records
exactly where each class box sits on a canvas and how relationship lines are routed between
those boxes. Diagrams are purely presentational — they carry no execution semantics and are
not used during compilation or plan generation.

---

## Syntax overview

A diagram file begins with the `###Diagram` section marker, optional imports, then one or
more `Diagram` blocks. Each block contains three kinds of view:

```
###Diagram
[import <package>::*;]…

Diagram <package>::<Name>
{
  // ── Nodes ──────────────────────────────────────────────
  classView <id>
  {
    class:           <package>::<ClassName>;
    position:        (<x>, <y>);
    rectangle:       (<width>, <height>);
    [hideProperties:  true;]
    [hideTaggedValue: true;]
    [hideStereotype:  true;]
  }

  // ── Property / association edges ───────────────────────
  propertyView
  {
    property: <package>::<ClassName>.<propertyName>;
    source:   <classViewId>;
    target:   <classViewId>;
    points:   [(<x>,<y>), …];
  }

  // ── Inheritance edges ──────────────────────────────────
  generalizationView
  {
    source: <classViewId>;     // subclass end
    target: <classViewId>;     // superclass end
    points: [(<x>,<y>), …];
  }
}
```

Items in `[…]` are optional. The three view types may appear in any order and any number of
times within the diagram block.

---

## ⚠️ This grammar is different from the legend-pure Diagram grammar

Legend-pure (the upstream Pure runtime) also contains a `###Diagram` section with the
**same section keyword** but a **different syntax**. The two grammars are incompatible:

| Feature | legend-engine grammar | legend-pure grammar |
|---|---|---|
| Section keyword | `###Diagram` | `###Diagram` |
| Diagram header | `Diagram path::Name { }` | `Diagram path::Name(width=w, height=h) { }` |
| Class-box keyword | `classView` | `TypeView` |
| Class-box ID | arbitrary string / UUID | `cview_N` (sequential integer suffix) |
| Property edge keyword | `propertyView` | `PropertyView pview_N(…)` |
| Association edge | represented as `propertyView` | separate `AssociationView aview_N(…)` |
| Generalisation edge keyword | `generalizationView` | `GeneralizationView gview_N(…)` |
| Property syntax | `key: value;` | `key=value` (no semicolon) |
| Position / size | `position: (x,y);` + `rectangle: (w,h);` | `position=(x,y), width=w, height=h` |
| Visibility flags | `hideProperties`, `hideTaggedValue`, `hideStereotype` | `stereotypesVisible`, `attributesVisible`, `attributeStereotypesVisible`, `attributeTypesVisible` |
| Style properties | none | `color`, `lineWidth`, `lineStyle`, `label`, `nameVisible`, `propertyPosition`, `multiplicityPosition` |

The legend-engine grammar is parsed by
`DiagramParserExtension` / `DiagramParseTreeWalker` in
`legend-engine-xts-diagram/legend-engine-xt-diagram-grammar` and maps to the protocol
classes in `legend-engine-xt-diagram-protocol`. The legend-pure grammar is handled by the
Pure IDE parser and is only used internally for Pure metamodel self-description diagrams.

---

## 1. File structure

### `###Diagram` section marker

Every diagram definition lives inside a `###Diagram` section. The marker must be the first
non-blank, non-comment line in the section and is **case-sensitive**:

```pure
###Diagram
Diagram my::package::MyDiagram
{
}
```

### One file per diagram (convention)

By convention, each diagram element lives in its own `.pure` file, named after the diagram:

```
src/main/resources/
  model/
    domain/
      Person.pure          ← ###Pure classes
    diagrams/
      EmploymentDiagram.pure  ← ###Diagram, single diagram per file
```

The file must start with `###Diagram` (after any licence header comment). A file **may**
contain multiple diagrams in the same `###Diagram` section, but keeping one diagram per
file makes version-control diffs easier to read.

### Ordering within the section

Within a `###Diagram` section, the order is:

1. `import` statements (optional, all before any `Diagram` block)
2. One or more `Diagram { … }` blocks

```pure
###Diagram
import model::domain::*;
import model::common::*;

Diagram model::diagrams::PersonDiagram
{
}

Diagram model::diagrams::OrgDiagram
{
}
```

Multiple diagrams can co-exist in the same section provided they have different fully
qualified names.

---

## 2. Imports

An `import` statement brings a package into scope so that class and property references
inside the section can omit the leading package path. Imports are scoped to the section.

```pure
###Diagram
import model::domain::*;

Diagram my::package::MyDiagram
{
  classView cv1
  {
    class: Person;           // resolves to model::domain::Person via the import
    position: (100.0,100.0);
    rectangle: (120.0,44.0);
  }
}
```

---

## 3. Diagram declaration

```pure
Diagram <packagePath>::<Name>
{
  // one or more classView, propertyView, generalizationView blocks
}
```

| Part | Description |
|---|---|
| `<packagePath>` | Optional double-colon-separated package path, e.g. `model::domain` |
| `<Name>` | Diagram element name. Must be unique within its package |

---

## 4. `classView` — class node

A `classView` represents one class box on the canvas. It names the Pure `Class` element
to visualise and specifies its exact position and size in canvas-coordinate space.

### Syntax

```pure
classView <id>
{
  class:           <qualifiedName>;     // required
  position:        (<x>,<y>);          // required
  rectangle:       (<width>,<height>); // required
  hideProperties:  true;               // optional, default false
  hideTaggedValue: true;               // optional, default false
  hideStereotype:  true;               // optional, default false
}
```

### Fields

| Field | Required | Type | Description |
|---|---|---|---|
| `class` | ✅ | Qualified class name | The Pure `Class<Any>` element this box represents. Resolved against any `import` statements in scope |
| `position` | ✅ | `(x, y)` float pair | Canvas coordinates of the **top-left corner** of the box. The canvas origin `(0,0)` is the top-left; x increases rightward, y increases downward. Negative values are valid |
| `rectangle` | ✅ | `(width, height)` float pair | Dimensions of the box in canvas units. Typically driven by the renderer; hand-authored values should match expected content |
| `hideProperties` | ❌ | `true` / `false` | When `true`, the renderer suppresses the properties/attributes section of the class box. Useful to reduce clutter for classes with many properties |
| `hideTaggedValue` | ❌ | `true` / `false` | When `true`, suppresses any tagged-value annotations on the class. Omitting or setting to `false` leaves them visible |
| `hideStereotype` | ❌ | `true` / `false` | When `true`, suppresses stereotype labels (e.g. `<<service>>`) on the class box |

The three `hide*` flags are **independent** and all default to `false` when absent. Only
`true` values need to be written; setting any flag to `false` explicitly is equivalent to
omitting it entirely.

### View ID

`<id>` is an arbitrary string identifier used by `propertyView` and `generalizationView`
blocks to reference this box as a `source` or `target`. It must be **unique within the
diagram**. Both UUID strings and plain identifiers are accepted:

```pure
classView a1b2c3d4-0000-0000-0000-000000000001 { … }  // UUID form (typical from tooling)
classView PersonBox                            { … }  // plain identifier form
```

UUIDs are generated automatically by visual tooling (Legend Studio). Hand-authored diagrams
may use readable identifiers.

### Example

```pure
classView a1b2c3d4-0000-0000-0000-000000000001
{
  class: model::domain::Person;
  position: (100.0,150.0);
  rectangle: (130.0,58.0);
  hideProperties: true;
}
```

---

## 5. `propertyView` — property edge

A `propertyView` draws a directed edge representing a property or an association property
between two class boxes.

> **Association properties**: there is no separate `associationView` keyword. Association
> properties are represented using `propertyView`, referencing the property via its owning
> class (or the association class itself — see the association example below).

### Syntax

```pure
propertyView
{
  property: <ClassName>.<propertyName>;   // required
  source:   <classViewId>;               // required
  target:   <classViewId>;               // required
  points:   [(<x>,<y>), …];             // required (may be empty [])
}
```

### Fields

| Field | Required | Description |
|---|---|---|
| `property` | ✅ | The property to represent, written as `<OwnerClass>.<propertyName>`. `<OwnerClass>` must be a fully-qualified class or association path (subject to any `import` in scope). For association properties, use the **association** class as the owner (e.g. `model::Employment.employer`) |
| `source` | ✅ | ID of the `classView` where the edge **starts** (typically the class that owns the property or the "many" side of an association) |
| `target` | ✅ | ID of the `classView` where the edge **ends** (typically the property's type / the class the property points to) |
| `points` | ✅ | Ordered list of intermediate waypoints the edge line passes through. Use `[]` for a straight direct line, or one or more `(x,y)` float pairs to route around other elements |

`propertyView` is **anonymous** — it has no identifier of its own and cannot be referenced
by other elements. Multiple `propertyView` blocks for different properties are allowed.

### Examples

**Property on a plain class:**

```pure
propertyView
{
  property: model::domain::Person.address;
  source:   PersonBox;
  target:   AddressBox;
  points:   [(215.0,179.0),(340.0,179.0)];
}
```

**Property on an association:**

```pure
propertyView
{
  property: model::domain::Employment.employer;
  source:   8ce9f436-2a96-4015-9572-cc3aeeeec404;
  target:   24ec35ba-8656-4561-93c5-c77a84ba5f4f;
  points:   [(549.7,252.0),(376.2,158.0)];
}
```

**Straight edge (no waypoints):**

```pure
propertyView
{
  property: model::domain::Order.customer;
  source:   OrderBox;
  target:   CustomerBox;
  points:   [];
}
```

---

## 6. `generalizationView` — inheritance edge

A `generalizationView` draws a directed edge representing a supertype (generalisation /
inheritance) relationship between two class boxes.

### Syntax

```pure
generalizationView
{
  source: <classViewId>;     // required — subclass end
  target: <classViewId>;     // required — superclass end
  points: [(<x>,<y>), …];  // required (may be empty [])
}
```

### Fields

| Field | Required | Description |
|---|---|---|
| `source` | ✅ | ID of the `classView` for the **subclass** (the more specific type). The arrowhead (in typical UML notation) is drawn at the `target` end |
| `target` | ✅ | ID of the `classView` for the **superclass** (the more general / parent type) |
| `points` | ✅ | Ordered list of intermediate waypoints the edge line passes through. Use `[]` for a straight line, or one or more `(x,y)` float pairs to route around other elements |

`generalizationView` is **anonymous** — no identifier, and it carries no property
reference (the generalisation relationship is derived from the Pure class hierarchy, not
stored in the diagram).

### Example

```pure
generalizationView
{
  source: EmployeeBox;
  target: PersonBox;
  points: [(165.0,300.0),(165.0,208.0)];
}
```

---

## 7. Numbers and coordinates

All coordinates and sizes are floating-point numbers. Both integer literals and decimal
literals are accepted; the grammar also accepts scientific notation (`2.23E10`). Values are
stored and rendered as `Double`. Negative values are valid (e.g. a class positioned to the
left of the canvas origin).

```pure
position:  (0.0, 0.0);         // origin
position:  (-50.5, 120.0);     // negative x is valid
rectangle: (98.123, 44.0);
points:    [(1.5e2, 3.0E-1)];  // scientific notation accepted
```

---

## 8. Complete example

```pure
###Diagram
import model::domain::*;

Diagram model::diagrams::EmploymentDiagram
{
  classView 24ec35ba-8656-4561-93c5-c77a84ba5f4f
  {
    class: Person;
    position: (100.0,150.0);
    rectangle: (130.0,58.0);
  }

  classView 8ce9f436-2a96-4015-9572-cc3aeeeec404
  {
    class: Organisation;
    position: (400.0,150.0);
    rectangle: (120.0,44.0);
    hideProperties: true;
  }

  classView EmployeeBox
  {
    class: Employee;
    position: (100.0,350.0);
    rectangle: (130.0,72.0);
    hideTaggedValue: true;
    hideStereotype: true;
  }

  // Property edge: Person.address (plain class property)
  propertyView
  {
    property: Person.address;
    source:   24ec35ba-8656-4561-93c5-c77a84ba5f4f;
    target:   8ce9f436-2a96-4015-9572-cc3aeeeec404;
    points:   [(165.0,179.0),(460.0,179.0)];
  }

  // Property edge from an association
  propertyView
  {
    property: model::domain::Employment.employer;
    source:   EmployeeBox;
    target:   8ce9f436-2a96-4015-9572-cc3aeeeec404;
    points:   [];
  }

  // Inheritance edge: Employee extends Person
  generalizationView
  {
    source: EmployeeBox;
    target: 24ec35ba-8656-4561-93c5-c77a84ba5f4f;
    points: [(165.0,350.0),(165.0,208.0)];
  }
}


###Pure
Class model::domain::Person
{
  name: String[1];
}

Class model::domain::Employee extends model::domain::Person
{
  employeeId: String[1];
}

Class model::domain::Organisation
{
  legalName: String[1];
}

Association model::domain::Employment
{
  employer:  model::domain::Organisation[1];
  employees: model::domain::Employee[*];
}
```

---

## 9. Grammar summary (BNF sketch)

```
diagram            ::= 'Diagram' qualifiedName '{' view* '}'

view               ::= classView | propertyView | generalizationView

classView          ::= 'classView' viewId '{'
                         'class:' qualifiedName ';'
                         'position:' numberPair ';'
                         'rectangle:' numberPair ';'
                         ('hideProperties:' boolean ';')?
                         ('hideTaggedValue:' boolean ';')?
                         ('hideStereotype:' boolean ';')?
                       '}'

propertyView       ::= 'propertyView' '{'
                         'property:' qualifiedName '.' identifier ';'
                         'source:' viewId ';'
                         'target:' viewId ';'
                         'points:' '[' (numberPair (',' numberPair)*)? ']' ';'
                       '}'

generalizationView ::= 'generalizationView' '{'
                         'source:' viewId ';'
                         'target:' viewId ';'
                         'points:' '[' (numberPair (',' numberPair)*)? ']' ';'
                       '}'

viewId             ::= identifier | UUID-string
numberPair         ::= '(' number ',' number ')'
number             ::= float | integer   // scientific notation accepted
```

---

## 10. Common parser errors

The ANTLR4 parser enforces required fields and rejects duplicates at parse time. Errors are
reported with a source range in the form `PARSER error at [line:col-line:col]: …`.

### Required fields

Every field marked ✅ in the tables above is **enforced by the parser**. Omitting one
produces an error like:

```
PARSER error at [4:3-8:3]: Field 'class' is required
```

Full list by view type:

| View type | Required fields |
|---|---|
| `classView` | `class`, `position`, `rectangle` |
| `propertyView` | `property`, `source`, `target`, `points` |
| `generalizationView` | `source`, `target`, `points` |

### Duplicate fields

Specifying the same field more than once in a single view block is also a parse error:

```
PARSER error at [4:3-10:3]: Field 'class' should be specified only once
```

This applies to **every** field in every view type, including the optional `hide*` flags.

### Common mistakes and fixes

| Mistake | Error message | Fix |
|---|---|---|
| Missing `class:` in a `classView` | `Field 'class' is required` | Add `class: my::pkg::MyClass;` |
| Missing `position:` in a `classView` | `Field 'position' is required` | Add `position: (0.0,0.0);` |
| Missing `rectangle:` in a `classView` | `Field 'rectangle' is required` | Add `rectangle: (100.0,44.0);` |
| Missing `property:` in a `propertyView` | `Field 'property' is required` | Add `property: pkg::Owner.propName;` |
| Missing `source:` in any edge view | `Field 'source' is required` | Add `source: <classViewId>;` |
| Missing `target:` in any edge view | `Field 'target' is required` | Add `target: <classViewId>;` |
| Missing `points:` in any edge view | `Field 'points' is required` | Add `points: [];` (empty list is valid) |
| `property` written twice | `Field 'property' should be specified only once` | Remove the duplicate line |
| Using `=` instead of `:` | Syntax/lexer error | Use `key: value;` — not `key=value` (that is the legend-pure syntax) |
| `###diagram` (lowercase) | Section not recognised | Use exactly `###Diagram` |
| Referencing a non-existent `classViewId` in `source`/`target` | Compilation error | Ensure the referenced `classView <id>` exists in the same diagram |

---

## 11. Module locations

| Concern | Module / path |
|---|---|
| ANTLR4 lexer + parser grammar | `legend-engine-xts-diagram/legend-engine-xt-diagram-grammar/src/main/antlr4/…/DiagramLexerGrammar.g4` and `DiagramParserGrammar.g4` |
| Parser (grammar → protocol) | `…/grammar/from/DiagramParserExtension.java`, `DiagramParseTreeWalker.java` |
| Composer (protocol → grammar) | `…/grammar/to/DiagramGrammarComposerExtension.java` |
| Protocol POJOs | `legend-engine-xts-diagram/legend-engine-xt-diagram-protocol/…/diagram/` |
| Pure metamodel | `legend-engine-xts-diagram/legend-engine-xt-diagram-pure-metamodel/…/metamodel.pure` |
| Analytics (model coverage) | `legend-engine-xts-diagram/legend-engine-xt-diagram-pure/…/analytics/analytics.pure` |
| Round-trip tests | `legend-engine-xts-diagram/legend-engine-xt-diagram-grammar/…/test/TestDiagramGrammarRoundtrip.java` |







