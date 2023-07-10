    lexer grammar MasteryLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
TRUE:                                       'true';
FALSE:                                      'false';
IMPORT:                                     'import';

//**********
// MASTERY
//**********
MASTER_RECORD_DEFINITION:                       'MasterRecordDefinition';

//RecordSource
RECORD_SOURCES:                                 'recordSources';
RECORD_SOURCE_STATUS:                           'status';
PARSE_SERVICE:                                  'parseService';
TRANSFORM_SERVICE:                              'transformService';
RECORD_SOURCE_SEQUENTIAL:                       'sequentialData';
RECORD_SOURCE_STAGED:                           'stagedLoad';
RECORD_SOURCE_CREATE_PERMITTED:                 'createPermitted';
RECORD_SOURCE_CREATE_BLOCKED_EXCEPTION:         'createBlockedException';
RECORD_SOURCE_STATUS_DEVELOPMENT:               'Development';
RECORD_SOURCE_STATUS_TEST_ONLY:                 'TestOnly';
RECORD_SOURCE_STATUS_PRODUCTION:                'Production';
RECORD_SOURCE_STATUS_DORMANT:                   'Dormant';
RECORD_SOURCE_STATUS_DECOMMINISSIONED:          'Decomissioned';

//SourcePartitions
SOURCE_PARTITIONS:                              'partitions';

//IdentityResolution
IDENTITY_RESOLUTION:                           'identityResolution';
RESOLUTION_QUERIES:                             'resolutionQueries';
RESOLUTION_QUERY_EXPRESSIONS:                   'queries';
RESOLUTION_QUERY_KEY_TYPE:                      'keyType';
RESOLUTION_QUERY_KEY_TYPE_GENERATED_PRIMARY_KEY:'GeneratedPrimaryKey'; //Validated against equality key to ensure an acrual PK and fail if don't find match
RESOLUTION_QUERY_KEY_TYPE_SUPPLIED_PRIMARY_KEY: 'SuppliedPrimaryKey'; //Validated against equality key to ensure an actuial PK and create if don't find match
RESOLUTION_QUERY_KEY_TYPE_ALTERNATE_KEY:        'AlternateKey'; //AlternateKey (In an AlternateKey is specified then at least one required in the input record or fail resolution). AlternateKey && (CurationModel field == Create) then the input source is attempting to create a new record (e.g. from UI) block if existing record found
RESOLUTION_QUERY_KEY_TYPE_OPTIONAL:             'Optional';

//PrecedenceRules
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
AGGREGATOR:                                      'Aggregator';
EXCHANGE:                                       'Exchange';

//*************
// COMMON
//*************
ID:                                             'id';
MODEL_CLASS:                                    'modelClass';
DESCRIPTION:                                    'description';
TAGS:                                           'tags';
PRECEDENCE:                                     'precedence';