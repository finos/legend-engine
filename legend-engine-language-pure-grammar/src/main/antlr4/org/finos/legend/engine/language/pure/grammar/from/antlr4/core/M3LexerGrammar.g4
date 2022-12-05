lexer grammar M3LexerGrammar;

import CoreLexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

ALL:                                        'all';
LET:                                        'let';
ALL_VERSIONS:                               'allVersions';
ALL_VERSIONS_IN_RANGE:                      'allVersionsInRange';

BYTE_STREAM_FUNCTION:                       'byteStream';

// ----------------------------------- BUILDING BLOCK -----------------------------------

NAVIGATION_PATH_BLOCK:                      '#/' (~[#])*  '#';