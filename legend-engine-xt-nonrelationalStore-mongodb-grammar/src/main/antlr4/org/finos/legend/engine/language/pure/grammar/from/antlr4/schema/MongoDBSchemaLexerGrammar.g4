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

lexer grammar MongoDBSchemaLexerGrammar;

import M3LexerGrammar;


fragment ESC
   : '\\' (["\\/bfnrt] | UNICODE)
   ;

fragment UNICODE
   : 'u' HEX HEX HEX HEX
   ;


fragment HEX
   : [0-9a-fA-F]
   ;

fragment SAFECODEPOINT
   : ~ ["\\\u0000-\u001F]
   ;

QUOTED_STRING
   : '"' (ESC | SAFECODEPOINT)* '"'
   ;

NUMBER
   : '-'? INT ('.' [0-9] +)? EXP?
   ;

fragment INT
   : '0' | [1-9] [0-9]*
   ;

// no leading zeros

fragment EXP
   : [Ee] [+\-]? INT
   ;

// \- since - means "range" inside [...]

WS
   : [ \t\n\r] + -> skip
   ;

//BRACE_OPEN:                                 '{';
//BRACE_CLOSE:                                '}';
//BRACKET_OPEN:                               '[';
//BRACKET_CLOSE:                              ']';
//PAREN_OPEN:                                 '(';
//PAREN_CLOSE:                                ')';
//COLON:                                      ':';
//DOT:                                        '.';
//COMMA:                                      ',';
DOLLAR:                                     '$';
//PATH_SEPARATOR:                             '::';

//PLUS:                                       '+';
//STAR:                                       '*';
//MINUS:                                      '-';
//DIVIDE:                                     '/';

DATABASE:                                   'Database';
COLLECTION:                                 'Collection';
JSON_SCHEMA:                                'jsonSchema';
VALIDATION_LEVEL:                           'validationLevel';
VALIDATION_ACTION:                          'validationAction';
JOIN:                                       'Join';
INCLUDE:                                    'include';

ERROR:                                      'error'; //default
WARN:                                       'warn';
STRICT:                                     'strict'; // default
MODERATE:                                   'moderate';


TRUE:                                       'true';
FALSE:                                      'false';
NULL:                                       'null';

//INVALID:                                    Invalid;



// Not using Field Identifer. REMOVE TBD
//fragment FieldIdentifier:                   ('$' | '$$') (Letter | Digit | '_' )*
//;

//fragment Invalid:                       .
//;
