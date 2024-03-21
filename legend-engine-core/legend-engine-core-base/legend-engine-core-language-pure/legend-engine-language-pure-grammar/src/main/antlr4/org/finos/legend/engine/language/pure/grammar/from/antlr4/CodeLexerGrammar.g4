lexer grammar CodeLexerGrammar;

import CoreFragmentGrammar;

fragment ParserPrefix:             '\n###';

SECTION_START:                      ParserPrefix ValidString;
NON_HASH:                           ~[#];
HASH:                               '#';


// --------------------------------------- INVALID -------------------------------------------

INVALID:                                    Invalid;
