parser grammar PersistenceParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = PersistenceLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING
                                            | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE
                                            | BYTE_STREAM_FUNCTION      // from M3Parser
                                            | TRUE | FALSE | IMPORT | DERIVATION | NONE | DATE_TIME
                                            | CONTEXT | CONTEXT_PERSISTENCE | CONTEXT_PLATFORM | CONTEXT_SERVICE_PARAMETERS | CONTEXT_SINK_CONNECTION
                                            | PERSISTENCE | PERSISTENCE_DOC | PERSISTENCE_TRIGGER | PERSISTENCE_SERVICE | PERSISTENCE_SERVICE_OUTPUT_TARGETS | PERSISTENCE_PERSISTER | PERSISTENCE_NOTIFIER
                                            | PERSISTER_STREAMING | PERSISTER_BATCH | PERSISTER_SINK | PERSISTER_TARGET_SHAPE | PERSISTER_INGEST_MODE
                                            | NOTIFIER | NOTIFIER_NOTIFYEES | NOTIFYEE_EMAIL | NOTIFYEE_EMAIL_ADDRESS | NOTIFYEE_PAGER_DUTY| NOTIFYEE_PAGER_DUTY_URL
                                            | PERSISTENCE_TESTS | PERSISTENCE_TEST_DATA | PERSISTENCE_TEST_CONNECTION_DATA | PERSISTENCE_TEST_BATCHES | PERSISTENCE_TEST_ASSERTS | PERSISTENCE_TEST_DATA_FROM_SERVICE_OUTPUT
                                            | SERVICE_OUTPUT_TARGET_SERVICE_OUTPUT | SERVICE_OUTPUT_TARGET_TARGET
                                            | SERVICE_OUTPUT_ROOT | DATASET_KEYS | DATASET_DEDUPLICATION | DATASET_TYPE
                                            | DEDUPLICATION_ANY | DEDUPLICATION_MAX | DEDUPLICATION_MAX_VERSION_FIELD
                                            | DATASET_SNAPSHOT | DATASET_SNAPSHOT_PARTITIONING | DATASET_DELTA | DATASET_DELTA_ACTION_INDICATOR
                                            | PARTITIONING_FIELD_BASED | PARTITIONING_FIELD_BASED_FIELDS | PARTITIONING_NONE_EMPTY_DATASET_HANDLING
                                            | EMPTY_DATASET_HANDLING_NOOP | EMPTY_DATASET_HANDLING_DELETE_TARGET_DATA
                                            | ACTION_INDICATOR_DELETE_INDICATOR | ACTION_INDICATOR_DELETE_INDICATOR_FIELD | ACTION_INDICATOR_DELETE_INDICATOR_VALUES

                                            //TODO: ledav -- remove once v2 is rolled out | START
                                            | SINK_RELATIONAL | SINK_OBJECT_STORAGE | SINK_DATABASE | SINK_BINDING
                                            | TARGET_SHAPE_MODEL_CLASS | TARGET_SHAPE_NAME | TARGET_SHAPE_DEDUPLICATION
                                            | TARGET_SHAPE_FLAT | TARGET_SHAPE_MULTI | TARGET_SHAPE_MULTI_TXN_SCOPE | TARGET_SHAPE_MULTI_PARTS | TARGET_PART_MODEL_PROPERTY | TXN_SCOPE_SINGLE | TXN_SCOPE_ALL
                                            | DEDUPLICATION_DUPLICATE_COUNT | DEDUPLICATION_DUPLICATE_COUNT_NAME
                                            | INGEST_MODE_NONTEMPORAL_SNAPSHOT | INGEST_MODE_UNITEMPORAL_SNAPSHOT | INGEST_MODE_BITEMPORAL_SNAPSHOT | INGEST_MODE_NONTEMPORAL_DELTA | INGEST_MODE_UNITEMPORAL_DELTA | INGEST_MODE_BITEMPORAL_DELTA | INGEST_MODE_APPEND_ONLY
                                            | FILTER_DUPLICATES
                                            | AUDITING | AUDITING_DATE_TIME_NAME
                                            | TXN_MILESTONING | TXN_MILESTONING_BATCH_ID | TXN_MILESTONING_BOTH | BATCH_ID_IN_NAME | BATCH_ID_OUT_NAME | DATE_TIME_IN_NAME | DATE_TIME_OUT_NAME
                                            | TRANSACTION_DERIVATION_SOURCE_IN | TRANSACTION_DERIVATION_SOURCE_IN_OUT | SOURCE_DATE_TIME_IN_FIELD | SOURCE_DATE_TIME_OUT_FIELD
                                            | VALIDITY_MILESTONING | DATE_TIME_FROM_NAME | DATE_TIME_THRU_NAME
                                            | VALIDITY_DERIVATION_SOURCE_FROM | VALIDITY_DERIVATION_SOURCE_FROM_THRU | SOURCE_DATE_TIME_FROM_FIELD | SOURCE_DATE_TIME_THRU_FIELD
                                            | MERGE_STRATEGY | MERGE_STRATEGY_NO_DELETES
                                            //TODO: ledav -- remove once v2 is rolled out | END
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 imports
                                                elementDefinition*
                                            EOF
;
imports:                                    (importStatement)*
;
importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
elementDefinition:                          (
                                                context
                                                | persistence
                                            )
;

// -------------------------------------- CONTEXT --------------------------------------

context:                                    CONTEXT qualifiedName
                                                BRACE_OPEN
                                                    (
                                                        contextPersistence
                                                        | contextPlatform
                                                        | contextServiceParameters
                                                        | contextSinkConnection
                                                    )*
                                                BRACE_CLOSE
;
contextPersistence:                         CONTEXT_PERSISTENCE COLON qualifiedName SEMI_COLON
;
contextPlatform:                            CONTEXT_PLATFORM COLON platformSpecification SEMI_COLON
;
platformSpecification:                      platformType (platformValue)?
;
platformType:                               identifier
;
platformValue:                              ISLAND_OPEN (platformValueContent)* ISLAND_END
;
platformValueContent:                       ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_BRACE_CLOSE
;
contextServiceParameters:                   CONTEXT_SERVICE_PARAMETERS COLON
                                                BRACKET_OPEN
                                                    (serviceParameter (COMMA serviceParameter)*)?
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
serviceParameter:                           identifier EQUAL
                                                (
                                                    primitiveValue
                                                    | connectionPointer
                                                    | embeddedConnection
                                                )
;
contextSinkConnection:                      CONTEXT_SINK_CONNECTION COLON
                                                (
                                                    connectionPointer
                                                    | embeddedConnection
                                                )
                                            SEMI_COLON
;
connectionPointer:                          qualifiedName
;
embeddedConnection:                         ISLAND_OPEN (embeddedConnectionContent)*
;
embeddedConnectionContent:                  ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;

// -------------------------------------- PERSISTENCE --------------------------------------

persistence:                                PERSISTENCE qualifiedName
                                                BRACE_OPEN
                                                    (
                                                        documentation
                                                        | trigger
                                                        | service
                                                        | serviceOutputTargets
                                                        | persister
                                                        | notifier
                                                        | tests
                                                    )*
                                                BRACE_CLOSE
;
documentation:                              PERSISTENCE_DOC COLON STRING SEMI_COLON
;

// -------------------------------------- TRIGGER --------------------------------------

trigger:                                    PERSISTENCE_TRIGGER COLON triggerSpecification SEMI_COLON
;
triggerSpecification:                       triggerType (triggerValue)?
;
triggerType:                                identifier
;
triggerValue:                               ISLAND_OPEN (triggerValueContent)* ISLAND_END
;
triggerValueContent:                        ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_BRACE_CLOSE
;

// -------------------------------------- SERVICE --------------------------------------

service:                                    PERSISTENCE_SERVICE COLON qualifiedName SEMI_COLON
;

// -------------------------------------- SERVICE OUTPUT TARGET --------------------------------------

serviceOutputTargets:                       PERSISTENCE_SERVICE_OUTPUT_TARGETS COLON
                                                BRACKET_OPEN
                                                     serviceOutputTarget (COMMA serviceOutputTarget)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
serviceOutputTarget:                        serviceOutput ARROW target
;
// -------------------------------------- NOTIFIER --------------------------------------

notifier:                                   PERSISTENCE_NOTIFIER COLON
                                                BRACE_OPEN
                                                    (notifyees)*
                                                BRACE_CLOSE
;
notifyees:                                  NOTIFIER_NOTIFYEES COLON
                                                BRACKET_OPEN
                                                    notifyee (COMMA notifyee)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
notifyee:                                   (emailNotifyee | pagerDutyNotifyee)
;
emailNotifyee:                              NOTIFYEE_EMAIL
                                                BRACE_OPEN
                                                    (emailAddress)*
                                                BRACE_CLOSE
;
emailAddress:                               NOTIFYEE_EMAIL_ADDRESS COLON STRING SEMI_COLON
;
pagerDutyNotifyee:                          NOTIFYEE_PAGER_DUTY
                                                BRACE_OPEN
                                                    (pagerDutyUrl)*
                                                BRACE_CLOSE
;
pagerDutyUrl:                               NOTIFYEE_PAGER_DUTY_URL COLON STRING SEMI_COLON
;

// -------------------------------------- DATASET --------------------------------------

serviceOutput:                              (SERVICE_OUTPUT_ROOT | STRING)
                                                BRACE_OPEN
                                                    (
                                                        datasetKeys
                                                        | deduplication
                                                        | datasetType
                                                    )*
                                                BRACE_CLOSE
;
target:                                     (BRACE_OPEN BRACE_CLOSE)
                                            | (targetSpecification)
;
targetSpecification:                        targetType (targetValue)?
;
targetType:                                 identifier
;
targetValue:                                ISLAND_OPEN (targetValueContent)* ISLAND_END
;
targetValueContent:                         ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_BRACE_CLOSE
;
datasetKeys:                                DATASET_KEYS COLON
                                                BRACKET_OPEN
                                                    (identifier (COMMA identifier)*)?
                                                BRACKET_CLOSE
;
deduplication:                              DATASET_DEDUPLICATION COLON
                                                (
                                                    deduplicationNone
                                                    | deduplicationAny
                                                    | deduplicationMax
                                                )
;
deduplicationNone:                          NONE
                                                (
                                                    SEMI_COLON
                                                    | (BRACE_OPEN BRACE_CLOSE)
                                                )
;
deduplicationAny:                           DEDUPLICATION_ANY
                                                (
                                                    SEMI_COLON
                                                    | (BRACE_OPEN BRACE_CLOSE)
                                                )

;
deduplicationMax:                           DEDUPLICATION_MAX
                                                BRACE_OPEN
                                                    (deduplicationMaxVersionField)*
                                                BRACE_CLOSE
;
deduplicationMaxVersionField:               DEDUPLICATION_MAX_VERSION_FIELD COLON identifier SEMI_COLON
;
datasetType:                                DATASET_TYPE COLON
                                                (
                                                    datasetSnapshot
                                                    | datasetDelta
                                                )
;
datasetSnapshot:                            DATASET_SNAPSHOT
                                                BRACE_OPEN
                                                    (partitioning)*
                                                BRACE_CLOSE
;
partitioning:                               DATASET_SNAPSHOT_PARTITIONING COLON
                                                (
                                                    partitioningNone
                                                    | partitioningFieldBased
                                                )
;
partitioningNone:                           NONE
                                                BRACE_OPEN
                                                    (emptyDatasetHandling)*
                                                BRACE_CLOSE
;
emptyDatasetHandling:                       PARTITIONING_NONE_EMPTY_DATASET_HANDLING COLON
                                                (
                                                    emptyDatasetHandlingNoOp
                                                    | emptyDatasetHandlingDeleteTargetData
                                                )
;
emptyDatasetHandlingNoOp:                   EMPTY_DATASET_HANDLING_NOOP
                                                (
                                                    SEMI_COLON
                                                    | (BRACE_OPEN BRACE_CLOSE)
                                                )
;
emptyDatasetHandlingDeleteTargetData:       EMPTY_DATASET_HANDLING_DELETE_TARGET_DATA
                                                (
                                                    SEMI_COLON
                                                    | (BRACE_OPEN BRACE_CLOSE)
                                                )
;
partitioningFieldBased:                     PARTITIONING_FIELD_BASED
                                                BRACE_OPEN
                                                    (partitionFields)*
                                                BRACE_CLOSE
;
partitionFields:                            PARTITIONING_FIELD_BASED_FIELDS COLON
                                                BRACKET_OPEN
                                                    (identifier (COMMA identifier)*)?
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
datasetDelta:                               DATASET_DELTA
                                                BRACE_OPEN
                                                    (actionIndicator)*
                                                BRACE_CLOSE
;
actionIndicator:                            DATASET_DELTA_ACTION_INDICATOR COLON
                                                (
                                                    noActionIndicator
                                                    | deleteIndicator
                                                )
;
noActionIndicator:                          NONE
                                                (
                                                    SEMI_COLON
                                                    | (BRACE_OPEN BRACE_CLOSE)
                                                )
;
deleteIndicator:                            ACTION_INDICATOR_DELETE_INDICATOR
                                                BRACE_OPEN
                                                    (
                                                        deleteIndicatorField
                                                        | deleteIndicatorValues
                                                    )*
                                                BRACE_CLOSE
;
deleteIndicatorField:                       ACTION_INDICATOR_DELETE_INDICATOR_FIELD COLON identifier SEMI_COLON
;
deleteIndicatorValues:                      ACTION_INDICATOR_DELETE_INDICATOR_VALUES COLON
                                                BRACKET_OPEN
                                                    (identifier (COMMA identifier)*)?
                                                BRACKET_CLOSE
                                            SEMI_COLON
;

// ---------------------------------- TESTS ----------------------------------

tests:                                  PERSISTENCE_TESTS COLON BRACKET_OPEN ( test ( COMMA test )* )? BRACKET_CLOSE
;
test:                                   identifier COLON BRACE_OPEN ( persistenceTestBatches ( isTestDataFromServiceOutput )? )* BRACE_CLOSE
;
isTestDataFromServiceOutput:            PERSISTENCE_TEST_DATA_FROM_SERVICE_OUTPUT COLON identifier SEMI_COLON
;
persistenceTestBatches:                 PERSISTENCE_TEST_BATCHES COLON BRACKET_OPEN ( persistenceTestBatch ( COMMA persistenceTestBatch )* )? BRACKET_CLOSE
;
persistenceTestBatch:                   identifier COLON BRACE_OPEN ( persistenceTestData | persistenceTestBatchAssert )* BRACE_CLOSE
;
persistenceTestData:                    PERSISTENCE_TEST_DATA COLON BRACE_OPEN persistenceTestConnectionData BRACE_CLOSE
;
persistenceTestConnectionData:          PERSISTENCE_TEST_CONNECTION_DATA COLON BRACE_OPEN embeddedData BRACE_CLOSE
;
embeddedData:                           identifier ISLAND_OPEN ( embeddedDataContent )*
;
embeddedDataContent:                    ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
persistenceTestBatchAssert:             PERSISTENCE_TEST_ASSERTS COLON BRACKET_OPEN ( persistenceTestAssert ( COMMA persistenceTestAssert)* )? BRACKET_CLOSE
;
persistenceTestAssert:                  identifier COLON testAssertion
;
testAssertion:                          identifier ISLAND_OPEN ( testAssertionContent )*
;
testAssertionContent:                   ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;

//TODO: ledav -- remove once v2 is rolled out | START

// -------------------------------------- PERSISTER --------------------------------------

persister:                                  PERSISTENCE_PERSISTER COLON
                                                (
                                                    streamingPersister
                                                    | batchPersister
                                                )
;
streamingPersister:                         PERSISTER_STREAMING
                                                BRACE_OPEN
                                                    (
                                                        persisterSink
                                                    )*
                                                BRACE_CLOSE
;
batchPersister:                             PERSISTER_BATCH
                                                BRACE_OPEN
                                                    (
                                                        persisterSink
                                                        | targetShape
                                                        | ingestMode
                                                    )*
                                                BRACE_CLOSE
;

// -------------------------------------- SINK --------------------------------------

persisterSink:                              PERSISTER_SINK COLON
                                                (
                                                    relationalSink
                                                    | objectStorageSink
                                                )
;
relationalSink:                             SINK_RELATIONAL
                                                BRACE_OPEN
                                                    (sinkDatabase)*
                                                BRACE_CLOSE
;
objectStorageSink:                          SINK_OBJECT_STORAGE
                                                BRACE_OPEN
                                                    (sinkBinding)*
                                                BRACE_CLOSE
;
sinkDatabase:                               SINK_DATABASE COLON qualifiedName SEMI_COLON
;
sinkBinding:                                SINK_BINDING COLON qualifiedName SEMI_COLON
;

// -------------------------------------- TARGET SHAPE --------------------------------------

targetShape:                                PERSISTER_TARGET_SHAPE COLON
                                                (
                                                    multiTargetShape
                                                    | flatTargetShape
                                                )
;
multiTargetShape:                           TARGET_SHAPE_MULTI
                                                BRACE_OPEN
                                                    (
                                                        targetModelClass
                                                        | targetTransactionScope
                                                        | targetParts
                                                    )*
                                                BRACE_CLOSE
;
flatTargetShape:                            TARGET_SHAPE_FLAT
                                                BRACE_OPEN
                                                    (
                                                        targetName
                                                        | targetModelClass
                                                        | partitionFields
                                                        | deduplicationStrategy
                                                    )*
                                                BRACE_CLOSE
;
targetModelClass:                           TARGET_SHAPE_MODEL_CLASS COLON qualifiedName SEMI_COLON
;
targetTransactionScope:                     TARGET_SHAPE_MULTI_TXN_SCOPE COLON
                                                (
                                                    TXN_SCOPE_SINGLE
                                                    | TXN_SCOPE_ALL
                                                )
                                            SEMI_COLON
;
targetParts:                                TARGET_SHAPE_MULTI_PARTS COLON
                                                BRACKET_OPEN
                                                    targetPart (COMMA targetPart)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
targetPart:                                 BRACE_OPEN
                                                (
                                                    targetModelProperty
                                                    | targetName
                                                    | partitionFields
                                                    | deduplicationStrategy
                                                )*
                                            BRACE_CLOSE
;
targetModelProperty:                        TARGET_PART_MODEL_PROPERTY COLON identifier SEMI_COLON
;
targetName:                                 TARGET_SHAPE_NAME COLON STRING SEMI_COLON
;
deduplicationStrategy:                      TARGET_SHAPE_DEDUPLICATION COLON
                                                (
                                                    noDeduplicationStrategy
                                                    | anyVersionDeduplicationStrategy
                                                    | maxVersionDeduplicationStrategy
                                                    | duplicateCountDeduplicationStrategy
                                                )
;
noDeduplicationStrategy:                    NONE SEMI_COLON
;
anyVersionDeduplicationStrategy:            DEDUPLICATION_ANY SEMI_COLON
;
maxVersionDeduplicationStrategy:            DEDUPLICATION_MAX
                                                BRACE_OPEN
                                                    (
                                                        deduplicationMaxVersionField
                                                    )*
                                                BRACE_CLOSE
;
duplicateCountDeduplicationStrategy:        DEDUPLICATION_DUPLICATE_COUNT
                                                BRACE_OPEN
                                                    (
                                                      deduplicationDuplicateCountName
                                                    )*
                                                BRACE_CLOSE
;
deduplicationDuplicateCountName:            DEDUPLICATION_DUPLICATE_COUNT_NAME COLON STRING SEMI_COLON
;

// -------------------------------------- INGEST MODE --------------------------------------

ingestMode:                                 PERSISTER_INGEST_MODE COLON
                                                (
                                                    nontemporalSnapshot
                                                    | unitemporalSnapshot
                                                    | bitemporalSnapshot
                                                    | nontemporalDelta
                                                    | unitemporalDelta
                                                    | bitemporalDelta
                                                    | appendOnly
                                                )
;
nontemporalSnapshot:                        INGEST_MODE_NONTEMPORAL_SNAPSHOT
                                                BRACE_OPEN
                                                    (
                                                        auditing
                                                    )*
                                                BRACE_CLOSE
;
unitemporalSnapshot:                        INGEST_MODE_UNITEMPORAL_SNAPSHOT
                                                BRACE_OPEN
                                                    (
                                                        transactionMilestoning
                                                    )*
                                                BRACE_CLOSE
;
bitemporalSnapshot:                         INGEST_MODE_BITEMPORAL_SNAPSHOT
                                                BRACE_OPEN
                                                    (
                                                        transactionMilestoning
                                                        | validityMilestoning
                                                    )*
                                                BRACE_CLOSE
;
nontemporalDelta:                           INGEST_MODE_NONTEMPORAL_DELTA
                                                BRACE_OPEN
                                                    (
                                                        mergeStrategy
                                                        | auditing
                                                    )*
                                                BRACE_CLOSE
;
unitemporalDelta:                           INGEST_MODE_UNITEMPORAL_DELTA
                                                BRACE_OPEN
                                                    (
                                                        mergeStrategy
                                                        | transactionMilestoning
                                                    )*
                                                BRACE_CLOSE
;
bitemporalDelta:                            INGEST_MODE_BITEMPORAL_DELTA
                                                BRACE_OPEN
                                                    (
                                                        mergeStrategy
                                                        | transactionMilestoning
                                                        | validityMilestoning
                                                    )*
                                                BRACE_CLOSE
;
appendOnly:                                 INGEST_MODE_APPEND_ONLY
                                                BRACE_OPEN
                                                    (
                                                        auditing
                                                        | filterDuplicates
                                                    )*
                                                BRACE_CLOSE
;
auditing:                                   AUDITING COLON
                                                (
                                                    noAuditing
                                                    | dateTimeAuditing
                                                )
;
noAuditing:                                 NONE SEMI_COLON
;
dateTimeAuditing:                           DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeName
                                                    )*
                                                BRACE_CLOSE
;
dateTimeName:                               AUDITING_DATE_TIME_NAME COLON STRING SEMI_COLON
;
filterDuplicates:                           FILTER_DUPLICATES COLON (TRUE | FALSE) SEMI_COLON
;
transactionMilestoning:                     TXN_MILESTONING COLON
                                                (
                                                    batchIdTransactionMilestoning
                                                    | dateTimeTransactionMilestoning
                                                    | bothTransactionMilestoning
                                                )
;
batchIdTransactionMilestoning:              TXN_MILESTONING_BATCH_ID
                                                BRACE_OPEN
                                                    (
                                                        batchIdInName
                                                        | batchIdOutName
                                                    )*
                                                BRACE_CLOSE
;
batchIdInName:                              BATCH_ID_IN_NAME COLON STRING SEMI_COLON
;
batchIdOutName:                             BATCH_ID_OUT_NAME COLON STRING SEMI_COLON
;
dateTimeTransactionMilestoning:             DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeInName
                                                        | dateTimeOutName
                                                        | transactionDerivation
                                                    )*
                                                BRACE_CLOSE
;
dateTimeInName:                             DATE_TIME_IN_NAME COLON STRING SEMI_COLON
;
dateTimeOutName:                            DATE_TIME_OUT_NAME COLON STRING SEMI_COLON
;
bothTransactionMilestoning:                 TXN_MILESTONING_BOTH
                                                BRACE_OPEN
                                                    (
                                                        batchIdInName
                                                        | batchIdOutName
                                                        | dateTimeInName
                                                        | dateTimeOutName
                                                        | transactionDerivation
                                                    )*
                                                BRACE_CLOSE
;
transactionDerivation:                      DERIVATION COLON
                                                (
                                                    sourceSpecifiesInTransactionDerivation
                                                    | sourceSpecifiesInOutTransactionDerivation
                                                )
;
sourceSpecifiesInTransactionDerivation:     TRANSACTION_DERIVATION_SOURCE_IN
                                                BRACE_OPEN
                                                    (
                                                        transactionDerivationInField
                                                    )*
                                                BRACE_CLOSE
;
sourceSpecifiesInOutTransactionDerivation:  TRANSACTION_DERIVATION_SOURCE_IN_OUT
                                                BRACE_OPEN
                                                    (
                                                        transactionDerivationInField
                                                        | transactionDerivationOutField
                                                    )*
                                                BRACE_CLOSE
;
transactionDerivationInField:               SOURCE_DATE_TIME_IN_FIELD COLON identifier SEMI_COLON
;
transactionDerivationOutField:              SOURCE_DATE_TIME_OUT_FIELD COLON identifier SEMI_COLON
;
validityMilestoning:                        VALIDITY_MILESTONING COLON
                                                (
                                                    dateTimeValidityMilestoning
                                                )
;
dateTimeValidityMilestoning:                DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeFromName
                                                        | dateTimeThruName
                                                        | validityDerivation
                                                    )*
                                                BRACE_CLOSE
;
dateTimeFromName:                           DATE_TIME_FROM_NAME COLON STRING SEMI_COLON
;
dateTimeThruName:                           DATE_TIME_THRU_NAME COLON STRING SEMI_COLON
;
validityDerivation:                         DERIVATION COLON
                                                (
                                                    sourceSpecifiesFromValidityDerivation
                                                    | sourceSpecifiesFromThruValidityDerivation
                                                )
;
sourceSpecifiesFromValidityDerivation:      VALIDITY_DERIVATION_SOURCE_FROM
                                                BRACE_OPEN
                                                    (
                                                        validityDerivationFromField
                                                    )*
                                                BRACE_CLOSE
;
sourceSpecifiesFromThruValidityDerivation:  VALIDITY_DERIVATION_SOURCE_FROM_THRU
                                                BRACE_OPEN
                                                    (
                                                        validityDerivationFromField
                                                        | validityDerivationThruField
                                                    )*
                                                BRACE_CLOSE
;
validityDerivationFromField:                SOURCE_DATE_TIME_FROM_FIELD COLON identifier SEMI_COLON
;
validityDerivationThruField:                SOURCE_DATE_TIME_THRU_FIELD COLON identifier SEMI_COLON
;
mergeStrategy:                              MERGE_STRATEGY COLON
                                                (
                                                    noDeletesMergeStrategy
                                                    | deleteIndicatorMergeStrategy
                                                )
;
noDeletesMergeStrategy:                     MERGE_STRATEGY_NO_DELETES SEMI_COLON
;
deleteIndicatorMergeStrategy:               ACTION_INDICATOR_DELETE_INDICATOR
                                                BRACE_OPEN
                                                    (
                                                        mergeStrategyDeleteField
                                                        | mergeStrategyDeleteValues
                                                    )*
                                                BRACE_CLOSE
;
mergeStrategyDeleteField:                   ACTION_INDICATOR_DELETE_INDICATOR_FIELD COLON identifier SEMI_COLON
;
mergeStrategyDeleteValues:                  ACTION_INDICATOR_DELETE_INDICATOR_VALUES COLON
                                                BRACKET_OPEN
                                                    STRING (COMMA STRING)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;

//TODO: ledav -- remove once v2 is rolled out | END
