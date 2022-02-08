lexer grammar PersistenceLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
TRUE:                                       'true';
FALSE:                                      'false';
IMPORT:                                     'import';

//**********
// PERSISTENCE PIPE
//**********

PERSISTENCE_PIPE:                           'PersistencePipe';
PERSISTENCE_PIPE_DOC:                       'doc';
PERSISTENCE_PIPE_OWNERS:                    'owners';
PERSISTENCE_PIPE_TRIGGER:                   'trigger';
PERSISTENCE_PIPE_READER:                    'reader';
PERSISTENCE_PIPE_PERSISTER:                 'persister';

// EVENT
EVENT_SCHEDULE_FIRED:                   'ScheduleTriggered';
EVENT_OPAQUE:                               'OpaqueEvent';

// READER
SERViCE_READER:                             'Service';
SERViCE_READER_SERVICE:                     'service';

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
AUDITING_NONE:                              'NoAudit';
AUDITING_BATCH_DATE_TIME:                   'BatchDateTime';
AUDITING_BATCH_DATE_TIME_PROPERTY:          'transactionDateTimePropertyName';
AUDITING_OPAQUE:                            'OpaqueAudit';

TXN_MILESTONING:                            'transactionMilestoning';
TXN_MILESTONING_BATCH_ID:                   'BatchIdOnly';
TXN_MILESTONING_DATE_TIME:                  'DateTimeOnly';
TXN_MILESTONING_BOTH:                       'BatchIdAndDateTime';
TXN_MILESTONING_OPAQUE:                     'OpaqueTransactionMilestoning';
BATCH_ID_IN_PROPERTY:                       'batchIdInProperty';
BATCH_ID_OUT_PROPERTY:                      'batchIdOutProperty';
DATE_TIME_IN_PROPERTY:                      'dateTimeInProperty';
DATE_TIME_OUT_PROPERTY:                     'dateTimeOutProperty';

VALIDITY_MILESTONING:                       'validityMilestoning';
VALIDITY_MILESTONING_DATE_TIME:             'DateTime';
VALIDITY_MILESTONING_OPAQUE:                'OpaqueValidityMilestoning';
DATE_TIME_FROM_PROPERTY:                    'dateTimeFromProperty';
DATE_TIME_THRU_PROPERTY:                    'dateTimeThruProperty';

VALIDITY_DERIVATION:                        'validityDerivation';
VALIDITY_DERIVATION_SOURCE_FROM:            'SourceProvidesFromDateTime';
VALIDITY_DERIVATION_SOURCE_FROM_THRU:       'SourceProvidesFromAndThruDateTime';
SOURCE_DATE_TIME_FROM_PROPERTY:             'sourceDateTimeFromProperty';
SOURCE_DATE_TIME_THRU_PROPERTY:             'sourceDateTimeThruProperty';
VALIDITY_DERIVATION_OPAQUE:                 'OpaqueValidityDerivation';

MERGE_STRATEGY:                             'mergeStrategy';
MERGE_STRATEGY_NO_DELETES:                  'NoDeletes';
MERGE_STRATEGY_DELETE_INDICATOR:            'DeleteIndicator';
MERGE_STRATEGY_DELETE_INDICATOR_PROPERTY:   'deleteProperty';
MERGE_STRATEGY_DELETE_INDICATOR_VALUES:     'deleteValues';
MERGE_STRATEGY_OPAQUE:                      'OpaqueMerge';
