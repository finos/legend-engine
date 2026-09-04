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
                                    | DATA_SPACE_MAPPING_PROVIDER
                                    | DATA_SPACE_DEFAULT_RUNTIME
                                    | DATA_SPACE_TEST_DATA
                                    | DATA_SPACE_DIAGRAMS
                                    | DATA_SPACE_DIAGRAM
                                    | DATA_SPACE_ELEMENTS
                                    | DATA_SPACE_EXECUTABLES
                                    | DATA_SPACE_EXECUTABLE
                                    | DATA_SPACE_EXECUTABLE_SAMPLE_VALUES
                                    | DATA_SPACE__TEMPLATE_QUERY
                                    | DATA_SPACE__EXECUTABLE__ID
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

                                    | DATA_SPACE_SUPPORT_DOCUMENTATION
                                    | DATA_SPACE_SUPPORT_LINK_LABEL
                                    | DATA_SPACE_SUPPORT_LINK_URL
                                    | DATA_SPACE_SUPPORT_EXPERTISE
                                    | DATA_SPACE_SUPPORT_EXPERT_IDS

                                    | DATA_SPACE_OPERATIONAL_METADATA
                                    | DATA_SPACE_OM_COVERAGE_REGIONS
                                    | DATA_SPACE_OM_UPDATE_FREQUENCY

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
dataSpaceElement:                   documentation? DATA_SPACE stereotypes? taggedValues? qualifiedName
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
                                                | operationalMetadata

                                                // deprecated
                                                | groupId
                                                | artifactId
                                                | versionId
                                                | featuredDiagrams
                                            )*
                                        BRACE_CLOSE
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
                                            | executionContextMappingProvider
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
executionContextMappingProvider:    DATA_SPACE_MAPPING_PROVIDER COLON qualifiedName (DOT identifier (COMMA identifier)*)? SEMI_COLON
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
                                            | executableId
                                            | executableExecutionContextKey
                                            | executableSampleValues
                                        )*
                                    BRACE_CLOSE
;
executableTitle:                    DATA_SPACE__TITLE COLON STRING SEMI_COLON
;
executableDescription:              DATA_SPACE__DESCRIPTION COLON STRING SEMI_COLON
;
executablePath:                     DATA_SPACE_EXECUTABLE COLON (qualifiedName | functionIdentifier) SEMI_COLON
;
executableTemplateQuery:            DATA_SPACE__TEMPLATE_QUERY COLON combinedExpression SEMI_COLON
;
executableId:                       DATA_SPACE__EXECUTABLE__ID COLON (VALID_STRING | INTEGER | DECIMAL) SEMI_COLON
;
executableExecutionContextKey:      DATA_SPACE__EXECUTION_CONTEXT_KEY COLON STRING SEMI_COLON
;
executableSampleValues:             DATA_SPACE_EXECUTABLE_SAMPLE_VALUES COLON embeddedData SEMI_COLON
;

// NOTE: we would need to potentially come up with extension mechanism later
// if we are to support more types of support info
supportInfo:                        DATA_SPACE_SUPPORT_INFO COLON
                                        (
                                            supportEmail
                                            | supportCombinedInfo
                                            | supportFullInfo
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

// ---- Full (keyword-less) support info: supportInfo: { ... }; ----
supportFullInfo:                    BRACE_OPEN
                                        (
                                            fullInfoDocumentation
                                            | fullInfoWebsite
                                            | fullInfoFaqUrl
                                            | fullInfoSupportUrl
                                            | fullInfoEmails
                                            | fullInfoExpertise
                                        )*
                                    BRACE_CLOSE
;
fullInfoDocumentation:              DATA_SPACE_SUPPORT_DOCUMENTATION COLON linkValue SEMI_COLON
;
fullInfoWebsite:                    DATA_SPACE_SUPPORT_WEBSITE COLON linkValue SEMI_COLON
;
fullInfoFaqUrl:                     DATA_SPACE_SUPPORT_FAQ_URL COLON linkValue SEMI_COLON
;
fullInfoSupportUrl:                 DATA_SPACE_SUPPORT_SUPPORT_URL COLON linkValue SEMI_COLON
;
linkValue:                          BRACE_OPEN
                                        (
                                            linkLabel
                                            | linkUrl
                                        )*
                                    BRACE_CLOSE
;
linkLabel:                          DATA_SPACE_SUPPORT_LINK_LABEL COLON STRING SEMI_COLON
;
linkUrl:                            DATA_SPACE_SUPPORT_LINK_URL COLON STRING SEMI_COLON
;
fullInfoEmails:                     DATA_SPACE_SUPPORT_EMAILS COLON BRACKET_OPEN ( emailValue (COMMA emailValue)* )? BRACKET_CLOSE SEMI_COLON
;
emailValue:                         BRACE_OPEN
                                        (
                                            emailTitle
                                            | emailAddress
                                        )*
                                    BRACE_CLOSE
;
emailTitle:                         DATA_SPACE__TITLE COLON STRING SEMI_COLON
;
emailAddress:                       DATA_SPACE_SUPPORT_EMAIL_ADDRESS COLON STRING SEMI_COLON
;
fullInfoExpertise:                  DATA_SPACE_SUPPORT_EXPERTISE COLON BRACKET_OPEN ( expertiseValue (COMMA expertiseValue)* )? BRACKET_CLOSE SEMI_COLON
;
expertiseValue:                     BRACE_OPEN
                                        (
                                            expertiseDescription
                                            | expertiseExpertIds
                                        )*
                                    BRACE_CLOSE
;
expertiseDescription:               DATA_SPACE__DESCRIPTION COLON STRING SEMI_COLON
;
expertiseExpertIds:                 DATA_SPACE_SUPPORT_EXPERT_IDS COLON BRACKET_OPEN ( STRING (COMMA STRING)* )? BRACKET_CLOSE SEMI_COLON
;


// -------------------------------------- OPERATIONAL METADATA --------------------------------------

operationalMetadata:                DATA_SPACE_OPERATIONAL_METADATA COLON
                                        BRACE_OPEN
                                            (
                                                omCoverageRegions
                                                | omUpdateFrequency
                                            )*
                                        BRACE_CLOSE
                                    SEMI_COLON
;
omCoverageRegions:                  DATA_SPACE_OM_COVERAGE_REGIONS COLON BRACKET_OPEN ( identifier (COMMA identifier)* )? BRACKET_CLOSE SEMI_COLON
;
omUpdateFrequency:                  DATA_SPACE_OM_UPDATE_FREQUENCY COLON identifier SEMI_COLON
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