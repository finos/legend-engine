parser grammar FunctionJarParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = FunctionJarLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                             VALID_STRING | STRING
                                        | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE | TO_BYTES_FUNCTION      // from M3Parser
                                        | STEREOTYPES | TAGS
                                        | SERVICE | IMPORT
                                        | SERVICE_OWNERSHIP | SERVICE_DOCUMENTATION | SERVICE_MAPPING
                                        | SERVICE_FUNCTION| SERVICE_BINDING| SERVICE_CONTENT_TYPE| SERVICE_ACTIVATION | SERVICE_LINEAGE | SERVICE_MODEL
                                        |SERVICE_RUNTIME
                                        | SERVICE_TEST_SUITES | SERVICE_TEST_DATA | SERVICE_TEST_CONNECTION_DATA | SERVICE_TEST_TESTS | SERVICE_TEST_ASSERTS | SERVICE_TEST_PARAMETERS
                                        | SERVICE_TEST_SERIALIZATION_FORMAT  | PARAM_GROUP | ASSERT_FOR_KEYS | SERVICE_POST_VALIDATION | SERVICE_POST_VALIDATION_DESCRIPTION
                                        | SERVICE_POST_VALIDATION_PARAMETERS | SERVICE_POST_VALIDATION_ASSERTIONS
                                        | EXEC_ENV| SERVICE_EXECUTION_EXECUTIONS
                                        | SERVICE_OWNERSHIP_DEPLOYMENT | SERVICE_OWNERSHIP_DEPLOYMENT_IDENTIFIER
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                             imports
                                            (service| execEnvs)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
service:                                SERVICE stereotypes? taggedValues? qualifiedName
                                            BRACE_OPEN
                                                (
                                                    serviceOwnership
                                                    | serviceDocumentation
                                                    | serviceFunc
                                                    | serviceTestSuites
                                                    | serviceBindingOrContent
                                                    | servicePostValidations
                                                    | serviceActivationConfiguration
                                                    | serviceLineage
                                                    | serviceModel
                                                )*
                                            BRACE_CLOSE
;

actionBody:                             ISLAND_OPEN actionValue
;
actionValue:                            (ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_BRACE_CLOSE | ISLAND_END)*
;
actionType:                             VALID_STRING
;
stereotypes:                            LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN
;
stereotype:                             qualifiedName DOT identifier
;
taggedValues:                           BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE
;
taggedValue:                            qualifiedName DOT identifier EQUAL STRING
;

serviceActivationConfiguration:         SERVICE_ACTIVATION COLON qualifiedName SEMI_COLON
;

serviceOwnership:                          SERVICE_OWNERSHIP COLON deployment SEMI_COLON
;

deployment:                             SERVICE_OWNERSHIP_DEPLOYMENT
                                            BRACE_OPEN
                                                SERVICE_OWNERSHIP_DEPLOYMENT_IDENTIFIER COLON STRING
                                            BRACE_CLOSE
;


serviceBindingOrContent:                (serviceBinding|serviceContentType) SEMI_COLON
;

serviceBinding:                         SERVICE_BINDING COLON qualifiedName SEMI_COLON
;

serviceContentType:                     SERVICE_CONTENT_TYPE COLON STRING
;

serviceDocumentation:                   SERVICE_DOCUMENTATION COLON STRING SEMI_COLON
;

serviceLineage:                                SERVICE_LINEAGE COLON BOOLEAN SEMI_COLON
;

serviceModel:                                   SERVICE_MODEL COLON BOOLEAN SEMI_COLON
;
// -------------------------------------- EXECUTION --------------------------------------

serviceFunc:                            SERVICE_FUNCTION COLON functionIdentifier SEMI_COLON
;


// -------------------------------------- TEST --------------------------------------

serviceTestSuites:                      SERVICE_TEST_SUITES COLON BRACKET_OPEN (serviceTestSuite ( COMMA serviceTestSuite )*)? BRACKET_CLOSE
;
serviceTestSuite:                       identifier COLON BRACE_OPEN ( serviceTestSuiteData | serviceTestSuiteTests )* BRACE_CLOSE
;
serviceTestSuiteData:                   SERVICE_TEST_DATA COLON BRACKET_OPEN (serviceTestConnectionsData)* BRACKET_CLOSE
;
serviceTestConnectionsData:             SERVICE_TEST_CONNECTION_DATA COLON BRACKET_OPEN (serviceTestConnectionData ( COMMA serviceTestConnectionData )*)? BRACKET_CLOSE
;
serviceTestConnectionData:              identifier COLON embeddedData
;
embeddedData:                           identifier ISLAND_OPEN (embeddedDataContent)*
;
embeddedDataContent:                    ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
serviceTestSuiteTests:                  SERVICE_TEST_TESTS COLON BRACKET_OPEN ( serviceTestBlock ( COMMA serviceTestBlock )* )? BRACKET_CLOSE
;
serviceTestBlock:                       identifier COLON BRACE_OPEN ( serviceTestParameters | serviceTestSerialization | keys | serviceTestAsserts )* BRACE_CLOSE
;
serviceTestParameters:                  SERVICE_TEST_PARAMETERS COLON BRACKET_OPEN ( serviceTestParameter ( COMMA serviceTestParameter )* )? BRACKET_CLOSE
;
serviceTestSerialization:               SERVICE_TEST_SERIALIZATION_FORMAT COLON identifier SEMI_COLON
;
serviceTestParameter:                   identifier EQUAL primitiveValue
;
serviceTestAsserts:                     SERVICE_TEST_ASSERTS COLON BRACKET_OPEN ( serviceTestAssert ( COMMA serviceTestAssert )* )? BRACKET_CLOSE
;
serviceTestAssert:                      identifier COLON testAssertion
;
keys:                                   ASSERT_FOR_KEYS COLON
                                                 BRACKET_OPEN
                                                            (STRING (COMMA STRING)*)
                                                 BRACKET_CLOSE
                                        SEMI_COLON
;
testAssertion:                          identifier ISLAND_OPEN (testAssertionContent)*
;
testAssertionContent:                   ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;

// -------------------------------------- VALIDATION ----------------------------------
servicePostValidations:                 SERVICE_POST_VALIDATION COLON BRACKET_OPEN ( postValidation ( COMMA postValidation) * )? BRACKET_CLOSE
;
postValidation:                         BRACE_OPEN
                                             (
                                                 postValidationDescription
                                                 | postValidationParameters
                                                 | postValidationAssertions
                                             )*
                                        BRACE_CLOSE
;
postValidationDescription:              SERVICE_POST_VALIDATION_DESCRIPTION COLON STRING SEMI_COLON
;
postValidationParameters:               SERVICE_POST_VALIDATION_PARAMETERS COLON BRACKET_OPEN ( combinedExpression ( COMMA combinedExpression)* )? BRACKET_CLOSE SEMI_COLON
;
postValidationAssertions:               SERVICE_POST_VALIDATION_ASSERTIONS COLON BRACKET_OPEN ( postValidationAssertion ( COMMA postValidationAssertion)* )? BRACKET_CLOSE SEMI_COLON
;
postValidationAssertion:                identifier COLON combinedExpression
;

// ----------------------------------- EXECUTION_ENVIRONMENT ------------------------------------------------------
execEnvs:                               EXEC_ENV qualifiedName
                                            BRACE_OPEN
                                                executions
                                            BRACE_CLOSE
;
executions:                             SERVICE_EXECUTION_EXECUTIONS COLON BRACKET_OPEN execParams (COMMA execParams)* BRACKET_CLOSE SEMI_COLON
;
execParams:                             singleExecEnv | multiExecEnv
;
singleExecEnv:                          identifier COLON
                                            BRACE_OPEN
                                                serviceMapping
                                                serviceRuntime
                                            BRACE_CLOSE
;
multiExecEnv:                           identifier COLON BRACKET_OPEN singleExecEnv (COMMA singleExecEnv)* BRACKET_CLOSE
;

serviceMapping:                         SERVICE_MAPPING COLON qualifiedName SEMI_COLON
;

serviceRuntime:                         SERVICE_RUNTIME COLON (runtimePointer | embeddedRuntime)
;

runtimePointer:                         qualifiedName SEMI_COLON
;
embeddedRuntime:                        ISLAND_OPEN (embeddedRuntimeContent)* SEMI_COLON
;
embeddedRuntimeContent:                 ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
