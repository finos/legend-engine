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

parser grammar GraphQLConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = GraphQLConnectionLexerGrammar;
}

// -------------------------------------- IDENTIFIER -------------------------------------

unquotedIdentifier:                         VALID_STRING | STORE | URL | AUTHENTICATION
;

identifier:                                 unquotedIdentifier | STRING
;

// -------------------------------------- DEFINITION -------------------------------------

definition:                                 (
                                                connectionStore
                                                | connectionUrl
                                                | authentication
                                            )*
                                            EOF
;

connectionStore:                            STORE COLON qualifiedName SEMI_COLON
;

connectionUrl:                              URL COLON identifier SEMI_COLON
;

authentication:                             AUTHENTICATION COLON islandDefinition SEMI_COLON
;
