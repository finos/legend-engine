lexer grammar JarServiceLexerGrammar;


import M3LexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

STEREOTYPES:                                'stereotypes';
TAGS:                                       'tags';

SERVICE:                                    'JarService';
IMPORT:                                     'import';

SERVICE_OWNERSHIP:                          'ownership';
SERVICE_OWNERSHIP_USERLIST:                 'UserList';
SERVICE_OWNERSHIP_USERLIST_USERS:           'users';
SERVICE_ACTIVATION:                         'activationConfiguration';
SERVICE_DOCUMENTATION:                      'documentation';
SERVICE_LINEAGE:                            'generateLineage';
SERVICE_MODEL:                              'storeModel';
SERVICE_FUNCTION:                           'function';
SERVICE_BINDING:                            'binding';
SERVICE_CONTENT_TYPE:                       'contentType';
SERVICE_TEST_SUITES:                        'testSuites';
SERVICE_TEST_DATA:                          'data';
SERVICE_TEST_CONNECTION_DATA:               'connections';
SERVICE_TEST_TESTS:                         'tests';
SERVICE_TEST_ASSERTS:                       'asserts';
SERVICE_TEST_SERIALIZATION_FORMAT:          'serializationFormat';
SERVICE_TEST_PARAMETERS:                    'parameters';
ASSERT_FOR_KEYS:                            'keys';
PARAM_GROUP:                                'list';

SERVICE_POST_VALIDATION:                    'postValidations';
SERVICE_POST_VALIDATION_DESCRIPTION:        'description';
SERVICE_POST_VALIDATION_PARAMETERS:         'params';
SERVICE_POST_VALIDATION_ASSERTIONS:         'assertions';

// -------------------------------------- EXECUTION_ENVIRONMENT-------------------------

EXEC_ENV:                                   'ExecutionEnvironment';
SERVICE_MAPPING:                            'mapping';
SERVICE_RUNTIME:                            'runtime';
SERVICE_EXECUTION_EXECUTIONS:               'executions';


// ------------------------------------- CONFIGURATION -------------------------------
SERVICE_CONFIGURATION:                      'JarServiceDeploymentConfiguration';
SERVICE_DEPLOYMENT_STAGE:                    'stage';