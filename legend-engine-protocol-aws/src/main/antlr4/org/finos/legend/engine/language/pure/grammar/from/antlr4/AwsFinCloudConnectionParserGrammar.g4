parser grammar AwsFinCloudConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AwsFinCloudConnectionLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING
                                            | STORE
                                            | IMPORT | NONE
                                            | FINCLOUD_DATASET_ID | FINCLOUD_AUTHENTICATION_STRATEGY
                                            | FINCLOUD_API_URL | FINCLOUD_QUERY_INFO
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 (
                                                connectionStore
                                                | datasetId
                                                | authenticationStrategy
                                                | apiUrl
                                                | queryInfo
                                            )*
                                            EOF
;
connectionStore:                            STORE COLON qualifiedName SEMI_COLON
;
datasetId:                                  FINCLOUD_DATASET_ID COLON STRING SEMI_COLON
;
authenticationStrategy:                     FINCLOUD_AUTHENTICATION_STRATEGY COLON specification SEMI_COLON
;
apiUrl:                                     FINCLOUD_API_URL COLON STRING SEMI_COLON
;
queryInfo:                                  FINCLOUD_QUERY_INFO COLON STRING SEMI_COLON
;

specification:                specificationType (specificationValueBody)?
;

specificationType:            VALID_STRING
;

specificationValueBody:       BRACE_OPEN (specificationValue)*
;

specificationValue:           SPECIFICATION_BRACE_OPEN | SPECIFICATION_CONTENT | SPECIFICATION_BRACE_CLOSE
;