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

parser grammar AllRowsEquivalentToJsonAssertionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AllRowsEquivalentToJsonAssertionLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                            VALID_STRING | STRING | ALL_ROWS_EQUIVALENT_TO_JSON | EXPECTED
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                            expectedDefinition
                                       EOF
;
expectedDefinition:                    EXPECTED COLON embeddedData SEMI_COLON
;
embeddedData:                          identifier ISLAND_OPEN (embeddedDataContent)*
;
embeddedDataContent:                   ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
