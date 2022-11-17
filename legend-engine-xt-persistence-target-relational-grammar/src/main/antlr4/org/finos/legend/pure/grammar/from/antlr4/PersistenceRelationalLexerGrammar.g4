lexer grammar PersistenceRelationalLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
NONE:                                       'None';
DATE_TIME:                                  'DateTime';

//**********
// RELATIONAL PERSISTENCE TARGET
//**********

TARGET_RELATIONAL:                          'Relational';
TARGET_RELATIONAL_TABLE:                    'table';
TARGET_RELATIONAL_TEMPORAL:                 'temporality';

// TEMPORALITY
TEMPORAL_UNI:                               'Unitemporal';
TEMPORAL_BI:                                'Bitemporal';
TEMPORAL_PROCESSING_DIMENSION:              'processingDimension';
TEMPORAL_SOURCE_DERIVED_DIMENSION:          'sourceDerivedDimension';

// PROCESSING DIMENSION
PROCESSING_BATCH_ID:                        'BatchId';
PROCESSING_BATCH_ID_AND_DATE_TIME:          'BatchIdAndDateTime';

BATCH_ID_IN:                                'batchIdIn';
BATCH_ID_OUT:                               'batchIdOut';
DATE_TIME_IN:                               'dateTimeIn';
DATE_TIME_OUT:                              'dateTimeOut';

// SOURCE-DERIVED DIMENSION
DATE_TIME_START:                            'dateTimeStart';
DATE_TIME_END:                              'dateTimeEnd';
SOURCE_DERIVED_SOURCE_FIELDS:               'sourceFields';

// SOURCE FIELDS
SOURCE_FIELDS_START:                        'Start';
SOURCE_FIELDS_START_AND_END:                'StartAndEnd';

SOURCE_FIELD_START:                         'startField';
SOURCE_FIELD_END:                           'endField';
