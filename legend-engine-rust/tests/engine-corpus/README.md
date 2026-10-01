# Engine corpus

This is golden test data from Legend Engine: what the engine's test suites
parse, with the engine's Java parser result.
`crates/api/tests/engine_corpus.rs` checks the Rust parser against it.

- `sections/`: every section body of the types the Rust parser has a grammar
  for (so far `###Pure`, `###Data`, `###Relational`, `###Mapping`,
  `###Connection`, `###Runtime`, `###Service`, `###Persistence` and
  `###DataSpace`), and of the types step 7 of `docs/porting-status.md` ports
  (all the others), that these suites
  parse:
  - the 22 parser and roundtrip suites of `legend-engine-language-pure-grammar`,
    run by `TestRustParityCoreGrammar`;
  - the grammar-based compilation suites of
    `legend-engine-language-pure-compiler` (`Test*CompilationFromGrammar`,
    `TestMilestoned*`, `TestRelation*`);
  - every test of the grammar and compiler HTTP APIs, which Studio calls;
  - every test of the xts modules listed in `docs/engine-integration.md`
    ("Golden corpus"): store grammars, DSLs, compilers, generators and
    execution plans.
- `expressions/`: every standalone expression (lambda, value specification,
  graph fetch tree) that these suites parse:
  - the core grammar suites, as above;
  - every test of `legend-engine-language-pure-compiler`, of the grammar and
    compiler HTTP APIs, and of the xts modules whose code or tests call the
    expression parser (relational grammar, data quality, SQL and TDS
    compilers, flat data runtime), listed in `docs/engine-integration.md`.

  Their offsets are all zero: the `api` crate's own tests cover offsets.
- `known-failures.txt`: the entries that don't match, with reasons.

Each entry is `<hash>.pure` (the text), `<hash>.meta.json` (section type or
expression kind, source id, offsets) and `<hash>.java.json` (the Java result
with positions). The suites other than the core grammar ones run with the
Rust grammar module on their classpath. Never edit an entry by hand.

To compare a freshly dumped corpus without replacing this one, point the test
at it: `ENGINE_CORPUS=<dir> cargo test -p legend-pure-parser-api --test
engine_corpus`, where `<dir>/sections` and `<dir>/expressions` hold the dump
(either may be missing) and `<dir>/known-failures.txt` is optional.

Generated from legend-engine branch `rust-parser` (finos master
`1bd16703167`): `sections/` at `739d9efca9c` (the HTTP APIs' `###Pure`
sections at `8fa8ae050ca`, the `###Data` sections at `a4f8c151c07`, the
`###Relational` and `###Mapping` ones at `a042af3bc1e`, the
`###Connection` and `###Runtime` ones at `ed209171d0f`, and the `###Service`
ones and step 7's at `15e83162476`, where a full dump reproduces the others
byte for byte), `expressions/` at `8fa8ae050ca`. See
`docs/engine-integration.md` ("Golden corpus") for the comparison and the
regeneration commands.
