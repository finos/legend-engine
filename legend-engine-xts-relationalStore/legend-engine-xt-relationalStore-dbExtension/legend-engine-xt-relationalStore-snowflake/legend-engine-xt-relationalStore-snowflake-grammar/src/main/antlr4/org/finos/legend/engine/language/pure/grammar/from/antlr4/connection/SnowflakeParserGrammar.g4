parser grammar SnowflakeParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = SnowflakeLexerGrammar;
}

identifier:                      VALID_STRING
;

// -------------------------------------- DEFINITION --------------------------------------

snowflakeDatasourceSpecification:           SNOWFLAKE
                                                BRACE_OPEN
                                                    (
                                                        dbName
                                                        | dbAccount
                                                        | dbWarehouse
                                                        | snowflakeRegion
                                                        | cloudType
                                                        | snowflakeQuotedIdentifiersIgnoreCase
                                                        | dbProxyHost
                                                        | dbProxyPort
                                                        | dbNonProxyHosts
                                                        | dbTempTableDb
                                                        | dbTempTableSchema
                                                        | dbAccountType
                                                        | dbOrganization
                                                        | dbRole
                                                        | enableQueryTags
                                                    )*
                                                BRACE_CLOSE
;
dbName:                                     NAME COLON STRING SEMI_COLON
;
dbWarehouse:                                WAREHOUSE COLON STRING SEMI_COLON
;
dbAccount:                                  ACCOUNT COLON STRING SEMI_COLON
;
dbProxyHost:                                PROXYHOST COLON STRING SEMI_COLON
;
dbProxyPort:                                PROXYPORT COLON STRING SEMI_COLON
;
dbNonProxyHosts:                            NONPROXYHOSTS COLON STRING SEMI_COLON
;
dbTempTableDb:                              TEMPTABLEDB COLON STRING SEMI_COLON
;
dbTempTableSchema:                          TEMPTABLESCHEMA COLON STRING SEMI_COLON
;
dbAccountType:                              ACCOUNTTYPE COLON identifier SEMI_COLON
;
dbOrganization:                             ORGANIZATION COLON STRING SEMI_COLON
;
snowflakeRegion:                            REGION COLON STRING SEMI_COLON
;
cloudType:                                  CLOUDTYPE COLON STRING SEMI_COLON
;
snowflakeQuotedIdentifiersIgnoreCase:       QUOTED_IDENTIFIERS_IGNORE_CASE COLON BOOLEAN SEMI_COLON
;
dbRole:                                     ROLE COLON STRING SEMI_COLON
;
enableQueryTags:                            ENABLE_QUERY_TAGS COLON BOOLEAN SEMI_COLON
;

snowflakePublicAuth:                    SNOWFLAKE_PUBLIC_AUTH
                                            BRACE_OPEN
                                                (
                                                    snowflakePublicAuthKeyVaultRef
                                                    | snowflakePublicAuthPassPhraseVaultRef
                                                    | snowflakePublicAuthUserName
                                                )*
                                            BRACE_CLOSE
;

snowflakePublicAuthKeyVaultRef:         SNOWFLAKE_AUTH_KEY_VAULT_REFERENCE COLON STRING SEMI_COLON
;

snowflakePublicAuthPassPhraseVaultRef:  SNOWFLAKE_AUTH_PASSPHRASE_VAULT_REFERENCE COLON STRING SEMI_COLON
;

snowflakePublicAuthUserName:  SNOWFLAKE_AUTH_PUBLIC_USERNAME COLON STRING SEMI_COLON
;
