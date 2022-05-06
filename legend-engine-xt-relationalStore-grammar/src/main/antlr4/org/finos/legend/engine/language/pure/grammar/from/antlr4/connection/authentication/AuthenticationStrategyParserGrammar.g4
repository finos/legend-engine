parser grammar AuthenticationStrategyParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AuthenticationStrategyLexerGrammar;
}

identifier:                      VALID_STRING
;

// ----------------------------- RELATIONAL DATABASE CONNECTION AUTH STRATEGY -----------------------------

apiTokenAuth:                          API_TOKEN_AUTH
                                            BRACE_OPEN
                                                (
                                                    apiToken
                                                )*
                                            BRACE_CLOSE
;

apiToken:                               API_TOKEN_AUTH_TOKEN COLON STRING SEMI_COLON
;

defaultH2Auth:                          H2_DEFAULT_AUTH
;
testDBAuth:                             TEST_DB_AUTH
;
delegatedKerberosAuth:                  DELEGATED_KERBEROS_AUTH delegatedKerberosAuthConfig?
;
delegatedKerberosAuthConfig:            BRACE_OPEN
                                            (
                                                serverPrincipalConfig
                                            )*
                                        BRACE_CLOSE
;
serverPrincipalConfig:                  SERVER_PRINCIPAL COLON STRING SEMI_COLON
;
userNamePasswordAuth:                   USERNAME_PASSWORD_AUTH
                                            BRACE_OPEN
                                                (
                                                    userNamePasswordAuthBaseVaultRef
                                                    | userNamePasswordAuthUserNameVaultRef
                                                    | userNamePasswordAuthPasswordVaultRef
                                                )*
                                            BRACE_CLOSE
;
userNamePasswordAuthBaseVaultRef:       USERNAME_PASSWORD_AUTH_BASE_VAULT_REF COLON STRING SEMI_COLON
;
userNamePasswordAuthUserNameVaultRef:   USERNAME_PASSWORD_AUTH_USERNAME_VAULT_REF COLON STRING SEMI_COLON
;
userNamePasswordAuthPasswordVaultRef:   USERNAME_PASSWORD_AUTH_PASSWORD_VAULT_REF COLON STRING SEMI_COLON
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

gcpApplicationDefaultCredentialsAuth : GCP_APPLICATION_DEFAULT_CREDENTIALS_AUTH
;