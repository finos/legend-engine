parser grammar MasteryParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = MasteryLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING
                                            | TRUE | FALSE
                                            | MASTER_RECORD_DEFINITION | MODEL_CLASS | RECORD_SOURCES | SOURCE_PARTITIONS
;

masteryIdentifier:                          (VALID_STRING | '-' | INTEGER) (VALID_STRING | '-' | INTEGER)*;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 //imports
                                            (mastery)*
                                            EOF
;
imports:                                    (importStatement)*
;
importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;

// -------------------------------------- COMMON --------------------------------------

boolean_value:                              TRUE | FALSE
;
modelClass:                                 MODEL_CLASS COLON qualifiedName SEMI_COLON
;
id:                                         ID COLON STRING SEMI_COLON
;
description:                                DESCRIPTION COLON STRING SEMI_COLON
;
tags:                                       TAGS COLON
                                            BRACKET_OPEN
                                            (
                                                STRING (COMMA STRING)*
                                            )*
                                            BRACKET_CLOSE
                                            SEMI_COLON
;

// -------------------------------------- MASTER_RECORD_DEFINITION --------------------------------------

mastery:                                    MASTER_RECORD_DEFINITION qualifiedName
                                                BRACE_OPEN
                                                (
                                                    modelClass
                                                    | identityResolution
                                                    | recordSources
                                                    | precedenceRules
                                                )*
                                                BRACE_CLOSE
;

// -------------------------------------- RECORD_SOURCES --------------------------------------

recordSources:                              RECORD_SOURCES COLON
                                            BRACKET_OPEN
                                            (
                                                recordSource
                                                (
                                                    COMMA
                                                    recordSource
                                                )*
                                            )
                                            BRACKET_CLOSE
;
recordSource:                               masteryIdentifier COLON BRACE_OPEN
                                            (
                                                recordStatus
                                                | description
                                                | parseService
                                                | transformService
                                                | sequentialData
                                                | stagedLoad
                                                | createPermitted
                                                | createBlockedException
                                                | tags
                                                | sourcePartitions
                                            )*
                                            BRACE_CLOSE
;
recordStatus:                               RECORD_SOURCE_STATUS COLON
                                            (
                                                RECORD_SOURCE_STATUS_DEVELOPMENT
                                                    | RECORD_SOURCE_STATUS_TEST_ONLY
                                                    | RECORD_SOURCE_STATUS_PRODUCTION
                                                    | RECORD_SOURCE_STATUS_DORMANT
                                                    | RECORD_SOURCE_STATUS_DECOMMINISSIONED
                                            )
                                            SEMI_COLON
;
sequentialData:                             RECORD_SOURCE_SEQUENTIAL COLON boolean_value SEMI_COLON
;
stagedLoad:                                 RECORD_SOURCE_STAGED COLON boolean_value SEMI_COLON
;
createPermitted:                            RECORD_SOURCE_CREATE_PERMITTED COLON boolean_value SEMI_COLON
;
createBlockedException:                     RECORD_SOURCE_CREATE_BLOCKED_EXCEPTION COLON boolean_value SEMI_COLON
;
parseService:                               PARSE_SERVICE COLON qualifiedName SEMI_COLON
;
transformService:                           TRANSFORM_SERVICE COLON qualifiedName SEMI_COLON
;
sourcePartitions:                           SOURCE_PARTITIONS COLON
                                            BRACKET_OPEN
                                            (
                                                sourcePartition
                                                (
                                                    COMMA
                                                    sourcePartition
                                                )*
                                            )
                                            BRACKET_CLOSE
;
sourcePartition:                             masteryIdentifier COLON BRACE_OPEN
                                            (
                                                 tags
                                            )*
                                            BRACE_CLOSE
;


// -------------------------------------- RESOLUTION --------------------------------------

identityResolution:                         IDENTITY_RESOLUTION COLON
                                            BRACE_OPEN
                                            (
                                                modelClass
                                                | resolutionQueries
                                            )*
                                            BRACE_CLOSE
;
resolutionQueries:                          RESOLUTION_QUERIES COLON
                                            BRACKET_OPEN
                                            (
                                                resolutionQuery
                                                (
                                                    COMMA
                                                    resolutionQuery
                                                )*
                                            )
                                            BRACKET_CLOSE
;
resolutionQuery:                            BRACE_OPEN
                                            (queryExpressions
                                             | resolutionQueryKeyType
                                             | resolutionQueryPrecedence
                                            )*
                                            BRACE_CLOSE
;

queryExpressions:                           RESOLUTION_QUERY_EXPRESSIONS COLON
                                                BRACKET_OPEN
                                                    (lambdaFunction (COMMA lambdaFunction)*)
                                                BRACKET_CLOSE
                                            SEMI_COLON
;

resolutionQueryKeyType:                  RESOLUTION_QUERY_KEY_TYPE COLON (
                                            RESOLUTION_QUERY_KEY_TYPE_GENERATED_PRIMARY_KEY
                                            | RESOLUTION_QUERY_KEY_TYPE_SUPPLIED_PRIMARY_KEY
                                            | RESOLUTION_QUERY_KEY_TYPE_ALTERNATE_KEY
                                            | RESOLUTION_QUERY_KEY_TYPE_OPTIONAL
                                            )
                                            SEMI_COLON
;
resolutionQueryPrecedence:               PRECEDENCE COLON INTEGER SEMI_COLON
;
// -------------------------------------- PRECEDENCE RULES--------------------------------------

precedenceRules:                         PRECEDENCE_RULES COLON
                                            BRACKET_OPEN
                                            (
                                                precedenceRule
                                                (
                                                    COMMA
                                                    precedenceRule
                                                )*
                                            )
                                            BRACKET_CLOSE
;

precedenceRule:                         (sourcePrecedenceRule
                                        | deleteRule
                                        | createRule
                                        | conditionalRule
                                        )
;
precedenceRuleBase:                     ruleScope
                                        | path
;
sourcePrecedenceRule:                   SOURCE_PRECEDENCE_RULE COLON
                                        BRACE_OPEN
                                            (ruleScope
                                            | path
                                            | action
                                            )*
                                        BRACE_CLOSE
;
deleteRule:                             DELETE_RULE COLON
                                        BRACE_OPEN
                                            (precedenceRuleBase
                                            )*
                                        BRACE_CLOSE
;
createRule:                             CREATE_RULE COLON
                                        BRACE_OPEN
                                            (precedenceRuleBase
                                            )*
                                        BRACE_CLOSE
;
conditionalRule:                   CONDITIONAL_RULE COLON
                                         BRACE_OPEN
                                             (precedenceRuleBase
                                             | predicate
                                             )*
                                         BRACE_CLOSE
 ;
path:                                   PATH COLON
                                            masterRecordFilter
                                                (
                                                pathExtension
                                                )*
                                        SEMI_COLON
;
masterRecordFilter:                     qualifiedName filter?
;
pathExtension:                           subPath filter?
;
subPath:                                '.' VALID_STRING
;
filter:                                 BRACE_OPEN '$' '.' combinedExpression BRACE_CLOSE
;
predicate:                              PREDICATE COLON
                                            lambdaFunction
                                        SEMI_COLON
;
action:                                 ACTION COLON
                                            validAction
                                        SEMI_COLON
;
validAction:                            OVERWRITE
                                        | BLOCK
;
precedence:                             PRECEDENCE COLON INTEGER
;
ruleScope:                              RULE_SCOPE COLON
                                        BRACKET_OPEN
                                        (
                                        scope (COMMA scope)*
                                        )
                                        BRACKET_CLOSE
                                        SEMI_COLON
;
scope:                                  validScopeType
                                            (COMMA precedence)?
                                        BRACE_CLOSE
;
validScopeType:                        recordSourceScope|dataProviderTypeScope
;
recordSourceScope:                      RECORD_SOURCE_SCOPE
                                        BRACE_OPEN
                                        masteryIdentifier
;
dataProviderTypeScope:                  DATA_PROVIDER_TYPE_SCOPE
                                        BRACE_OPEN
                                        validDataProviderType
;
validDataProviderType:                  AGGREGATOR
                                        | EXCHANGE
;
dataProviderIdScope:                    DATA_PROVIDER_ID_SCOPE
                                        BRACE_OPEN
                                        qualifiedName
;
