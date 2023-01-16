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

lexer grammar MongodbSchemaLexer;

KEYWORDS:                                    SCHEMA
                                            | ID | TITLE | DESCRIPTION | TYPE
                                            | PROPERTIES | REQUIRED
                                            | UNIQUE_ITEMS | MIN_ITEMS | MAX_ITEMS
                                            | ADDITIONAL_PROPERTIES
;

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

BRACE_OPEN:                                 '{';
BRACE_CLOSE:                                '}';
BRACKET_OPEN:                               '[';
BRACKET_CLOSE:                              ']';
PAREN_OPEN:                                 '(';
PAREN_CLOSE:                                ')';
COLON:                                      ':';
DOT:                                        '.';
COMMA:                                      ',';
DOLLAR:                                     '$';

PLUS:                                       '+';
STAR:                                       '*';
MINUS:                                      '-';
DIVIDE:                                     '/';
LESS_THAN:                                  '<';
LESS_OR_EQUAL:                              '<=';
GREATER_THAN:                               '>';
GREATER_OR_EQUAL:                           '>=';

//VALID_STRING:                               ValidString;
SCHEMA:                                     '"$schema"';
ID:                                         '$id';
TITLE:                                      '"title"';
DESCRIPTION:                                '"description"';
BSONTYPE:                                   '"bsonType"';
TYPE:                                       '"type"';
PROPERTIES:                                 '"properties"';
REQUIRED:                                   '"required"';
UNIQUE_ITEMS:                               '"uniqueItems"';
MIN_ITEMS:                                  '"minItems"';
MAX_ITEMS:                                  '"maxItems"';
ADDITIONAL_PROPERTIES:                      '"additionalProperties"';
TRUE:                                       'true';
FALSE:                                      'false';
NULL:                                       'null';



// Fragments
//fragment ValidString:                       (Letter | Digit | '_' ) (Letter | Digit | '_')*
//;
fragment FieldIdentifier:                   ('$' | '$$') (Letter | Digit | '_' )*
;
fragment Letter:                        [A-Za-z]
;
fragment Digit:                         [0-9]
;
//fragment Whitespace:                    [ \r\t\n]+
//;
//fragment HexDigit:                      [0-9a-fA-F]
//;
//fragment String:                        ('\'' ( EscSeq | ~['\r\n\\] )*  '\'' )
//;
//fragment UnicodeEsc:	                'u' (HexDigit (HexDigit (HexDigit HexDigit?)?)?)?
//;
//fragment Esc:                           '\\'
//;
//fragment EscSeq:	                    Esc
//                        		        (
//                        		            [btnfr"'\\]	// The standard escaped character set such as tab, newline, etc.
//		                                    | UnicodeEsc	// A Unicode escape sequence
//		                                    | .				// Invalid escape character
//		                                    | EOF			// Incomplete at EOF
//		                                )
//;
//fragment Integer:                       (Digit)+
//;


STRING
   : STRINGFRAGMENT
;

fragment STRINGFRAGMENT
   : '"' (ESC | SAFECODEPOINT)* '"'
;