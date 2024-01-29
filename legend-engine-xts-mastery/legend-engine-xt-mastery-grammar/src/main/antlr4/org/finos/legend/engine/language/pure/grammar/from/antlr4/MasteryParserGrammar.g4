parser grammar MasteryParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = MasteryLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

validString:                                VALID_STRING | .
;

identifier:                                 validString | STRING
                                            | TRUE | FALSE
                                            | MASTER_RECORD_DEFINITION | RECORD_SOURCES | RUNTIME
;

masteryIdentifier:                          (validString | '-' | INTEGER) (validString | '-' | INTEGER)*?;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 //imports
                                                (elementDefinition)*
                                            EOF
;
imports:                                    (importStatement)*
;
importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
elementDefinition:                          (
                                             masterRecordDefinition
                                                | dataProviderDef
                                                | connection
                                                | masteryRuntime
                                            )
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
postCurationEnrichmentService:              POST_CURATION_ENRICHMENT_SERVICE COLON qualifiedName SEMI_COLON
;
exceptionWorkflowTransformService:          EXCEPTION_WORKFLOW_TRANSFORM_SERVICE COLON qualifiedName SEMI_COLON
;
elasticSearchTransformService:              ELASTIC_SEARCH_TRANSFORM_SERVICE COLON qualifiedName SEMI_COLON
;
publishToElasticSearch:                     PUBLISH_TO_ELASTIC_SEARCH COLON boolean_value SEMI_COLON
;
collectionEqualities:                       COLLECTION_EQUALITIES COLON
                                            BRACKET_OPEN
                                            (
                                            collectionEquality (COMMA collectionEquality)*
                                            )
                                            BRACKET_CLOSE
;
collectionEquality:                         BRACE_OPEN
                                            (
                                                modelClass
                                                | equalityFunction
                                            )*
                                            BRACE_CLOSE
;
equalityFunction:                           EQUALITY_FUNCTION COLON qualifiedName SEMI_COLON
;

// -------------------------------------- MASTER_RECORD_DEFINITION --------------------------------------

masterRecordDefinition:                       MASTER_RECORD_DEFINITION qualifiedName
                                                BRACE_OPEN
                                                (
                                                    modelClass
                                                    | identityResolution
                                                    | recordSources
                                                    | precedenceRules
                                                    | postCurationEnrichmentService
                                                    | exceptionWorkflowTransformService
                                                    | elasticSearchTransformService
                                                    | publishToElasticSearch
                                                    | collectionEqualities
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
                                                | dataProvider
                                                | trigger
                                                | recordService
                                                | allowFieldDelete
                                                | authorization
                                                | sourcePartitions
                                                | dependencies
                                                | timeoutInMinutes
                                                | runProfile
                                                | raiseExceptionWorkflow
                                            )*
                                            BRACE_CLOSE
;
recordStatus:                               RECORD_SOURCE_STATUS COLON
                                            (
                                                RECORD_SOURCE_STATUS_DEVELOPMENT
                                                    | RECORD_SOURCE_STATUS_TEST_ONLY
                                                    | RECORD_SOURCE_STATUS_PRODUCTION
                                                    | RECORD_SOURCE_STATUS_DORMANT
                                                    | RECORD_SOURCE_STATUS_DECOMMISSIONED
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
sourcePartition:                            masteryIdentifier COLON BRACE_OPEN
                                            BRACE_CLOSE
;
allowFieldDelete:                           RECORD_SOURCE_ALLOW_FIELD_DELETE COLON boolean_value SEMI_COLON
;
dataProvider:                               RECORD_SOURCE_DATA_PROVIDER COLON qualifiedName SEMI_COLON
;
timeoutInMinutes:                           RECORD_SOURCE_TIMEOUT_IN_MINUTES COLON INTEGER SEMI_COLON
;
raiseExceptionWorkflow:                     RECORD_SOURCE_RAISE_EXCEPTION_WORKFLOW COLON boolean_value SEMI_COLON
;
runProfile:                                 RECORD_SOURCE_RUN_PROFILE COLON
                                            (
                                                RECORD_SOURCE_RUN_PROFILE_LARGE
                                                    | RECORD_SOURCE_RUN_PROFILE_MEDIUM
                                                    | RECORD_SOURCE_RUN_PROFILE_SMALL
                                                    | RECORD_SOURCE_RUN_PROFILE_XTRA_SMALL
                                            )
                                            SEMI_COLON
;

// -------------------------------------- RECORD SERVICE --------------------------------------

recordService:                             RECORD_SOURCE_SERVICE COLON
                                           BRACE_OPEN
                                           (
                                                parseService
                                                | transformService
                                                | acquisitionProtocol
                                           )*
                                           BRACE_CLOSE SEMI_COLON
;
parseService:                              PARSE_SERVICE COLON qualifiedName SEMI_COLON
;
transformService:                          TRANSFORM_SERVICE COLON qualifiedName SEMI_COLON
;

// -------------------------------------- ACQUISITION PROTOCOL --------------------------------------

acquisitionProtocol:                        ACQUISITION_PROTOCOL COLON (islandSpecification | qualifiedName) SEMI_COLON
;

// -------------------------------------- TRIGGER --------------------------------------

trigger:                                    RECORD_SOURCE_TRIGGER COLON islandSpecification SEMI_COLON
;

// -------------------------------------- DATA PROVIDER --------------------------------------

dataProviderDef:                           identifier qualifiedName SEMI_COLON
;

// -------------------------------------- AUTHORIZATION --------------------------------------

authorization:                              RECORD_SOURCE_AUTHORIZATION COLON islandSpecification SEMI_COLON
;

// -------------------------------------- DEPENDENCIES --------------------------------------

dependencies:                               RECORD_SOURCE_DEPENDENCIES COLON
                                            BRACKET_OPEN
                                            (
                                            recordSourceDependency (COMMA recordSourceDependency)*
                                            )
                                            BRACKET_CLOSE
                                            SEMI_COLON
;

recordSourceDependency:                     RECORD_SOURCE_DEPENDENCY
                                            BRACE_OPEN
                                            masteryIdentifier
                                            BRACE_CLOSE
;

// -------------------------------------- CONNECTION --------------------------------------
connection:                                 MASTERY_CONNECTION qualifiedName
                                                BRACE_OPEN
                                                (
                                                    specification
                                                )*
                                                BRACE_CLOSE
;
specification:                              SPECIFICATION COLON islandSpecification SEMI_COLON
;

// -------------------------------------- MASTERY RUNTIME --------------------------------------
masteryRuntime:                              MASTERY_RUNTIME qualifiedName
                                                BRACE_OPEN
                                                (
                                                    runtime
                                                )*
                                                BRACE_CLOSE
;
runtime:                                    RUNTIME COLON islandSpecification SEMI_COLON
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
                                             | resolutionQueryOptional
                                             | resolutionQueryPrecedence
                                             | resolutionQueryFilter
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
resolutionQueryOptional:                 RESOLUTION_QUERY_OPTIONAL COLON boolean_value SEMI_COLON
;
resolutionQueryPrecedence:               PRECEDENCE COLON INTEGER SEMI_COLON
;
resolutionQueryFilter:                   RESOLUTION_QUERY_FILTER COLON lambdaFunction SEMI_COLON
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
subPath:                                '.' validString
;
filter:                                 BRACE_OPEN combinedExpression BRACE_CLOSE
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
validScopeType:                        recordSourceScope|dataProviderTypeScope|dataProviderIdScope
;
recordSourceScope:                      RECORD_SOURCE_SCOPE
                                        BRACE_OPEN
                                        masteryIdentifier
;
dataProviderTypeScope:                  DATA_PROVIDER_TYPE_SCOPE
                                        BRACE_OPEN
                                        validString
;
dataProviderIdScope:                    DATA_PROVIDER_ID_SCOPE
                                        BRACE_OPEN
                                        qualifiedName
;

// -------------------------------------- ISLAND SPECIFICATION --------------------------------------
islandSpecification:                        islandType (islandValue)?
;
islandType:                                 identifier
;
islandValue:                                ISLAND_OPEN (islandValueContent)* ISLAND_END
;
islandValueContent:                         ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_BRACE_CLOSE | ISLAND_START | ISLAND_END
;