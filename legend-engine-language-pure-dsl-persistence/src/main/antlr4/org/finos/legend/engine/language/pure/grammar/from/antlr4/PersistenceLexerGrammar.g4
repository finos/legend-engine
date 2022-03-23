lexer grammar PersistenceLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
TRUE:                                       'true';
FALSE:                                      'false';
IMPORT:                                     'import';
NONE:                                       'None';
DATE_TIME:                                  'DateTime';

//**********
// PERSISTENCE
//**********

PERSISTENCE:                                'Persistence';
PERSISTENCE_DOC:                            'doc';

PERSISTENCE_TRIGGER:                        'trigger';
PERSISTENCE_SERVICE:                        'service';
PERSISTENCE_PERSISTER:                      'persister';
PERSISTENCE_NOTIFIER:                       'notifier';

// TRIGGER
TRIGGER_MANUAL:                             'Manual';
TRIGGER_CRON:                               'Cron';

// PERSISTER
PERSISTER_CONNECTION:                       'connection';
PERSISTER_BINDING:                          'binding';
PERSISTER_TARGET_SHAPE:                     'targetShape';
PERSISTER_INGEST_MODE:                      'ingestMode';
PERSISTER_STREAMING:                        'Streaming';
PERSISTER_BATCH:                            'Batch';

// NOTIFIER
NOTIFIER:                                   'Notifier';
NOTIFIER_NOTIFYEES:                         'notifyees';
NOTIFYEE_EMAIL:                             'Email';
NOTIFYEE_EMAIL_ADDRESS:                     'address';
NOTIFYEE_PAGER_DUTY:                        'PagerDuty';
NOTIFYEE_PAGER_DUTY_URL:                    'url';

//**********
// TARGET SHAPE
//**********

TARGET_SHAPE_MODEL_CLASS:                   'modelClass';
TARGET_SHAPE_NAME:                          'targetName';
TARGET_SHAPE_PARTITION_FIELDS:              'partitionFields';
TARGET_SHAPE_DEDUPLICATION:                 'deduplicationStrategy';

// FLAT
TARGET_SHAPE_FLAT:                          'Flat';

// MULTI-FLAT
TARGET_SHAPE_MULTI:                         'MultiFlat';
TARGET_SHAPE_MULTI_TXN_SCOPE:               'transactionScope';
TARGET_SHAPE_MULTI_PARTS:                   'parts';

TARGET_PART_MODEL_PROPERTY:                 'modelProperty';

TXN_SCOPE_SINGLE:                           'SINGLE_TARGET';
TXN_SCOPE_ALL:                              'ALL_TARGETS';

// DEDUPLICATION VALUES
DEDUPLICATION_ANY_VERSION:                  'AnyVersion';
DEDUPLICATION_MAX_VERSION:                  'MaxVersion';
DEDUPLICATION_MAX_VERSION_FIELD:            'versionField';
DEDUPLICATION_DUPLICATE_COUNT:              'DuplicateCount';
DEDUPLICATION_DUPLICATE_COUNT_NAME:         'duplicateCountName';

//**********
// INGEST MODE
//**********

INGEST_MODE_NONTEMPORAL_SNAPSHOT:           'NontemporalSnapshot';
INGEST_MODE_UNITEMPORAL_SNAPSHOT:           'UnitemporalSnapshot';
INGEST_MODE_BITEMPORAL_SNAPSHOT:            'BitemporalSnapshot';
INGEST_MODE_NONTEMPORAL_DELTA:              'NontemporalDelta';
INGEST_MODE_UNITEMPORAL_DELTA:              'UnitemporalDelta';
INGEST_MODE_BITEMPORAL_DELTA:               'BitemporalDelta';
INGEST_MODE_APPEND_ONLY:                    'AppendOnly';

//**********
// INGEST MODE MIX-INS
//**********

FILTER_DUPLICATES:                          'filterDuplicates';

AUDITING:                                   'auditing';
AUDITING_DATE_TIME_NAME:                    'dateTimeName';

TXN_MILESTONING:                            'transactionMilestoning';
TXN_MILESTONING_BATCH_ID:                   'BatchId';
TXN_MILESTONING_BOTH:                       'BatchIdAndDateTime';

BATCH_ID_IN_NAME:                           'batchIdInName';
BATCH_ID_OUT_NAME:                          'batchIdOutName';
DATE_TIME_IN_NAME:                          'dateTimeInName';
DATE_TIME_OUT_NAME:                         'dateTimeOutName';

VALIDITY_MILESTONING:                       'validityMilestoning';

DATE_TIME_FROM_NAME:                        'dateTimeFromName';
DATE_TIME_THRU_NAME:                        'dateTimeThruName';

VALIDITY_DERIVATION:                        'derivation';
VALIDITY_DERIVATION_SOURCE_FROM:            'SourceSpecifiesFromDateTime';
VALIDITY_DERIVATION_SOURCE_FROM_THRU:       'SourceSpecifiesFromAndThruDateTime';
SOURCE_DATE_TIME_FROM_FIELD:                'sourceDateTimeFromField';
SOURCE_DATE_TIME_THRU_FIELD:                'sourceDateTimeThruField';

MERGE_STRATEGY:                             'mergeStrategy';
MERGE_STRATEGY_NO_DELETES:                  'NoDeletes';
MERGE_STRATEGY_DELETE_INDICATOR:            'DeleteIndicator';
MERGE_STRATEGY_DELETE_INDICATOR_FIELD:      'deleteField';
MERGE_STRATEGY_DELETE_INDICATOR_VALUES:     'deleteValues';
