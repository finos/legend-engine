lexer grammar CoreLexerGrammar;

import CoreFragmentGrammar;


// -------------------------------------- ACTION --------------------------------------

WHITESPACE:                                 Whitespace      -> skip;
COMMENT:                                    Comment         -> skip;
LINE_COMMENT:                               LineComment     -> skip;
ISLAND_OPEN:                                '#{'            -> pushMode (ISLAND_MODE);


// -------------------------------------- TYPE --------------------------------------

STRING:                                     String;
BOOLEAN:                                    Boolean;
TRUE:                                       True;
FALSE:                                      False;
INTEGER:                                    Integer;
FLOAT:                                      Float;
DECIMAL:                                    Decimal;
DATE:                                       Date;
LATEST_DATE:                                '%latest';


// ----------------------------------- BUILDING BLOCK -----------------------------------

FILE_NAME:                                  FileName;
FILE_NAME_END:                              FileNameEnd;
PATH_SEPARATOR:                             PathSeparator;

TIMEZONE:                                   TimeZone;

AND:                                        '&&';
OR:                                         '||';
NOT:                                        '!';
COMMA:                                      ',';
EQUAL:                                      '=';
TEST_EQUAL:                                 '==';
TEST_NOT_EQUAL:                             '!=';
PERCENT:                                    '%';
ARROW:                                      '->';
BRACE_OPEN:                                 '{';
BRACE_CLOSE:                                '}';
BRACKET_OPEN:                               '[';
BRACKET_CLOSE:                              ']';
PAREN_OPEN:                                 '(';
PAREN_CLOSE:                                ')';
COLON:                                      ':';
DOT:                                        '.';
DOLLAR:                                     '$';
DOT_DOT:                                     '..';
SEMI_COLON:                                 ';';
NEW_SYMBOL:                                 '^';
PIPE:                                       '|';
TILDE:                                      '~';

AT:                                         '@';
PLUS:                                       '+';
STAR:                                       '*';
MINUS:                                      '-';
DIVIDE:                                     '/';
LESS_THAN:                                  '<';
LESS_OR_EQUAL:                              '<=';
GREATER_THAN:                               '>';
GREATER_OR_EQUAL:                           '>=';

VALID_STRING:                               ValidString;


// --------------------------------------- INVALID -------------------------------------------

INVALID:                                    Invalid;


// ----------------------------------- ISLAND GRAMMAR ------------------------------------

mode ISLAND_MODE;
ISLAND_START:                               '#{'        -> pushMode (ISLAND_MODE);
ISLAND_END:                                 '}#'        -> popMode;
ISLAND_HASH:                                '#';
ISLAND_BRACE_OPEN:                          '{';
ISLAND_BRACE_CLOSE:                         '}';
ISLAND_CONTENT:                             (~[{}#])+;
