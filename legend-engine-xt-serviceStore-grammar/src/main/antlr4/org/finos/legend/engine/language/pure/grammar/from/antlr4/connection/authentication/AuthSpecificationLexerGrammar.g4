lexer grammar AuthSpecificationLexerGrammar;

import CoreLexerGrammar;

OAUTH_AUTHENTICATION:                                       'OauthAuthentication';
GRANT_TYPE:                                                 'grantType';
CLIENT_ID:                                                  'clientId';
CLIENT_SECRET_VAULT_REFERENCE:                              'clientSecretVaultReference';
AUTH_SERVER_URL:                                            'authorizationServerUrl';

BASIC_AUTHENTICATION:                                       'UsernamePasswordAuthentication';
USERNAME:                                                   'username';
PASSWORD:                                                   'password';

API_KEY_AUTHENTICATION:                                     'ApiKeyAuthentication';
VALUE:                                                      'value';

BRACKET_OPEN:                                               '[';
BRACKET_CLOSE:                                              ']';
