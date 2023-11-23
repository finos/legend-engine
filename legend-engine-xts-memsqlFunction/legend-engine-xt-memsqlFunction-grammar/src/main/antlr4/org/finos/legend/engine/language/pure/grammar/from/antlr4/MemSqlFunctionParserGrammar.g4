parser grammar MemSqlFunctionParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = MemSqlFunctionLexerGrammar;
}

identifier:     VALID_STRING | STRING |
                MEMSQL_FUNCTION |
                MEMSQL_FUNCTION__FUNCTION_NAME |
                MEMSQL_FUNCTION__DESCRIPTION |
                MEMSQL_FUNCTION__FUNCTION |
                MEMSQL_FUNCTION__OWNER |
                MEMSQL_FUNCTION__ACTIVATION|
                CONFIGURATION| DEPLOYMENT_STAGE
                | ACTIVATION_CONNECTION |
                ALL |
                LET |
                ALL_VERSIONS |
                ALL_VERSIONS_IN_RANGE |
                TO_BYTES_FUNCTION
                ;
// -------------------------------------- DEFINITION --------------------------------------

definition:                         (memSqlFunction | deploymentConfig)*
                                    EOF
;
memSqlFunction:               MEMSQL_FUNCTION stereotypes? taggedValues? qualifiedName
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

functionName:                   MEMSQL_FUNCTION__FUNCTION_NAME COLON STRING SEMI_COLON;

description:                    MEMSQL_FUNCTION__DESCRIPTION COLON STRING SEMI_COLON;

function:                       MEMSQL_FUNCTION__FUNCTION COLON functionIdentifier SEMI_COLON;

owner :                         MEMSQL_FUNCTION__OWNER COLON STRING SEMI_COLON;

activation:                     MEMSQL_FUNCTION__ACTIVATION COLON qualifiedName SEMI_COLON ;

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