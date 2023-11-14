lexer grammar BigQueryFunctionLexerGrammar;

import M3LexerGrammar;

BIGQUERY_FUNCTION:                                  'BigQueryFunction';
BIGQUERY_FUNCTION__FUNCTION_NAME:                   'functionName';
BIGQUERY_FUNCTION__DESCRIPTION:                     'description';
BIGQUERY_FUNCTION__FUNCTION:                        'function';
BIGQUERY_FUNCTION__OWNER:                           'owner';
BIGQUERY_FUNCTION__ACTIVATION:                      'activationConfiguration';

// ------------------------------------- CONFIGURATION -------------------------------
CONFIGURATION:                          'BigQueryFunctionDeploymentConfiguration';
ACTIVATION_CONNECTION:                  'activationConnection';
DEPLOYMENT_STAGE:                       'stage';