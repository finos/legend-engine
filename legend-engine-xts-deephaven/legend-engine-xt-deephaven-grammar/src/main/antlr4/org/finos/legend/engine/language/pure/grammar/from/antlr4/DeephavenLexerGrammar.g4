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
