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

/** Taken from "The Definitive ANTLR 4 Reference" by Terence Parr */
// Derived from http://json.org as starting point.

parser grammar MongoDBSchemaParserGrammar;

import M3ParserGrammar;

options { tokenVocab = MongoDBSchemaLexerGrammar; }


unquotedIdentifier:                     VALID_STRING
                                        | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE
                                        | TO_BYTES_FUNCTION  //ABOVE FROM M3LEXER
                                        | DATABASE | COLLECTION
                                        | JSON_SCHEMA
                                        | VALIDATION_LEVEL | VALIDATION_ACTION
                                        | JOIN | INCLUDE
                                        | ERROR | WARN | STRICT | MODERATE
                                        | TRUE | FALSE | NULL
;

identifier:                             unquotedIdentifier | QUOTED_STRING
;

definition:                             (mongodb)*
                                        EOF
;

mongodb:                               DATABASE qualifiedName
                                            PAREN_OPEN
                                                 include*
                                                    (
                                                        collection
                                                        | collectionView
                                                        | join
                                                    )*
                                            PAREN_CLOSE
;

include:                                    INCLUDE qualifiedName
;

collection:                             COLLECTION identifier
                                            PAREN_OPEN
                                                    validationLevel?
                                                    validationAction?
                                                    jsonSchema
                                            PAREN_CLOSE
;
collectionView:                             COLLECTION identifier
                                            PAREN_OPEN
                                                    validationLevel?
                                                    validationAction?
                                                    //jsonSchema?
                                            PAREN_CLOSE
;

join:                                       JOIN identifier PAREN_OPEN joinOperation PAREN_CLOSE
;

joinOperation:                          databasePointer? fieldPath EQUAL databasePointer? fieldPath
;

fieldPath:                              databasePointer? identifier DOT identifier
;

databasePointer:                        BRACKET_OPEN qualifiedName BRACKET_CLOSE
;


validationLevel:                        VALIDATION_LEVEL COLON validationLevelValues SEMI_COLON
;
validationAction:                       VALIDATION_ACTION COLON validationActionValues SEMI_COLON
;
validationLevelValues:                  STRICT | MODERATE
;
validationActionValues:                 ERROR  | WARN
;


jsonSchema:                             JSON_SCHEMA COLON json SEMI_COLON
;

json
   : value
;

obj
   : BRACE_OPEN pair (COMMA pair)* BRACE_CLOSE
   | BRACE_OPEN BRACE_CLOSE
;

pair
   : QUOTED_STRING COLON value
;



arr
   : BRACKET_OPEN value (COMMA value)* BRACKET_CLOSE
   | BRACKET_OPEN BRACKET_CLOSE
;



value
   : QUOTED_STRING
   | NUMBER
   | obj
   | arr
   | TRUE
   | FALSE
   | NULL
   ;

