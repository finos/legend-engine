lexer grammar AuthenticationStrategyLexerGrammar;

import CoreLexerGrammar;

H2_DEFAULT_AUTH:                            'DefaultH2';
TEST_DB_AUTH:                               'Test';
DELEGATED_KERBEROS_AUTH:                    'DelegatedKerberos';
SERVER_PRINCIPAL:                           'serverPrincipal';
MIDDLETIER_USERNAME_PASSWORD_AUTH:          'MiddleTierUserNamePassword';
MIDDLETIER_USERNAME_PASSWORD_VAULT_REFERENCE:  'vaultReference';
USERNAME_PASSWORD_AUTH:                     'UserNamePassword';
USERNAME_PASSWORD_AUTH_BASE_VAULT_REF:      'baseVaultReference';
USERNAME_PASSWORD_AUTH_USERNAME_VAULT_REF:  'userNameVaultReference';
USERNAME_PASSWORD_AUTH_PASSWORD_VAULT_REF:  'passwordVaultReference';
HOST:                                       'host';
PORT:                                       'port';
NAME:                                       'name';
MODE:                                       'mode';
DIRECTORY:                                  'directory';
ACCOUNT:                                    'account';
WAREHOUSE:                                  'warehouse';
REGION:                                     'region';

SNOWFLAKE_PUBLIC_AUTH:                      'SnowflakePublic';
SNOWFLAKE_AUTH_KEY_VAULT_REFERENCE:         'privateKeyVaultReference';
SNOWFLAKE_AUTH_PASSPHRASE_VAULT_REFERENCE:  'passPhraseVaultReference';
SNOWFLAKE_AUTH_PUBLIC_USERNAME:             'publicUserName';

PROJECT:                                                    'projectId';
DATASET:                                                    'defaultDataset';
GCP_APPLICATION_DEFAULT_CREDENTIALS_AUTH:                   'GCPApplicationDefaultCredentials';

API_TOKEN_AUTH:                             'ApiToken';
API_TOKEN_AUTH_TOKEN:                       'apiToken';

GCP_WORKLOAD_IDENTITY_FEDERATION_AUTH:                      'GCPWorkloadIdentityFederation';
SERVICE_ACCOUNT_EMAIL:                                      'serviceAccountEmail';
ADDITIONAL_GCP_SCOPES:                                                  'additionalGcpScopes';
BRACKET_OPEN:                                               '[';
BRACKET_CLOSE:                                              ']';

