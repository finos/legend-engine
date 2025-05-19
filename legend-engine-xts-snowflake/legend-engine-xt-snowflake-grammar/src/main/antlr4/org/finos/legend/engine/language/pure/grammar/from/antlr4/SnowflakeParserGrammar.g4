parser grammar SnowflakeParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = SnowflakeLexerGrammar;
}

identifier:     VALID_STRING | STRING |
                SNOWFLAKE_APP | SNOWFLAKE_M2M_UDF |
                SNOWFLAKE_APP__APPLICATION_NAME | SNOWFLAKE_M2M_UDF__UDF_NAME |
                SNOWFLAKE_APP__CONFIGURATION | SNOWFLAKE_M2M_UDF__CONFIGURATION |
                SNOWFLAKE__DESCRIPTION |
                SNOWFLAKE__FUNCTION |
                SNOWFLAKE__OWNER |
                SNOWFLAKE__OWNER_DEPLOYMENT |
                SNOWFLAKE__OWNER_DEPLOYMENT_ID |
                SNOWFLAKE__ACTIVATION|
                SNOWFLAKE__USAGE_ROLE |
                SNOWFLAKE__PERMISSION |
                SNOWFLAKE__DEPLOYMENT_SCHEMA |
                DEPLOYMENT_STAGE |
                ACTIVATION_CONNECTION |
                ALL |
                LET |
                ALL_VERSIONS |
                ALL_VERSIONS_IN_RANGE |
                TO_BYTES_FUNCTION
                ;
// -------------------------------------- DEFINITION --------------------------------------

definition:                         (snowflakeApp| snowflakeM2MUdf | deploymentConfig)*
                                    EOF
;
snowflakeApp:                   SNOWFLAKE_APP stereotypes? taggedValues? qualifiedName
                                        BRACE_OPEN
                                            (
                                                applicationName
                                                | description
                                                | function
                                                | ownership
                                                | activation
                                                | role
                                                | scheme
                                                | deploymentSchema
                                            )*
                                        BRACE_CLOSE;

snowflakeM2MUdf:                   SNOWFLAKE_M2M_UDF stereotypes? taggedValues? qualifiedName
                                        BRACE_OPEN
                                            (
                                                udfName
                                                | description
                                                | function
                                                | ownership
                                                | activation
                                                | role
                                                | scheme
                                                | deploymentSchema
                                                | deploymentStage
                                            )*
                                        BRACE_CLOSE;

stereotypes:                        LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN;
stereotype:                         qualifiedName DOT identifier;
taggedValues:                       BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE;
taggedValue:                        qualifiedName DOT identifier EQUAL STRING;

applicationName:                SNOWFLAKE_APP__APPLICATION_NAME COLON STRING SEMI_COLON;

udfName:                        SNOWFLAKE_M2M_UDF__UDF_NAME COLON STRING SEMI_COLON;

description:                    SNOWFLAKE__DESCRIPTION COLON STRING SEMI_COLON;

scheme:                         SNOWFLAKE__PERMISSION COLON identifier SEMI_COLON;

role:                           SNOWFLAKE__USAGE_ROLE COLON STRING SEMI_COLON;

function:                       SNOWFLAKE__FUNCTION COLON functionIdentifier SEMI_COLON;

ownership :                         SNOWFLAKE__OWNER COLON
                                    SNOWFLAKE__OWNER_DEPLOYMENT
                                        BRACE_OPEN
                                            SNOWFLAKE__OWNER_DEPLOYMENT_ID COLON STRING
                                        BRACE_CLOSE SEMI_COLON;

activation:                     SNOWFLAKE__ACTIVATION COLON qualifiedName SEMI_COLON ;

deploymentSchema:               SNOWFLAKE__DEPLOYMENT_SCHEMA COLON STRING SEMI_COLON;

// ----------------------------------- Deployment ------------------------------------------------------
deploymentConfig:                      SNOWFLAKE_APP__CONFIGURATION qualifiedName
                                            BRACE_OPEN
                                                activationConnection
                                            BRACE_CLOSE
;

activationConnection:                   ACTIVATION_CONNECTION COLON qualifiedName SEMI_COLON
;

deploymentStage:                        DEPLOYMENT_STAGE COLON STRING SEMI_COLON
;