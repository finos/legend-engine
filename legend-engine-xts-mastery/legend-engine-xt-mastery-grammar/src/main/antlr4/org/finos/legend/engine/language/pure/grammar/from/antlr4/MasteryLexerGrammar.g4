lexer grammar MasteryLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
TRUE:                                       'true';
FALSE:                                      'false';
IMPORT:                                     'import';

// -------------------------------------- MASTERY --------------------------------------------
MASTER_RECORD_DEFINITION:                       'MasterRecordDefinition';
MASTER_RECORD_DEFINITIONS:                      'masterRecordDefinitions';

// -------------------------------------- RECORD SERVICE --------------------------------------
PARSE_SERVICE:                                  'parseService';
TRANSFORM_SERVICE:                              'transformService';

// -------------------------------------- RECORD SOURCE --------------------------------------
RECORD_SOURCES:                                 'recordSources';
RECORD_SOURCE_STATUS:                           'status';
RECORD_SOURCE_SEQUENTIAL:                       'sequentialData';
RECORD_SOURCE_STAGED:                           'stagedLoad';
RECORD_SOURCE_CREATE_PERMITTED:                 'createPermitted';
RECORD_SOURCE_CREATE_BLOCKED_EXCEPTION:         'createBlockedException';
RECORD_SOURCE_STATUS_DEVELOPMENT:               'Development';
RECORD_SOURCE_STATUS_TEST_ONLY:                 'TestOnly';
RECORD_SOURCE_STATUS_PRODUCTION:                'Production';
RECORD_SOURCE_STATUS_DORMANT:                   'Dormant';
RECORD_SOURCE_STATUS_DECOMMISSIONED:            'Decommissioned';
RECORD_SOURCE_DATA_PROVIDER:                    'dataProvider';
RECORD_SOURCE_TRIGGER:                          'trigger';
RECORD_SOURCE_SERVICE:                          'recordService';
RECORD_SOURCE_ALLOW_FIELD_DELETE:               'allowFieldDelete';
RECORD_SOURCE_AUTHORIZATION:                    'authorization';
RECORD_SOURCE_DEPENDENCIES:                     'dependencies';
RECORD_SOURCE_DEPENDENCY:                       'RecordSourceDependency';
RECORD_SOURCE_TIMEOUT_IN_MINUTES:               'timeoutInMinutes';
RECORD_SOURCE_RAISE_EXCEPTION_WORKFLOW:         'raiseExceptionWorkflow';
RECORD_SOURCE_RUN_PROFILE:                      'runProfile';
RECORD_SOURCE_RUN_PROFILE_LARGE:                'Large';
RECORD_SOURCE_RUN_PROFILE_MEDIUM:               'Medium';
RECORD_SOURCE_RUN_PROFILE_SMALL:                'Small';
RECORD_SOURCE_RUN_PROFILE_XTRA_SMALL:           'ExtraSmall';

//SourcePartitions - these are now deprecated, keeping for backwards compatibility
SOURCE_PARTITIONS:                              'partitions';

// -------------------------------------- IDENTITY RESOLUTION --------------------------------------
IDENTITY_RESOLUTION:                           'identityResolution';
RESOLUTION_QUERIES:                             'resolutionQueries';
RESOLUTION_QUERY_EXPRESSIONS:                   'queries';
RESOLUTION_QUERY_KEY_TYPE:                      'keyType';
RESOLUTION_QUERY_OPTIONAL:                      'optional';
RESOLUTION_QUERY_KEY_TYPE_GENERATED_PRIMARY_KEY:'GeneratedPrimaryKey'; //Validated against equality key to ensure an acrual PK and fail if don't find match
RESOLUTION_QUERY_KEY_TYPE_SUPPLIED_PRIMARY_KEY: 'SuppliedPrimaryKey'; //Validated against equality key to ensure an actuial PK and create if don't find match
RESOLUTION_QUERY_KEY_TYPE_ALTERNATE_KEY:        'AlternateKey'; //AlternateKey (In an AlternateKey is specified then at least one required in the input record or fail resolution). AlternateKey && (CurationModel field == Create) then the input source is attempting to create a new record (e.g. from UI) block if existing record found
RESOLUTION_QUERY_KEY_TYPE_OPTIONAL:             'Optional';
RESOLUTION_QUERY_FILTER:                        'filter';

// -------------------------------------- PRECEDENCE RULES --------------------------------------
PRECEDENCE_RULES:                               'precedenceRules';
SOURCE_PRECEDENCE_RULE:                         'SourcePrecedenceRule';
CONDITIONAL_RULE:                               'ConditionalRule';
DELETE_RULE:                                    'DeleteRule';
CREATE_RULE:                                    'CreateRule';
ATTRIBUTE_BLOCK_RULE:                           'AttributeBlockRule';
DATA_PROVIDER_TYPE_SCOPE:                       'DataProviderTypeScope';
DATA_PROVIDER_ID_SCOPE:                         'DataProviderIdScope';
RECORD_SOURCE:                                  'RecordSource';
PATH:                                           'path';
PREDICATE:                                      'predicate';
ACTION:                                         'action';
OVERWRITE:                                      'Overwrite';
BLOCK:                                          'Block';
RULE_SCOPE:                                     'ruleScope';
RECORD_SOURCE_SCOPE:                            'RecordSourceScope';

// -------------------------------------- ACQUISITION PROTOCOL --------------------------------------
ACQUISITION_PROTOCOL:                           'acquisitionProtocol';

// -------------------------------------- CONNECTION --------------------------------------
MASTERY_CONNECTION:                             'MasteryConnection';

// -------------------------------------- MASTERY_RUNTIME --------------------------------------
MASTERY_RUNTIME:                                'MasteryRuntime';
RUNTIME:                                        'runtime';

// -------------------------------------- COMMON --------------------------------------

ID:                                             'id';
MODEL_CLASS:                                    'modelClass';
DESCRIPTION:                                    'description';
TAGS:                                           'tags';
PRECEDENCE:                                     'precedence';
POST_CURATION_ENRICHMENT_SERVICE:               'postCurationEnrichmentService';
SPECIFICATION:                                  'specification';
EXCEPTION_WORKFLOW_TRANSFORM_SERVICE:           'exceptionWorkflowTransformService';
ELASTIC_SEARCH_TRANSFORM_SERVICE:               'elasticSearchTransformService';
PUBLISH_TO_ELASTIC_SEARCH:                      'publishToElasticSearch';
COLLECTION_EQUALITIES:                          'collectionEqualities';
EQUALITY_FUNCTION:                              'equalityFunction';