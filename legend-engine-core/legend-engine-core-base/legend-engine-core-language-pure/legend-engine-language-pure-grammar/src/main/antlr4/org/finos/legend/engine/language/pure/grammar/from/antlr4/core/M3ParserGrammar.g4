parser grammar M3ParserGrammar;

import CoreParserGrammar;


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                     VALID_STRING | STRING
                                                | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE
                                                | TO_BYTES_FUNCTION
;

// -------------------------------------- EXPRESSION & VALUE SPECIFICATION --------------------------------------

nonArrowOrEqualExpression :
                                                (
                                                    atomicExpression
                                                    | notExpression
                                                    | signedExpression
                                                    | expressionsArray
                                                    | (PAREN_OPEN combinedExpression PAREN_CLOSE)
                                                )
;

expression:                                     (
                                                    nonArrowOrEqualExpression
                                                    (
                                                        (propertyOrFunctionExpression)*
                                                        (equalNotEqual)?
                                                    )
                                                )
;


instance:                                       NEW_SYMBOL qualifiedName (LESS_THAN typeArguments? (PIPE multiplicityArguments)? GREATER_THAN)? identifier?
                                                (FILE_NAME COLON INTEGER COMMA INTEGER COMMA INTEGER COMMA INTEGER COMMA INTEGER COMMA INTEGER FILE_NAME_END)? (AT qualifiedName)?
                                                    PAREN_OPEN
                                                        (instancePropertyAssignment (COMMA instancePropertyAssignment)*)?
                                                    PAREN_CLOSE
;
unitInstance:                                   unitInstanceLiteral unitName
;
unitName:                                       qualifiedName TILDE identifier
;
instancePropertyAssignment:                     identifier EQUAL instanceRightSide
;
instanceRightSide:                              instanceAtomicRightSideScalar | instanceAtomicRightSideVector
;
instanceAtomicRightSideScalar:                  instanceAtomicRightSide
;
instanceAtomicRightSideVector:                  BRACKET_OPEN (instanceAtomicRightSide (COMMA instanceAtomicRightSide)* )? BRACKET_CLOSE
;
instanceAtomicRightSide:                        instanceLiteral
                                                | LATEST_DATE
                                                | instance
                                                | qualifiedName
                                                | enumReference
                                                | stereotypeReference
                                                | tagReference
                                                | identifier
;
enumReference:                                  qualifiedName  DOT identifier
;
stereotypeReference:                            qualifiedName AT identifier
;
tagReference:                                   qualifiedName PERCENT identifier
;
propertyReturnType:                             type multiplicity
;
codeBlock:                                      programLine (SEMI_COLON (programLine SEMI_COLON)*)?
;
programLine:                                    combinedExpression | letExpression
;
equalNotEqual:                                  (TEST_EQUAL | TEST_NOT_EQUAL) combinedArithmeticOnly
;
combinedArithmeticOnly:                         expression arithmeticPart*
;
expressionPart:                                 booleanPart | arithmeticPart
;
letExpression:                                  LET identifier EQUAL combinedExpression
;
combinedExpression:                             expression expressionPart*
;
expressionsArray:                               BRACKET_OPEN ( expression (COMMA expression)* )? BRACKET_CLOSE
;
propertyOrFunctionExpression:                   propertyExpression | functionExpression | propertyBracketExpression
;
propertyExpression:                             DOT identifier (functionExpressionLatestMilestoningDateParameter | functionExpressionParameters)?
;
propertyBracketExpression:                      BRACKET_OPEN (STRING | INTEGER)  BRACKET_CLOSE
;
functionExpression:                             ARROW qualifiedName functionExpressionParameters (ARROW qualifiedName functionExpressionParameters)*
;
functionExpressionLatestMilestoningDateParameter:
                                                PAREN_OPEN LATEST_DATE (COMMA LATEST_DATE)? PAREN_CLOSE
;
functionExpressionParameters:                   PAREN_OPEN (combinedExpression (COMMA combinedExpression)*)? PAREN_CLOSE
;
atomicExpression:                               dsl
                                                | instanceLiteralToken
                                                | expressionInstance
                                                | unitInstance
                                                | variable
                                                | columnBuilders
                                                | (AT type)
                                                | anyLambda
                                                | instanceReference
;

columnBuilders: TILDE (oneColSpec | colSpecArray)
;
oneColSpec: identifier ((COLON (type | anyLambda) extraFunction? ))?
;
colSpecArray: (BRACKET_OPEN (oneColSpec(COMMA oneColSpec)*)? BRACKET_CLOSE)
;
extraFunction: (COLON anyLambda)
;

anyLambda : lambdaPipe | lambdaFunction | lambdaParam lambdaPipe
;

instanceReference:                              (PATH_SEPARATOR | qualifiedName | unitName) allOrFunction?
;
lambdaFunction:                                 BRACE_OPEN (lambdaParam (COMMA lambdaParam)* )? lambdaPipe BRACE_CLOSE
;
variable:                                       DOLLAR identifier
;
allOrFunction:                                  allFunction
                                                | allVersionsFunction
                                                | allVersionsInRangeFunction
                                                | allFunctionWithMilestoning
                                                | functionExpressionParameters
;
allFunction:                                    DOT ALL PAREN_OPEN PAREN_CLOSE
;
allVersionsFunction:                            DOT ALL_VERSIONS PAREN_OPEN PAREN_CLOSE
;
allVersionsInRangeFunction:                     DOT ALL_VERSIONS_IN_RANGE PAREN_OPEN buildMilestoningVariableExpression COMMA buildMilestoningVariableExpression PAREN_CLOSE
;
allFunctionWithMilestoning:                     DOT ALL PAREN_OPEN buildMilestoningVariableExpression (COMMA buildMilestoningVariableExpression)? PAREN_CLOSE
;
buildMilestoningVariableExpression:             LATEST_DATE | DATE | variable
;
expressionInstance:                             NEW_SYMBOL (variable | qualifiedName)
                                                (LESS_THAN typeArguments? (PIPE multiplicityArguments)? GREATER_THAN)? (identifier)?
                                                PAREN_OPEN
                                                    expressionInstanceParserPropertyAssignment? (COMMA expressionInstanceParserPropertyAssignment)*
                                                PAREN_CLOSE
;
expressionInstanceRightSide:                    expressionInstanceAtomicRightSide
;
expressionInstanceAtomicRightSide:              combinedExpression | expressionInstance | qualifiedName
;
expressionInstanceParserPropertyAssignment:     identifier (DOT identifier)* PLUS? EQUAL expressionInstanceRightSide
;
notExpression:                                  NOT expression
;
signedExpression:                               (MINUS | PLUS) expression
;
lambdaPipe:                                     PIPE codeBlock
;
lambdaParam:                                    identifier lambdaParamType?
;
lambdaParamType:                                COLON type multiplicity
;
primitiveValue:                                 primitiveValueAtomic | primitiveValueVector
;
primitiveValueVector:                           BRACKET_OPEN (primitiveValueAtomic (COMMA primitiveValueAtomic)* )? BRACKET_CLOSE
;
primitiveValueAtomic:                           instanceLiteral | toBytesLiteral | enumReference
;
instanceLiteral:                                instanceLiteralToken | (MINUS INTEGER) | (MINUS FLOAT) | (MINUS DECIMAL) | (PLUS INTEGER) | (PLUS FLOAT) | (PLUS DECIMAL)
;
instanceLiteralToken:                           STRING | INTEGER | FLOAT | DECIMAL | DATE | BOOLEAN | STRICTTIME
;
toBytesLiteral:                                 TO_BYTES_FUNCTION PAREN_OPEN STRING PAREN_CLOSE
;
unitInstanceLiteral:                            (MINUS? INTEGER) | (MINUS? FLOAT) | (MINUS? DECIMAL) | (PLUS INTEGER) | (PLUS FLOAT) | (PLUS DECIMAL)
;
arithmeticPart:                                 (
                                                    PLUS expression (PLUS expression)*
                                                    | (STAR expression (STAR expression)*)
                                                    | (MINUS expression (MINUS expression)*)
                                                    | (DIVIDE expression (DIVIDE expression)*)
                                                    | (LESS_THAN expression)
                                                    | (LESS_OR_EQUAL expression)
                                                    | (GREATER_THAN expression)
                                                    | (GREATER_OR_EQUAL expression)
                                                )
;
booleanPart:                                    (AND expression) | (OR  expression)
;
functionVariableExpression:                     identifier COLON type multiplicity
;
dsl:                                            dslExtension | dslNavigationPath
;
dslNavigationPath:                              NAVIGATION_PATH_BLOCK
;
dslExtension:                                   ISLAND_OPEN (dslExtensionContent)*
;
dslExtensionContent:                            ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
type:                                           (qualifiedName (LESS_THAN typeArguments? (PIPE multiplicityArguments)? GREATER_THAN)?)
                                                |
                                                (
                                                    BRACE_OPEN
                                                        functionTypePureType? (COMMA functionTypePureType)*
                                                        ARROW type multiplicity
                                                    BRACE_CLOSE
                                                )
                                                |
                                                unitName
;
functionTypePureType:                           type multiplicity
;
typeAndMultiplicityParameters:                  LESS_THAN ((typeParameters multiplictyParameters?) | multiplictyParameters) GREATER_THAN
;
typeParametersWithContravarianceAndMultiplicityParameters:
                                                LESS_THAN ((contravarianceTypeParameters multiplictyParameters?) | multiplictyParameters) GREATER_THAN
;
typeParameters:                                 typeParameter (COMMA typeParameter)*
;
typeParameter:                                  identifier
;
contravarianceTypeParameters:                   contravarianceTypeParameter (COMMA contravarianceTypeParameter)*
;
contravarianceTypeParameter:                    MINUS? identifier
;
multiplicityArguments:                          multiplicityArgument (COMMA multiplicityArgument)*
;
typeArguments:                                  type (COMMA type)*
;
multiplictyParameters:                          PIPE identifier (COMMA identifier)*
;




multiplicity:                                   BRACKET_OPEN multiplicityArgument BRACKET_CLOSE
;
multiplicityArgument:                           identifier | ((fromMultiplicity DOT_DOT)? toMultiplicity)
;
fromMultiplicity:                               INTEGER
;
toMultiplicity:                                 INTEGER | STAR
;



functionIdentifier:                         qualifiedName PAREN_OPEN (qualifiedName multiplicity (COMMA qualifiedName multiplicity)*)? PAREN_CLOSE COLON qualifiedName multiplicity
;