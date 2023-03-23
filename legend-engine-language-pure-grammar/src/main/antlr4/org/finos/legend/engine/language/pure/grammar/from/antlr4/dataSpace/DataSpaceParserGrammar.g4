parser grammar DataSpaceParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DataSpaceLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                         VALID_STRING | STRING
                                    | STEREOTYPES | TAGS
                                    | DATA_SPACE
                                    | DATA_SPACE__NAME
                                    | DATA_SPACE__DESCRIPTION
                                    | DATA_SPACE__TITLE
                                    | DATA_SPACE_EXECUTION_CONTEXTS
                                    | DATA_SPACE_DEFAULT_EXECUTION_CONTEXT
                                    | DATA_SPACE_MAPPING
                                    | DATA_SPACE_DEFAULT_RUNTIME
                                    | DATA_SPACE_FEATURED_DIAGRAMS
                                    | DATA_SPACE_ELEMENTS
                                    | DATA_SPACE_EXECUTABLES
                                    | DATA_SPACE_EXECUTABLE
                                    | DATA_SPACE_SUPPORT_INFO
                                    | DATA_SPACE_SUPPORT_EMAIL
                                    | DATA_SPACE_SUPPORT_EMAIL_ADDRESS

                                    // deprecated
                                    | DATA_SPACE_GROUP_ID
                                    | DATA_SPACE_ARTIFACT_ID
                                    | DATA_SPACE_VERSION_ID
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                         (dataSpaceElement)*
                                    EOF
;
dataSpaceElement:                   DATA_SPACE stereotypes? taggedValues? qualifiedName
                                        BRACE_OPEN
                                            (
                                                executionContexts
                                                | defaultExecutionContext
                                                | title
                                                | description
                                                | featuredDiagrams
                                                | elements
                                                | executables
                                                | supportInfo

                                                // deprecated
                                                | groupId
                                                | artifactId
                                                | versionId
                                            )*
                                        BRACE_CLOSE
;
stereotypes:                        LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN
;
stereotype:                         qualifiedName DOT identifier
;
taggedValues:                       BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE
;
taggedValue:                        qualifiedName DOT identifier EQUAL STRING
;

title:                              DATA_SPACE__TITLE COLON STRING SEMI_COLON
;
description:                        DATA_SPACE__DESCRIPTION COLON STRING SEMI_COLON
;

executionContexts:                  DATA_SPACE_EXECUTION_CONTEXTS COLON BRACKET_OPEN ( executionContext (COMMA executionContext)* )? BRACKET_CLOSE SEMI_COLON
;
executionContext:                   BRACE_OPEN
                                        (
                                            executionContextName
                                            | executionContextDescription
                                            | executionContextMapping
                                            | executionContextDefaultRuntime
                                        )*
                                    BRACE_CLOSE
;
executionContextName:               DATA_SPACE__NAME COLON STRING SEMI_COLON
;
executionContextDescription:        DATA_SPACE__DESCRIPTION COLON STRING SEMI_COLON
;
executionContextMapping:            DATA_SPACE_MAPPING COLON qualifiedName SEMI_COLON
;
executionContextDefaultRuntime:     DATA_SPACE_DEFAULT_RUNTIME COLON qualifiedName SEMI_COLON
;
defaultExecutionContext:            DATA_SPACE_DEFAULT_EXECUTION_CONTEXT COLON STRING SEMI_COLON
;

featuredDiagrams:                   DATA_SPACE_FEATURED_DIAGRAMS COLON BRACKET_OPEN ( qualifiedName (COMMA qualifiedName)* )? BRACKET_CLOSE SEMI_COLON
;

elements:                           DATA_SPACE_ELEMENTS COLON BRACKET_OPEN ( qualifiedName (COMMA qualifiedName)* )? BRACKET_CLOSE SEMI_COLON
;

executables:                        DATA_SPACE_EXECUTABLES COLON BRACKET_OPEN ( executable (COMMA executable)* )? BRACKET_CLOSE SEMI_COLON
;
executable:                         BRACE_OPEN
                                        (
                                            executableTitle
                                            | executableDescription
                                            | executablePath
                                        )*
                                    BRACE_CLOSE
;
executableTitle:                    DATA_SPACE__TITLE COLON STRING SEMI_COLON
;
executableDescription:              DATA_SPACE__DESCRIPTION COLON STRING SEMI_COLON
;
executablePath:                     DATA_SPACE_EXECUTABLE COLON qualifiedName SEMI_COLON
;

supportInfo:                        DATA_SPACE_SUPPORT_INFO COLON
                                        DATA_SPACE_SUPPORT_EMAIL BRACE_OPEN
                                           (
                                               supportEmail
                                           )*
                                        BRACE_CLOSE SEMI_COLON
;
supportEmail:                       DATA_SPACE_SUPPORT_EMAIL_ADDRESS COLON STRING SEMI_COLON
;


// -------------------------------------- DEPRECATED --------------------------------------

groupId:                            DATA_SPACE_GROUP_ID COLON STRING SEMI_COLON
;
artifactId:                         DATA_SPACE_ARTIFACT_ID COLON STRING SEMI_COLON
;
versionId:                          DATA_SPACE_VERSION_ID COLON STRING SEMI_COLON
;