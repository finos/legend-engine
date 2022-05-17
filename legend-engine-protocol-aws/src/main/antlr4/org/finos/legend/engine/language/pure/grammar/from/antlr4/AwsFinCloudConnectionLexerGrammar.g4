lexer grammar AwsFinCloudConnectionLexerGrammar;

import CoreLexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
IMPORT:                                     'import';
NONE:                                       'None';

STORE:                                      'store';

//**********
// FINCLOUD CONNECTION
//**********

FINCLOUD_DATASET_ID:                        'datasetId';
FINCLOUD_AUTHENTICATION_STRATEGY:           'authenticationStrategy';
FINCLOUD_API_URL:                           'apiUrl';
FINCLOUD_QUERY_INFO:                        'queryInfo';

BRACE_OPEN:                             '{' -> pushMode(SPECIFICATION_ISLAND_MODE);

mode SPECIFICATION_ISLAND_MODE;
SPECIFICATION_BRACE_OPEN: '{' -> pushMode (SPECIFICATION_ISLAND_MODE);
SPECIFICATION_BRACE_CLOSE: '}' -> popMode;
SPECIFICATION_CONTENT: (~[{}])+;