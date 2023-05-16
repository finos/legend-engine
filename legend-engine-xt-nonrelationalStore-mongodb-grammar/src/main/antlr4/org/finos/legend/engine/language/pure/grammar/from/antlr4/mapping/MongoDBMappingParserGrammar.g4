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


parser grammar MongoDBMappingParserGrammar;

import M3ParserGrammar;

options { tokenVocab = MongoDBMappingLexerGrammar; }

unquotedIdentifier:                         VALID_STRING
                                            | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE
                                            | TO_BYTES_FUNCTION  // From M3Parser
                                            | FILTER_CMD | DISTINCT_CMD
                                            | MAIN_COLLECTION_CMD | PRIMARY_KEY_CMD
                                            | BINDING
;

identifier:                                 unquotedIdentifier    | STRING
;

definition:                                    classMapping
;

// Excluding association mapping for now

classMapping:                               mappingFilter?
                                            (mappingMainCollection
                                            | mappingBinding)*
                                            EOF
;


mappingFilter:                              FILTER_CMD databasePointer identifier
;

mappingMainCollection:                      MAIN_COLLECTION_CMD databasePointer mappingScopeInfo
;

mappingBinding:                             BINDING qualifiedName
;

mappingScopeInfo:                           identifier (DOT scopeInfo)?
;

scopeInfo:                                  identifier (DOT identifier)?
;
databasePointer:                            BRACKET_OPEN qualifiedName BRACKET_CLOSE
;
