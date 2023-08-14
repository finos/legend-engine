parser grammar MappingParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = MappingLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                     VALID_STRING | STRING
                                | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE
                                | TO_BYTES_FUNCTION      // from M3Parser
                                | MAPPING | IMPORT
                                | INCLUDE | TESTS | EXTENDS
                                | MAPPING_TESTABLE_FUNCTION | MAPPING_TESTABLE_DATA | MAPPING_TESTABLE_ASSERT
                                | MAPPING_TESTABLE_SUITES | MAPPING_TEST_ASSERTS | MAPPING_TESTS
                                | MAPPING_TESTABLE_DOC | MAPPING_TESTABLE_TYPE | MAPPING_TESTS_QUERY
;

// -------------------------------------- DEFINITION -------------------------------------

definition:                     imports
                                    (mapping)*
                                EOF
;
imports:                        importStatement*
;
importStatement:                IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
mapping:                        MAPPING qualifiedName
                                    PAREN_OPEN
                                        (includeMapping)*
                                        (mappingElement)*
                                        (tests)?
                                        (mappingTestableDefinition)?
                                    PAREN_CLOSE
;
includeMapping:                 (INCLUDETYPE|INCLUDE) qualifiedName
                                    (BRACKET_OPEN (storeSubPath (COMMA storeSubPath)*)? BRACKET_CLOSE)?
;


// -------------------------------------- STORE -------------------------------------

storeSubPath:                   sourceStore ARROW targetStore
;
sourceStore:                    qualifiedName
;
targetStore:                    qualifiedName
;


// -------------------------------------- MAPPING ELEMENT -------------------------------------

mappingElement:                 (STAR)? qualifiedName
                                    (BRACKET_OPEN mappingElementId BRACKET_CLOSE)?
                                    (EXTENDS BRACKET_OPEN superClassMappingId BRACKET_CLOSE)?
                                COLON parserName (mappingElementName)? mappingElementBody
;
mappingElementBody:             BRACE_OPEN (mappingElementBodyContent)*
;
mappingElementBodyContent:      MAPPING_ISLAND_BRACE_OPEN | MAPPING_ISLAND_CONTENT | MAPPING_ISLAND_BRACE_CLOSE
;
mappingElementName:             word
;
parserName:                     identifier
;
superClassMappingId:            mappingElementId
;
mappingElementId:               word
;

// ------------------------------------- TESTABLE ---------------------------------------------
mappingTestableDefinition:     MAPPING_TESTABLE_SUITES COLON BRACKET_OPEN (mappingTestSuite (COMMA mappingTestSuite)*)? BRACKET_CLOSE
;
mappingTestSuite:               identifier COLON
                                  BRACE_OPEN
                                  (
                                      mappingTestableDoc
                                    | mappingTestableFunc
                                    | mappingTests
                                  )*
                                  BRACE_CLOSE
;
mappingTestableDoc:             MAPPING_TESTABLE_DOC COLON STRING SEMI_COLON
;
mappingTestableData:            MAPPING_TESTABLE_DATA COLON BRACKET_OPEN (mappingTestDataContent (COMMA mappingTestDataContent)*)? BRACKET_CLOSE SEMI_COLON
;
mappingTestDataContent:         qualifiedName COLON embeddedData
;
embeddedData:                   identifier ISLAND_OPEN (embeddedDataContent)*
;
embeddedDataContent:            ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
mappingTests:                   MAPPING_TESTS COLON
                                  BRACKET_OPEN
                                    (mappingTestContent (COMMA mappingTestContent)*)?
                                  BRACKET_CLOSE
                                SEMI_COLON
;
mappingTestContent:             identifier COLON
                                  BRACE_OPEN
                                    (
                                      mappingTestableDoc
                                      | mappingTestAsserts
                                      | mappingTestableData
                                    )*
                                  BRACE_CLOSE
;
mappingTestableFunc:           MAPPING_TESTABLE_FUNCTION COLON combinedExpression SEMI_COLON
;
mappingTestAsserts:             MAPPING_TEST_ASSERTS COLON BRACKET_OPEN (mappingTestAssert (COMMA mappingTestAssert)*)? BRACKET_CLOSE SEMI_COLON
;
mappingTestAssert:              identifier COLON testAssertion
;
testAssertion:                  identifier ISLAND_OPEN (testAssertionContent)*
;
testAssertionContent:           ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;

// -------------------------------------- LEGACY_TEST -------------------------------------

tests:                          TESTS
                                    BRACKET_OPEN
                                        (test (COMMA test)*)?
                                    BRACKET_CLOSE
;
test:                           testName
                                    PAREN_OPEN
                                        (
                                            testQuery
                                            | testInputData
                                            | testAssert
                                        )*
                                    PAREN_CLOSE
;
testQuery:                     MAPPING_TESTS_QUERY COLON combinedExpression SEMI_COLON
;
testInputData:                  MAPPING_TESTABLE_DATA COLON testInput SEMI_COLON
;
// NOTE: it's important to have `STRING` before `combinedExpression` since the latter also matches the former
testAssert:                     MAPPING_TESTABLE_ASSERT COLON (STRING | combinedExpression) SEMI_COLON
;
testName:                       identifier
;
testInput:                      BRACKET_OPEN
                                    (testInputElement (COMMA testInputElement)*)?
                                BRACKET_CLOSE
;
testInputElement:               LESS_THAN testInputType (COMMA testInputFormat)? COMMA testInputSrc COMMA testInputDataContent GREATER_THAN
;
testInputType:                  identifier
;
testInputFormat:                identifier
;
testInputSrc:                   qualifiedName
;
testInputDataContent:           STRING ('+' STRING)*
;
