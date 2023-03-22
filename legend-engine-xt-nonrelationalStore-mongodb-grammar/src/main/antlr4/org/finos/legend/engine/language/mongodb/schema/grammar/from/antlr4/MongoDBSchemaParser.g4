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

parser grammar MongoDBSchemaParser;

import CoreParserGrammar;

options { tokenVocab=MongoDBSchemaLexer; }

unquotedIdentifier:                     VALID_STRING
                                        | DATABASE | COLLECTION
                                        | JSON_SCHEMA
                                        | VALIDATION_LEVEL | VALIDATION_ACTION
;

identifier:                             unquotedIdentifier | STRING
;

definition:                             (database)*
                                        EOF
;
database:                               DATABASE qualifiedName
                                            PAREN_OPEN
                                                 include*
                                                    (
                                                     collection
                                                     | collectionView
                                                     | join
                                                     )*
                                            PAREN_CLOSE
;

include:                                INCLUDE qualifiedName
;

collection:                             COLLECTION mongodbIdentifier
                                            PAREN_OPEN
                                                    validationLevel?
                                                    validationAction?
                                                    jsonSchema
                                            PAREN_CLOSE
;
collectionView:                             COLLECTION mongodbIdentifier
                                            PAREN_OPEN
                                                    validationLevel?
                                                    validationAction?
                                                    jsonSchema
                                            PAREN_CLOSE
;

join:                                       JOIN identifier PAREN_OPEN joinOperation PAREN_CLOSE
;

joinOperation:                          databasePointer? fieldPath EQUAL databasePointer? fieldPath
;

fieldPath:                              databasePointer? mongodbIdentifier DOT mongodbIdentifier
;

databasePointer:                        BRACKET_OPEN qualifiedName BRACKET_CLOSE
;

mongodbIdentifier:                      unquotedIdentifier    //| QUOTED_STRING
;

validationLevel:                        VALIDATION_LEVEL validationLevelValues COMMA
;
validationAction:                       VALIDATION_ACTION validationActionValues COMMA
;
validationLevelValues:                  STRICT | MODERATE
;
validationActionValues:                 ERROR  | WARN
;


jsonSchema:                             JSON_SCHEMA json
;

json
   : value
;

obj
   : BRACE_OPEN pair (COMMA pair)* BRACE_CLOSE
   | BRACE_OPEN BRACE_CLOSE
;

pair
   : STRING ':' value
;



arr
   : BRACKET_OPEN value (COMMA value)* BRACKET_CLOSE
   | BRACKET_OPEN BRACKET_CLOSE
;



value
   : STRING
   | NUMBER
   | obj
   | arr
   | TRUE
   | FALSE
   | NULL
   ;

