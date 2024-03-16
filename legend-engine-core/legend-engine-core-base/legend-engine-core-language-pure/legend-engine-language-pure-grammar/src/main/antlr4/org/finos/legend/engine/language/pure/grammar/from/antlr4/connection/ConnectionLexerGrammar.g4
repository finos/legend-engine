lexer grammar ConnectionLexerGrammar;

import CoreLexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

IMPORT:                                 'import';


// -------------------------------------- ACTION --------------------------------------

BRACE_OPEN:                                 '{'             -> pushMode (CONNECTION_ISLAND_MODE);


// -------------------------------------- ISLAND --------------------------------------

mode CONNECTION_ISLAND_MODE;
CONNECTION_ISLAND_BRACE_OPEN:               '{' -> pushMode (CONNECTION_ISLAND_MODE);
CONNECTION_ISLAND_BRACE_CLOSE:              '}' -> popMode;
CONNECTION_ISLAND_CONTENT:                  (~[{}])+;