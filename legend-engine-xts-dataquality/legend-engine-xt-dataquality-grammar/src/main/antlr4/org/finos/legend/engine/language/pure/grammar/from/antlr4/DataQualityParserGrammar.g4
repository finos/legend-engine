parser grammar  DataQualityParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = DataQualityLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                             VALID_STRING | STRING
                                        | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE | TO_BYTES_FUNCTION
                                        | DATAQUALITYVALIDATION
                                        | DQVALIDATIONCONSTRAINTS
                                        | FROM_DATASPACE
                                        | FROM_MAPPING_AND_RUNTIME
                                        | DQCONTEXT
                                        | FILTER
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                           (validationDefinition)*
                                      EOF
;
validationDefinition:                  DATAQUALITYVALIDATION stereotypes? taggedValues? qualifiedName
                                        BRACE_OPEN
                                             (
                                                 dqContext
                                                 | filter
                                                 | validationTree
                                             )*
                                        BRACE_CLOSE
;
stereotypes:                           LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN;
stereotype:                            qualifiedName DOT identifier;
taggedValues:                          BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE;
taggedValue:                           qualifiedName DOT identifier EQUAL STRING;


dqContext:                            DQCONTEXT COLON (fromDataSpace| fromMappingAndRuntime)
                                      SEMI_COLON
;
fromDataSpace:                        FROM_DATASPACE PAREN_OPEN dataspace COMMA contextName PAREN_CLOSE;
fromMappingAndRuntime:                FROM_MAPPING_AND_RUNTIME PAREN_OPEN mapping COMMA runtime PAREN_CLOSE;
mapping:                              qualifiedName;
runtime:                              qualifiedName;
dataspace:                            qualifiedName;
contextName:                          STRING;
validationTree:                       DQVALIDATIONCONSTRAINTS COLON dqGraphDefinition SEMI_COLON
;
filter:                               FILTER COLON combinedExpression SEMI_COLON;


// -------------------------------------- TREE DEFINITION --------------------------------------

dqGraphDefinition:                  GRAPH_START qualifiedName(constraintList)? graphDefinition GRAPH_END
;
graphDefinition:                    BRACE_OPEN
                                        graphPaths
                                    BRACE_CLOSE
;
graphPaths:                         (graphPath | subTypeGraphPath) (COMMA (graphPath | subTypeGraphPath))*
;
graphPath:                          alias? identifier(constraintList)? propertyParameters? subtype? graphDefinition?
;
subTypeGraphPath:                   subtype graphDefinition
;
alias:                              STRING COLON
;
propertyParameters:                 PAREN_OPEN
                                        (parameter (COMMA parameter)*)?
                                    PAREN_CLOSE
;
subtype:                            SUBTYPE_START qualifiedName PAREN_CLOSE
;
parameter:                          scalarParameter | collectionParameter
;
scalarParameter:                    LATEST_DATE | instanceLiteral | variable | enumReference
;
collectionParameter:                BRACKET_OPEN
                                        (scalarParameter (COMMA scalarParameter)*)?
                                    BRACKET_CLOSE
;
instanceLiteral:                    instanceLiteralToken | (MINUS INTEGER) | (MINUS FLOAT) | (MINUS DECIMAL) | (PLUS INTEGER) | (PLUS FLOAT) | (PLUS DECIMAL)
;
instanceLiteralToken:               STRING | INTEGER | FLOAT | DECIMAL | DATE | BOOLEAN
;
variable:                           DOLLAR identifier
;
enumReference:                      qualifiedName DOT identifier
;
constraintList:                     LESS_THAN
                                      dqConstraintName (COMMA dqConstraintName)*
                                    GREATER_THAN
;
dqConstraintName:                   identifier
;