lexer grammar PersistenceLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
IMPORT:                                 'import';

// MODEL
SERVICE_PERSISTENCE:                    'ServicePersistence';
SERVICE_PERSISTENCE_DOCUMENTATION:      'doc';
SERVICE_PERSISTENCE_OWNERS:             'owners';
SERVICE_PERSISTENCE_TRIGGER:            'trigger';
SERVICE_PERSISTENCE_SERVICE:            'service';

EVENT_TYPE_SCHEDULE_TRIGGERED:          'ScheduleTriggered';
EVENT_TYPE_REGISTRY_DATASET_AVAILABLE:  'RegistryDatasetAvailable';

PERSISTENCE:                            'persistence';
PERSISTENCE_STREAMING:                  'Streaming';
PERSISTENCE_BATCH:                      'Batch';

INPUT_SHAPE:                            'inputShape';
INPUT_SHAPE_FLAT:                       'FLAT';
INPUT_SHAPE_GROUPED_FLAT:               'GROUPED_FLAT';
INPUT_SHAPE_NESTED:                     'NESTED';

INPUT_CLASS:                            'inputClass';

TRANSACTION_MODE:                       'transactionMode';
TRANSACTION_MODE_SINGLE_DATASET:        'SINGLE_DATASET';
TRANSACTION_MODE_ALL_DATASETS:          'ALL_DATASETS';

TARGET:                                 'target';

DATASTORE:                              'Datastore';
DATASTORE_NAME:                         'datastoreName';

DATASET:                                'dataset';
DATASETS:                               'datasets';
DATASET_NAME:                           'datasetName';

PARTITION_PROPERTIES:                   'partitionProperties';

DEDUPLICATION_STRATEGY:                 'deduplicationStrategy';
DEDUPLICATION_STRATEGY_NONE:            'NoDedup';
DEDUPLICATION_STRATEGY_ANY:             'AnyDedup';
DEDUPLICATION_STRATEGY_COUNT:           'CountDed';
DEDUPLICATION_STRATEGY_MAX_VERSION:     'MaxVersionDedup';

BATCH_MODE:                             'batchMode';
SNAPSHOT_NON_MILESTONED:                'NonMilestonedSnapshot';
SNAPSHOT_UNITEMPORAL:                   'UnitemporalSnapshot';
SNAPSHOT_BITEMPORAL:                    'BitemporalSnapshot';
DELTA_NON_MILESTONED:                   'NonMilestonedDelta';
DELTA_UNITEMPORAL:                      'UnitemporalDelta';
DELTA_BITEMPORAL:                       'BitemporalDelta';
APPEND_ONLY:                            'AppendOnly';

AUDIT_SCHEME:                           'auditScheme';
AUDIT_SCHEME_NONE:                      'NoAudit';
AUDIT_SCHEME_BATCH_DATE_TIME:           'BatchDateTime';
AUDIT_SCHEME_OPAQUE:                    'OpaqueAudit';

TRANSACTION_SCHEME:                     'transactionMilestoning';
TRANSACTION_SCHEME_BATCH_ID:            'BatchIdOnly';
TRANSACTION_SCHEME_DATE_TIME:           'DateTimeOnly';
TRANSACTION_SCHEME_BOTH:                'BatchIdAndDateTime';
TRANSACTION_SCHEME_OPAQUE:              'OpaqueTransactionMilestoning';

VALIDITY_SCHEME:                        'validityMilestoning';
VALIDITY_SCHEME_DATE_TIME:              'DateTime';
VALIDITY_SCHEME_OPAQUE:                 'OpaqueValidityMilestoning';

VALIDITY_DERIVATION:                    'validityDerivation';
VALIDITY_DERIVATION_SOURCE_FROM:        'SourceProvidesFromDateTime';
VALIDITY_DERIVATION_SOURCE_FROM_THRU:   'SourceProvidesFromAndThruDateTime';

MERGE_SCHEME:                           'mergeScheme';
MERGE_SCHEME_NO_DELETES:                'NoDeletes';
MERGE_SCHEME_DELETE_INDICATOR:          'DeleteIndicator';
