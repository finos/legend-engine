parser grammar DataSpaceParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DataSpaceLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                 VALID_STRING | STRING
                            | STEREOTYPES | TAGS
                            | DATA_SPACE
                            | DATA_SPACE_GROUP_ID
                            | DATA_SPACE_ARTIFACT_ID
                            | DATA_SPACE_VERSION_ID
                            | DATA_SPACE_EXECUTION_CONTEXTS
                            | DATA_SPACE_DEFAULT_EXECUTION_CONTEXT
                            | DATA_SPACE_EXECUTION_CONTEXT_NAME
                            | DATA_SPACE_MAPPING
                            | DATA_SPACE_DEFAULT_RUNTIME
                            | DATA_SPACE_DESCRIPTION
                            | DATA_SPACE_FEATURED_DIAGRAMS
                            | DATA_SPACE_SUPPORT_INFO
                            | DATA_SPACE_SUPPORT_CONTACTS
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                 (dataSpaceElement)*
                            EOF
;
dataSpaceElement:           DATA_SPACE stereotypes? taggedValues? qualifiedName
                                BRACE_OPEN
                                    (
                                        groupId
                                        | artifactId
                                        | versionId
                                        | executionContexts
                                        | defaultExecutionContext
                                        | description
                                        | featuredDiagrams
                                        | supportInfo
                                    )*
                                BRACE_CLOSE
;
stereotypes:                LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN
;
stereotype:                 qualifiedName DOT identifier
;
taggedValues:               BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE
;
taggedValue:                qualifiedName DOT identifier EQUAL STRING
;

groupId:                    DATA_SPACE_GROUP_ID COLON STRING SEMI_COLON
;
artifactId:                 DATA_SPACE_ARTIFACT_ID COLON STRING SEMI_COLON
;
versionId:                  DATA_SPACE_VERSION_ID COLON STRING SEMI_COLON
;

executionContexts:          DATA_SPACE_EXECUTION_CONTEXTS COLON BRACKET_OPEN ( executionContext (COMMA executionContext)* )? BRACKET_CLOSE SEMI_COLON
;
executionContext:           BRACE_OPEN
                                (
                                    executionContextName
                                    | description
                                    | mapping
                                    | defaultRuntime
                                )*
                            BRACE_CLOSE
;
executionContextName:       DATA_SPACE_EXECUTION_CONTEXT_NAME COLON STRING SEMI_COLON
;
mapping:                    DATA_SPACE_MAPPING COLON qualifiedName SEMI_COLON
;
defaultRuntime:             DATA_SPACE_DEFAULT_RUNTIME COLON qualifiedName SEMI_COLON
;
defaultExecutionContext:    DATA_SPACE_DEFAULT_EXECUTION_CONTEXT COLON STRING SEMI_COLON
;

description:                DATA_SPACE_DESCRIPTION COLON STRING SEMI_COLON
;

featuredDiagrams:           DATA_SPACE_FEATURED_DIAGRAMS COLON BRACKET_OPEN ( qualifiedName (COMMA qualifiedName)* )? BRACKET_CLOSE SEMI_COLON
;
supportInfo:                DATA_SPACE_SUPPORT_INFO
                                BRACE_OPEN
                                   (
                                       description
                                       | supportContacts
                                   )*
                                BRACE_CLOSE SEMI_COLON
;
supportContacts:            DATA_SPACE_SUPPORT_CONTACTS COLON BRACKET_OPEN ( STRING (COMMA STRING)* )? BRACKET_CLOSE SEMI_COLON
;
