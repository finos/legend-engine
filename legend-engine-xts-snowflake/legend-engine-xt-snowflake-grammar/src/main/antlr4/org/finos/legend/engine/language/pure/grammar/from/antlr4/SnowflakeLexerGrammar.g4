lexer grammar SnowflakeLexerGrammar;

import M3LexerGrammar;

//SnowflakeApp Specific tokens
SNOWFLAKE_APP:                                  'SnowflakeApp';
SNOWFLAKE_APP__APPLICATION_NAME:                'applicationName';
SNOWFLAKE_APP__CONFIGURATION:                   'SnowflakeAppDeploymentConfiguration';
SNOWFLAKE_APP__PERMISSION:                      'permissionScheme';
SNOWFLAKE_APP__USAGE_ROLE:                      'usageRole';

//SnowflakeM2MUdf Specific tokens
SNOWFLAKE_M2M_UDF:                              'SnowflakeM2MUdf';
SNOWFLAKE_M2M_UDF__UDF_NAME:                    'udfName';
SNOWFLAKE_M2M_UDF__CONFIGURATION:               'SnowflakeUDFDeploymentConfiguration';

//Common tokens
SNOWFLAKE__DESCRIPTION:                         'description';
SNOWFLAKE__FUNCTION:                            'function';
SNOWFLAKE__OWNER:                               'ownership';
SNOWFLAKE__OWNER_DEPLOYMENT:                    'Deployment';
SNOWFLAKE__OWNER_DEPLOYMENT_ID:                 'identifier';
SNOWFLAKE__ACTIVATION:                          'activationConfiguration';
SNOWFLAKE__DEPLOYMENT_SCHEMA:                   'deploymentSchema';
SNOWFLAKE__ACTIVATION_CONNECTION:               'activationConnection';
SNOWFLAKE__DEPLOYMENT_STAGE:                    'deploymentStage';