parser grammar PersistenceRelationalParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = PersistenceRelationalLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING
                                            | NONE | DATE_TIME
                                            | TARGET_RELATIONAL | TARGET_RELATIONAL_TABLE | TARGET_RELATIONAL_DATABASE | TARGET_RELATIONAL_TEMPORAL
                                            | TEMPORAL_UNI | TEMPORAL_BI | TEMPORAL_PROCESSING_DIMENSION | TEMPORAL_SOURCE_DERIVED_DIMENSION
                                            | PROCESSING_BATCH_ID | PROCESSING_BATCH_ID_AND_DATE_TIME
                                            | BATCH_ID_IN | BATCH_ID_OUT | DATE_TIME_IN | DATE_TIME_OUT
                                            | DATE_TIME_START | DATE_TIME_END | SOURCE_DERIVED_SOURCE_FIELDS
                                            | SOURCE_FIELDS_START | SOURCE_FIELDS_START_AND_END | SOURCE_FIELD_START | SOURCE_FIELD_END
                                            | AUDIT | AUDIT_DATE_TIME_NAME | UPDATES_HANDLING | UPDATES_HANDLING_APPEND_ONLY | UPDATES_HANDLING_OVERWRITE
                                            | APPEND_STRATEGY | APPEND_STRATEGY_ALLOW_DUPLICATES | APPEND_STRATEGY_FAIL_ON_DUPLICATES | APPEND_STRATEGY_FILTER_DUPLICATES
;

// -------------------------------------- RELATIONAL PERSISTENCE TARGET --------------------------------------

definition:                                 (
                                                table
                                                | database
                                                | temporality
                                            )*
                                            EOF
;
table:                                      TARGET_RELATIONAL_TABLE COLON identifier SEMI_COLON
;
database:                                   TARGET_RELATIONAL_DATABASE COLON qualifiedName SEMI_COLON
;
temporality:                                TARGET_RELATIONAL_TEMPORAL COLON
                                                (
                                                    nontemporal
                                                    | unitemporal
                                                    | bitemporal
                                                )
;
nontemporal:                                NONE
                                                BRACE_OPEN
                                                    (
                                                        auditing
                                                        | updatesHandling
                                                    )*
                                                BRACE_CLOSE
;
unitemporal:                                TEMPORAL_UNI
                                                BRACE_OPEN
                                                    (processingDimension)*
                                                BRACE_CLOSE
;
bitemporal:                                 TEMPORAL_BI
                                                BRACE_OPEN
                                                    (
                                                        processingDimension
                                                        | sourceDerivedDimension
                                                    )*
                                                BRACE_CLOSE
;
auditing:                                   AUDIT COLON
                                                (
                                                    auditingDateTime
                                                    | auditingNone
                                                )
;
auditingDateTime:                           DATE_TIME
                                                BRACE_OPEN
                                                    (auditingDateTimeName)*
                                                BRACE_CLOSE
;
auditingDateTimeName:                       AUDIT_DATE_TIME_NAME COLON identifier SEMI_COLON
;
auditingNone:                               NONE
                                                (
                                                    SEMI_COLON
                                                    | (BRACE_OPEN BRACE_CLOSE)
                                                )
;
updatesHandling:                            UPDATES_HANDLING COLON
                                                (
                                                    updatesHandlingAppendOnly
                                                    | updatesHandlingOverwrite
                                                )
;
updatesHandlingAppendOnly:                  UPDATES_HANDLING_APPEND_ONLY
                                                BRACE_OPEN
                                                    (appendStrategy)*
                                                BRACE_CLOSE
;
updatesHandlingOverwrite:                   UPDATES_HANDLING_OVERWRITE
                                                (
                                                    SEMI_COLON
                                                    | (BRACE_OPEN BRACE_CLOSE)
                                                )
;
appendStrategy:                             APPEND_STRATEGY COLON
                                                (
                                                    appendStrategyAllowDuplicates
                                                    | appendStrategyFailOnDuplicates
                                                    | appendStrategyFilterDuplicates
                                                )
;
appendStrategyAllowDuplicates:              APPEND_STRATEGY_ALLOW_DUPLICATES
                                                (
                                                    SEMI_COLON
                                                    | (BRACE_OPEN BRACE_CLOSE)
                                                )
;
appendStrategyFailOnDuplicates:             APPEND_STRATEGY_FAIL_ON_DUPLICATES
                                                (
                                                    SEMI_COLON
                                                    | (BRACE_OPEN BRACE_CLOSE)
                                                )
;
appendStrategyFilterDuplicates:             APPEND_STRATEGY_FILTER_DUPLICATES
                                                (
                                                    SEMI_COLON
                                                    | (BRACE_OPEN BRACE_CLOSE)
                                                )
;
processingDimension:                        TEMPORAL_PROCESSING_DIMENSION COLON
                                                (
                                                    processingBatchId
                                                    | processingDateTime
                                                    | processingBatchIdAndDateTime
                                                )
;
processingBatchId:                          PROCESSING_BATCH_ID
                                                BRACE_OPEN
                                                    (
                                                        batchIdIn
                                                        | batchIdOut
                                                    )*
                                                BRACE_CLOSE
;
processingDateTime:                         DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeIn
                                                        | dateTimeOut
                                                    )*
                                                BRACE_CLOSE
;
processingBatchIdAndDateTime:               PROCESSING_BATCH_ID_AND_DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        batchIdIn
                                                        | batchIdOut
                                                        | dateTimeIn
                                                        | dateTimeOut
                                                    )*
                                                BRACE_CLOSE
;
batchIdIn:                                  BATCH_ID_IN COLON identifier SEMI_COLON
;
batchIdOut:                                 BATCH_ID_OUT COLON identifier SEMI_COLON
;
dateTimeIn:                                 DATE_TIME_IN COLON identifier SEMI_COLON
;
dateTimeOut:                                DATE_TIME_OUT COLON identifier SEMI_COLON
;
sourceDerivedDimension:                     TEMPORAL_SOURCE_DERIVED_DIMENSION COLON
                                                (
                                                    sourceDerivedDateTime
                                                )
;
sourceDerivedDateTime:                      DATE_TIME
                                                BRACE_OPEN
                                                    (
                                                        dateTimeStart
                                                        | dateTimeEnd
                                                        | sourceFields
                                                    )*
                                                BRACE_CLOSE
;
dateTimeStart:                              DATE_TIME_START COLON identifier SEMI_COLON
;
dateTimeEnd:                                DATE_TIME_END COLON identifier SEMI_COLON
;
sourceFields:                               SOURCE_DERIVED_SOURCE_FIELDS COLON
                                                (
                                                    sourceFieldsStart
                                                    | sourceFieldsStartAndEnd
                                                )
;
sourceFieldsStart:                          SOURCE_FIELDS_START
                                                BRACE_OPEN
                                                    (
                                                        sourceFieldStart
                                                    )*
                                                BRACE_CLOSE
;
sourceFieldsStartAndEnd:                    SOURCE_FIELDS_START_AND_END
                                                BRACE_OPEN
                                                    (
                                                        sourceFieldStart
                                                        | sourceFieldEnd
                                                    )*
                                                BRACE_CLOSE
;
sourceFieldStart:                           SOURCE_FIELD_START COLON identifier SEMI_COLON
;
sourceFieldEnd:                             SOURCE_FIELD_END COLON identifier SEMI_COLON
;