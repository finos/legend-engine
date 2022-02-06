parser grammar PersistenceParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = PersistenceLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING
                                            | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE      // from M3Parser
                                            | TRUE | FALSE | IMPORT
                                            | SERVICE_PERSISTENCE | SERVICE_PERSISTENCE_DOCUMENTATION | SERVICE_PERSISTENCE_OWNERS | SERVICE_PERSISTENCE_TRIGGER | SERVICE_PERSISTENCE_SERVICE
                                            | EVENT_TYPE_SCHEDULE_TRIGGERED | EVENT_TYPE_REGISTRY_DATASET_AVAILABLE
                                            | PERSISTENCE | PERSISTENCE_STREAMING | PERSISTENCE_BATCH
                                            | INPUT_SHAPE | INPUT_SHAPE_FLAT | INPUT_SHAPE_GROUPED_FLAT | INPUT_SHAPE_NESTED | INPUT_CLASS
                                            | TRANSACTION_MODE | TRANSACTION_MODE_SINGLE_DATASET | TRANSACTION_MODE_ALL_DATASETS
                                            | TARGET | DATASTORE | DATASTORE_NAME | DATASET | DATASETS | DATASET_NAME
                                            | PARTITION_PROPERTIES
                                            | DEDUPLICATION_STRATEGY | DEDUPLICATION_STRATEGY_NONE | DEDUPLICATION_STRATEGY_ANY | DEDUPLICATION_STRATEGY_COUNT | DEDUPLICATION_STRATEGY_COUNT_PROPERTY | DEDUPLICATION_STRATEGY_MAX_VERSION | DEDUPLICATION_STRATEGY_VERSION_PROPERTY
                                            | BATCH_MODE | SNAPSHOT_NON_MILESTONED | SNAPSHOT_UNITEMPORAL | SNAPSHOT_BITEMPORAL | DELTA_NON_MILESTONED | DELTA_UNITEMPORAL | DELTA_BITEMPORAL | APPEND_ONLY
                                            | FILTER_DUPLICATES | AUDIT_SCHEME | AUDIT_SCHEME_NONE | AUDIT_SCHEME_BATCH_DATE_TIME | AUDIT_SCHEME_BATCH_DATE_TIME_PROPERTY | AUDIT_SCHEME_OPAQUE
                                            | TRANSACTION_SCHEME | TRANSACTION_SCHEME_BATCH_ID | TRANSACTION_SCHEME_DATE_TIME | TRANSACTION_SCHEME_BOTH | TRANSACTION_SCHEME_OPAQUE
                                            | BATCH_ID_IN_PROPERTY | BATCH_ID_OUT_PROPERTY | TRANSACTION_DATE_TIME_IN_PROPERTY | TRANSACTION_DATE_TIME_OUT_PROPERTY
                                            | VALIDITY_SCHEME | VALIDITY_SCHEME_DATE_TIME | VALIDITY_SCHEME_OPAQUE
                                            | VALIDITY_DATE_TIME_FROM_PROPERTY | VALIDITY_DATE_TIME_THRU_PROPERTY
                                            | VALIDITY_DERIVATION | VALIDITY_DERIVATION_SOURCE_FROM | VALIDITY_DERIVATION_SOURCE_FROM_THRU
                                            | MERGE_SCHEME | MERGE_SCHEME_NO_DELETES | MERGE_SCHEME_DELETE_INDICATOR | MERGE_SCHEME_DELETE_INDICATOR_PROPERTY | MERGE_SCHEME_DELETE_INDICATOR_VALUES
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 imports
                                                (servicePersistence)*
                                            EOF
;
imports:                                    (importStatement)*
;
importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
servicePersistence:                         SERVICE_PERSISTENCE qualifiedName
                                                BRACE_OPEN
                                                    (
                                                        documentation
                                                        | owners
                                                        | trigger
                                                        | service
                                                        | persistence
                                                    )*
                                                BRACE_CLOSE
;
documentation:                              SERVICE_PERSISTENCE_DOCUMENTATION COLON STRING SEMI_COLON
;
owners:                                     SERVICE_PERSISTENCE_OWNERS COLON
                                                BRACKET_OPEN
                                                    (STRING (COMMA STRING)*)?
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
//TODO: ledav - support config in event
trigger:                                    SERVICE_PERSISTENCE_TRIGGER COLON
                                                (
                                                    EVENT_TYPE_SCHEDULE_TRIGGERED
                                                    | EVENT_TYPE_REGISTRY_DATASET_AVAILABLE
                                                )
                                            SEMI_COLON
;
service:                                    SERVICE_PERSISTENCE_SERVICE COLON qualifiedName SEMI_COLON
;
persistence:                                PERSISTENCE COLON
                                                (
                                                    streamingPersistence
                                                    | batchPersistence
                                                )
;
streamingPersistence:                       PERSISTENCE_STREAMING
                                                BRACE_OPEN
                                                    (
                                                        inputShape
                                                        | inputClass
                                                    )*
                                                BRACE_CLOSE
;
batchPersistence:                           PERSISTENCE_BATCH
                                                BRACE_OPEN
                                                    (
                                                        inputShape
                                                        | inputClass
                                                        | transactionMode
                                                        | targetSpecification
                                                    )*
                                                BRACE_CLOSE
;
inputShape:                                 INPUT_SHAPE COLON
                                                (
                                                    INPUT_SHAPE_FLAT
                                                    | INPUT_SHAPE_GROUPED_FLAT
                                                    | INPUT_SHAPE_NESTED
                                                )
                                            SEMI_COLON
;
inputClass:                                 INPUT_CLASS COLON qualifiedName SEMI_COLON
;
transactionMode:                            TRANSACTION_MODE COLON
                                                (
                                                    TRANSACTION_MODE_SINGLE_DATASET
                                                    | TRANSACTION_MODE_ALL_DATASETS
                                                )
                                            SEMI_COLON
;
targetSpecification:                        TARGET COLON datastore
;
datastore:                                  DATASTORE
                                                BRACE_OPEN
                                                    (
                                                        datastoreName
                                                        | datasets
                                                    )*
                                                BRACE_CLOSE
;
datastoreName:                              DATASTORE_NAME COLON identifier SEMI_COLON
;
datasets:                                   DATASETS COLON
                                                BRACKET_OPEN
                                                    dataset (COMMA dataset)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
dataset:                                    BRACE_OPEN
                                                (
                                                    datasetName
                                                    | partitionProperties
                                                    | deduplicationStrategy
                                                    | batchMode
                                                )*
                                            BRACE_CLOSE

;
datasetName:                                DATASET_NAME COLON identifier SEMI_COLON
;
partitionProperties:                        PARTITION_PROPERTIES COLON
                                                BRACKET_OPEN
                                                    (qualifiedName ARROW identifier (COMMA qualifiedName ARROW identifier)*)?
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
//TODO: ledav - support config within deduplication strategy
deduplicationStrategy:                      DEDUPLICATION_STRATEGY COLON
                                                (
                                                    deduplicationStrategyNone
                                                    | deduplicationStrategyAny
                                                    | deduplicationStrategyCount
                                                    | deduplicationStrategyMaxVersion
                                                )
;
deduplicationStrategyNone:                  DEDUPLICATION_STRATEGY_NONE SEMI_COLON
;
deduplicationStrategyAny:                   DEDUPLICATION_STRATEGY_ANY SEMI_COLON
;
deduplicationStrategyCount:                 DEDUPLICATION_STRATEGY_COUNT
                                                BRACE_OPEN
                                                    deduplicationCountPropertyName
                                                BRACE_CLOSE
;
deduplicationCountPropertyName:             DEDUPLICATION_STRATEGY_COUNT_PROPERTY COLON identifier SEMI_COLON
;
deduplicationStrategyMaxVersion:            DEDUPLICATION_STRATEGY_MAX_VERSION
                                                BRACE_OPEN
                                                    deduplicationVersionPropertyName
                                                BRACE_CLOSE
;
deduplicationVersionPropertyName:           DEDUPLICATION_STRATEGY_VERSION_PROPERTY COLON identifier SEMI_COLON
;
batchMode:                                  BATCH_MODE COLON
                                                (
                                                    snapshotNonMilestoned
                                                    | snapshotUnitemporal
                                                    | snapshotBitemporal
                                                    | deltaNonMilestoned
                                                    | deltaUnitemporal
                                                    | deltaBitemporal
                                                    | appendOnly
                                                )
;
snapshotNonMilestoned:                      SNAPSHOT_NON_MILESTONED
                                                BRACE_OPEN
                                                    auditing
                                                BRACE_CLOSE
;
snapshotUnitemporal:                        SNAPSHOT_UNITEMPORAL
                                                BRACE_OPEN
                                                    transactionScheme
                                                BRACE_CLOSE
;
snapshotBitemporal:                        SNAPSHOT_BITEMPORAL
                                                BRACE_OPEN
                                                    (
                                                        transactionScheme
                                                        | validityScheme
                                                        | validityDerivation
                                                    )*
                                                BRACE_CLOSE
;
deltaNonMilestoned:                         DELTA_NON_MILESTONED
                                                BRACE_OPEN
                                                    auditing
                                                BRACE_CLOSE
;
deltaUnitemporal:                           DELTA_UNITEMPORAL
                                                BRACE_OPEN
                                                    (
                                                        mergeStrategy
                                                        | transactionScheme
                                                    )*
                                                BRACE_CLOSE
;
deltaBitemporal:                           DELTA_BITEMPORAL
                                                BRACE_OPEN
                                                    (
                                                        mergeStrategy
                                                        | transactionScheme
                                                        | validityScheme
                                                        | validityDerivation
                                                    )*
                                                BRACE_CLOSE
;
appendOnly:                                 APPEND_ONLY
                                                BRACE_OPEN
                                                    (
                                                        auditing
                                                        | filterDuplicates
                                                    )*
                                                BRACE_CLOSE
;
//TODO: ledav - support config for all flags below
auditing:                                AUDIT_SCHEME COLON
                                                (
                                                    auditSchemeNone
                                                    | auditSchemeBatchDateTime
                                                    | auditSchemeOpaque
                                                )
;
auditSchemeNone:                            AUDIT_SCHEME_NONE SEMI_COLON
;
auditSchemeBatchDateTime:                   AUDIT_SCHEME_BATCH_DATE_TIME
                                                BRACE_OPEN
                                                    transactionDateTimePropertyName
                                                BRACE_CLOSE
;
transactionDateTimePropertyName:            AUDIT_SCHEME_BATCH_DATE_TIME_PROPERTY COLON identifier SEMI_COLON
;
auditSchemeOpaque:                          AUDIT_SCHEME_OPAQUE SEMI_COLON
;
filterDuplicates:                           FILTER_DUPLICATES COLON (TRUE | FALSE) SEMI_COLON
;
transactionScheme:                          TRANSACTION_SCHEME COLON
                                                (
                                                    transactionSchemeBatchId
                                                    | transactionSchemeDateTime
                                                    | transactionSchemeBoth
                                                    | transactionSchemeOpaque
                                                )
;
transactionSchemeBatchId:                   TRANSACTION_SCHEME_BATCH_ID
                                                BRACE_OPEN
                                                    (
                                                        batchIdInProperty
                                                        | batchIdOutProperty
                                                    )*
                                                BRACE_CLOSE
;
batchIdInProperty:                          BATCH_ID_IN_PROPERTY COLON identifier SEMI_COLON
;
batchIdOutProperty:                         BATCH_ID_OUT_PROPERTY COLON identifier SEMI_COLON
;
transactionSchemeDateTime:                  TRANSACTION_SCHEME_DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        transactionDateTimeInProperty
                                                        | transactionDateTimeOutProperty
                                                    )*
                                                BRACE_CLOSE
;
transactionDateTimeInProperty:              TRANSACTION_DATE_TIME_IN_PROPERTY COLON identifier SEMI_COLON
;
transactionDateTimeOutProperty:             TRANSACTION_DATE_TIME_OUT_PROPERTY COLON identifier SEMI_COLON
;
transactionSchemeBoth:                      TRANSACTION_SCHEME_BOTH
                                                BRACE_OPEN
                                                    (
                                                        batchIdInProperty
                                                        | batchIdOutProperty
                                                        | transactionDateTimeInProperty
                                                        | transactionDateTimeOutProperty
                                                    )*
                                                BRACE_CLOSE
;
transactionSchemeOpaque:                    TRANSACTION_SCHEME_OPAQUE SEMI_COLON
;
validityScheme:                             VALIDITY_SCHEME COLON
                                                (
                                                    validitySchemeDateTime
                                                    | validitySchemeOpaque
                                                )
                                            SEMI_COLON
;
validitySchemeDateTime:                     VALIDITY_SCHEME_DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        validityDateTimeFromProperty
                                                        | validityDateTimeThruProperty
                                                    )*
                                                BRACE_CLOSE
;
validityDateTimeFromProperty:               VALIDITY_DATE_TIME_FROM_PROPERTY COLON identifier SEMI_COLON
;
validityDateTimeThruProperty:               VALIDITY_DATE_TIME_THRU_PROPERTY COLON identifier SEMI_COLON
;
validitySchemeOpaque:                       VALIDITY_SCHEME_OPAQUE SEMI_COLON
;
validityDerivation:                         VALIDITY_DERIVATION COLON
                                                (
                                                    validityDerivationSourceProvidesFrom
                                                    | validityDerivationSourceProvidesFromThru
                                                    | validityDerivationOpaque
                                                )
                                            SEMI_COLON
;
validityDerivationSourceProvidesFrom:       VALIDITY_DERIVATION_SOURCE_FROM
                                                BRACE_OPEN
                                                    validityDerivationFromProperty
                                                BRACE_CLOSE
;
validityDerivationSourceProvidesFromThru:   VALIDITY_DERIVATION_SOURCE_FROM_THRU
                                                BRACE_OPEN
                                                    (
                                                        validityDerivationFromProperty
                                                        | validityDerivationThruProperty
                                                    )*
                                                BRACE_CLOSE
;
validityDerivationFromProperty:
;
validityDerivationThruProperty:
;
validityDerivationOpaque:                   VALIDITY_DERIVATION_OPAQUE SEMI_COLON
;
mergeStrategy:                                MERGE_SCHEME COLON
                                                (
                                                    mergeSchemeNoDeletes
                                                    | mergeSchemeDeleteIndicator
                                                )
;
mergeSchemeNoDeletes:                       MERGE_SCHEME_NO_DELETES SEMI_COLON
;
mergeSchemeDeleteIndicator:                 MERGE_SCHEME_DELETE_INDICATOR
                                                BRACE_OPEN
                                                    (
                                                        mergeSchemeDeleteIndicatorProperty
                                                        | mergeSchemeDeleteIndicatorValues
                                                    )*
                                                BRACE_CLOSE
;
mergeSchemeDeleteIndicatorProperty:         MERGE_SCHEME_DELETE_INDICATOR_PROPERTY COLON identifier SEMI_COLON
;
mergeSchemeDeleteIndicatorValues:           MERGE_SCHEME_DELETE_INDICATOR_VALUES COLON
                                                BRACKET_OPEN
                                                    STRING (COMMA STRING)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;