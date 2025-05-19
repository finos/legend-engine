lexer grammar SnowflakeLexerGrammar;

import M3LexerGrammar;

//SnowflakeApp Specific tokens
SNOWFLAKE_APP:                                  'SnowflakeApp';
SNOWFLAKE_APP__APPLICATION_NAME:                'applicationName';
SNOWFLAKE_APP__CONFIGURATION:                   'SnowflakeAppDeploymentConfiguration';

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
SNOWFLAKE__PERMISSION:                          'permissionScheme';
SNOWFLAKE__USAGE_ROLE:                          'usageRole';
SNOWFLAKE__DEPLOYMENT_SCHEMA:                   'deploymentSchema';


ACTIVATION_CONNECTION:                          'activationConnection';
DEPLOYMENT_STAGE:                               'deploymentStage';