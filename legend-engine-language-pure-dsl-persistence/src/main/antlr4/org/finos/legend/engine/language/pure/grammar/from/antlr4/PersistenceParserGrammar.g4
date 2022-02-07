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
                                            | PERSISTENCE_PIPE | PERSISTENCE_PIPE_DOC | PERSISTENCE_PIPE_OWNERS | PERSISTENCE_PIPE_TRIGGER | PERSISTENCE_PIPE_INPUT | PERSISTENCE_PIPE_PERSISTENCE
                                            | EVENT_SCHEDULE_TRIGGERED | EVENT_REGISTRY_DATASET_AVAILABLE
                                            | INPUT_SERVICE | INPUT_SERVICE_SERVICE
                                            | PERSISTENCE_STREAMING | PERSISTENCE_BATCH | PERSISTENCE_BATCH_TARGET
                                            | TARGET_SPEC_NAME | TARGET_SPEC_MODEL_CLASS
                                            | TARGET_SPEC_GROUPED | TARGET_SPEC_GROUPED_TXN_SCOPE | TARGET_SPEC_GROUPED_COMPONENTS | TARGET_COMPONENT_PROPERTY | TARGET_COMPONENT_TARGET_SPEC
                                            | TARGET_SPEC_FLAT | TARGET_SPEC_FLAT_PARTITION_PROPERTIES | TARGET_SPEC_FLAT_DEDUPLICATION | TARGET_SPEC_FLAT_BATCH_MODE
                                            | TARGET_SPEC_NESTED
                                            | TXN_SCOPE_SINGLE | TXN_SCOPE_ALL
                                            | DEDUPLICATION_NONE | DEDUPLICATION_ANY_VERSION | DEDUPLICATION_MAX_VERSION | DEDUPLICATION_MAX_VERSION_PROPERTY
                                            | BATCH_MODE_NON_MILESTONED_SNAPSHOT | BATCH_MODE_UNITEMPORAL_SNAPSHOT | BATCH_MODE_BITEMPORAL_SNAPSHOT | BATCH_MODE_NON_MILESTONED_DELTA | BATCH_MODE_UNITEMPORAL_DELTA | BATCH_MODE_BITEMPORAL_DELTA | BATCH_MODE_APPEND_ONLY
                                            | FILTER_DUPLICATES
                                            | AUDITING | AUDITING_NONE | AUDITING_BATCH_DATE_TIME | AUDITING_BATCH_DATE_TIME_PROPERTY | AUDITING_OPAQUE
                                            | TXN_MILESTONING | TXN_MILESTONING_BATCH_ID | TXN_MILESTONING_DATE_TIME | TXN_MILESTONING_BOTH | TXN_MILESTONING_OPAQUE | BATCH_ID_IN_PROPERTY | BATCH_ID_OUT_PROPERTY | DATE_TIME_IN_PROPERTY | DATE_TIME_OUT_PROPERTY
                                            | VALIDITY_MILESTONING | VALIDITY_MILESTONING_DATE_TIME | VALIDITY_MILESTONING_OPAQUE | DATE_TIME_FROM_PROPERTY | DATE_TIME_THRU_PROPERTY
                                            | VALIDITY_DERIVATION | VALIDITY_DERIVATION_SOURCE_FROM | VALIDITY_DERIVATION_SOURCE_FROM_THRU | VALIDITY_DERIVATION_OPAQUE
                                            | MERGE_STRATEGY | MERGE_STRATEGY_NO_DELETES | MERGE_STRATEGY_DELETE_INDICATOR | MERGE_STRATEGY_DELETE_INDICATOR_PROPERTY | MERGE_STRATEGY_DELETE_INDICATOR_VALUES
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 imports
                                                (persistencePipe)*
                                            EOF
;
imports:                                    (importStatement)*
;
importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
persistencePipe:                            PERSISTENCE_PIPE qualifiedName
                                                BRACE_OPEN
                                                    (
                                                        documentation
                                                        | owners
                                                        | trigger
                                                        | inputSource
                                                        | persistence
                                                    )*
                                                BRACE_CLOSE
;
documentation:                              PERSISTENCE_PIPE_DOC COLON STRING SEMI_COLON
;
owners:                                     PERSISTENCE_PIPE_OWNERS COLON
                                                BRACKET_OPEN
                                                    (STRING (COMMA STRING)*)?
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
//TODO: ledav - support config in event
trigger:                                    PERSISTENCE_PIPE_TRIGGER COLON
                                                (
                                                    EVENT_SCHEDULE_TRIGGERED
                                                    | EVENT_REGISTRY_DATASET_AVAILABLE
                                                )
                                            SEMI_COLON
;
inputSource:                                PERSISTENCE_PIPE_INPUT COLON serviceInputSource
;
serviceInputSource:                         INPUT_SERVICE
                                                BRACE_OPEN
                                                    service
                                                BRACE_CLOSE
;
service:                                    INPUT_SERVICE_SERVICE COLON qualifiedName SEMI_COLON
;
persistence:                                PERSISTENCE_PIPE_PERSISTENCE COLON
                                                (
                                                    streamingPersistence
                                                    | batchPersistence
                                                )
;
streamingPersistence:                       PERSISTENCE_STREAMING
                                                BRACE_OPEN
                                                BRACE_CLOSE
;
batchPersistence:                           PERSISTENCE_BATCH
                                                BRACE_OPEN
                                                    targetSpecification
                                                BRACE_CLOSE
;
targetSpecification:                        PERSISTENCE_BATCH_TARGET COLON
                                                (
                                                    groupedTargetSpecification
                                                    | flatTargetSpecification
                                                    | nestedTargetSpecification
                                                )
;
groupedTargetSpecification:                 TARGET_SPEC_GROUPED
                                                BRACE_OPEN
                                                    (
                                                        targetName
                                                        | targetModelClass
                                                        | targetTransactionScope
                                                        | targetComponents
                                                    )*
                                                BRACE_CLOSE
;
flatTargetSpecification:                    TARGET_SPEC_FLAT
                                                BRACE_OPEN
                                                    flatTargetSpecificationProperties
                                                BRACE_CLOSE
;
flatTargetSpecificationProperties:          (
                                                targetName
                                                | targetModelClass
                                                | partitionProperties
                                                | deduplicationStrategy
                                                | batchMode
                                            )*
;
nestedTargetSpecification:                  TARGET_SPEC_NESTED
                                                BRACE_OPEN
                                                    (
                                                        targetName
                                                        | targetModelClass
                                                    )*
                                                BRACE_CLOSE
;
targetName:                                 TARGET_SPEC_NAME COLON STRING SEMI_COLON
;
targetModelClass:                           TARGET_SPEC_MODEL_CLASS COLON qualifiedName SEMI_COLON
;
targetTransactionScope:                     TARGET_SPEC_GROUPED_TXN_SCOPE COLON
                                                (
                                                    TXN_SCOPE_SINGLE
                                                    | TXN_SCOPE_ALL
                                                )
                                            SEMI_COLON
;
targetComponents:                           TARGET_SPEC_GROUPED_COMPONENTS COLON
                                                BRACKET_OPEN
                                                    targetComponent (COMMA targetComponent)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
targetComponent:                            BRACE_OPEN
                                                (
                                                    targetComponentProperty
                                                    | targetComponentTargetSpecification
                                                )*
                                            BRACE_CLOSE
;
targetComponentProperty:                    TARGET_COMPONENT_PROPERTY COLON (qualifiedName ARROW identifier) SEMI_COLON
;
targetComponentTargetSpecification:         TARGET_COMPONENT_TARGET_SPEC COLON
                                                BRACE_OPEN
                                                    flatTargetSpecificationProperties
                                                BRACE_CLOSE
;
partitionProperties:                        TARGET_SPEC_FLAT_PARTITION_PROPERTIES COLON
                                                BRACKET_OPEN
                                                    (qualifiedName ARROW identifier (COMMA qualifiedName ARROW identifier)*)?
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
deduplicationStrategy:                      TARGET_SPEC_FLAT_DEDUPLICATION COLON
                                                (
                                                    noDeduplicationStrategy
                                                    | anyVersionDeduplicationStrategy
                                                    | maxVersionDeduplicationStrategy
                                                )
;
noDeduplicationStrategy:                    DEDUPLICATION_NONE SEMI_COLON
;
anyVersionDeduplicationStrategy:            DEDUPLICATION_ANY_VERSION SEMI_COLON
;
maxVersionDeduplicationStrategy:            DEDUPLICATION_MAX_VERSION
                                                BRACE_OPEN
                                                    deduplicationVersionPropertyName
                                                BRACE_CLOSE
;
deduplicationVersionPropertyName:           DEDUPLICATION_MAX_VERSION_PROPERTY COLON identifier SEMI_COLON
;
batchMode:                                  TARGET_SPEC_FLAT_BATCH_MODE COLON
                                                (
                                                    nonMilestonedSnapshot
                                                    | unitemporalSnapshot
                                                    | bitemporalSnapshot
                                                    | nonMilestonedDelta
                                                    | unitemporalDelta
                                                    | bitemporalDelta
                                                    | appendOnly
                                                )
;
nonMilestonedSnapshot:                      BATCH_MODE_NON_MILESTONED_SNAPSHOT
                                                BRACE_OPEN
                                                    auditing
                                                BRACE_CLOSE
;
unitemporalSnapshot:                        BATCH_MODE_UNITEMPORAL_SNAPSHOT
                                                BRACE_OPEN
                                                    transactionMilestoning
                                                BRACE_CLOSE
;
bitemporalSnapshot:                         BATCH_MODE_BITEMPORAL_SNAPSHOT
                                                BRACE_OPEN
                                                    (
                                                        transactionMilestoning
                                                        | validityMilestoning
                                                        | validityDerivation
                                                    )*
                                                BRACE_CLOSE
;
nonMilestonedDelta:                         BATCH_MODE_NON_MILESTONED_DELTA
                                                BRACE_OPEN
                                                    auditing
                                                BRACE_CLOSE
;
unitemporalDelta:                           BATCH_MODE_UNITEMPORAL_DELTA
                                                BRACE_OPEN
                                                    (
                                                        mergeStrategy
                                                        | transactionMilestoning
                                                    )*
                                                BRACE_CLOSE
;
bitemporalDelta:                            BATCH_MODE_BITEMPORAL_DELTA
                                                BRACE_OPEN
                                                    (
                                                        mergeStrategy
                                                        | transactionMilestoning
                                                        | validityMilestoning
                                                        | validityDerivation
                                                    )*
                                                BRACE_CLOSE
;
appendOnly:                                 BATCH_MODE_APPEND_ONLY
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
                                                    | batchDateTimeAuditing
                                                    | opaqueAuditing
                                                )
;
noAuditing:                                 AUDITING_NONE SEMI_COLON
;
batchDateTimeAuditing:                      AUDITING_BATCH_DATE_TIME
                                                BRACE_OPEN
                                                    batchDateTimePropertyName
                                                BRACE_CLOSE
;
batchDateTimePropertyName:                  AUDITING_BATCH_DATE_TIME_PROPERTY COLON identifier SEMI_COLON
;
opaqueAuditing:                             AUDITING_OPAQUE SEMI_COLON
;
filterDuplicates:                           FILTER_DUPLICATES COLON (TRUE | FALSE) SEMI_COLON
;
transactionMilestoning:                     TXN_MILESTONING COLON
                                                (
                                                    transactionSchemeBatchId
                                                    | dateTimeTransactionMilestoning
                                                    | bothTransactionMilestoning
                                                    | opaqueTransactionMilestoning
                                                )
;
transactionSchemeBatchId:                   TXN_MILESTONING_BATCH_ID
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
dateTimeTransactionMilestoning:             TXN_MILESTONING_DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeInProperty
                                                        | dateTimeOutProperty
                                                    )*
                                                BRACE_CLOSE
;
dateTimeInProperty:                         DATE_TIME_IN_PROPERTY COLON identifier SEMI_COLON
;
dateTimeOutProperty:                        DATE_TIME_OUT_PROPERTY COLON identifier SEMI_COLON
;
bothTransactionMilestoning:                 TXN_MILESTONING_BOTH
                                                BRACE_OPEN
                                                    (
                                                        batchIdInProperty
                                                        | batchIdOutProperty
                                                        | dateTimeInProperty
                                                        | dateTimeOutProperty
                                                    )*
                                                BRACE_CLOSE
;
opaqueTransactionMilestoning:               TXN_MILESTONING_OPAQUE SEMI_COLON
;
validityMilestoning:                        VALIDITY_MILESTONING COLON
                                                (
                                                    dateTimeValidityMilestoning
                                                    | opaqueValidityMilestoning
                                                )
                                            SEMI_COLON
;
dateTimeValidityMilestoning:                VALIDITY_MILESTONING_DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeFromProperty
                                                        | dateTimeThruProperty
                                                    )*
                                                BRACE_CLOSE
;
dateTimeFromProperty:                       DATE_TIME_FROM_PROPERTY COLON identifier SEMI_COLON
;
dateTimeThruProperty:                       DATE_TIME_THRU_PROPERTY COLON identifier SEMI_COLON
;
opaqueValidityMilestoning:                  VALIDITY_MILESTONING_OPAQUE SEMI_COLON
;
validityDerivation:                         VALIDITY_DERIVATION COLON
                                                (
                                                    sourceProvidesFromValidityDerivation
                                                    | sourceProvidesFromThruValidityDerivation
                                                    | opaqueValidityDerivation
                                                )
                                            SEMI_COLON
;
sourceProvidesFromValidityDerivation:       VALIDITY_DERIVATION_SOURCE_FROM
                                                BRACE_OPEN
//                                                    validityDerivationFromProperty
                                                BRACE_CLOSE
;
sourceProvidesFromThruValidityDerivation:   VALIDITY_DERIVATION_SOURCE_FROM_THRU
                                                BRACE_OPEN
//                                                    (
//                                                        validityDerivationFromProperty
//                                                        | validityDerivationThruProperty
//                                                    )*
                                                BRACE_CLOSE
;
//validityDerivationFromProperty:
//;
//validityDerivationThruProperty:
//;
opaqueValidityDerivation:                   VALIDITY_DERIVATION_OPAQUE SEMI_COLON
;
mergeStrategy:                              MERGE_STRATEGY COLON
                                                (
                                                    noDeletesMergeStrategy
                                                    | deleteIndicatorMergeStrategy
                                                )
;
noDeletesMergeStrategy:                     MERGE_STRATEGY_NO_DELETES SEMI_COLON
;
deleteIndicatorMergeStrategy:               MERGE_STRATEGY_DELETE_INDICATOR
                                                BRACE_OPEN
                                                    (
                                                        mergeStrategyDeleteIndicatorProperty
                                                        | mergeStrategyDeleteIndicatorValues
                                                    )*
                                                BRACE_CLOSE
;
mergeStrategyDeleteIndicatorProperty:       MERGE_STRATEGY_DELETE_INDICATOR_PROPERTY COLON identifier SEMI_COLON
;
mergeStrategyDeleteIndicatorValues:         MERGE_STRATEGY_DELETE_INDICATOR_VALUES COLON
                                                BRACKET_OPEN
                                                    STRING (COMMA STRING)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
