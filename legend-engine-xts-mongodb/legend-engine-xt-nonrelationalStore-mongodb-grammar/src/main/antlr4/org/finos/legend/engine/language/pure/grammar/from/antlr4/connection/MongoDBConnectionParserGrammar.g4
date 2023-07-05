// Copyright 2023 Goldman Sachs
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


parser grammar MongoDBConnectionParserGrammar;

import CoreParserGrammar;

options { tokenVocab = MongoDBConnectionLexerGrammar; }

unquotedIdentifier:                         VALID_STRING | HOST_STRING
                                            | STORE | SERVER_URLS
                                            | DATABASE | DEBUG
                                            | AUTHENTICATION
;

identifier:                                 unquotedIdentifier | STRING
;

definition:                                 (
                                                connectionStore
                                                | serverDetails
                                                | authentication
                                                | database
                                            )*
                                            EOF
;

connectionStore:                            STORE COLON qualifiedName SEMI_COLON
;

serverDetails:                              SERVER_URLS COLON
                                                BRACKET_OPEN
                                                    serverURLDef (COMMA serverURLDef)*
                                                BRACKET_CLOSE SEMI_COLON
;

serverURLDef:                               HOST_STRING COLON INTEGER
;

database:                                   DATABASE COLON identifier SEMI_COLON
;

authentication:                             AUTHENTICATION COLON islandDefinition SEMI_COLON
;
