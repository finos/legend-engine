lexer grammar ElasticsearchLexerGrammar;

import CoreLexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

IMPORT:                             'import';

ES_V7_CLUSTER:                      'Elasticsearch7Cluster';

INDICES:                            'indices';

PROPERTIES:                         'properties';
FIELDS:                             'fields';

// -------------------------------------- Property Types --------------------------------------

KEYWORD:                            'Keyword';
TEXT:                               'Text';
DATE:                               'Date';
SHORT:                              'Short';
BYTE:                               'Byte';
INTEGER:                            'Integer';
LONG:                               'Long';
FLOAT:                              'Float';
HALF_FLOAT:                         'HalfFloat';
DOUBLE:                             'Double';
BOOLEAN:                            'Boolean';
OBJECT:                             'Object';
NESTED:                             'Nested';
