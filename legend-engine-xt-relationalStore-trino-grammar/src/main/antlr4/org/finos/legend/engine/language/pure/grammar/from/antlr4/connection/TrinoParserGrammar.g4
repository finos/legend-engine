parser grammar TrinoParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = TrinoLexerGrammar;
}

identifier:                                 VALID_STRING
;
trinoDatasourceSpecification:               TRINO
                                                BRACE_OPEN
                                                    (
                                                        trinoHost
                                                        | trinoPort
                                                        | trinoCatalog
                                                        | trinoSchema
                                                        | trinoClientTags
                                                        | trinoSSL
                                                        | trinoTrustStorePathVaultReference
                                                        | trinoTrustStorePasswordVaultReference
                                                        | trinoKerberosRemoteServiceName
                                                        | trinoKerberosUseCanonicalHostname
                                                        | baseVaultRef
                                                        | userNameVaultRef
                                                        | passwordVaultRef
                                                    )*
                                                BRACE_CLOSE
;

trinoHost:                                  HOST COLON STRING SEMI_COLON
;

trinoPort:                                  PORT COLON INTEGER SEMI_COLON
;

trinoCatalog:                               CATALOG COLON STRING SEMI_COLON
;

trinoSchema:                               SCHEMA COLON STRING SEMI_COLON
;

trinoClientTags:                            CLIENT_TAGS COLON STRING SEMI_COLON
;

trinoSSL:                                   SSL COLON BOOLEAN SEMI_COLON
;

trinoTrustStorePathVaultReference:          TRUST_STORE_PATH_VAULT_REFERENCE COLON STRING SEMI_COLON
;

trinoTrustStorePasswordVaultReference:       TRUST_STORE_PASSWORD_VAULT_REFERENCE COLON STRING SEMI_COLON
;

trinoKerberosRemoteServiceName:             KERBEROS_REMOTE_SERVICE_NAME COLON STRING SEMI_COLON
;

trinoKerberosUseCanonicalHostname:          KERBEROS_USE_CANONICAL_HOSTNAME COLON BOOLEAN SEMI_COLON
;

baseVaultRef:                           BASE_VAULT_REF COLON STRING SEMI_COLON
;
userNameVaultRef:                       USERNAME_VAULT_REF COLON STRING SEMI_COLON
;
passwordVaultRef:                       PASSWORD_VAULT_REF COLON STRING SEMI_COLON
;


