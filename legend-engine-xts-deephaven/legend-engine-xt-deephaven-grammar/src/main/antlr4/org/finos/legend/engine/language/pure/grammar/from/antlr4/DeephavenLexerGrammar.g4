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

DATE_TIME:                                 'DateTime';
STRING:                                    'String';
INT:                                       'Integer';
BOOLEAN:                                   'Boolean';
FLOAT:                                     'Float';
