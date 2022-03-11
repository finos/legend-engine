parser grammar PersistenceParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = PersistenceLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING
                                            | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE      // from M3Parser
                                            | TRUE | FALSE | IMPORT | NONE | DATE_TIME
                                            | PERSISTENCE | PERSISTENCE_DOC | PERSISTENCE_TRIGGER | PERSISTENCE_READER | PERSISTENCE_PERSISTER
                                            | TRIGGER_MANUAL | TRIGGER_OPAQUE
                                            | READER_SERVICE | READER_SERVICE_SERVICE
                                            | PERSISTER_TARGET | PERSISTER_STREAMING | PERSISTER_BATCH
                                            | TARGET_SHAPE_NAME | TARGET_SHAPE_MODEL_CLASS
                                            | TARGET_SHAPE_MULTI | TARGET_SHAPE_MULTI_TXN_SCOPE | TARGET_SHAPE_MULTI_PARTS | TARGET_PART_PROPERTY | TARGET_PART_FLAT_TARGET | TXN_SCOPE_SINGLE | TXN_SCOPE_ALL
                                            | TARGET_SHAPE_FLAT | TARGET_SHAPE_FLAT_PARTITION_PROPERTIES | TARGET_SHAPE_FLAT_DEDUPLICATION | TARGET_SHAPE_FLAT_INGEST_MODE
                                            | TARGET_SHAPE_OPAQUE
                                            | DEDUPLICATION_ANY_VERSION | DEDUPLICATION_MAX_VERSION | DEDUPLICATION_MAX_VERSION_PROPERTY | DEDUPLICATION_OPAQUE
                                            | INGEST_MODE_NONTEMPORAL_SNAPSHOT | INGEST_MODE_UNITEMPORAL_SNAPSHOT | INGEST_MODE_BITEMPORAL_SNAPSHOT | INGEST_MODE_NONTEMPORAL_DELTA | INGEST_MODE_UNITEMPORAL_DELTA | INGEST_MODE_BITEMPORAL_DELTA | INGEST_MODE_APPEND_ONLY
                                            | FILTER_DUPLICATES
                                            | AUDITING | AUDITING_DATE_TIME_FIELD_NAME | AUDITING_OPAQUE
                                            | TXN_MILESTONING | TXN_MILESTONING_BATCH_ID | TXN_MILESTONING_BOTH | TXN_MILESTONING_OPAQUE | BATCH_ID_IN_FIELD_NAME | BATCH_ID_OUT_FIELD_NAME | DATE_TIME_IN_FIELD_NAME | DATE_TIME_OUT_FIELD_NAME
                                            | VALIDITY_MILESTONING | VALIDITY_MILESTONING_OPAQUE | DATE_TIME_FROM_FIELD_NAME | DATE_TIME_THRU_FIELD_NAME
                                            | VALIDITY_DERIVATION | VALIDITY_DERIVATION_SOURCE_FROM | VALIDITY_DERIVATION_SOURCE_FROM_THRU | VALIDITY_DERIVATION_OPAQUE | SOURCE_DATE_TIME_FROM_PROPERTY | SOURCE_DATE_TIME_THRU_PROPERTY
                                            | MERGE_STRATEGY | MERGE_STRATEGY_NO_DELETES | MERGE_STRATEGY_DELETE_INDICATOR | MERGE_STRATEGY_DELETE_INDICATOR_PROPERTY | MERGE_STRATEGY_DELETE_INDICATOR_VALUES | MERGE_STRATEGY_OPAQUE
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 imports
                                                (persistence)*
                                            EOF
;
imports:                                    (importStatement)*
;
importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
persistence:                                PERSISTENCE qualifiedName
                                                BRACE_OPEN
                                                    (
                                                        documentation
                                                        | trigger
                                                        | reader
                                                        | persister
                                                    )*
                                                BRACE_CLOSE
;
documentation:                              PERSISTENCE_DOC COLON STRING SEMI_COLON
;
trigger:                                    PERSISTENCE_TRIGGER COLON
                                                (
                                                    TRIGGER_MANUAL
                                                    | TRIGGER_OPAQUE
                                                )
                                            SEMI_COLON
;
reader:                                     PERSISTENCE_READER COLON serviceReader
;
serviceReader:                              READER_SERVICE
                                                BRACE_OPEN
                                                    (service)*
                                                BRACE_CLOSE
;
service:                                    READER_SERVICE_SERVICE COLON qualifiedName SEMI_COLON
;
persister:                                  PERSISTENCE_PERSISTER COLON
                                                (
                                                    streamingPersister
                                                    | batchPersister
                                                )
;
streamingPersister:                         PERSISTER_STREAMING
                                                BRACE_OPEN
                                                    (targetShape)*
                                                BRACE_CLOSE
;
batchPersister:                             PERSISTER_BATCH
                                                BRACE_OPEN
                                                    (targetShape)*
                                                BRACE_CLOSE
;
targetShape:                                PERSISTER_TARGET COLON
                                                (
                                                    multiTargetShape
                                                    | flatTargetShape
                                                    | opaqueTargetShape
                                                )
;
multiTargetShape:                           TARGET_SHAPE_MULTI
                                                BRACE_OPEN
                                                    (
                                                        targetModelClass
                                                        | targetTransactionScope
                                                        | targetChildren
                                                    )*
                                                BRACE_CLOSE
;
flatTargetShape:                          TARGET_SHAPE_FLAT
                                                BRACE_OPEN
                                                    (
                                                        targetName
                                                        | targetModelClass
                                                        | partitionProperties
                                                        | deduplicationStrategy
                                                        | ingestMode
                                                    )*
                                                BRACE_CLOSE
;
opaqueTargetShape:                          TARGET_SHAPE_OPAQUE
                                                BRACE_OPEN
                                                    (targetName)*
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
targetChildren:                             TARGET_SHAPE_MULTI_PARTS COLON
                                                BRACKET_OPEN
                                                    targetChild (COMMA targetChild)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
targetChild:                                BRACE_OPEN
                                                (
                                                    targetChildProperty
                                                    | targetChildTargetShape
                                                )*
                                            BRACE_CLOSE
;
targetChildProperty:                        TARGET_PART_PROPERTY COLON identifier SEMI_COLON
;
targetChildTargetShape:                     TARGET_PART_FLAT_TARGET COLON
                                                BRACE_OPEN
                                                    (
                                                        targetName
                                                        | partitionProperties
                                                        | deduplicationStrategy
                                                        | ingestMode
                                                    )*
                                                BRACE_CLOSE
;
targetName:                                 TARGET_SHAPE_NAME COLON STRING SEMI_COLON
;
partitionProperties:                        TARGET_SHAPE_FLAT_PARTITION_PROPERTIES COLON
                                                BRACKET_OPEN
                                                    (identifier (COMMA identifier)*)?
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
deduplicationStrategy:                      TARGET_SHAPE_FLAT_DEDUPLICATION COLON
                                                (
                                                    noDeduplicationStrategy
                                                    | anyVersionDeduplicationStrategy
                                                    | maxVersionDeduplicationStrategy
                                                    | opaqueDeduplicationStrategy
                                                )
;
noDeduplicationStrategy:                    NONE SEMI_COLON
;
anyVersionDeduplicationStrategy:            DEDUPLICATION_ANY_VERSION SEMI_COLON
;
maxVersionDeduplicationStrategy:            DEDUPLICATION_MAX_VERSION
                                                BRACE_OPEN
                                                    (
                                                        deduplicationVersionProperty
                                                    )*
                                                BRACE_CLOSE
;
deduplicationVersionProperty:               DEDUPLICATION_MAX_VERSION_PROPERTY COLON identifier SEMI_COLON
;
opaqueDeduplicationStrategy:                DEDUPLICATION_OPAQUE SEMI_COLON
;
ingestMode:                                 TARGET_SHAPE_FLAT_INGEST_MODE COLON
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
                                                    | opaqueAuditing
                                                )
;
noAuditing:                                 NONE SEMI_COLON
;
dateTimeAuditing:                           DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeFieldName
                                                    )*
                                                BRACE_CLOSE
;
dateTimeFieldName:                          AUDITING_DATE_TIME_FIELD_NAME COLON STRING SEMI_COLON
;
opaqueAuditing:                             AUDITING_OPAQUE SEMI_COLON
;
filterDuplicates:                           FILTER_DUPLICATES COLON (TRUE | FALSE) SEMI_COLON
;
transactionMilestoning:                     TXN_MILESTONING COLON
                                                (
                                                    batchIdTransactionMilestoning
                                                    | dateTimeTransactionMilestoning
                                                    | bothTransactionMilestoning
                                                    | opaqueTransactionMilestoning
                                                )
;
batchIdTransactionMilestoning:              TXN_MILESTONING_BATCH_ID
                                                BRACE_OPEN
                                                    (
                                                        batchIdInFieldName
                                                        | batchIdOutFieldName
                                                    )*
                                                BRACE_CLOSE
;
batchIdInFieldName:                         BATCH_ID_IN_FIELD_NAME COLON STRING SEMI_COLON
;
batchIdOutFieldName:                        BATCH_ID_OUT_FIELD_NAME COLON STRING SEMI_COLON
;
dateTimeTransactionMilestoning:             DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeInFieldName
                                                        | dateTimeOutFieldName
                                                    )*
                                                BRACE_CLOSE
;
dateTimeInFieldName:                        DATE_TIME_IN_FIELD_NAME COLON STRING SEMI_COLON
;
dateTimeOutFieldName:                       DATE_TIME_OUT_FIELD_NAME COLON STRING SEMI_COLON
;
bothTransactionMilestoning:                 TXN_MILESTONING_BOTH
                                                BRACE_OPEN
                                                    (
                                                        batchIdInFieldName
                                                        | batchIdOutFieldName
                                                        | dateTimeInFieldName
                                                        | dateTimeOutFieldName
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
;
dateTimeValidityMilestoning:                DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeFromFieldName
                                                        | dateTimeThruFieldName
                                                        | validityDerivation
                                                    )*
                                                BRACE_CLOSE
;
dateTimeFromFieldName:                      DATE_TIME_FROM_FIELD_NAME COLON STRING SEMI_COLON
;
dateTimeThruFieldName:                      DATE_TIME_THRU_FIELD_NAME COLON STRING SEMI_COLON
;
opaqueValidityMilestoning:                  VALIDITY_MILESTONING_OPAQUE SEMI_COLON
;
validityDerivation:                         VALIDITY_DERIVATION COLON
                                                (
                                                    sourceSpecifiesFromValidityDerivation
                                                    | sourceSpecifiesFromThruValidityDerivation
                                                    | opaqueValidityDerivation
                                                )
;
sourceSpecifiesFromValidityDerivation:      VALIDITY_DERIVATION_SOURCE_FROM
                                                BRACE_OPEN
                                                    (
                                                        validityDerivationFromProperty
                                                    )*
                                                BRACE_CLOSE
;
sourceSpecifiesFromThruValidityDerivation:  VALIDITY_DERIVATION_SOURCE_FROM_THRU
                                                BRACE_OPEN
                                                    (
                                                        validityDerivationFromProperty
                                                        | validityDerivationThruProperty
                                                    )*
                                                BRACE_CLOSE
;
validityDerivationFromProperty:             SOURCE_DATE_TIME_FROM_PROPERTY COLON identifier SEMI_COLON
;
validityDerivationThruProperty:             SOURCE_DATE_TIME_THRU_PROPERTY COLON identifier SEMI_COLON
;
opaqueValidityDerivation:                   VALIDITY_DERIVATION_OPAQUE SEMI_COLON
;
mergeStrategy:                              MERGE_STRATEGY COLON
                                                (
                                                    noDeletesMergeStrategy
                                                    | deleteIndicatorMergeStrategy
                                                    | opaqueMergeStrategy
                                                )
;
noDeletesMergeStrategy:                     MERGE_STRATEGY_NO_DELETES SEMI_COLON
;
deleteIndicatorMergeStrategy:               MERGE_STRATEGY_DELETE_INDICATOR
                                                BRACE_OPEN
                                                    (
                                                        mergeStrategyDeleteProperty
                                                        | mergeStrategyDeleteValues
                                                    )*
                                                BRACE_CLOSE
;
mergeStrategyDeleteProperty:                MERGE_STRATEGY_DELETE_INDICATOR_PROPERTY COLON identifier SEMI_COLON
;
mergeStrategyDeleteValues:                  MERGE_STRATEGY_DELETE_INDICATOR_VALUES COLON
                                                BRACKET_OPEN
                                                    STRING (COMMA STRING)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
opaqueMergeStrategy:                        MERGE_STRATEGY_OPAQUE SEMI_COLON
;
