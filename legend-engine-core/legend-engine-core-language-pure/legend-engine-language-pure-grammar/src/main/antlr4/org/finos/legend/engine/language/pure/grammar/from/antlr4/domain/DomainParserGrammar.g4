parser grammar DomainParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = DomainLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                     VALID_STRING | STRING
                                                | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE
                                                | TO_BYTES_FUNCTION      // from M3Parser
                                                | IMPORT
                                                | CLASS | FUNCTION | PROFILE | ASSOCIATION | ENUM | MEASURE
                                                | EXTENDS
                                                | STEREOTYPES | TAGS
                                                | NATIVE | PROJECTS | AS
                                                | CONSTRAINT_ENFORCEMENT_LEVEL_ERROR | CONSTRAINT_ENFORCEMENT_LEVEL_WARN
                                                | AGGREGATION_TYPE_COMPOSITE | AGGREGATION_TYPE_SHARED | AGGREGATION_TYPE_NONE
                                                | FUNCTION_TEST_DATA | FUNCTION_SUITE_TESTS | FUNCTION_TEST_PARAMETERS
                                                | FUNCTION_TEST_ASSERTS | FUNCTION_TEST_DATA_STORE
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                                     imports
                                                    elementDefinition*
                                                EOF
;
imports:                                        importStatement*
;
importStatement:                                IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
elementDefinition:                              (
                                                    profile
                                                    | classDefinition
                                                    | association
                                                    | enumDefinition
                                                    | nativeFunction
                                                    | functionDefinition
                                                    | instance
                                                    | measureDefinition
                                                )
;


// -------------------------------------- SHARED --------------------------------------

stereotypes:                                    LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN
;
stereotype:                                     qualifiedName DOT identifier
;
taggedValues:                                   BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE
;
taggedValue:                                    qualifiedName DOT identifier EQUAL STRING
;


// -------------------------------------- CLASS --------------------------------------

classDefinition:                                CLASS stereotypes? taggedValues? qualifiedName typeParametersWithContravarianceAndMultiplicityParameters?
                                                (
                                                    (PROJECTS projection)
                                                    |
                                                    (
                                                        (EXTENDS type (COMMA type)*)?
                                                        constraints?
                                                        classBody
                                                    )
                                                )
;
classBody:                                      BRACE_OPEN
                                                    properties
                                                BRACE_CLOSE
;
properties:                                     (property | qualifiedProperty)*
;
property:                                       stereotypes? taggedValues? aggregation? identifier COLON propertyReturnType defaultValue? SEMI_COLON
;
qualifiedProperty:                              stereotypes? taggedValues? identifier qualifiedPropertyBody COLON propertyReturnType  SEMI_COLON
;
qualifiedPropertyBody:                          PAREN_OPEN (functionVariableExpression (COMMA functionVariableExpression)*)? PAREN_CLOSE
                                                    BRACE_OPEN codeBlock BRACE_CLOSE
;
aggregation:                                    PAREN_OPEN aggregationType PAREN_CLOSE
;
aggregationType:                                AGGREGATION_TYPE_COMPOSITE | AGGREGATION_TYPE_SHARED | AGGREGATION_TYPE_NONE
;

defaultValue: EQUAL defaultValueExpression
;

defaultValueExpression: (instanceReference)(propertyExpression) | expressionInstance | instanceLiteralToken | defaultValueExpressionsArray
;

defaultValueExpressionsArray: BRACKET_OPEN ( defaultValueExpression (COMMA defaultValueExpression)* )? BRACKET_CLOSE
;


// -------------------------------------- ASSOCIATION --------------------------------------

association:                                    ASSOCIATION stereotypes? taggedValues? qualifiedName
                                                    (associationProjection | associationBody)
;
associationBody:                                BRACE_OPEN properties BRACE_CLOSE
;
associationProjection:                          PROJECTS qualifiedName LESS_THAN qualifiedName COMMA qualifiedName GREATER_THAN
;


// -------------------------------------- PROFILE --------------------------------------

profile:                                        PROFILE qualifiedName
                                                    BRACE_OPEN
                                                        (
                                                            stereotypeDefinitions
                                                            | tagDefinitions
                                                        )*
                                                    BRACE_CLOSE
;
stereotypeDefinitions:                          (STEREOTYPES COLON BRACKET_OPEN (identifier (COMMA identifier)*)? BRACKET_CLOSE SEMI_COLON)
;
tagDefinitions:                                 (TAGS COLON BRACKET_OPEN (identifier (COMMA identifier)*)? BRACKET_CLOSE SEMI_COLON)
;


// -------------------------------------- ENUM --------------------------------------

enumDefinition:                                 ENUM stereotypes? taggedValues? qualifiedName
                                                    BRACE_OPEN
                                                        (enumValue (COMMA enumValue)*)?
                                                    BRACE_CLOSE
;
enumValue:                                      stereotypes? taggedValues? identifier
;


// -------------------------------------- MEASURE --------------------------------------

measureDefinition:                              MEASURE qualifiedName
                                                    measureBody
;
measureBody:                                    BRACE_OPEN
                                                    (
                                                        (measureExpr* canonicalExpr measureExpr*)
                                                        | nonConvertibleMeasureExpr+
                                                    )
                                                BRACE_CLOSE
;
canonicalExpr:                                  STAR measureExpr
;
measureExpr:                                    qualifiedName COLON unitExpr
;
nonConvertibleMeasureExpr:                      qualifiedName SEMI_COLON
;
unitExpr:                                       identifier ARROW codeBlock
;


// -------------------------------------- FUNCTION --------------------------------------

nativeFunction:                                 NATIVE FUNCTION qualifiedName typeAndMultiplicityParameters? functionTypeSignature SEMI_COLON
;
functionTypeSignature:                          PAREN_OPEN (functionVariableExpression (COMMA functionVariableExpression)*)? PAREN_CLOSE COLON type multiplicity
;
functionDefinition:                             FUNCTION stereotypes? taggedValues? qualifiedName typeAndMultiplicityParameters? functionTypeSignature
                                                constraints?
                                                    BRACE_OPEN
                                                        codeBlock
                                                    BRACE_CLOSE
                                                functionTestSuites?
;
functionTestSuites:                             BRACKET_OPEN
                                                    functionTestSuite (COMMA functionTestSuite)*
                                                BRACKET_CLOSE
;
functionTestSuite:                              identifier COLON BRACE_OPEN (testData | functionTestSuiteTests )* BRACE_CLOSE
;
testData:                                       FUNCTION_TEST_DATA COLON BRACKET_OPEN (storeTestData ( COMMA storeTestData )*)? BRACKET_CLOSE SEMI_COLON
;
storeTestData:                                  BRACE_OPEN
                                                (
                                                    storePointer |
                                                    storeData
                                                )*
                                                BRACE_CLOSE
;
storePointer:                                   FUNCTION_TEST_DATA_STORE COLON qualifiedName SEMI_COLON
;
storeData:                                      FUNCTION_TEST_DATA COLON embeddedData SEMI_COLON
;
embeddedData:                                   identifier ISLAND_OPEN (embeddedDataContent)*
;
embeddedDataContent:                            ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
functionTestSuiteTests:                         FUNCTION_SUITE_TESTS COLON BRACKET_OPEN (functionTestBlock ( COMMA functionTestBlock )*)? BRACKET_CLOSE
;
functionTestBlock:                              identifier COLON BRACE_OPEN (functionTestParameters | functionTestAsserts )* BRACE_CLOSE
;
functionTestParameters:                         FUNCTION_TEST_PARAMETERS COLON BRACKET_OPEN ( functionTestParameter ( COMMA functionTestParameter )* )? BRACKET_CLOSE
;
functionTestParameter:                          identifier EQUAL primitiveValue
;
functionTestAsserts:                            FUNCTION_TEST_ASSERTS COLON BRACKET_OPEN ( functionTestAssert ( COMMA functionTestAssert )* )? BRACKET_CLOSE
;
functionTestAssert:                             identifier COLON testAssertion
;
testAssertion:                                  identifier ISLAND_OPEN (testAssertionContent)*
;
testAssertionContent:                           ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
// -------------------------------------- CONSTRAINT --------------------------------------

constraints:                                    BRACKET_OPEN
                                                    constraint (COMMA constraint)*
                                                BRACKET_CLOSE
;
constraint:                                     simpleConstraint | complexConstraint
;
simpleConstraint:                               constraintId? combinedExpression
;
complexConstraint:                              identifier
                                                    PAREN_OPEN
                                                        constraintOwner?
                                                        constraintExternalId?
                                                        constraintFunction
                                                        constraintEnforcementLevel?
                                                        constraintMessage?
                                                    PAREN_CLOSE
;
constraintOwner:                                CONSTRAINT_OWNER COLON identifier
;
constraintExternalId:                           CONSTRAINT_EXTERNAL_ID COLON STRING
;
constraintFunction:                             CONSTRAINT_FUNCTION COLON combinedExpression
;
constraintEnforcementLevel:                     CONSTRAINT_ENFORCEMENT COLON constraintEnforcementLevelType
;
constraintEnforcementLevelType:                 CONSTRAINT_ENFORCEMENT_LEVEL_ERROR | CONSTRAINT_ENFORCEMENT_LEVEL_WARN
;
constraintMessage:                              CONSTRAINT_MESSAGE COLON combinedExpression
;
constraintId:                                   identifier COLON
;


// -------------------------------------- PROJECTION --------------------------------------

projection:                                     dsl | treePath
;
treePath:                                       type alias? stereotypes? taggedValues? treePathClassBody
;
treePathClassBody:                              BRACE_OPEN
                                                    simplePropertyFilter?
                                                    (derivedProperty | complexProperty)*
                                                BRACE_CLOSE
;
alias:                                          AS identifier
;
simplePropertyFilter:                           STAR | ((PLUS | MINUS) (BRACKET_OPEN simpleProperty (COMMA simpleProperty)* BRACKET_CLOSE))
;
simpleProperty:                                 propertyRef stereotypes? taggedValues?
;
complexProperty:                                propertyRef alias? stereotypes? taggedValues? treePathClassBody?
;
derivedProperty:                                GREATER_THAN propertyRef BRACKET_OPEN codeBlock BRACKET_CLOSE alias? stereotypes? taggedValues? treePathClassBody?
;
propertyRef:                                    identifier (PAREN_OPEN (treePathPropertyParameterType (COMMA treePathPropertyParameterType)*)? PAREN_CLOSE)*
;
treePathPropertyParameterType:                  type multiplicity
;
