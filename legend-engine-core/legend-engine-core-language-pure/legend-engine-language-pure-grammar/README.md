# PURE Grammar

## Introduction

This module includes logic for parsing [PURE grammar](https://legend.finos.org/docs/getting-started/legend-language) to protocol JSON as well as transforming protocol to PURE.

## Philosophy

Ideally, we want the relation between the grammar and the protocol model to be __bijective__. This is needed to ensure that the grammar parser and transformer are symmetrical. As such, the round-trip tests are designed to enforce this.

In other words, we should aim to make the parser as simple as possible since the parser's job is solely to build the protocol models. It should not be "smart" enough to do any context-aware inference - this will be handled by the compiler (e.g. sometimes we want to initialize a default value for missing information because a particular attribute is supposed to be required in the protocol model, we can consider changing the protocol model to have the attribute as optional and setting this default value in the compiler instead).

Another aspect is that the parser should only throw parser errors. This is aligned with our core philosophy because the moment you find yourself trying to do some smart validation check which is more compiler-specific in nature, we will be confused if we should throw a compilation error or a parser error and surely, throwing the former in the context of parsing is a no-no. What we really aim for at the end of parsing process is to have a set of protocol models with source information, so we can pass that to the compiler. As for the composing JSON to grammar, our policy is not to throw error at all.

## Test

Please add test, it's very helpful for other developers to see example of the grammar as well. We have 2 important sets of tests:

- `Test...GrammarRoundtrip` This set of tests is for testing both the grammar parser and composer since they take in the input, create protocol model out of it and then render the model back into grammar text. These files __do not__ test for parsing errors. Also, this set of tests is a great place to test formatting. So when you add new tests, make sure you have a test which really messes up the ordering of fields and spacings to check if the grammar composer corrects this.
- `Test...GrammarParser` This set of tests is to verify that parsing errors are being thrown appropriately, either by ANTLR or by the grammar parse tree walkers. Tests for keywords inclusion in `identifier` (see below), required fields, duplicated fields, etc. should also be included.

## Conventions

We use ANTLR for parsing text. As of now, we made the decision to use [imports](https://github.com/antlr/antlr4/blob/master/doc/grammars.md#grammar-imports) to help modularize the grammar. The good side of this is we don't end up with a giant ANTLR definition that hard to keep track of, but the downside is we need to follow several conventions. Followings are some we would want contributors to follow for consistency and good UX/DX.

### Use unordered set where possible

When we create a new parser grammar for a new type of element, we would want it to be as flexible as possible, we certainly don't want to fix the order of the fields. For example:

```antlr
definition:                     BRACE_OPEN
                                    field1Rule
                                    field2Rule
                                BRACE_CLOSE
;
```
With the above parser rule, `field1` and `field2` are both required but if user specifies `field2` first, ANTLR will throw a parsing error. As such, we want to follow the convention of leveraging the `(rule1 | rule2)*` syntax to make the grammar more flexible. In particular, we should rewrite the above rule to:

```antlr
definition:                     BRACE_OPEN
                                    (
                                        field1Rule
                                        | field2Rule
                                    )*
                                BRACE_CLOSE
;
```
We then will handle the logic to make sure the field is required and not duplicated when we build protocol models from ANTLR tokens.

### Make sure `identifier` parser rule includes lexer keyword tokens

A parser rule which always present in any parser grammar is `identifier`. This takes any valid strings, so naturally it will compete with any keyword lexer rules. For example, if we have a lexer rule for the word `Class`, and we don't include this keyword in the definition of `identifier` parser rule, when user types `Class` in a place that we expect an `identifier`, ANTLR error would be thrown. As such, we have to follow the following convention to make it work.

```antlr
identifier:                     VALID_STRING
                                | KEYWORD1
                                | KEYWORD2
;
```

Also, we should add a new keyword to test for `identifier` to make sure we have no keyword that supersedes identifier. For this, we have a parser test to automatically scan the vocabulary of a grammar generated by ANTLR and find all keywords to check if the parser grammar which defined `identifier` have proper exclusions. Unfortunately, due to the way the vocabulary is constructed, it cannot properly record composite lexer keyword tokens such as `LEXER_TOKEN: 'keyword1' | 'keyword2'`. For this case, it is on the one who worked on the grammar definition to add `keyword1` and `keyword2` to the test or to simply break `LEXER_TOKEN` into 2 distinct keyword rules.

### Add a catch-all INVALID lexer rule

Always end your lexer file with a catch-all [INVALID](https://github.com/antlr/antlr4/issues/1540#issuecomment-268738030) lexer rule in order to fail-fast any unknown token. This aims to boost performance.

However, since most lexer grammar imports `CoreLexer` which already had this rule, there's usually no need to explicitly include it in the main lexer grammar.

### Avoid polluting protocol model with source information

Source information in the protocol models are used to store information about the source code line and column that gives the information to form the protocol model object. This is used by various processors further down in the pipeline, such as compilation, generation, execution, etc. in order to pin-point problem in the source code. As such, generally, while parsing the grammar and building the model protocol, we will try to have as much source information recorded as possible. One downside to this is it can pollute the protocol model and increase the size of these models significantly.

Therefore, it is recommended to only add source information which is valuable, for example: paths pointing to elements (e.g. `class`, `enumeration`), or a sub-element (e.g. `property`, `tag`).

To also avoid confusion, try to pack the source information with the protocol model attribute which the source information is used for, such as in `ClassMapping`, if we need a source information for the class, it's best that we use a class pointer that groups the class path and the source information together, instead of having a property like `classSourceInformation` (unfortunately, due to legacy code, we still have some of these in our codebase). This enhances the readability and clarity of the protocol model.


### Avoid using `island grammar` when possible

[ANTLR modes feature](https://github.com/antlr/antlr4/blob/master/doc/lexer-rules.md#lexical-modes) allow island grammar. As such, we can basically treat a code block as text and delegate the parsing to another grammar parser. We do this at several places, such as class mapping parsing. This technique is powerful in that it lets us break down the parser into smaller unit and allow modularization, but it has several downsides:
1. Potentially, it is slower, as it implies duplicated ANTLR parsing on the same code
2. When delegating the code to another parser, we have to be extremely careful to accurately compute the source information offsets. Always add tests for this.
