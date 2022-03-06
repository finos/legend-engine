lexer grammar AuthenticationStrategyLexerGrammar;

import CoreLexerGrammar;

H2_DEFAULT_AUTH:                            'DefaultH2';
TEST_DB_AUTH:                               'Test';
DELEGATED_KERBEROS_AUTH:                    'DelegatedKerberos';
SERVER_PRINCIPAL:                           'serverPrincipal';
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