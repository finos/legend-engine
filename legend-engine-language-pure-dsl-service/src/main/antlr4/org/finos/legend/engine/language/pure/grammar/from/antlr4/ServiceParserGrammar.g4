parser grammar ServiceParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = ServiceLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                             VALID_STRING | STRING
                                        | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE      // from M3Parser
                                        | STEREOTYPES | TAGS
                                        | SERVICE | IMPORT
                                        | SERVICE_SINGLE | SERVICE_MULTI
                                        | SERVICE_PATTERN | SERVICE_OWNERS | SERVICE_DOCUMENTATION | SERVICE_AUTO_ACTIVATE_UPDATES
                                        | SERVICE_EXECUTION | SERVICE_FUNCTION | SERVICE_EXECUTION_KEY | SERVICE_EXECUTION_EXECUTIONS | SERVICE_RUNTIME | SERVICE_MAPPING
                                        | SERVICE_TEST_SUITES | SERVICE_TEST_DATA | SERVICE_TEST_CONNECTION_DATA | SERVICE_TEST_TESTS | SERVICE_TEST_ASSERTS | SERVICE_TEST_PARAMETERS
                                        | SERVICE_TEST_SERIALIZATION_FORMAT | SERVICE_TEST | PARAM_GROUP | ASSERT_FOR_KEYS | SERVICE_POST_VALIDATION | SERVICE_POST_VALIDATION_DESCRIPTION
                                        | SERVICE_POST_VALIDATION_PARAMETERS | SERVICE_POST_VALIDATION_ASSERTIONS
                                        | EXEC_ENV
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                             imports
                                            (service | execEnvs)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
service:                                SERVICE stereotypes? taggedValues? qualifiedName
                                            BRACE_OPEN
                                                (
                                                    servicePattern
                                                    | serviceOwners
                                                    | serviceDocumentation
                                                    | serviceAutoActivateUpdates
                                                    | serviceExec
                                                    | serviceTest
                                                    | serviceTestSuites
                                                    | servicePostValidations
                                                )*
                                            BRACE_CLOSE
;
stereotypes:                            LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN
;
stereotype:                             qualifiedName DOT identifier
;
taggedValues:                           BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE
;
taggedValue:                            qualifiedName DOT identifier EQUAL STRING
;
servicePattern:                         SERVICE_PATTERN COLON STRING SEMI_COLON
;
serviceOwners:                          SERVICE_OWNERS COLON
                                            BRACKET_OPEN
                                                (STRING (COMMA STRING)*)?
                                            BRACKET_CLOSE
                                        SEMI_COLON
;
serviceDocumentation:                   SERVICE_DOCUMENTATION COLON STRING SEMI_COLON
;
serviceAutoActivateUpdates:             SERVICE_AUTO_ACTIVATE_UPDATES COLON BOOLEAN SEMI_COLON
;


// -------------------------------------- EXECUTION --------------------------------------

serviceFunc:                            SERVICE_FUNCTION COLON combinedExpression SEMI_COLON
;
serviceExec:                            SERVICE_EXECUTION COLON (singleExec|multiExec)
;
singleExec:                             SERVICE_SINGLE
                                            BRACE_OPEN
                                                (
                                                    serviceFunc
                                                    | serviceMapping
                                                    | serviceRuntime
                                                )*
                                            BRACE_CLOSE
;
multiExec:                              SERVICE_MULTI
                                            BRACE_OPEN
                                                (
                                                    serviceFunc
                                                    | execKey
                                                    | execParameter
                                                )*
                                            BRACE_CLOSE
;
execParameter:                          execParameterSignature COLON
                                            BRACE_OPEN
                                                (
                                                    serviceMapping
                                                    | serviceRuntime
                                                )*
                                            BRACE_CLOSE
;
execParameterSignature:                 SERVICE_EXECUTION_EXECUTIONS BRACKET_OPEN STRING BRACKET_CLOSE
;
execKey:                                SERVICE_EXECUTION_KEY COLON STRING SEMI_COLON
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
// -------------------------------------- LEGACY --------------------------------------

serviceTest:                            SERVICE_TEST COLON (singleTest|multiTest)
;
singleTest:                             SERVICE_SINGLE
                                            BRACE_OPEN
                                                (
                                                    testData
                                                    | testAsserts
                                                )*
                                            BRACE_CLOSE
;
multiTest:                              SERVICE_MULTI
                                            BRACE_OPEN
                                                (multiTestElement)*
                                            BRACE_CLOSE
;
multiTestElement:                       multiTestElementSignature COLON
                                            BRACE_OPEN
                                                (
                                                    testData
                                                    | testAsserts
                                                )*
                                            BRACE_CLOSE
;
multiTestElementSignature:              SERVICE_TEST_TESTS BRACKET_OPEN STRING BRACKET_CLOSE
;
testData:                               SERVICE_TEST_DATA COLON STRING SEMI_COLON
;
testAsserts:                            SERVICE_TEST_ASSERTS COLON
                                            BRACKET_OPEN
                                                (testAssert (COMMA testAssert)*)?
                                            BRACKET_CLOSE
                                        SEMI_COLON
;
testAssert:                             BRACE_OPEN
                                            testParameters COMMA combinedExpression
                                        BRACE_CLOSE
;
testParameters:                         BRACKET_OPEN (testParam (COMMA testParam)*)? BRACKET_CLOSE
;

testListValueParam:                          PARAM_GROUP PAREN_OPEN
                                                        BRACKET_OPEN
                                                            (primitiveValue (COMMA primitiveValue)*)?
                                                        BRACKET_CLOSE
                                                  PAREN_CLOSE
;

testSingleValueParam:                        primitiveValue
;

testParam:                              testListValueParam | testSingleValueParam
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