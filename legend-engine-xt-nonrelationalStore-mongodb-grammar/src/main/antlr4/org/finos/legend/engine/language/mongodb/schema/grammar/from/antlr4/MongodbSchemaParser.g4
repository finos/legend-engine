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

parser grammar MongodbSchemaParser;

options { tokenVocab=MongodbSchemaLexer; }

keywords:                                   (DATABASE | DATABASE_NAME
                                            | COLLECTIONS | COLLECTION_NAME
                                            | SCHEMA | OPTIONS | VALIDATOR
                                            | SCHEMAS
                                            | VALIDATION_LEVEL | VALIDATION_ACTION
                                            | ID | REF
                                            | TITLE | DESCRIPTION
                                            | TYPE | BSONTYPE
                                            | PROPERTIES | REQUIRED
                                            | UNIQUE_ITEMS | MIN_ITEMS | MAX_ITEMS
                                            | MAXIMUM | MINIMUM
                                            | MIN_LENGTH | MAX_LENGTH
                                            | ENUM | ALL_OF | ONE_OF | ANY_OF
                                            | ADDITIONAL_PROPERTIES)
;

json
   : value EOF
   ;

obj
   : BRACE_OPEN pair (COMMA pair)* BRACE_CLOSE
   | BRACE_OPEN BRACE_CLOSE
   ;

pair
   : key ':' value
   ;


key
   : (keywords | STRING)
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

