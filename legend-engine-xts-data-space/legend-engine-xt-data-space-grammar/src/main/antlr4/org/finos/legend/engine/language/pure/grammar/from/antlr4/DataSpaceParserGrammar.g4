parser grammar DataSpaceParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = DataSpaceLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                         VALID_STRING | STRING
                                    | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE | TO_BYTES_FUNCTION      // from M3Parser
                                    | STEREOTYPES | TAGS
                                    | DATA_SPACE
                                    | DATA_SPACE__NAME
                                    | DATA_SPACE__DESCRIPTION
                                    | DATA_SPACE__TITLE
                                    | DATA_SPACE_EXECUTION_CONTEXTS
                                    | DATA_SPACE_DEFAULT_EXECUTION_CONTEXT
                                    | DATA_SPACE_MAPPING
                                    | DATA_SPACE_DEFAULT_RUNTIME
                                    | DATA_SPACE_TEST_DATA
                                    | DATA_SPACE_DIAGRAMS
                                    | DATA_SPACE_DIAGRAM
                                    | DATA_SPACE_ELEMENTS
                                    | DATA_SPACE_EXECUTABLES
                                    | DATA_SPACE_EXECUTABLE
                                    | DATA_SPACE__TEMPLATE_QUERY
                                    | DATA_SPACE__EXECUTION_CONTEXT_KEY
                                    | DATA_SPACE_SUPPORT_INFO
                                    | DATA_SPACE_SUPPORT_DOC_URL
                                    | DATA_SPACE_SUPPORT_EMAIL
                                    | DATA_SPACE_SUPPORT_EMAIL_ADDRESS
                                    | DATA_SPACE_SUPPORT_COMBINED_INFO
                                    | DATA_SPACE_SUPPORT_EMAILS
                                    | DATA_SPACE_SUPPORT_WEBSITE
                                    | DATA_SPACE_SUPPORT_FAQ_URL
                                    | DATA_SPACE_SUPPORT_SUPPORT_URL

                                    // deprecated
                                    | DATA_SPACE_GROUP_ID
                                    | DATA_SPACE_ARTIFACT_ID
                                    | DATA_SPACE_VERSION_ID
                                    | DATA_SPACE_FEATURED_DIAGRAMS
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
                                                | diagrams
                                                | elements
                                                | executables
                                                | supportInfo

                                                // deprecated
                                                | groupId
                                                | artifactId
                                                | versionId
                                                | featuredDiagrams
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
                                            | executionContextTitle
                                            | executionContextDescription
                                            | executionContextMapping
                                            | executionContextDefaultRuntime
                                            | executionContextTestData
                                        )*
                                    BRACE_CLOSE
;
executionContextName:               DATA_SPACE__NAME COLON STRING SEMI_COLON
;
executionContextTitle:              DATA_SPACE__TITLE COLON STRING SEMI_COLON
;
executionContextDescription:        DATA_SPACE__DESCRIPTION COLON STRING SEMI_COLON
;
executionContextMapping:            DATA_SPACE_MAPPING COLON qualifiedName SEMI_COLON
;
executionContextTestData:           DATA_SPACE_TEST_DATA COLON embeddedData SEMI_COLON
;
embeddedData:                       identifier ISLAND_OPEN (embeddedDataContent)*
;
embeddedDataContent:                ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
executionContextDefaultRuntime:     DATA_SPACE_DEFAULT_RUNTIME COLON qualifiedName SEMI_COLON
;
defaultExecutionContext:            DATA_SPACE_DEFAULT_EXECUTION_CONTEXT COLON STRING SEMI_COLON
;
diagrams:                           DATA_SPACE_DIAGRAMS COLON BRACKET_OPEN ( diagram (COMMA diagram)* )? BRACKET_CLOSE SEMI_COLON
;
diagram:                            BRACE_OPEN
                                        (
                                            diagramTitle
                                            | diagramDescription
                                            | diagramPath
                                        )*
                                    BRACE_CLOSE
;
diagramTitle:                       DATA_SPACE__TITLE COLON STRING SEMI_COLON
;
diagramDescription:                 DATA_SPACE__DESCRIPTION COLON STRING SEMI_COLON
;
diagramPath:                        DATA_SPACE_DIAGRAM COLON qualifiedName SEMI_COLON
;
elements:                           DATA_SPACE_ELEMENTS COLON BRACKET_OPEN ( elementScopePath (COMMA elementScopePath)* )? BRACKET_CLOSE SEMI_COLON
;
elementScopePath:                   ( MINUS )? qualifiedName
;
executables:                        DATA_SPACE_EXECUTABLES COLON BRACKET_OPEN ( executable (COMMA executable)* )? BRACKET_CLOSE SEMI_COLON
;
executable:                         BRACE_OPEN
                                        (
                                            executableTitle
                                            | executableDescription
                                            | executablePath
                                            | executableTemplateQuery
                                            | executableExecutionContextKey
                                        )*
                                    BRACE_CLOSE
;
executableTitle:                    DATA_SPACE__TITLE COLON STRING SEMI_COLON
;
executableDescription:              DATA_SPACE__DESCRIPTION COLON STRING SEMI_COLON
;
executablePath:                     DATA_SPACE_EXECUTABLE COLON qualifiedName SEMI_COLON
;
executableTemplateQuery:            DATA_SPACE__TEMPLATE_QUERY COLON combinedExpression SEMI_COLON
;
executableExecutionContextKey:      DATA_SPACE__EXECUTION_CONTEXT_KEY COLON STRING SEMI_COLON
;

// NOTE: we would need to potentially come up with extension mechanism later
// if we are to support more types of support info
supportInfo:                        DATA_SPACE_SUPPORT_INFO COLON
                                        (
                                            supportEmail
                                            | supportCombinedInfo
                                        )
                                    SEMI_COLON
;
supportDocumentationUrl:            DATA_SPACE_SUPPORT_DOC_URL COLON STRING SEMI_COLON
;
supportEmail:                       DATA_SPACE_SUPPORT_EMAIL
                                        BRACE_OPEN
                                            (
                                                supportDocumentationUrl
                                                | supportEmailAddress
                                            )*
                                        BRACE_CLOSE
;
supportEmailAddress:                DATA_SPACE_SUPPORT_EMAIL_ADDRESS COLON STRING SEMI_COLON
;

supportCombinedInfo:                DATA_SPACE_SUPPORT_COMBINED_INFO
                                        BRACE_OPEN
                                            (
                                                supportDocumentationUrl
                                                | combinedInfoEmails
                                                | combinedInfoWebsite
                                                | combinedInfoFaqUrl
                                                | combinedInfoSupportUrl
                                            )*
                                        BRACE_CLOSE
;
combinedInfoEmails:                 DATA_SPACE_SUPPORT_EMAILS COLON BRACKET_OPEN ( STRING (COMMA STRING)* )? BRACKET_CLOSE SEMI_COLON
;
combinedInfoWebsite:                DATA_SPACE_SUPPORT_WEBSITE COLON STRING SEMI_COLON
;
combinedInfoFaqUrl:                 DATA_SPACE_SUPPORT_FAQ_URL COLON STRING SEMI_COLON
;
combinedInfoSupportUrl:             DATA_SPACE_SUPPORT_SUPPORT_URL COLON STRING SEMI_COLON
;


// -------------------------------------- DEPRECATED --------------------------------------

groupId:                            DATA_SPACE_GROUP_ID COLON STRING SEMI_COLON
;
artifactId:                         DATA_SPACE_ARTIFACT_ID COLON STRING SEMI_COLON
;
versionId:                          DATA_SPACE_VERSION_ID COLON STRING SEMI_COLON
;
featuredDiagrams:                   DATA_SPACE_FEATURED_DIAGRAMS COLON BRACKET_OPEN ( qualifiedName (COMMA qualifiedName)* )? BRACKET_CLOSE SEMI_COLON
;