// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

parser grammar GraphQLStoreParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = GraphQLStoreLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

unquotedIdentifier:                         VALID_STRING
                                            | GRAPHQL_STORE
                                            | SCHEMA
;

identifier:                                 unquotedIdentifier | STRING
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 (graphQLStore)*
                                            EOF
;

graphQLStore:                               GRAPHQL_STORE qualifiedName
                                                BRACE_OPEN
                                                (schemaContent)?
                                                BRACE_CLOSE
;
// Flat repetition: brace nesting is already tracked by the lexer's island-mode push/pop, so the
// parser just consumes whatever flat token run results; parsed for real by
// legend-engine-xt-graphQL-grammar's own parser in the walker.
schemaContent:                              SCHEMA COLON BRACE_OPEN (schemaIslandFragment)* SEMI_COLON
;
schemaIslandFragment:                       GRAPHQL_SCHEMA_ISLAND_BRACE_OPEN | GRAPHQL_SCHEMA_ISLAND_CONTENT | GRAPHQL_SCHEMA_ISLAND_BRACE_CLOSE
;
