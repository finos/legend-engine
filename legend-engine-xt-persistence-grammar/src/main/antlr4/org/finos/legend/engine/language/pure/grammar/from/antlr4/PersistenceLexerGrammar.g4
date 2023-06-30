lexer grammar PersistenceLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
TRUE:                                       'true';
FALSE:                                      'false';
IMPORT:                                     'import';
DERIVATION:                                 'derivation';
NONE:                                       'None';
DATE_TIME:                                  'DateTime';

//**********
// PERSISTENCE
//**********

// CONTEXT
CONTEXT:                                    'PersistenceContext';
CONTEXT_PERSISTENCE:                        'persistence';
CONTEXT_PLATFORM:                           'platform';
CONTEXT_SERVICE_PARAMETERS:                 'serviceParameters';
CONTEXT_SINK_CONNECTION:                    'sinkConnection';

// PERSISTENCE
PERSISTENCE:                                'Persistence';
PERSISTENCE_DOC:                            'doc';
PERSISTENCE_TRIGGER:                        'trigger';
PERSISTENCE_SERVICE:                        'service';
PERSISTENCE_SERVICE_OUTPUT_TARGETS:         'serviceOutputTargets';
PERSISTENCE_PERSISTER:                      'persister';
PERSISTENCE_NOTIFIER:                       'notifier';
PERSISTENCE_TESTS:                          'tests';

// SERVICE OUTPUT TARGET
SERVICE_OUTPUT_TARGET_SERVICE_OUTPUT:       'serviceOutput';
SERVICE_OUTPUT_TARGET_TARGET:               'target';

// NOTIFIER
NOTIFIER:                                   'Notifier';
NOTIFIER_NOTIFYEES:                         'notifyees';
NOTIFYEE_EMAIL:                             'Email';
NOTIFYEE_EMAIL_ADDRESS:                     'address';
NOTIFYEE_PAGER_DUTY:                        'PagerDuty';
NOTIFYEE_PAGER_DUTY_URL:                    'url';

// TEST
//TODO: ledav -- reorganize these
PERSISTENCE_TEST_DATA_FROM_SERVICE_OUTPUT:  'isTestDataFromServiceOutput';
PERSISTENCE_TEST_BATCHES:                   'testBatches';
PERSISTENCE_TEST_DATA:                      'data';
PERSISTENCE_TEST_CONNECTION_DATA:           'connection';
PERSISTENCE_TEST_ASSERTS:                   'asserts';

//**********
// SERVICE OUTPUT
//**********

// DATASET
SERVICE_OUTPUT_TDS:                         'TDS';
DATASET_KEYS:                               'keys';
DATASET_DEDUPLICATION:                      'deduplication';
DATASET_TYPE:                               'datasetType';

// DEDUPLICATION
DEDUPLICATION_ANY:                          'AnyVersion';
DEDUPLICATION_MAX:                          'MaxVersion';
DEDUPLICATION_MAX_VERSION_FIELD:            'versionField';

// DATASET TYPE
DATASET_SNAPSHOT:                           'Snapshot';
DATASET_SNAPSHOT_PARTITIONING:              'partitioning';
DATASET_DELTA:                              'Delta';
DATASET_DELTA_ACTION_INDICATOR:             'actionIndicator';

// PARTITIONING
PARTITIONING_FIELD_BASED:                   'FieldBased';
PARTITIONING_FIELD_BASED_FIELDS:            'partitionFields';
PARTITIONING_NONE_EMPTY_DATASET_HANDLING:   'emptyDatasetHandling';

// EMPTY DATASET HANDLING
EMPTY_DATASET_HANDLING_NOOP:                'NoOp';
EMPTY_DATASET_HANDLING_DELETE_TARGET_DATA:  'DeleteTargetData';

// ACTION INDICATOR
ACTION_INDICATOR_DELETE_INDICATOR:          'DeleteIndicator';
ACTION_INDICATOR_DELETE_INDICATOR_FIELD:    'deleteField';
ACTION_INDICATOR_DELETE_INDICATOR_VALUES:   'deleteValues';

//TODO: ledav -- remove once v2 is rolled out | START

// PERSISTER
PERSISTER_STREAMING:                        'Streaming';
PERSISTER_BATCH:                            'Batch';
PERSISTER_SINK:                             'sink';
PERSISTER_TARGET_SHAPE:                     'targetShape';
PERSISTER_INGEST_MODE:                      'ingestMode';

//**********
// SINK
//**********

SINK_RELATIONAL:                            'Relational';
SINK_OBJECT_STORAGE:                        'ObjectStorage';
SINK_DATABASE:                              'database';
SINK_BINDING:                               'binding';

//**********
// TARGET SHAPE
//**********

TARGET_SHAPE_MODEL_CLASS:                   'modelClass';
TARGET_SHAPE_NAME:                          'targetName';
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

TRANSACTION_DERIVATION_SOURCE_IN:           'SourceSpecifiesInDateTime';
TRANSACTION_DERIVATION_SOURCE_IN_OUT:       'SourceSpecifiesInAndOutDateTime';
SOURCE_DATE_TIME_IN_FIELD:                  'sourceDateTimeInField';
SOURCE_DATE_TIME_OUT_FIELD:                 'sourceDateTimeOutField';

VALIDITY_MILESTONING:                       'validityMilestoning';

DATE_TIME_FROM_NAME:                        'dateTimeFromName';
DATE_TIME_THRU_NAME:                        'dateTimeThruName';

VALIDITY_DERIVATION_SOURCE_FROM:            'SourceSpecifiesFromDateTime';
VALIDITY_DERIVATION_SOURCE_FROM_THRU:       'SourceSpecifiesFromAndThruDateTime';
SOURCE_DATE_TIME_FROM_FIELD:                'sourceDateTimeFromField';
SOURCE_DATE_TIME_THRU_FIELD:                'sourceDateTimeThruField';

MERGE_STRATEGY:                             'mergeStrategy';
MERGE_STRATEGY_NO_DELETES:                  'NoDeletes';

//TODO: ledav -- remove once v2 is rolled out | END
