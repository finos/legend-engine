parser grammar BigQueryFunctionParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = BigQueryFunctionLexerGrammar;
}

identifier:     VALID_STRING | STRING |
                BIGQUERY_FUNCTION |
                BIGQUERY_FUNCTION__FUNCTION_NAME |
                BIGQUERY_FUNCTION__DESCRIPTION |
                BIGQUERY_FUNCTION__FUNCTION |
                BIGQUERY_FUNCTION__OWNER |
                BIGQUERY_FUNCTION__OWNER_DEPLOYMENT |
                BIGQUERY_FUNCTION__OWNER_DEPLOYMENT_ID |
                BIGQUERY_FUNCTION__ACTIVATION|
                CONFIGURATION| DEPLOYMENT_STAGE
                | ACTIVATION_CONNECTION |
                ALL |
                LET |
                ALL_VERSIONS |
                ALL_VERSIONS_IN_RANGE |
                TO_BYTES_FUNCTION
                ;
// -------------------------------------- DEFINITION --------------------------------------

definition:                         (bigQueryFunction | deploymentConfig)*
                                    EOF
;
bigQueryFunction:               BIGQUERY_FUNCTION stereotypes? taggedValues? qualifiedName
                                        BRACE_OPEN
                                            (
                                                functionName
                                                | description
                                                | function
                                                | owner
                                                | activation
                                            )*
                                        BRACE_CLOSE;

stereotypes:                        LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN;
stereotype:                         qualifiedName DOT identifier;
taggedValues:                       BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE;
taggedValue:                        qualifiedName DOT identifier EQUAL STRING;

functionName:                   BIGQUERY_FUNCTION__FUNCTION_NAME COLON STRING SEMI_COLON;

description:                    BIGQUERY_FUNCTION__DESCRIPTION COLON STRING SEMI_COLON;

function:                       BIGQUERY_FUNCTION__FUNCTION COLON functionIdentifier SEMI_COLON;

owner :                         BIGQUERY_FUNCTION__OWNER COLON
                                    BIGQUERY_FUNCTION__OWNER_DEPLOYMENT
                                        BRACE_OPEN
                                            BIGQUERY_FUNCTION__OWNER_DEPLOYMENT_ID COLON STRING
                                        BRACE_CLOSE SEMI_COLON;

activation:                     BIGQUERY_FUNCTION__ACTIVATION COLON qualifiedName SEMI_COLON ;

// ----------------------------------- Deployment ------------------------------------------------------
deploymentConfig:                      CONFIGURATION qualifiedName
                                            BRACE_OPEN
                                                activationConnection
                                            BRACE_CLOSE
;

activationConnection:                   ACTIVATION_CONNECTION COLON qualifiedName SEMI_COLON
;

stage:                                  DEPLOYMENT_STAGE COLON STRING SEMI_COLON
;