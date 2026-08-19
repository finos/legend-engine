<h1 align="center">MODELLING SEMI-STRUCTURED DATA</h1>

# Overview

A `SEMISTRUCTURED` column holds a JSON document. This guide covers writing expressions over that
document **inside your store and mapping** — in a property mapping, a `View` column, a `Filter` or
a `Join` — so that consumers of your model see ordinary typed properties and never have to know the
data was JSON.

It is written for modellers rather than engine developers. For the engine-side reasoning behind
these behaviours, see
[relational-dynafunctions.md](../engineering/architecture/relational-dynafunctions.md) §11.

Every behaviour described here is covered by a test that runs against a live database. The list of
functions is **not exhaustive** — there are combinations that likely work but have not been
exercised, and a few that are known not to. If you need something that is not here, ask rather than
assume.

Your model never mentions `Variant`. JSON is typed either through an ExternalFormat `Binding` or
read value-by-value with the expressions below; both give you plain Pure classes and primitives.

# Reaching a value

`extractFromSemiStructured` takes the column, a path into the document, and the type you expect to
find there.

```
legalName: extractFromSemiStructured(
             [store::DB]FIRM_SCHEMA.FIRM_TABLE.FIRM_DETAILS,
             'firm.legalName',
             'VARCHAR')
```

## Path syntax

| Form | Example | Reaches |
|---|---|---|
| `a.b.c` | `'firm.address.city'` | nested members |
| `["key"]` | `'firm["legal name"]'` | a key containing spaces or dots |
| `[0]` | `'divisions[0].name'` | a fixed position in an array |
| `[*]` | `'divisions[*].name'` | every element at that level |

Not supported: `[-1]`, `..`, and `a."key"`. Use `array_last`, an explicit path, and `a["key"]`.

## Types you can ask for

The third argument tells the database what to produce. Getting it right is what lets the value be
compared, sorted and joined as the thing it actually is.

`VARCHAR` · `STRING` · `CHAR` · `INTEGER` · `DECIMAL` · `FLOAT` · `BOOLEAN` · `DATE` · `DATETIME` ·
`TIMESTAMP` · `SEMISTRUCTURED`

`SEMISTRUCTURED` means a nested document. Add `[]` to say the path holds an *array* of that type —
`'INTEGER[]'`, `'SEMISTRUCTURED[]'` — which is what the array functions need.

> **The `[]` is a promise, not a request.** It states that the value *is* an array. If the document
> holds a single object or a bare number there instead, the query fails rather than quietly
> wrapping it. A model that silently invents a one-element array gives you wrong answers rather
> than an error.

# Every element at a level: `[*]`

A `[*]` segment means "each element here". The result comes back as one flat collection, however
many levels deep the path went — the same way a property chain over a to-many behaves in Pure.

Because the result is a collection, the type carries the array suffix: `'VARCHAR[]'`, not
`'VARCHAR'`. That is the same rule as everywhere else — the type names what the expression returns
— and a bare scalar type on a `[*]` path is refused when the model compiles.

```
divisionNames: array_to_string(
                 extractFromSemiStructured(T.FIRM_DETAILS, 'divisions[*].name', 'VARCHAR[]'),
                 ',')
```
→ `Alpha,Beta`

```
teamLabels: array_to_string(
              extractFromSemiStructured(T.FIRM_DETAILS, 'divisions[*].teams[*].label', 'VARCHAR[]'),
              ',')
```
→ `Core,Edge,Ops` — flat, not grouped by division

A path ending in `[*]` gives you the elements themselves:

```
divisionCount: array_size(
                 extractFromSemiStructured(T.FIRM_DETAILS, 'divisions[*]', 'SEMISTRUCTURED[]'))
```
→ `2`

> **Why it flattens.** Pure has no collection-of-collections. `String[*]` is flat, so a two-level
> wildcard has to produce a flat result — there is no other shape it could return. An empty array,
> a JSON `null` and a missing key all contribute nothing, which is what you would expect from an
> optional field.

# Working with arrays

Once an extraction names an array type, the array functions apply to it. Each of these has been run
and its result checked:

`array_size` · `array_first` · `array_last` · `array_init` · `array_tail` · `array_max` ·
`array_min` · `array_sum` · `array_sort` · `array_reverse` · `array_distinct` · `array_contains` ·
`array_position` · `array_append` · `array_concatenate` · `array_slice` · `array_take` ·
`array_drop` · `array_to_string`

```
largestDivision: array_max(
                   extractFromSemiStructured(T.FIRM_DETAILS, 'divisions[*].headcount', 'INTEGER[]'))
```
→ `100` — compared as numbers, not as text

Three behaviours worth knowing rather than discovering:

| | |
|---|---|
| `array_position` | counts from **0**, where Pure's `indexOf` counts from 1 |
| `array_distinct` | does not promise to keep the original order — wrap it in `array_sort` if order matters |
| `array_to_string` | on an empty collection gives null, not an empty string |

# Filter, map and reduce

Three functions take a small expression of their own, written with a parameter name, a `|`, and a
body that uses `$name`.

```
array_filter(
  extractFromSemiStructured(T.FIRM_DETAILS, 'firm.divisions', 'SEMISTRUCTURED[]'),
  d | extractFromSemiStructured($d, 'headcount', 'INTEGER') > 100)
```

```
array_transform(
  extractFromSemiStructured(T.FIRM_DETAILS, 'firm.divisions', 'SEMISTRUCTURED[]'),
  d | extractFromSemiStructured($d, 'name', 'VARCHAR'))
```

```
array_reduce(
  extractFromSemiStructured(T.FIRM_DETAILS, 'firm.divisions[*].headcount', 'INTEGER[]'),
  (h, acc | plus($acc, $h)),
  0)
```

Note the starting value comes last, and the lambda takes two parameters — the element first, then
the accumulator.

They compose, which is the point. This is one property mapping:

```
firstLargeCity: extractFromSemiStructured(
                  array_first(
                    array_filter(
                      extractFromSemiStructured(T.FIRM_DETAILS, 'firm.divisions', 'SEMISTRUCTURED[]'),
                      d | extractFromSemiStructured($d, 'headcount', 'INTEGER') > 100)),
                  'address.city',
                  'VARCHAR')
```
→ `Oslo` — the consumer of your model just sees a `String` property

# One row per element

Everything above produces a **value**. When you want an array to become **rows**, so each element
can join to another table, that is `explodeSemiStructured`, and it belongs in a `Join`.

```
Join Block_Trade(
  extractFromSemiStructured(
    explodeSemiStructured(Semistructured.Blocks.BLOCKDATA, 'relatedEntities', 'SEMISTRUCTURED'),
    'tagId', 'VARCHAR') = Semistructured.Trades.ID)
```

Arrays inside arrays work too — a row per combination:

```
Join Firm_Team(
  extractFromSemiStructured(
    explodeSemiStructured(
      explodeSemiStructured(FIRM_TABLE.FIRM_DETAILS, 'divisions', 'SEMISTRUCTURED'),
      'teams', 'SEMISTRUCTURED'),
    'tid', 'VARCHAR') = TEAM_TABLE.TID)
```

> **Two things explode needs.** The table you are joining *from* must declare a primary key — the
> exploded rows are joined back to it, so without one the model will not plan. And the third
> argument behaves differently here than in `extractFromSemiStructured`: a name it does not
> recognise is treated as a nested document rather than rejected, so `'STRING'` quietly yields a
> document instead of text. Pass `'SEMISTRUCTURED'`, which is what nearly every explode wants.

## Choosing between `[*]` and explode

`[*]` gives you a collection you collapse into a property — a name list, a count, a maximum.
`explodeSemiStructured` gives you rows you can join. Same intuition, different result: pick by
whether you want a property or a relationship.

This also decides whether a **to-many property** works. A relational mapping gets its multiplicity
from rows, so `teams: Team[*]` mapped through an exploding join behaves like any other to-many. A
`[*]` path produces one row with an array in a single column, so there are no rows for a
`String[*]` to range over — it comes back as one value instead. If you want a real collection
today, reach for the join.

# What does not work yet

These are known limits, not bugs to report. One of them fails quietly rather than loudly — worth
reading that row even if you skim the rest.

| Shape | Status | What to do instead |
|---|---|---|
| A field that is a single value in some rows and an array in others | not supported | Common in XML-derived JSON. Handle it explicitly for now; a `toArray` helper is being considered. |
| Mapping `[*]` straight to a `String[*]` property | **compiles, wrong** | You get one value holding the array's text, and no error. Tracked as [#5099](https://github.com/finos/legend-engine/issues/5099). Collapse it with an array function, or get a real collection through a join. |
| Two independent explodes in one join | not supported | Split into separate joins. |
| `explodeSemiStructured` outside a `Join` | not supported | Use `[*]` if you want a value rather than rows. |
| Exploding something other than a plain column | not supported | The innermost explode must read a table or view column directly. |
| `[-1]`, `..`, `a."key"` | not supported | Use `array_last`, an explicit path, and `a["key"]`. |

# Where this is tested

These expressions run against **DuckDB**, **Snowflake** and **Databricks** as part of the build.
H2 supports a narrower set and is being retired.

Two caveats. The function list is what has been *checked*, not everything that exists — the
relational grammar has around 230 functions and this is the semi-structured corner of it. And
dialects differ in places; if you are modelling against one warehouse and will later point at
another, it is worth saying so early.

If something you need is not here, ask. Several of the capabilities on this page exist because
someone described a document they could not model, and the shape of that request is what made the
gap concrete.
