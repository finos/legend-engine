lexer grammar SnowflakeAppLexerGrammar;

import M3LexerGrammar;

SNOWFLAKE_APP:                                  'SnowflakeApp';
SNOWFLAKE_APP__APPLICATION_NAME:                'applicationName';
SNOWFLAKE_APP__DESCRIPTION:                     'description';
SNOWFLAKE_APP__FUNCTION:                        'function';
SNOWFLAKE_APP__OWNER:                           'owner';
SNOWFLAKE_APP__TYPE:                           'type';
SNOWFLAKE_APP__ACTIVATION:                      'activationConfiguration';

// ------------------------------------- CONFIGURATION -------------------------------
CONFIGURATION:                          'SnowflakeAppDeploymentConfiguration';
ACTIVATION_CONNECTION:                  'activationConnection';
DEPLOYMENT_STAGE:                        'stage';