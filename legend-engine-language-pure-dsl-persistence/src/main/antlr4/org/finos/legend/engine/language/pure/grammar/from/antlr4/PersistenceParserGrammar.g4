parser grammar PersistenceParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = PersistenceLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING
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
targetSpecification:                        TARGET COLON datastore SEMI_COLON
;
datastore:                                  DATASTORE
                                                BRACE_OPEN
                                                    (
                                                        datastoreName
                                                        | datasets
                                                    )
                                                BRACE_CLOSE
;
datastoreName:                              DATASTORE_NAME COLON identifier SEMI_COLON
;
datasets:                                   DATASETS COLON
                                                BRACKET_OPEN
                                                    (dataset (COMMA dataset)*)
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
dataset:                                    DATASET
                                                BRACE_OPEN
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
                                                    (identifier (COMMA identifier)*)
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
//TODO: ledav - support config within deduplication strategy
deduplicationStrategy:                      DEDUPLICATION_STRATEGY COLON
                                                (
                                                    DEDUPLICATION_STRATEGY_NONE
                                                    | DEDUPLICATION_STRATEGY_ANY
                                                    | DEDUPLICATION_STRATEGY_COUNT
                                                    | DEDUPLICATION_STRATEGY_MAX_VERSION
                                                )
                                            SEMI_COLON
;
batchMode:                                  BATCH_MODE COLON
                                                BRACE_OPEN
                                                    (
                                                        snapshotNonMilestoned
                                                        | snapshotUnitemporal
                                                        | snapshotBitemporal
                                                        | deltaNonMilestoned
                                                        | deltaUnitemporal
                                                        | deltaBitemporal
                                                        | appendOnly
                                                    )
                                                BRACE_CLOSE
                                            SEMI_COLON
;
snapshotNonMilestoned:                      SNAPSHOT_NON_MILESTONED
                                                BRACE_OPEN
                                                    auditScheme
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
                                                    auditScheme
                                                BRACE_CLOSE
;
deltaUnitemporal:                           DELTA_UNITEMPORAL
                                                BRACE_OPEN
                                                    (
                                                        mergeScheme
                                                        | transactionScheme
                                                    )*
                                                BRACE_CLOSE
;
deltaBitemporal:                           DELTA_BITEMPORAL
                                                BRACE_OPEN
                                                    (
                                                        mergeScheme
                                                        | transactionScheme
                                                        | validityScheme
                                                        | validityDerivation
                                                    )*
                                                BRACE_CLOSE
;
appendOnly:                                 APPEND_ONLY
                                                BRACE_OPEN
                                                    auditScheme
                                                BRACE_CLOSE
;
//TODO: ledav - support config for all flags below
auditScheme:                                AUDIT_SCHEME COLON
                                                (
                                                    AUDIT_SCHEME_NONE
                                                    | AUDIT_SCHEME_BATCH_DATE_TIME
                                                    | AUDIT_SCHEME_OPAQUE
                                                )
                                            SEMI_COLON
;
transactionScheme:                          TRANSACTION_SCHEME COLON
                                                (
                                                    TRANSACTION_SCHEME_BATCH_ID
                                                    | TRANSACTION_SCHEME_DATE_TIME
                                                    | TRANSACTION_SCHEME_BOTH
                                                    | TRANSACTION_SCHEME_OPAQUE
                                                )
                                            SEMI_COLON
;
validityScheme:                             VALIDITY_SCHEME COLON
                                                (
                                                    VALIDITY_SCHEME_DATE_TIME
                                                    | VALIDITY_SCHEME_OPAQUE
                                                )
                                            SEMI_COLON
;
validityDerivation:                         VALIDITY_DERIVATION COLON
                                                (
                                                    VALIDITY_DERIVATION_SOURCE_FROM
                                                    | VALIDITY_DERIVATION_SOURCE_FROM_THRU
                                                )
                                            SEMI_COLON
;
mergeScheme:                                MERGE_SCHEME COLON
                                                (
                                                    MERGE_SCHEME_NO_DELETES
                                                    | MERGE_SCHEME_DELETE_INDICATOR
                                                )
                                            SEMI_COLON
;
