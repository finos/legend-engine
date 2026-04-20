parser grammar DeephavenParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DeephavenLexerGrammar;
}

unquotedIdentifier:                         VALID_STRING
                                            | DEEPHAVEN
                                            | IMPORT
                                            | TABLE
                                            | TABLES | COLUMNS | COLUMNDEFINITION
                                            | BOOLEAN_TYPE | INT_TYPE | FLOAT_TYPE | DOUBLE_TYPE
                                            | DECIMAL_TYPE | STRING_TYPE | TIMESTAMP_TYPE | DATETIME_TYPE
                                            | DEEPHAVEN_APP
                                            | DEEPHAVEN_APP__APPLICATION_NAME
                                            | DEEPHAVEN_APP__FUNCTION
                                            | DEEPHAVEN_APP__OWNER
                                            | DEEPHAVEN_APP__OWNER_DEPLOYMENT
                                            | DEEPHAVEN_APP__OWNER_DEPLOYMENT_ID
                                            | DEEPHAVEN_APP__DESCRIPTION
;

identifier:                                 unquotedIdentifier | STRING
;

imports:                                    (importStatement)*
;

importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;

definition:                                 imports
                                                (deephavenDefinition | deephavenAppDefinition)*
                                            EOF
;

deephavenDefinition:                        DEEPHAVEN qualifiedName
                                                PAREN_OPEN
                                                    (
                                                        tables
                                                    )*
                                                PAREN_CLOSE
;

// -------------------------------------- DeephavenApp --------------------------------------

deephavenAppDefinition:                     DEEPHAVEN_APP qualifiedName
                                                BRACE_OPEN
                                                    (
                                                        appApplicationName
                                                        | appFunction
                                                        | appOwnership
                                                        | appDescription
                                                    )*
                                                BRACE_CLOSE
;

appApplicationName:                         DEEPHAVEN_APP__APPLICATION_NAME COLON STRING SEMI_COLON
;

appFunction:                                DEEPHAVEN_APP__FUNCTION COLON appFunctionIdentifier SEMI_COLON
;

appFunctionIdentifier:                      qualifiedName PAREN_OPEN (appFunctionTypePureType (COMMA appFunctionTypePureType)*)? PAREN_CLOSE COLON appFunctionTypePureType
;

appFunctionTypePureType:                    qualifiedName appMultiplicity
;

appMultiplicity:                            BRACKET_OPEN (STAR | INTEGER (DOT DOT (INTEGER | STAR))?) BRACKET_CLOSE
;

appOwnership:                               DEEPHAVEN_APP__OWNER COLON
                                                DEEPHAVEN_APP__OWNER_DEPLOYMENT
                                                    BRACE_OPEN
                                                        DEEPHAVEN_APP__OWNER_DEPLOYMENT_ID COLON STRING
                                                    BRACE_CLOSE SEMI_COLON
;

appDescription:                             DEEPHAVEN_APP__DESCRIPTION COLON STRING SEMI_COLON
;

// -------------------------------------- Deephaven Store Tables --------------------------------------

tables:                                     tableDefinition (tableDefinition)*
;

tableDefinition:                            TABLE tableName
                                            PAREN_OPEN
                                                (
                                                    columns
                                                )*
                                            PAREN_CLOSE
;

columns:                                    columnDefinition (COMMA columnDefinition)*
;

columnDefinition:                           columnName COLON columnType
;

// TODO - current grammar parser will pick up "\'" as unknown token - need to enable single quote for col names that may contain space or as a method of identifier escape
columnName:                                 VALID_STRING | STRING
;

columnType:                                 BOOLEAN_TYPE
                                            | INT_TYPE
                                            | FLOAT_TYPE
                                            | DOUBLE_TYPE
                                            | STRING_TYPE
                                            | TIMESTAMP_TYPE
                                            | DATETIME_TYPE
                                            | decimalType
;

decimalType:                                DECIMAL_TYPE PAREN_OPEN precision=INTEGER COMMA scale=INTEGER PAREN_CLOSE
;

tableName:                                  VALID_STRING | STRING
;
