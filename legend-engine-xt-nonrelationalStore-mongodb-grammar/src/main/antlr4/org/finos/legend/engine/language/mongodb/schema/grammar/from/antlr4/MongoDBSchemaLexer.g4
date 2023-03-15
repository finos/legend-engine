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

lexer grammar MongoDBSchemaLexer;

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
PATH_SEPARATOR:                             '::';

PLUS:                                       '+';
STAR:                                       '*';
MINUS:                                      '-';
DIVIDE:                                     '/';

DATABASE:                                   '"database"';
DATABASE_NAME:                              '"databaseName"';
COLLECTIONS:                                '"collections"';
COLLECTION_NAME:                            '"collectionName"';
SCHEMAS:                                    '"schemas"';
SCHEMA:                                     '"$schema"';
OPTIONS:                                    '"options"';
VALIDATOR:                                  '"validator"';
VALIDATION_LEVEL:                           '"validationLevel"';
VALIDATION_ACTION:                          '"validationAction"';
ID:                                         '"$id"';
REF:                                        '"$ref"';
JSON_SCHEMA:                                '"$jsonSchema"';
TITLE:                                      '"title"';
DESCRIPTION:                                '"description"';
BSONTYPE:                                   '"bsonType"';
TYPE:                                       '"type"';
PROPERTIES:                                 '"properties"';
MAX_PROPERTIES:                             '"maxProperties"';
MIN_PROPERTIES:                             '"minProperties"';
REQUIRED:                                   '"required"';
ITEMS:                                      '"items"';
UNIQUE_ITEMS:                               '"uniqueItems"';
MIN_ITEMS:                                  '"minItems"';
MAX_ITEMS:                                  '"maxItems"';
MINIMUM:                                    '"minimum"';
MAXIMUM:                                    '"maximum"';
MIN_LENGTH:                                 '"minLength"';
MAX_LENGTH:                                 '"maxLength"';
ADDITIONAL_PROPERTIES:                      '"additionalProperties"';
TRUE:                                       'true';
FALSE:                                      'false';
NULL:                                       'null';
ENUM:                                       '"enum"';
ALL_OF:                                     '"allOf"';
ANY_OF:                                     '"anyOf"';
ONE_OF:                                     '"oneOf"';


INVALID:                                    Invalid;

// Fragments
fragment ValidString:                       (Letter | Digit | '_' ) (Letter | Digit | '_')*
;
// Not using Field Identifer. REMOVE TBD
fragment FieldIdentifier:                   ('$' | '$$') (Letter | Digit | '_' )*
;
fragment Letter:                            [A-Za-z]
;
fragment Digit:                             [0-9]
;
//fragment UNQOTEDSTRINGFRAGMENT:             (ESC | SAFECODEPOINT)*
//;
fragment STRINGFRAGMENT:                    '"' (ESC | SAFECODEPOINT)* '"'
;

STRING
   : STRINGFRAGMENT
   {setText(getText().substring(1, getText().length()-1));}
;

VALID_STRING:                               ValidString;


//UNQUOTED_STRING
//    : UNQOTEDSTRINGFRAGMENT
//;

//WORD
// : ~[ \t\r\n[\]{}=,|&]+
//;



// --------------------------------------- INVALID -------------------------------------------

// Add a rule for INVALID token to the very end of the lexer grammar to make sure your lexer handles all
// input successfully in order to boost performance
// See https://github.com/antlr/antlr4/issues/1540#issuecomment-268738030

fragment Invalid:                       .
;