parser grammar AuthenticationParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AuthenticationLexerGrammar;
}

identifier:                      VALID_STRING
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 imports
                                                elementDefinition*
                                            EOF
;
imports:                                    (importStatement)*
;
importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
elementDefinition:                          (
                                                authenticationDemo
                                            )
;

// -------------------------------------- AuthenticationDemo --------------------------------------

authenticationDemo:     AUTHENTICATION_DEMO qualifiedName
                            BRACE_OPEN
                            (
                                authentication
                            )*
                            BRACE_CLOSE
;

// -------------------------------------- Authentication --------------------------------------

authentication:     AUTHENTICATION_DEMO_AUTHENTICATION COLON (
                        userPasswordAuthentication |
                        apiKeyAuthentication |
                        encryptedPrivateKeyAuthentication |
                        gcpWIFWithAWSIdPAuthentication
                    )
;

// -------------------------------------- UserPasswordAuthentication --------------------------------------

userPasswordAuthentication:     USER_PASSWORD_AUTHENTICATION
                                BRACE_OPEN
                                (
                                    userPasswordAuthentication_username |
                                    userPasswordAuthentication_password
                                )*
                                BRACE_CLOSE
;


userPasswordAuthentication_username:    USER_PASSWORD_AUTHENTICATION_USERNAME COLON STRING SEMI_COLON
;

userPasswordAuthentication_password:    USER_PASSWORD_AUTHENTICATION_PASSWORD COLON secret_value
;

// -------------------------------------- APIKeyAuthentication --------------------------------------

apiKeyAuthentication :     API_KEY_AUTHENTICATION
                                BRACE_OPEN
                                (
                                    apiKeyAuthentication_keyName |
                                    apiKeyAuthentication_location |
                                    apiKeyAuthentication_value
                                )*
                                BRACE_CLOSE
;


apiKeyAuthentication_keyName:    API_KEY_AUTHENTICATION_KEY_NAME COLON STRING SEMI_COLON
;

apiKeyAuthentication_location:    API_KEY_AUTHENTICATION_LOCATION COLON STRING SEMI_COLON
;

apiKeyAuthentication_value:    API_KEY_AUTHENTICATION_VALUE COLON secret_value
;

// -------------------------------------- Encrypted Private Key Authentication --------------------------------------

encryptedPrivateKeyAuthentication :     ENCRYPTED_PRIVATE_KEY_AUTHENTICATION
                                BRACE_OPEN
                                (
                                    encryptedPrivateKeyAuthentication_userName |
                                    encryptedPrivateKeyAuthentication_privateKey |
                                    encryptedPrivateKeyAuthentication_passphrase
                                )*
                                BRACE_CLOSE
;

encryptedPrivateKeyAuthentication_userName:     ENCRYPTED_PRIVATE_KEY_USERNAME COLON STRING SEMI_COLON
;

encryptedPrivateKeyAuthentication_privateKey:    ENCRYPTED_PRIVATE_KEY_PRIVATE_KEY COLON secret_value
;


encryptedPrivateKeyAuthentication_passphrase:    ENCRYPTED_PRIVATE_KEY_PASSPHRASE COLON secret_value
;

// -------------------------------------- GCP WIF With AWS IdP Authentication --------------------------------------

gcpWIFWithAWSIdPAuthentication : GCP_WIF_AWS_IDP_AUTHENTICATION
                                BRACE_OPEN
                                (
                                    gcpWIFWithAWSIdPAuthentication_serviceAccountEmail |
                                    gcpWIFWithAWSIdPAuthentication_awsIdp |
                                    gcpWIFWithAWSIdPAuthentication_gcpWorkload
                                )*
                                BRACE_CLOSE
;

gcpWIFWithAWSIdPAuthentication_serviceAccountEmail: GCP_WIF_AWS_IDP_AUTHENTICATION_SERVICEACCOUNT COLON STRING SEMI_COLON
;

gcpWIFWithAWSIdPAuthentication_awsIdp: GCP_WIF_AWS_IDP_AUTHENTICATION_AWS_IDP_IDP COLON GCP_WIF_AWS_IDP_AUTHENTICATION_AWS_IDP
                               BRACE_OPEN
                               (
                                    gcpWIFWithAWSIdPAuthentication_awsIdp_accountId |
                                    gcpWIFWithAWSIdPAuthentication_awsIdp_region |
                                    gcpWIFWithAWSIdPAuthentication_awsIdp_role |
                                    gcpWIFWithAWSIdPAuthentication_awsIdp_awsCredentials
                               )*
                               BRACE_CLOSE
;

gcpWIFWithAWSIdPAuthentication_awsIdp_accountId:  GCP_WIF_AWS_IDP_AUTHENTICATION_AWS_IDP_ACCOUNT COLON STRING SEMI_COLON
;
gcpWIFWithAWSIdPAuthentication_awsIdp_region:  GCP_WIF_AWS_IDP_AUTHENTICATION_AWS_IDP_REGION COLON STRING SEMI_COLON
;
gcpWIFWithAWSIdPAuthentication_awsIdp_role:  GCP_WIF_AWS_IDP_AUTHENTICATION_AWS_IDP_ROLE COLON STRING SEMI_COLON
;
gcpWIFWithAWSIdPAuthentication_awsIdp_awsCredentials: AWSCREDENTIALS COLON awsCredentialsValue
;
gcpWIFWithAWSIdPAuthentication_gcpWorkload:    GCP_WIF_AWS_IDP_AUTHENTICATION_GCP_WORKLOAD_WORKLOAD COLON GCP_WIF_AWS_IDP_AUTHENTICATION_GCP_WORKLOAD
                               BRACE_OPEN
                               (
                                    gcpWIFWithAWSIdPAuthentication_gcpWorkload_projectNumber |
                                    gcpWIFWithAWSIdPAuthentication_gcpWorkload_poolId |
                                    gcpWIFWithAWSIdPAuthentication_gcpWorkload_providerId
                               )*
                               BRACE_CLOSE
;

gcpWIFWithAWSIdPAuthentication_gcpWorkload_projectNumber: GCP_WIF_AWS_IDP_AUTHENTICATION_GCP_WORKLOAD_PROJECT_NUMBER COLON STRING SEMI_COLON
;

gcpWIFWithAWSIdPAuthentication_gcpWorkload_poolId: GCP_WIF_AWS_IDP_AUTHENTICATION_GCP_WORKLOAD_POOL_ID COLON STRING SEMI_COLON
;

gcpWIFWithAWSIdPAuthentication_gcpWorkload_providerId: GCP_WIF_AWS_IDP_AUTHENTICATION_GCP_WORKLOAD_PROVIDER_ID COLON STRING SEMI_COLON
;

// -------------------------------------- Secret Value --------------------------------------

secret_value:
                                        (
                                            propertiesSecret |
                                            environmentSecret |
                                            systemPropertiesSecret |
                                            awsSecretsManagerSecret
                                        )
;


// ------------------------------------- CredentialSecret -----------------------------------------------

propertiesSecret_propertyName:     PROPERTIES_VAULT_SECRET_PROPERTYNAME COLON STRING SEMI_COLON
;

propertiesSecret:  PROPERTIES_VAULT_SECRET
                        BRACE_OPEN
                        (
                            propertiesSecret_propertyName
                        )*
                        BRACE_CLOSE
;

environmentSecret_envVariableName:     ENVIRONMENT_VAULT_SECRET_VARIABLENAME COLON STRING SEMI_COLON
;

environmentSecret:     ENVIRONMENT_VAULT_SECRET
                            BRACE_OPEN
                            (
                                environmentSecret_envVariableName
                            )*
                            BRACE_CLOSE
;

systemPropertiesSecret_systemPropertyName: SYSTEM_PROPERTIES_VAULT_SECRET_PROPERTYNAME COLON STRING SEMI_COLON
;

systemPropertiesSecret:     SYSTEM_PROPERTIES_VAULT_SECRET
                            BRACE_OPEN
                            (
                                systemPropertiesSecret_systemPropertyName
                            )*
                            BRACE_CLOSE
;

awsSecretsManagerSecret_secretId:  AWS_SECRETS_MANAGER_VAULT_SECRET_SECRETID COLON STRING SEMI_COLON
;

versionId:  AWS_SECRETS_MANAGER_VAULT_SECRET_VERSIONID COLON STRING SEMI_COLON
;

versionStage:   AWS_SECRETS_MANAGER_VAULT_SECRET_VERSIONSTAGE COLON STRING SEMI_COLON
;

awsSecretsManagerSecret:   AWS_SECRETS_MANAGER_VAULT_SECRET
                                BRACE_OPEN
                                (
                                    awsSecretsManagerSecret_secretId |
                                    versionId |
                                    versionStage |
                                    awsSecretsManagerSecret_awsCredentials
                                )*
                                BRACE_CLOSE
;

// ------------------------------------- AWS Credentials  -----------------------------------------------

awsSecretsManagerSecret_awsCredentials:   AWSCREDENTIALS COLON awsCredentialsValue
;

awsCredentialsValue:    (
                            awsStaticCredentialsValue |
                            awsDefaultCredentialsValue |
                            awsSTSAssumeRoleCredentialsValue
                        )
;

awsDefaultCredentialsValue: AWS_CREDENTIALS_DEFAULT
                            BRACE_OPEN
                            (
                            )
                            BRACE_CLOSE
;

awsStaticCredentialsValue:  AWS_CREDENTIALS_STATIC
                            BRACE_OPEN
                            (
                                awsStaticCredentialsValue_accessKeyId |
                                awsStaticCredentialsValue_secretAccessKey
                            )*
                            BRACE_CLOSE
;

awsStaticCredentialsValue_accessKeyId:  AWS_CREDENTIALS_STATIC_ACCESSKEYID COLON secret_value
;

awsStaticCredentialsValue_secretAccessKey:  AWS_CREDENTIALS_STATIC_SECRETACCESSKEY COLON secret_value
;

awsSTSAssumeRoleCredentialsValue:  AWS_CREDENTIALS_STS_ASSUMEROLE
                            BRACE_OPEN
                            (
                                awsSTSAssumeRoleCredentialsValue_roleArn |
                                awsSTSAssumeRoleCredentialsValue_roleSessionName |
                                awsSTSAssumeRoleCredentialsValue_awsCredentials
                            )*
                            BRACE_CLOSE
;

awsSTSAssumeRoleCredentialsValue_awsCredentials:   AWSCREDENTIALS COLON awsCredentialsValue
;

awsSTSAssumeRoleCredentialsValue_roleArn: AWS_CREDENTIALS_STS_ASSUMEROLE_ROLE_ARN COLON STRING SEMI_COLON
;

awsSTSAssumeRoleCredentialsValue_roleSessionName: AWS_CREDENTIALS_STS_ASSUMEROLE_ROLE_SESSION_NAME COLON STRING SEMI_COLON
;