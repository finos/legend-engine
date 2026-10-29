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

parser grammar GraphQLMappingParserGrammar;

import M3ParserGrammar;

options { tokenVocab = GraphQLMappingLexerGrammar; }

unquotedIdentifier:                         VALID_STRING
                                            | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE
                                            | TO_BYTES_FUNCTION  // From M3Parser
                                            | TYPE_NAME_CMD
                                            | BINDING_CMD
                                            | JOIN_ACCESSOR_CMD
;

identifier:                                 unquotedIdentifier | STRING
;

definition:                                 classMapping
;

classMapping:                               (mappingTypeName
                                            | mappingBinding
                                            | mappingJoinAccessor)*
                                            EOF
;

mappingTypeName:                            TYPE_NAME_CMD storePointer identifier
;

storePointer:                               BRACKET_OPEN qualifiedName BRACKET_CLOSE
;

mappingBinding:                             BINDING_CMD qualifiedName
;

// Identifies the qualified property (declared on the SAME class mapping's own target class) that a
// cross-instance Association join should call, passing the resolved join-key value as its one argument
mappingJoinAccessor:                        JOIN_ACCESSOR_CMD identifier
;
