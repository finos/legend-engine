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

parser grammar MongoDBMappingParser;

options { tokenVocab = MongoDBMappingLexer; }

unquotedIdentifier:                         VALID_STRING
;

identifier:                                 unquotedIdentifier | MONGODB_FIELD_IDENTIFIER | STRING
;

mapping
        : classMapping
;


// Excluding association mapping for now

classMapping:                               mappingFilter?
                                            DISTINCT_CMD?
                                            mappingPrimaryKey?
                                            mappingMainCollection?
                                            (propertyMapping (COMMA propertyMapping)*)?
                                            EOF
;
mappingFilter:                              FILTER_CMD databasePointer identifier
;

mappingPrimaryKey:                          PRIMARY_KEY_CMD
                                                PAREN_OPEN
                                                    (booleanOperation (COMMA booleanOperation)*)?
                                                PAREN_CLOSE
;
mappingMainCollection:                     MAIN_COLLECTION_CMD databasePointer mappingScopeInfo
;
mappingScopeInfo:                           identifier (DOT scopeInfo)?
;



propertyMapping:                            singlePropertyMapping | propertyMappingWithScope
;
propertyMappingWithScope:                   SCOPE PAREN_OPEN databasePointer mappingScopeInfo? PAREN_CLOSE
                                                PAREN_OPEN
                                                    singlePropertyMapping (COMMA singlePropertyMapping)*
                                                PAREN_CLOSE
;

singlePropertyMapping:                      identifier
                                            (
                                                mongoDBPropertyMapping
                                            )
;

mongoDBPropertyMapping:                     COLON booleanOperation
;

scopeInfo:                                  identifier (DOT identifier)?
;
databasePointer:                            BRACKET_OPEN qualifiedName BRACKET_CLOSE
;
//mongoDBIdentifier:                          VALID_STRING | MONGODB_FIELD_IDENTIFIER | QUOTED_STRING
//;

booleanOperation:                           atomicOperation booleanOperationRight?
;
booleanOperationRight:                      booleanOperator booleanOperation
;
booleanOperator:                            AND | OR
;
atomicOperation:                            (
                                                fieldOperation
                                                | constant
                                            )
                                            atomicOperationRight?
;
atomicOperationRight:                       (atomicOperator atomicOperation) | atomicSelfOperator
;
atomicOperator:                             EQ | NE | LT | LTE | GT | GTE
;
atomicSelfOperator:                         IF_NULL
;
constant:                                   STRING | NUMBER
;
fieldOperation:                            databasePointer? collectionFieldOperation
;
collectionFieldOperation:                   identifier (DOT scopeInfo)?
;

qualifiedName:                              (packagePath PATH_SEPARATOR)? identifier
;
packagePath:                                identifier (PATH_SEPARATOR identifier)*
;

// Since BOOLEAN and INTEGER overlap with VALID_STRING, we have to account for them
// Also, here, we use `identifier` instead of VALID_STRING
// because in the main grammar, we will take care of keywords overlapping VALID_STRING
//word:                                           identifier | TRUE | FALSE | INT
//;
