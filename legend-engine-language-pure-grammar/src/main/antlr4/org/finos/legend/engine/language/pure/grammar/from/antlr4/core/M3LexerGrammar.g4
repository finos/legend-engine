lexer grammar M3LexerGrammar;

import CoreLexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

ALL:                                        'all';
LET:                                        'let';
ALL_VERSIONS:                               'allVersions';
ALL_VERSIONS_IN_RANGE:                      'allVersionsInRange';


// ----------------------------------- BUILDING BLOCK -----------------------------------

NAVIGATION_PATH_BLOCK:                      '#/' (~[#])*  '#';