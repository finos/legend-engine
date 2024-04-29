parser grammar ElasticsearchParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ElasticsearchLexerGrammar;
}

scalarPropertyTypes:                    KEYWORD |
                                        TEXT |       
                                        DATE |
                                        SHORT |
                                        BYTE |
                                        INTEGER |
                                        LONG |
                                        FLOAT |
                                        HALF_FLOAT |
                                        DOUBLE |
                                        BOOLEAN
;

complexPropertyTypes:                   OBJECT | NESTED
;

identifier:                             VALID_STRING | STRING | ES_V7_CLUSTER | IMPORT | INDICES | PROPERTIES | FIELDS | scalarPropertyTypes | complexPropertyTypes
;

imports:                                (importStatement)*
;

importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;

definition:                             imports
                                            (v7StoreDefinition)*
                                        EOF
;

v7StoreDefinition:                      ES_V7_CLUSTER qualifiedName
                                            BRACE_OPEN
                                                (
                                                    indices
                                                )*
                                            BRACE_CLOSE
;

indices:                                INDICES COLON
                                            BRACKET_OPEN
                                                indexDefinition (COMMA indexDefinition)*
                                            BRACKET_CLOSE
                                        SEMI_COLON
;

indexName:                              STRING | VALID_STRING
;

indexDefinition:                        indexName COLON
                                            BRACE_OPEN
                                                (
                                                    propertiesDefinition
                                                )*
                                            BRACE_CLOSE
;

propertiesDefinition:                   PROPERTIES COLON propertiesArrayDefinition SEMI_COLON
;

fieldsDefinition:                       FIELDS COLON propertiesArrayDefinition SEMI_COLON
;

propertiesArrayDefinition:              BRACKET_OPEN
                                                namedPropertyDefinition (COMMA namedPropertyDefinition)*
                                        BRACKET_CLOSE
;

propertyName:                           STRING | VALID_STRING
;

namedPropertyDefinition:               propertyName COLON propertyTypeDefinition
;

propertyTypeDefinition:                 scalarPropertyDefinition | complexPropertyDefinition
;

scalarPropertyContent:                  BRACE_OPEN
                                            (
                                                fieldsDefinition
                                            )*
                                        BRACE_CLOSE
;

scalarPropertyDefinition:              scalarPropertyTypes scalarPropertyContent?
;

complexPropertyContent:                  BRACE_OPEN
                                            (
                                                propertiesDefinition
                                            )*
                                         BRACE_CLOSE
;

complexPropertyDefinition:              complexPropertyTypes complexPropertyContent
;
