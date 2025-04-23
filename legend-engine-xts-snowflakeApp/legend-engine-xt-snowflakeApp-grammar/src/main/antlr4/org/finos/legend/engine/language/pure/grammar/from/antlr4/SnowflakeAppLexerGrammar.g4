lexer grammar SnowflakeAppLexerGrammar;

import M3LexerGrammar;

SNOWFLAKE_APP:                                  'SnowflakeApp';
SNOWFLAKE_APP__APPLICATION_NAME:                'applicationName';
SNOWFLAKE_APP__DESCRIPTION:                     'description';
SNOWFLAKE_APP__FUNCTION:                        'function';
SNOWFLAKE_APP__OWNER:                           'ownership';
SNOWFLAKE_APP__OWNER_DEPLOYMENT:                'Deployment';
SNOWFLAKE_APP__OWNER_DEPLOYMENT_ID:             'identifier';
SNOWFLAKE_APP__ACTIVATION:                      'activationConfiguration';
SNOWFLAKE_APP__PERMISSION:                      'permissionScheme';
SNOWFLAKE_APP__USAGE_ROLE:                      'usageRole';
SNOWFLAKE_APP__DEPLOYMENT_SCHEMA:               'deploymentSchema';

// ------------------------------------- CONFIGURATION -------------------------------
CONFIGURATION:                          'SnowflakeAppDeploymentConfiguration';
ACTIVATION_CONNECTION:                  'activationConnection';
DEPLOYMENT_STAGE:                        'stage';