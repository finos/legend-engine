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
                                        | SERVICE_TEST | SERVICE_TEST_TESTS | SERVICE_DATA | SERVICE_ASSERTS | SERVICE_TAGS | SERVICE_TAGNAME | SERVICE_TAGVALUE
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                             imports
                                            (service)*
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
                                                    | serviceTags
                                                    | serviceDocumentation
                                                    | serviceAutoActivateUpdates
                                                    | serviceExec
                                                    | serviceTest
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

// -------------------------------------- TAGS --------------------------------------
serviceTags:                            SERVICE_TAGS COLON
                                            BRACKET_OPEN
                                                (tagDescription (COMMA tagDescription)*)?
                                            BRACKET_CLOSE
                                        SEMI_COLON
;
tagDescription:                         BRACE_OPEN
                                            (
                                                serviceTagName
                                                serviceTagValue
                                            )
                                        BRACE_CLOSE
;
serviceTagName:                         SERVICE_TAGNAME COLON STRING SEMI_COLON
;
serviceTagValue:                        SERVICE_TAGVALUE COLON STRING SEMI_COLON
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
testData:                               SERVICE_DATA COLON STRING SEMI_COLON
;
testAsserts:                            SERVICE_ASSERTS COLON
                                            BRACKET_OPEN
                                                (testAssert (COMMA testAssert)*)?
                                            BRACKET_CLOSE
                                        SEMI_COLON
;
testAssert:                             BRACE_OPEN
                                            testParameters COMMA combinedExpression
                                        BRACE_CLOSE
;
testParameters:                         BRACKET_OPEN (instanceRightSide (COMMA instanceRightSide)*)? BRACKET_CLOSE
;
