lexer grammar ExternalFormatConnectionLexerGrammar;

import CoreLexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

STORE:                                  'store';
EXTERNAL_SOURCE:                        'source';

BRACE_OPEN:                             '{' -> pushMode(SPECIFICATION_ISLAND_MODE);

mode SPECIFICATION_ISLAND_MODE;
SPECIFICATION_BRACE_OPEN: '{' -> pushMode (SPECIFICATION_ISLAND_MODE);
SPECIFICATION_BRACE_CLOSE: '}' -> popMode;
SPECIFICATION_CONTENT: (~[{}])+;