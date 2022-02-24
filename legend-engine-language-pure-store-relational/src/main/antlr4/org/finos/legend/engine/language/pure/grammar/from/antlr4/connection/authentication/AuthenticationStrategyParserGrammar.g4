parser grammar AuthenticationStrategyParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AuthenticationStrategyLexerGrammar;
}

identifier:                      VALID_STRING
;

// ----------------------------- RELATIONAL DATABASE CONNECTION AUTH STRATEGY -----------------------------

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

gcpApplicationDefaultCredentialsAuth : GCP_APPLICATION_DEFAULT_CREDENTIALS_AUTH SEMI_COLON
;

gcpWorkloadIdentityFederationWithAWSAuth: GCP_WORKLOAD_IDENTITY_FEDERATION_WITH_AWS_AUTH
                                    BRACE_OPEN
                                        (
                                            workloadProjectNumberRef
                                            | serviceAccountEmailRef
                                            | additionalGcpScopesRef
                                            | workloadPoolIdRef
                                            | workloadProviderIdRef
                                            | awsAccountIdRef
                                            | awsRegionRef
                                            | awsRoleRef
                                            | awsAccessKeyIdVaultRef
                                            | awsSecretAccessKeyVaultRef
                                        )*
                                    BRACE_CLOSE
;

workloadProjectNumberRef:                WORKLOAD_PROJECT_NUMBER COLON STRING SEMI_COLON
;

serviceAccountEmailRef:          SERVICE_ACCOUNT_EMAIL COLON STRING SEMI_COLON
;

additionalGcpScopesRef:                            ADDITIONAL_GCP_SCOPES COLON gcpScopesArray SEMI_COLON
;

gcpScopesArray:                                  BRACKET_OPEN ( STRING (COMMA STRING)* )? BRACKET_CLOSE
;

workloadPoolIdRef:                       WORKLOAD_POOL_ID COLON STRING SEMI_COLON
;

workloadProviderIdRef:                   WORKLOAD_PROVIDER_ID COLON STRING SEMI_COLON
;

awsAccountIdRef:                 AWS_ACCOUNT_ID COLON STRING SEMI_COLON
;

awsRegionRef:                     AWS_REGION COLON STRING SEMI_COLON
;

awsRoleRef:                     AWS_ROLE COLON STRING SEMI_COLON
;

awsAccessKeyIdVaultRef:         AWS_ACCESS_KEY_ID_VAULT_REFERENCE COLON STRING SEMI_COLON
;

awsSecretAccessKeyVaultRef:             AWS_SECRET_ACCESS_KEY_VAULT_REFERENCE COLON STRING SEMI_COLON
;
