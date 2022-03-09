lexer grammar PersistenceLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
TRUE:                                       'true';
FALSE:                                      'false';
IMPORT:                                     'import';

//**********
// PERSISTENCE
//**********

PERSISTENCE:                                'Persistence';
PERSISTENCE_DOC:                            'doc';
PERSISTENCE_OWNERS:                         'owners';
PERSISTENCE_TRIGGER:                        'trigger';
PERSISTENCE_READER:                         'reader';
PERSISTENCE_PERSISTER:                      'persister';

// TRIGGER
TRIGGER_OPAQUE:                             'OpaqueTrigger';

// READER
READER_SERVICE:                             'Service';
READER_SERVICE_SERVICE:                     'service';

// PERSISTER
PERSISTER_STREAMING:                        'Streaming';
PERSISTER_BATCH:                            'Batch';
PERSISTER_BATCH_TARGET:                     'target';

//**********
// TARGET SPECIFICATION
//**********

TARGET_SPEC_NAME:                           'targetName';
TARGET_SPEC_MODEL_CLASS:                    'modelClass';

// GROUPED
TARGET_SPEC_GROUPED:                        'GroupedFlat';
TARGET_SPEC_GROUPED_TXN_SCOPE:              'transactionScope';
TARGET_SPEC_GROUPED_COMPONENTS:             'components';

TARGET_COMPONENT_PROPERTY:                  'property';
TARGET_COMPONENT_TARGET_SPEC:               'targetSpecification';

// FLAT
TARGET_SPEC_FLAT:                           'Flat';
TARGET_SPEC_FLAT_PARTITION_PROPERTIES:      'partitionProperties';
TARGET_SPEC_FLAT_DEDUPLICATION:             'deduplicationStrategy';
TARGET_SPEC_FLAT_BATCH_MODE:                'batchMode';

// NESTED
TARGET_SPEC_NESTED:                         'Nested';

// TXN_SCOPE VAUES
TXN_SCOPE_SINGLE:                           'SINGLE_TARGET';
TXN_SCOPE_ALL:                              'ALL_TARGETS';

// DEDUPLICATION VALUES
DEDUPLICATION_NONE:                         'NoDeduplication';
DEDUPLICATION_ANY_VERSION:                  'AnyVersion';
DEDUPLICATION_MAX_VERSION:                  'MaxVersion';
DEDUPLICATION_MAX_VERSION_PROPERTY:         'versionProperty';
DEDUPLICATION_OPAQUE:                       'OpaqueDeduplication';

// BATCH MODE VALUES
BATCH_MODE_NON_MILESTONED_SNAPSHOT:         'NonMilestonedSnapshot';
BATCH_MODE_UNITEMPORAL_SNAPSHOT:            'UnitemporalSnapshot';
BATCH_MODE_BITEMPORAL_SNAPSHOT:             'BitemporalSnapshot';
BATCH_MODE_NON_MILESTONED_DELTA:            'NonMilestonedDelta';
BATCH_MODE_UNITEMPORAL_DELTA:               'UnitemporalDelta';
BATCH_MODE_BITEMPORAL_DELTA:                'BitemporalDelta';
BATCH_MODE_APPEND_ONLY:                     'AppendOnly';

//**********
// BATCH MODE MIX-INS
//**********

FILTER_DUPLICATES:                          'filterDuplicates';

AUDITING:                                   'auditing';
AUDITING_NONE:                              'NoAuditing';
AUDITING_BATCH_DATE_TIME:                   'BatchDateTime';
AUDITING_BATCH_DATE_TIME_FIELD_NAME:        'batchDateTimeFieldName';
AUDITING_OPAQUE:                            'OpaqueAuditing';

TXN_MILESTONING:                            'transactionMilestoning';
TXN_MILESTONING_BATCH_ID:                   'BatchIdOnly';
TXN_MILESTONING_DATE_TIME:                  'DateTimeOnly';
TXN_MILESTONING_BOTH:                       'BatchIdAndDateTime';
TXN_MILESTONING_OPAQUE:                     'OpaqueTransactionMilestoning';
BATCH_ID_IN_FIELD_NAME:                     'batchIdInFieldName';
BATCH_ID_OUT_FIELD_NAME:                    'batchIdOutFieldName';
DATE_TIME_IN_FIELD_NAME:                    'dateTimeInFieldName';
DATE_TIME_OUT_FIELD_NAME:                   'dateTimeOutFieldName';

VALIDITY_MILESTONING:                       'validityMilestoning';
VALIDITY_MILESTONING_DATE_TIME:             'DateTime';
VALIDITY_MILESTONING_OPAQUE:                'OpaqueValidityMilestoning';
DATE_TIME_FROM_FIELD_NAME:                  'dateTimeFromFieldName';
DATE_TIME_THRU_FIELD_NAME:                  'dateTimeThruFieldName';

VALIDITY_DERIVATION:                        'derivation';
VALIDITY_DERIVATION_SOURCE_FROM:            'SourceSpecifiesFromDateTime';
VALIDITY_DERIVATION_SOURCE_FROM_THRU:       'SourceSpecifiesFromAndThruDateTime';
SOURCE_DATE_TIME_FROM_PROPERTY:             'sourceDateTimeFromProperty';
SOURCE_DATE_TIME_THRU_PROPERTY:             'sourceDateTimeThruProperty';
VALIDITY_DERIVATION_OPAQUE:                 'OpaqueValidityDerivation';

MERGE_STRATEGY:                             'mergeStrategy';
MERGE_STRATEGY_NO_DELETES:                  'NoDeletes';
MERGE_STRATEGY_DELETE_INDICATOR:            'DeleteIndicator';
MERGE_STRATEGY_DELETE_INDICATOR_PROPERTY:   'deleteProperty';
MERGE_STRATEGY_DELETE_INDICATOR_VALUES:     'deleteValues';
MERGE_STRATEGY_OPAQUE:                      'OpaqueMerge';
