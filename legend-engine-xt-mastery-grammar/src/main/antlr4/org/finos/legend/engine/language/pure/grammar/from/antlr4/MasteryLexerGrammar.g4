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

//SourcePartiotions
SOURCE_PARTITIONS:                              'partitions';

//IdentityResolution
IDENTITIY_RESOLUTION:                           'identityResolution';
RESOLUTION_QUERIES:                             'resolutionQueries';
RESOLUTION_QUERY_EXPRESSIONS:                   'queries';
RESOLUTION_QUERY_PRECEDENCE:                    'precedence';
RESOLUTION_QUERY_KEY_TYPE:                      'keyType';
RESOLUTION_QUERY_KEY_TYPE_GENERATED_PRIMARY_KEY:'GeneratedPrimaryKey'; //Validated against equality key to ensure an acrual PK and fail if don't find match
RESOLUTION_QUERY_KEY_TYPE_SUPPLIED_PRIMARY_KEY: 'SuppliedPrimaryKey'; //Validated against equality key to ensure an actuial PK and create if don't find match
RESOLUTION_QUERY_KEY_TYPE_ALTERNATE_KEY:        'AlternateKey'; //AlternateKey (In an AlternateKey is specified then at least one required in the input record or fail resolution). AlternateKey && (CurationModel field == Create) then the input source is attempting to create a new record (e.g. from UI) block if existing record found
RESOLUTION_QUERY_KEY_TYPE_OPTIONAL:             'Optional';


//*************
// COMMON
//*************
ID:                                             'id';
MODEL_CLASS:                                    'modelClass';
DESCRIPTION:                                    'description';
TAGS:                                           'tags';