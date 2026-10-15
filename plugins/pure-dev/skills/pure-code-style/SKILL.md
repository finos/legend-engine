---
name: pure-code-style
description: "Reformats a .pure file (or a block of Pure code) to a consistent house style: 2-space indentation with no column-aligned hanging indents, tight colons in type annotations, spaced pipes on if/match/lambda branches, one-parameter-per-line for long function signatures, and no stray blank lines or trailing whitespace. Whitespace-only - never changes tokens, logic, or semantics. Use when asked to 'format this Pure file', 'clean up the formatting', 'make this more readable', 'fix indentation', or 'make the style consistent' on .pure source."
---

# Pure code style

A whitespace-only formatting pass for `.pure` source. Every rule below changes layout, never
tokens — the function/class declarations, expressions, and comments must be byte-identical modulo
whitespace before and after. This is not a logic refactor: don't rename, reorder, inline, or alter
any expression while applying it.

## Rules

### 1. Indentation: 2 spaces per nesting level

No tabs. No column-aligned "hanging indent to match the opening paren" style — that's what makes
large Pure files look ragged as expressions get edited over time. Every continuation line is
indented exactly 2 spaces from the line that opened its block, not aligned to wherever a paren or
earlier token happened to land.

```
// Before (column-aligned, drifts every time something upstream gets renamed)
let x = someCall(a, b,
                  c, d);

// After (fixed 2-space continuation)
let x = someCall(a, b,
  c, d);
```

### 2. Type annotations: no space around `:`

Function parameters and `Class` properties: `name:Type[mult]`, never `name: Type[mult]` or
`name :Type[mult]`. This is the single most common inconsistency in hand-edited Pure files — the
same function signature often mixes both within one parameter list.

```
// Before
function f(a:String[1], b: Integer[1], c :Boolean[1]):String[1]

// After
function f(a:String[1], b:Integer[1], c:Boolean[1]):String[1]
```

### 3. Pipe (`|`) spacing: space on both sides, with one carve-out

`|` introduces a lambda body in Pure, and it shows up in three shapes. Two of them always get a
space on both sides; the third stays tight by established codebase convention:

- **`if(...)`/branch pipes** (zero-arg lambda): `| thenBranch`, not `|thenBranch`.
- **`match()` arms** (typed single-arg lambda): `binding:Type[mult] | body`, not
  `binding:Type[mult]|body`.
- **Short inline map/filter callbacks** (untyped single-arg lambda used as a tight one-liner):
  stays tight, no space — `map(x|$x->foo())`, not `map(x | $x->foo())`. This is the one
  deliberate exception; it matches how these appear throughout the existing codebase and keeps
  short `->map(...)`/`->filter(...)` chains compact.

```
// Before
if($x->isEmpty(),
  |$default,
  |$x->toOne()
);
$v->match([
  s:String[1]|$s->toUpper(),
  a:Any[1]|$a->toString()
]);

// After
if($x->isEmpty(),
  | $default,
  | $x->toOne()
);
$v->match([
  s:String[1] | $s->toUpper(),
  a:Any[1] | $a->toString()
]);

// Unchanged either way - short inline callback stays tight
$xs->map(x|$x->toOne())
```

Multi-param lambdas passed to `fold`/similar (`{a, b | ...}`) also get spaces around `|`, same as
match arms — only the single-arg *short callback* shape stays tight.

### 4. Long function signatures: one parameter per line

A signature that's hard to scan at a glance (rule of thumb: over ~140 characters, or 4+ parameters
with non-trivial types) breaks onto multiple lines — one parameter per line, each indented 2 spaces
under `function`, with the closing `)` and return type on their own line back at the base indent:

```
// Before
function meta::foo::bar(a:TypeA[1], b:TypeB[1], c:Function<{X[1] -> Y[1]}>[1], d:DebugContext[1]):Result[1]

// After
function meta::foo::bar(
  a:TypeA[1],
  b:TypeB[1],
  c:Function<{X[1] -> Y[1]}>[1],
  d:DebugContext[1]
):Result[1]
```

Short signatures that already read fine on one line stay on one line — don't break a signature just
because a rule says "always break", only because it's genuinely hard to scan. Splitting at the
*top-level* parameter commas is the hard part here: commas inside a parameter's own generic
(`Map<String, AccessorQualifier>`) or function type (`Function<{A[1], B[1] -> C[1]}>`) are **not**
split points. When in doubt, do this by hand per signature rather than with a blind
comma-splitting regex — a wrong split silently breaks the file.

### 5. Blank lines

Exactly one blank line between top-level declarations (functions, classes). Never a blank line
immediately after the opening `{` or immediately before the closing `}` of a function body. Never
two consecutive blank lines anywhere, including right after the import block.

### 6. No trailing whitespace

Strip trailing spaces/tabs from every line.

## Applying this to a file

1. Before touching anything, capture a declaration inventory so you can prove nothing was dropped
   or duplicated: `grep -c "^function\|^Class " <file>.pure`, and ideally a list of the actual
   `function`/`Class` signatures (name + arity), since a reformat is exactly the kind of change
   where a copy/paste slip silently drops or duplicates a block.
2. Reformat. For anything beyond trivial whitespace/indent fixes (i.e. rule 4's signature
   splitting), do it by hand, function by function, rather than with a single generic regex pass —
   Pure's generics and function types make "split on every comma" unsafe. Simple mechanical fixes
   (rules 2, 3, 5, 6) are safe to script with a line-based pass, since they never need to know
   where a parameter boundary is.
3. Re-run the same inventory command and diff it against the before-snapshot — the set of
   declaration names and their counts must match exactly.
4. If a build/compile loop is available for the project (e.g. `pure-lsp-check` for a fast
   single-file check, or the project's own Maven/LSP build), run it. Formatting changes should
   never fail a build; if one does, you introduced a real change, not a whitespace one — find and
   fix it before calling the pass done.

## Scope

This is a per-file or per-diff pass, not a repo-wide sweep. Apply it to the file(s) the user names,
not every `.pure` file in the project, unless explicitly asked to do a full-codebase pass.
