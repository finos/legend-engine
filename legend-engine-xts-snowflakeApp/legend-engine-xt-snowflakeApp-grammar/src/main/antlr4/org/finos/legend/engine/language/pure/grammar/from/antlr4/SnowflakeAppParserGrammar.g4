parser grammar SnowflakeAppParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = SnowflakeAppLexerGrammar;
}

identifier:     VALID_STRING | STRING |
                SNOWFLAKE_APP |
                SNOWFLAKE_APP__APPLICATION_NAME |
                SNOWFLAKE_APP__DESCRIPTION |
                SNOWFLAKE_APP__FUNCTION |
                SNOWFLAKE_APP__OWNER |
                SNOWFLAKE_APP__OWNER_DEPLOYMENT |
                SNOWFLAKE_APP__OWNER_DEPLOYMENT_ID |
                SNOWFLAKE_APP__ACTIVATION|
                CONFIGURATION| DEPLOYMENT_STAGE
                | ACTIVATION_CONNECTION |
                ALL |
                LET |
                ALL_VERSIONS |
                ALL_VERSIONS_IN_RANGE |
                TO_BYTES_FUNCTION
                ;
// -------------------------------------- DEFINITION --------------------------------------

definition:                         (snowflakeApp| deploymentConfig)*
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
                                            )*
                                        BRACE_CLOSE;

stereotypes:                        LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN;
stereotype:                         qualifiedName DOT identifier;
taggedValues:                       BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE;
taggedValue:                        qualifiedName DOT identifier EQUAL STRING;

applicationName:                SNOWFLAKE_APP__APPLICATION_NAME COLON STRING SEMI_COLON;

description:                    SNOWFLAKE_APP__DESCRIPTION COLON STRING SEMI_COLON;

function:                       SNOWFLAKE_APP__FUNCTION COLON functionIdentifier SEMI_COLON;

ownership :                         SNOWFLAKE_APP__OWNER COLON
                                    SNOWFLAKE_APP__OWNER_DEPLOYMENT
                                        BRACE_OPEN
                                            SNOWFLAKE_APP__OWNER_DEPLOYMENT_ID COLON STRING
                                        BRACE_CLOSE SEMI_COLON;

activation:                     SNOWFLAKE_APP__ACTIVATION COLON qualifiedName SEMI_COLON ;

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