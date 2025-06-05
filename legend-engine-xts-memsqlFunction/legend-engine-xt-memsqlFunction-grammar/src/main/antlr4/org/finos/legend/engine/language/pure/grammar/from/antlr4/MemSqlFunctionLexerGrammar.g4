lexer grammar MemSqlFunctionLexerGrammar;

import M3LexerGrammar;

MEMSQL_FUNCTION:                                  'MemSqlFunction';
MEMSQL_FUNCTION__FUNCTION_NAME:                   'functionName';
MEMSQL_FUNCTION__DESCRIPTION:                     'description';
MEMSQL_FUNCTION__FUNCTION:                        'function';
MEMSQL_FUNCTION__OWNER:                           'ownership';
MEMSQL_FUNCTION__DEPLOYMENT:                      'Deployment';
MEMSQL_FUNCTION__DEPLOYMENT_ID:                   'identifier';
MEMSQL_FUNCTION__ACTIVATION:                      'activationConfiguration';

// ------------------------------------- CONFIGURATION -------------------------------
CONFIGURATION:                          'MemSqlFunctionDeploymentConfiguration';
ACTIVATION_CONNECTION:                  'activationConnection';
DEPLOYMENT_STAGE:                       'stage';