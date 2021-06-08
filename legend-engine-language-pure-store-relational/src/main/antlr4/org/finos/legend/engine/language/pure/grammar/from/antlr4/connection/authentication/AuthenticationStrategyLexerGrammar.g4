lexer grammar AuthenticationStrategyLexerGrammar;

import CoreLexerGrammar;

H2_DEFAULT_AUTH:                            'DefaultH2';
TEST_DB_AUTH:                               'Test';
DELEGATED_KERBEROS_AUTH:                    'DelegatedKerberos';
SERVER_PRINCIPAL:                           'serverPrincipal';
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