lexer grammar AuthorizerLexerGrammar;

import CoreLexerGrammar;


// -------------------------------------- KEYWORD -------------------------------------

IMPORT:                         'import';
AUTHORIZER:                     'Authorizer';

// -------------------------------------- ACTION --------------------------------------

BRACE_OPEN:                     '{'                           -> pushMode (AUTHORIZER_ISLAND_MODE);

// -------------------------------------- ISLAND --------------------------------------

mode AUTHORIZER_ISLAND_MODE;
AUTHORIZER_ISLAND_BRACE_OPEN:               '{' -> pushMode (AUTHORIZER_ISLAND_MODE);
AUTHORIZER_ISLAND_BRACE_CLOSE:              '}' -> popMode;
AUTHORIZER_ISLAND_CONTENT:                  (~[{}])+;
