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
                                            | TABLES | COLUMNS | COLUMNDEFINITION | columnType
;

identifier:                                 unquotedIdentifier | STRING
;

imports:                                    (importStatement)*
;

importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;

definition:                                 imports
                                                (deephavenDefinition)*
                                            EOF
;

deephavenDefinition:                        DEEPHAVEN qualifiedName
                                                PAREN_OPEN
                                                    (
                                                        tables
                                                    )*
                                                PAREN_CLOSE
;

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

columnType:                                 DATE_TIME | STRING | INT | BOOLEAN | FLOAT
;

tableName:                                  VALID_STRING | STRING
;

