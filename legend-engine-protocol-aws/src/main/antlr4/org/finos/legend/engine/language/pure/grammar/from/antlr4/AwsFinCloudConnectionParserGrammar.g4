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
                                            | FINCLOUD_DATASET_ID | FINCLOUD_AUTHENTICATION_STRATEGY | FINCLOUD_TARGET_SPECIFICATION
                                            | FINCLOUD_DATASOURCE_SPECIFICATION | FINCLOUD_API_URL
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 (
                                                connectionStore
                                                | datasetId
                                                | authenticationStrategy
                                                | targetSpecification
                                            )*
                                            EOF
;
connectionStore:                            STORE COLON qualifiedName SEMI_COLON
;
datasetId:                                  FINCLOUD_DATASET_ID COLON STRING SEMI_COLON
;
authenticationStrategy:                    FINCLOUD_AUTHENTICATION_STRATEGY COLON STRING SEMI_COLON
;
targetSpecification:                       FINCLOUD_TARGET_SPECIFICATION COLON
                                                (
                                                    datasourceSpecification
                                                )
                                           SEMI_COLON
;
datasourceSpecification:                   FINCLOUD_DATASOURCE_SPECIFICATION
                                            BRACE_OPEN
                                            (apiUrl)*
                                            BRACE_CLOSE
;
apiUrl:                                    FINCLOUD_API_URL COLON STRING SEMI_COLON
;
