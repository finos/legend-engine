lexer grammar PersistenceLexerGrammar;

import CoreLexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
NAME:                                   'name';
NONE:                                   'none';
OPAQUE:                                 'opaque';
DATE_TIME:                              'dateTime';

// MODEL
SERVICE_PERSISTENCE:                    'ServicePersistence';
SERVICE_PERSISTENCE_DOCUMENTATION:      'doc';
SERVICE_PERSISTENCE_OWNERS:             'owners';
SERVICE_PERSISTENCE_TRIGGER:            'trigger';
SERVICE_PERSISTENCE_SERVICE:            'service';

EVENT_TYPE_SCHEDULE_TRIGGERED:          'scheduleTriggered';
EVENT_TYPE_REGISTRY_DATASET_AVAILABLE:  'registryDatasetAvailable';

PERSISTENCE:                            'persistence';
PERSISTENCE_STREAMING:                  'streaming';
PERSISTENCE_BATCH:                      'batch';

INPUT_SHAPE:                            'inputShape';
INPUT_SHAPE_FLAT:                       'flat';
INPUT_SHAPE_GROUPED_FLAT:               'groupedFlat';
INPUT_SHAPE_NESTED:                     'nested';

INPUT_CLASS:                            'inputClass';

TRANSACTION_MODE:                       'transactionMode';
TRANSACTION_MODE_SINGLE_DATASET:        'singleDataset';
TRANSACTION_MODE_ALL_DATASETS:          'allDatasets';

TARGET:                                 'target';

DATASTORE:                              'datastoreName';
DATASTORE_NAME:                         NAME;

DATASET:                                'dataset';
DATASET_NAME:                           NAME;
DATASET_PARTITION_PROPERTIES:           'partitionProperties';

DEDUPLICATION_STRATEGY:                 'deduplicationStrategy';
DEDUPLICATION_STRATEGY_NONE:            NONE;
DEDUPLICATION_STRATEGY_ANY:             'any';
DEDUPLICATION_STRATEGY_COUNT:           'count';
DEDUPLICATION_STRATEGY_MAX_VERSION:     'maxVersion';

MILESTONING_MODE:                       'milestoningMode';
MILESTONING_MODE_MILESTONED:            'milestoned';
MILESTONING_MODE_NON_MILESTONED:        'nonMilestoned';

SNAPSHOT:                               'snapshot';
DELTA:                                  'delta';
APPEND_ONLY:                            'appendOnly';

UNITEMPORAL:                            'unitemporal';
BITEMPORAL:                             'bitemporal';

AUDIT_SCHEME:                           'auditScheme';
AUDIT_SCHEME_NONE:                      NONE;
AUDIT_SCHEME_BATCH_DATE_TIME:           'batchDateTime';
AUDIT_SCHEME_OPAQUE:                    OPAQUE;

TRANSACTION_SCHEME:                     'transactionMilestoningScheme';
TRANSACTION_SCHEME_BATCH_ID:            'batchId';
TRANSACTION_SCHEME_DATE_TIME:           DATE_TIME;
TRANSACTION_SCHEME_BOTH:                'batchIdAndDateTime';
TRANSACTION_SCHEME_OPAQUE:              OPAQUE;

VALIDITY_SCHEME:                        'validityMilestoningScheme';
VALIDITY_SCHEME_DATE_TIME:              DATE_TIME;
VALIDITY_SCHEME_OPAQUE:                 OPAQUE;

VALIDITY_DERIVATION:                    'validityDerivation';
VALIDITY_DERIVATION_SOURCE_FROM:        'sourceProvidesFromDateTime';
VALIDITY_DERIVATION_SOURCE_FROM_THRU:   'sourceProvidesFromAndThruDateTime';

MERGE_SCHEME:                           'mergeScheme';
MERGE_SCHEME_NO_DELETES:                'noDeletes';
MERGE_SCHEME_DELETE_INDICATOR:          'deleteIndicator';
