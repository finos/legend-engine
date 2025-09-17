// Copyright 2025 Goldman Sachs
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

lexer grammar RelationElementsDataLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

RELATION:                          'relation';
TABLE_START: (COLON WHITESPACE*) -> pushMode(TABLE_MODE);

mode TABLE_MODE;

    ROW_VALUE: (EscSeq | ~[,\r\n;])+;
    ROW_COMMA: ',';
    NEWLINE: '\r'?'\n' [ \t]*;
    TABLE_END: ';' -> popMode;