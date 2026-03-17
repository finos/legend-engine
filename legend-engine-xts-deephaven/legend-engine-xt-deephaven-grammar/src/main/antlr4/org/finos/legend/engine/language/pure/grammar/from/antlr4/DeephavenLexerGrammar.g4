lexer grammar DeephavenLexerGrammar;

import CoreLexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

IMPORT:                                    'import';
DEEPHAVEN:                                 'Deephaven';
TABLE:                                     'Table';

//  -------------------------------------- Used for Parsing-----------------------------------
TABLES:                                    'tables';
COLUMNS:                                   'columns';
COLUMNDEFINITION:                          'columnDefinition';

// -------------------------------------- Column Types --------------------------------------

BOOLEAN_TYPE:                              'BOOLEAN';
INT_TYPE:                                  'INT';
FLOAT_TYPE:                                'FLOAT';
DOUBLE_TYPE:                               'DOUBLE';
DECIMAL_TYPE:                              'DECIMAL';
STRING_TYPE:                               'STRING';
TIMESTAMP_TYPE:                            'TIMESTAMP';
DATETIME_TYPE:                             'DATETIME';

// -------------------------------------- DeephavenApp Tokens --------------------------------------

DEEPHAVEN_APP:                             'DeephavenApp';
DEEPHAVEN_APP__APPLICATION_NAME:           'applicationName';
DEEPHAVEN_APP__FUNCTION:                   'function';
DEEPHAVEN_APP__OWNER:                      'ownership';
DEEPHAVEN_APP__OWNER_DEPLOYMENT:           'Deployment';
DEEPHAVEN_APP__OWNER_DEPLOYMENT_ID:        'identifier';
DEEPHAVEN_APP__DESCRIPTION:                'description';

