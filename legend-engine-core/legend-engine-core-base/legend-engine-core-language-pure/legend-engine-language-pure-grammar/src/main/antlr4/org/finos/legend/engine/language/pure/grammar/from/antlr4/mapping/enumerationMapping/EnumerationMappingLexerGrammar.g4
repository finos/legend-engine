lexer grammar EnumerationMappingLexerGrammar;

import CoreLexerGrammar;


// ---------------------------------- BUILDING BLOCK --------------------------------------

INTEGER:                    ('+' | '-')? (Digit)+;
