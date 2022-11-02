lexer grammar ServiceStoreConnectionLexerGrammar;

import CoreLexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

STORE:                                  'store';
BASE_URL:                               'baseUrl';
AUTH_SPECS:                             'auth';

// -------------------------------------- ISLAND ---------------------------------------
BRACE_OPEN:                    '{' -> pushMode (AUTH_SPECIFICATION_ISLAND_MODE);


mode AUTH_SPECIFICATION_ISLAND_MODE;
AUTH_SPECIFICATION_ISLAND_OPEN: '{' -> pushMode (AUTH_SPECIFICATION_ISLAND_MODE);
AUTH_SPECIFICATION_ISLAND_CLOSE: '}' -> popMode;
AUTH_SPECIFICATION_CONTENT: (~[{}])+;