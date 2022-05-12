parser grammar MappingParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = MappingLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                     VALID_STRING | STRING
                                | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE      // from M3Parser
                                | MAPPING | IMPORT
                                | INCLUDE | TESTS | EXTENDS
                                | TEST_QUERY | TEST_INPUT_DATA | TEST_ASSERT
                                | MAPPING_TEST_SUITES  | MAPPING_TEST_DATA | MAPPING_TEST_ELEMENT_DATA
                                | MAPPING_TEST_TESTS | MAPPING_TEST_ASSERTS
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
                                        (tests | mappingTestSuites)?
                                    PAREN_CLOSE
;
includeMapping:                 INCLUDE qualifiedName
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
//--------------------------------------- TEST --------------------------------------------

mappingTestSuites:                      MAPPING_TEST_SUITES COLON BRACKET_OPEN (mappingTestSuite ( COMMA mappingTestSuite )*)? BRACKET_CLOSE
;
mappingTestSuite:                       identifier COLON BRACE_OPEN mappingTestSuiteData? | (mappingTestSuiteTests)* BRACE_CLOSE
;
mappingTestSuiteData:                   MAPPING_TEST_DATA BRACKET_OPEN (mappingTestElementsData (COMMA mappingTestElementsData)*)? BRACKET_CLOSE
;
mappingTestElementsData:                BRACE_OPEN MAPPING_TEST_ELEMENT_DATA COLON identifier SEMI_COLON TEST_INPUT_DATA COLON embeddedData SEMI_COLON BRACE_CLOSE
;
embeddedData:                           identifier ISLAND_OPEN (embeddedDataContent)*
;
embeddedDataContent:                    ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
mappingTestSuiteTests:                  MAPPING_TEST_TESTS COLON BRACKET_OPEN ( mappingTestBlock ( COMMA mappingTestBlock )* )? BRACKET_CLOSE
;
mappingTestBlock:                       identifier COLON BRACE_OPEN TEST_QUERY COLON combinedExpression SEMI_COLON (mappingTestAsserts)* SEMI_COLON BRACE_CLOSE
;
mappingTestAsserts:                     MAPPING_TEST_ASSERTS COLON BRACKET_OPEN ( mappingTestAssert ( COMMA mappingTestAssert )* )? BRACKET_CLOSE
;
mappingTestAssert:                      identifier COLON testAssertion
;
testAssertion:                          identifier ISLAND_OPEN (testAssertionContent)*
;
testAssertionContent:                   ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
// -------------------------------------- LEGACY TEST -------------------------------------

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
testQuery:                      TEST_QUERY COLON combinedExpression SEMI_COLON
;
testInputData:                  TEST_INPUT_DATA COLON testInput SEMI_COLON
;
// NOTE: it's important to have `STRING` before `combinedExpression` since the latter also matches the former
testAssert:                     TEST_ASSERT COLON (STRING | combinedExpression) SEMI_COLON
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
