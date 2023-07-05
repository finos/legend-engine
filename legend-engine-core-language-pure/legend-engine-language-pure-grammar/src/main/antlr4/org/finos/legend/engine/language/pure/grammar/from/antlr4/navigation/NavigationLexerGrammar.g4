lexer grammar NavigationLexerGrammar;

import CoreLexerGrammar;


// ----------------------------------- BUILDING BLOCK -----------------------------------

INTEGER:                            ('+'|'-')? Integer;
FLOAT:                              ('+'|'-')? Float;
VALID_STRING:                       ValidString;
VALID_STRING_TYPE:                  (Letter | Digit | '_' ) (Letter | Digit | '_' | '$' | '<' | '>')*;
