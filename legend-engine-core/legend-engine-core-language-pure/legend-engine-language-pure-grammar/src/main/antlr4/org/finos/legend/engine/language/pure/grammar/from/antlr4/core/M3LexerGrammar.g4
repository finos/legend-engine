lexer grammar M3LexerGrammar;

import CoreLexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

ALL:                                        'all';
LET:                                        'let';
ALL_VERSIONS:                               'allVersions';
ALL_VERSIONS_IN_RANGE:                      'allVersionsInRange';

TO_BYTES_FUNCTION:                          'toBytes';

// ----------------------------------- BUILDING BLOCK -----------------------------------

NAVIGATION_PATH_BLOCK:                      '#/' (~[#])*  '#';