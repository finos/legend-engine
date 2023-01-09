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

parser grammar DataParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DataLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                 VALID_STRING | STRING
                            | DATA
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                 (dataElement)*
                            EOF
;
dataElement:                DATA stereotypes? taggedValues? qualifiedName BRACE_OPEN (documentation | embeddedData)* BRACE_CLOSE
;
documentation:              DATA_ELEMENT_DOCUMENTATION COLON STRING SEMI_COLON
;
embeddedData:               identifier ISLAND_OPEN (embeddedDataContent)*
;
embeddedDataContent:        ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
stereotypes:                LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN
;
stereotype:                 qualifiedName DOT identifier
;
taggedValues:               BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE
;
taggedValue:                qualifiedName DOT identifier EQUAL STRING
;

