lexer grammar RelationalLexerGrammar;

import CoreLexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

DATABASE:                                   'Database';

INCLUDE:                                    'include';

TABLE:                                      'Table';
SCHEMA:                                     'Schema';
VIEW:                                       'View';
FILTER:                                     'Filter';
MULTIGRAIN_FILTER:                          'MultiGrainFilter';
JOIN:                                       'Join';

FILTER_CMD:                                 '~filter';
DISTINCT_CMD:                               '~distinct';
GROUP_BY_CMD:                               '~groupBy';
MAIN_TABLE_CMD:                             '~mainTable';
PRIMARY_KEY_CMD:                            '~primaryKey';

TARGET:                                     '{target}';
PRIMARY_KEY:                                'PRIMARY KEY';
NOT_NULL:                                   'NOT NULL';
IS_NULL:                                    'is null';
IS_NOT_NULL:                                'is not null';
AND:                                        'and';
OR:                                         'or';

// Milestoning
MILESTONING:                                'milestoning';
BUSINESS_MILESTONING:                       'business';
BUSINESS_MILESTONING_FROM:                  'BUS_FROM';
BUSINESS_MILESTONING_THRU:                  'BUS_THRU';
THRU_IS_INCLUSIVE:                          'THRU_IS_INCLUSIVE';
BUS_SNAPSHOT_DATE:                          'BUS_SNAPSHOT_DATE';
PROCESSING_MILESTONING:                     'processing';
PROCESSING_MILESTONING_IN:                  'PROCESSING_IN';
PROCESSING_MILESTONING_OUT:                 'PROCESSING_OUT';
OUT_IS_INCLUSIVE:                           'OUT_IS_INCLUSIVE';
INFINITY_DATE:                              'INFINITY_DATE';

// Mapping
ASSOCIATION_MAPPING:                        'AssociationMapping';
ENUMERATION_MAPPING:                        'EnumerationMapping';
OTHERWISE:                                  'Otherwise';
INLINE:                                     'Inline';
BINDING:                                    'Binding';

SCOPE:                                      'scope';


// ----------------------------------- BUILDING BLOCK -----------------------------------

NOT_EQUAL:                                  '<>';
FLOAT:                                      ('+' | '-')? Float;
INTEGER:                                    ('+' | '-')? Integer;
QUOTED_STRING:                              ('"' ( EscSeq | ~["\r\n] )*  '"');


