// Copyright 2022 Goldman Sachs
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

parser grammar PureCollectionDataParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = PureCollectionDataLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                            VALID_STRING | STRING | DATA
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                            (data)*
                                       EOF
;
data:                                  DATA COLON BRACKET_OPEN ( instance (COMMA instance)* )? BRACKET_CLOSE SEMI_COLON
;
instance:                              NEW_SYMBOL qualifiedName
                                           PAREN_OPEN
                                               (instancePropertyAssignment (COMMA instancePropertyAssignment)*)?
                                           PAREN_CLOSE
;
instancePropertyAssignment:            identifier EQUAL instanceRightSide
;
instanceRightSide:                     instanceAtomicRightSide | ( BRACKET_OPEN (instanceAtomicRightSide (COMMA instanceAtomicRightSide)* )? BRACKET_CLOSE )
;
instanceAtomicRightSide:               instanceLiteral
                                           | instance
                                           | enumReference
;
enumReference:                         qualifiedName  DOT identifier
;
instanceLiteral:                       instanceLiteralToken | (MINUS INTEGER) | (MINUS FLOAT) | (MINUS DECIMAL) | (PLUS INTEGER) | (PLUS FLOAT) | (PLUS DECIMAL)
;
instanceLiteralToken:                  STRING | INTEGER | FLOAT | DECIMAL | DATE | BOOLEAN | STRICTTIME
;


