parser grammar DataProductParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = DataProductLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                         VALID_STRING | STRING
                                    | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE | TO_BYTES_FUNCTION      // from M3Parser
                                    | STEREOTYPES | TAGS
                                    | DATA_PRODUCT
                                    | DATA_PRODUCT__NAME
                                    | DATA_PRODUCT__DESCRIPTION
                                    | DATA_PRODUCT__TITLE
                                    | DATA_PRODUCT_EXECUTION_CONTEXTS
                                    | DATA_PRODUCT_DEFAULT_EXECUTION_CONTEXT
                                    | DATA_PRODUCT_MAPPING
                                    | DATA_PRODUCT_DEFAULT_RUNTIME
                                    | DATA_PRODUCT_TEST_DATA
                                    | DATA_PRODUCT_DIAGRAMS
                                    | DATA_PRODUCT_DIAGRAM
                                    | DATA_PRODUCT_ELEMENTS
                                    | DATA_PRODUCT_EXECUTABLES
                                    | DATA_PRODUCT_EXECUTABLE
                                    | DATA_PRODUCT__TEMPLATE_QUERY
                                    | DATA_PRODUCT__EXECUTABLE__ID
                                    | DATA_PRODUCT__EXECUTION_CONTEXT_KEY
                                    | DATA_PRODUCT_SUPPORT_INFO
                                    | DATA_PRODUCT_SUPPORT_DOC_URL
                                    | DATA_PRODUCT_SUPPORT_EMAIL
                                    | DATA_PRODUCT_SUPPORT_EMAIL_ADDRESS
                                    | DATA_PRODUCT_SUPPORT_COMBINED_INFO
                                    | DATA_PRODUCT_SUPPORT_EMAILS
                                    | DATA_PRODUCT_SUPPORT_WEBSITE
                                    | DATA_PRODUCT_SUPPORT_FAQ_URL
                                    | DATA_PRODUCT_SUPPORT_SUPPORT_URL

                                    // deprecated
                                    | DATA_PRODUCT_GROUP_ID
                                    | DATA_PRODUCT_ARTIFACT_ID
                                    | DATA_PRODUCT_VERSION_ID
                                    | DATA_PRODUCT_FEATURED_DIAGRAMS
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                         (dataProductElement)*
                                    EOF
;
dataProductElement:                   DATA_PRODUCT stereotypes? taggedValues? qualifiedName
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

title:                              DATA_PRODUCT__TITLE COLON STRING SEMI_COLON
;
description:                        DATA_PRODUCT__DESCRIPTION COLON STRING SEMI_COLON
;
executionContexts:                  DATA_PRODUCT_EXECUTION_CONTEXTS COLON BRACKET_OPEN ( executionContext (COMMA executionContext)* )? BRACKET_CLOSE SEMI_COLON
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
executionContextName:               DATA_PRODUCT__NAME COLON STRING SEMI_COLON
;
executionContextTitle:              DATA_PRODUCT__TITLE COLON STRING SEMI_COLON
;
executionContextDescription:        DATA_PRODUCT__DESCRIPTION COLON STRING SEMI_COLON
;
executionContextMapping:            DATA_PRODUCT_MAPPING COLON qualifiedName SEMI_COLON
;
executionContextTestData:           DATA_PRODUCT_TEST_DATA COLON embeddedData SEMI_COLON
;
embeddedData:                       identifier ISLAND_OPEN (embeddedDataContent)*
;
embeddedDataContent:                ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;
executionContextDefaultRuntime:     DATA_PRODUCT_DEFAULT_RUNTIME COLON qualifiedName SEMI_COLON
;
defaultExecutionContext:            DATA_PRODUCT_DEFAULT_EXECUTION_CONTEXT COLON STRING SEMI_COLON
;
diagrams:                           DATA_PRODUCT_DIAGRAMS COLON BRACKET_OPEN ( diagram (COMMA diagram)* )? BRACKET_CLOSE SEMI_COLON
;
diagram:                            BRACE_OPEN
                                        (
                                            diagramTitle
                                            | diagramDescription
                                            | diagramPath
                                        )*
                                    BRACE_CLOSE
;
diagramTitle:                       DATA_PRODUCT__TITLE COLON STRING SEMI_COLON
;
diagramDescription:                 DATA_PRODUCT__DESCRIPTION COLON STRING SEMI_COLON
;
diagramPath:                        DATA_PRODUCT_DIAGRAM COLON qualifiedName SEMI_COLON
;
elements:                           DATA_PRODUCT_ELEMENTS COLON BRACKET_OPEN ( elementScopePath (COMMA elementScopePath)* )? BRACKET_CLOSE SEMI_COLON
;
elementScopePath:                   ( MINUS )? qualifiedName
;
executables:                        DATA_PRODUCT_EXECUTABLES COLON BRACKET_OPEN ( executable (COMMA executable)* )? BRACKET_CLOSE SEMI_COLON
;
executable:                         BRACE_OPEN
                                        (
                                            executableTitle
                                            | executableDescription
                                            | executablePath
                                            | executableTemplateQuery
                                            | executableId
                                            | executableExecutionContextKey
                                        )*
                                    BRACE_CLOSE
;
executableTitle:                    DATA_PRODUCT__TITLE COLON STRING SEMI_COLON
;
executableDescription:              DATA_PRODUCT__DESCRIPTION COLON STRING SEMI_COLON
;
executablePath:                     DATA_PRODUCT_EXECUTABLE COLON (qualifiedName | functionIdentifier) SEMI_COLON
;
executableTemplateQuery:            DATA_PRODUCT__TEMPLATE_QUERY COLON combinedExpression SEMI_COLON
;
executableId:                       DATA_PRODUCT__EXECUTABLE__ID COLON (VALID_STRING | INTEGER | DECIMAL) SEMI_COLON
;
executableExecutionContextKey:      DATA_PRODUCT__EXECUTION_CONTEXT_KEY COLON STRING SEMI_COLON
;

// NOTE: we would need to potentially come up with extension mechanism later
// if we are to support more types of support info
supportInfo:                        DATA_PRODUCT_SUPPORT_INFO COLON
                                        (
                                            supportEmail
                                            | supportCombinedInfo
                                        )
                                    SEMI_COLON
;
supportDocumentationUrl:            DATA_PRODUCT_SUPPORT_DOC_URL COLON STRING SEMI_COLON
;
supportEmail:                       DATA_PRODUCT_SUPPORT_EMAIL
                                        BRACE_OPEN
                                            (
                                                supportDocumentationUrl
                                                | supportEmailAddress
                                            )*
                                        BRACE_CLOSE
;
supportEmailAddress:                DATA_PRODUCT_SUPPORT_EMAIL_ADDRESS COLON STRING SEMI_COLON
;

supportCombinedInfo:                DATA_PRODUCT_SUPPORT_COMBINED_INFO
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
combinedInfoEmails:                 DATA_PRODUCT_SUPPORT_EMAILS COLON BRACKET_OPEN ( STRING (COMMA STRING)* )? BRACKET_CLOSE SEMI_COLON
;
combinedInfoWebsite:                DATA_PRODUCT_SUPPORT_WEBSITE COLON STRING SEMI_COLON
;
combinedInfoFaqUrl:                 DATA_PRODUCT_SUPPORT_FAQ_URL COLON STRING SEMI_COLON
;
combinedInfoSupportUrl:             DATA_PRODUCT_SUPPORT_SUPPORT_URL COLON STRING SEMI_COLON
;


// -------------------------------------- DEPRECATED --------------------------------------

groupId:                            DATA_PRODUCT_GROUP_ID COLON STRING SEMI_COLON
;
artifactId:                         DATA_PRODUCT_ARTIFACT_ID COLON STRING SEMI_COLON
;
versionId:                          DATA_PRODUCT_VERSION_ID COLON STRING SEMI_COLON
;
featuredDiagrams:                   DATA_PRODUCT_FEATURED_DIAGRAMS COLON BRACKET_OPEN ( qualifiedName (COMMA qualifiedName)* )? BRACKET_CLOSE SEMI_COLON
;
