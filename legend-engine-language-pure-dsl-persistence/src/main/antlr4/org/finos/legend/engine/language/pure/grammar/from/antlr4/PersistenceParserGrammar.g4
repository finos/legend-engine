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
                                            | PERSISTENCE | PERSISTENCE_DOC | PERSISTENCE_TRIGGER | PERSISTENCE_SERVICE | PERSISTENCE_PERSISTER | PERSISTENCE_NOTIFIER
                                            | TRIGGER_MANUAL | TRIGGER_CRON
                                            | PERSISTER_CONNECTION | PERSISTER_BINDING | PERSISTER_TARGET_SHAPE | PERSISTER_INGEST_MODE | PERSISTER_STREAMING | PERSISTER_BATCH
                                            | NOTIFIER | NOTIFIER_NOTIFYEES | NOTIFYEE_EMAIL | NOTIFYEE_EMAIL_ADDRESS | NOTIFYEE_PAGER_DUTY| NOTIFYEE_PAGER_DUTY_URL
                                            | TARGET_SHAPE_MODEL_CLASS | TARGET_SHAPE_NAME | TARGET_SHAPE_PARTITION_FIELDS | TARGET_SHAPE_DEDUPLICATION
                                            | TARGET_SHAPE_FLAT | TARGET_SHAPE_MULTI | TARGET_SHAPE_MULTI_TXN_SCOPE | TARGET_SHAPE_MULTI_PARTS | TARGET_PART_MODEL_PROPERTY | TXN_SCOPE_SINGLE | TXN_SCOPE_ALL
                                            | DEDUPLICATION_ANY_VERSION | DEDUPLICATION_MAX_VERSION | DEDUPLICATION_MAX_VERSION_FIELD | DEDUPLICATION_DUPLICATE_COUNT | DEDUPLICATION_DUPLICATE_COUNT_NAME
                                            | INGEST_MODE_NONTEMPORAL_SNAPSHOT | INGEST_MODE_UNITEMPORAL_SNAPSHOT | INGEST_MODE_BITEMPORAL_SNAPSHOT | INGEST_MODE_NONTEMPORAL_DELTA | INGEST_MODE_UNITEMPORAL_DELTA | INGEST_MODE_BITEMPORAL_DELTA | INGEST_MODE_APPEND_ONLY
                                            | FILTER_DUPLICATES
                                            | AUDITING | AUDITING_DATE_TIME_NAME
                                            | TXN_MILESTONING | TXN_MILESTONING_BATCH_ID | TXN_MILESTONING_BOTH | BATCH_ID_IN_NAME | BATCH_ID_OUT_NAME | DATE_TIME_IN_NAME | DATE_TIME_OUT_NAME
                                            | VALIDITY_MILESTONING | DATE_TIME_FROM_NAME | DATE_TIME_THRU_NAME
                                            | VALIDITY_DERIVATION | VALIDITY_DERIVATION_SOURCE_FROM | VALIDITY_DERIVATION_SOURCE_FROM_THRU | SOURCE_DATE_TIME_FROM_FIELD | SOURCE_DATE_TIME_THRU_FIELD
                                            | MERGE_STRATEGY | MERGE_STRATEGY_NO_DELETES | MERGE_STRATEGY_DELETE_INDICATOR | MERGE_STRATEGY_DELETE_INDICATOR_FIELD | MERGE_STRATEGY_DELETE_INDICATOR_VALUES
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
                                                        | service
                                                        | persister
                                                        | notifier
                                                    )*
                                                BRACE_CLOSE
;
documentation:                              PERSISTENCE_DOC COLON STRING SEMI_COLON
;
trigger:                                    PERSISTENCE_TRIGGER COLON
                                                (
                                                    TRIGGER_MANUAL
                                                    | TRIGGER_CRON
                                                )
                                            SEMI_COLON
;
service:                                    PERSISTENCE_SERVICE COLON qualifiedName SEMI_COLON
;
persister:                                  PERSISTENCE_PERSISTER COLON
                                                (
                                                    streamingPersister
                                                    | batchPersister
                                                )
;
streamingPersister:                         PERSISTER_STREAMING
                                                BRACE_OPEN
                                                    (
                                                        persisterConnection
                                                        | bindingPointer
                                                    )*
                                                BRACE_CLOSE
;
batchPersister:                             PERSISTER_BATCH
                                                BRACE_OPEN
                                                    (
                                                        persisterConnection
                                                        | bindingPointer
                                                        | targetShape
                                                        | ingestMode
                                                    )*
                                                BRACE_CLOSE
;
notifier:                                   PERSISTENCE_NOTIFIER COLON
                                                BRACE_OPEN
                                                    (notifyees)*
                                                BRACE_CLOSE
;
notifyees:                                  NOTIFIER_NOTIFYEES COLON
                                                BRACKET_OPEN
                                                    notifyee (COMMA notifyee)*
                                                BRACKET_CLOSE
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
persisterConnection:                        PERSISTER_CONNECTION COLON
                                                (
                                                    connectionPointer
                                                    | embeddedConnection
                                                )
;
connectionPointer:                          qualifiedName SEMI_COLON
;
embeddedConnection:                         ISLAND_OPEN (embeddedConnectionContent)*
;
embeddedConnectionContent:                  ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
bindingPointer:                             PERSISTER_BINDING COLON qualifiedName SEMI_COLON
;
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
partitionFields:                            TARGET_SHAPE_PARTITION_FIELDS COLON
                                                BRACKET_OPEN
                                                    (identifier (COMMA identifier)*)?
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
deduplicationStrategy:                      TARGET_SHAPE_DEDUPLICATION COLON
                                                (
                                                    noDeduplicationStrategy
                                                    | anyVersionDeduplicationStrategy
                                                    | maxVersionDeduplicationStrategy
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
deduplicationVersionProperty:               DEDUPLICATION_MAX_VERSION_FIELD COLON identifier SEMI_COLON
;
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
                                                        dateTimeFieldName
                                                    )*
                                                BRACE_CLOSE
;
dateTimeFieldName:                          AUDITING_DATE_TIME_NAME COLON STRING SEMI_COLON
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
                                                        batchIdInFieldName
                                                        | batchIdOutFieldName
                                                    )*
                                                BRACE_CLOSE
;
batchIdInFieldName:                         BATCH_ID_IN_NAME COLON STRING SEMI_COLON
;
batchIdOutFieldName:                        BATCH_ID_OUT_NAME COLON STRING SEMI_COLON
;
dateTimeTransactionMilestoning:             DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeInFieldName
                                                        | dateTimeOutFieldName
                                                    )*
                                                BRACE_CLOSE
;
dateTimeInFieldName:                        DATE_TIME_IN_NAME COLON STRING SEMI_COLON
;
dateTimeOutFieldName:                       DATE_TIME_OUT_NAME COLON STRING SEMI_COLON
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
validityMilestoning:                        VALIDITY_MILESTONING COLON
                                                (
                                                    dateTimeValidityMilestoning
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
dateTimeFromFieldName:                      DATE_TIME_FROM_NAME COLON STRING SEMI_COLON
;
dateTimeThruFieldName:                      DATE_TIME_THRU_NAME COLON STRING SEMI_COLON
;
validityDerivation:                         VALIDITY_DERIVATION COLON
                                                (
                                                    sourceSpecifiesFromValidityDerivation
                                                    | sourceSpecifiesFromThruValidityDerivation
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
validityDerivationFromProperty:             SOURCE_DATE_TIME_FROM_FIELD COLON identifier SEMI_COLON
;
validityDerivationThruProperty:             SOURCE_DATE_TIME_THRU_FIELD COLON identifier SEMI_COLON
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
                                                        mergeStrategyDeleteProperty
                                                        | mergeStrategyDeleteValues
                                                    )*
                                                BRACE_CLOSE
;
mergeStrategyDeleteProperty:                MERGE_STRATEGY_DELETE_INDICATOR_FIELD COLON identifier SEMI_COLON
;
mergeStrategyDeleteValues:                  MERGE_STRATEGY_DELETE_INDICATOR_VALUES COLON
                                                BRACKET_OPEN
                                                    STRING (COMMA STRING)*
                                                BRACKET_CLOSE
                                            SEMI_COLON
;
