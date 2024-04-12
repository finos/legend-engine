lexer grammar DataSpaceLexerGrammar;

import M3LexerGrammar;


// ------------------------------------ KEYWORD --------------------------------------

DATA_SPACE:                                 'DataSpace';

STEREOTYPES:                                'stereotypes';
TAGS:                                       'tags';

// generic tokens
DATA_SPACE__NAME:                           'name';
DATA_SPACE__TITLE:                          'title';
DATA_SPACE__DESCRIPTION:                    'description';
DATA_SPACE__TEMPLATE_QUERY:                 'query';
DATA_SPACE__TEMPLATE_QUERY__ID:             'id';
DATA_SPACE__EXECUTION_CONTEXT_KEY:          'executionContextKey';

DATA_SPACE_EXECUTION_CONTEXTS:              'executionContexts';
DATA_SPACE_DEFAULT_EXECUTION_CONTEXT:       'defaultExecutionContext';

DATA_SPACE_MAPPING:                         'mapping';
DATA_SPACE_DEFAULT_RUNTIME:                 'defaultRuntime';
DATA_SPACE_TEST_DATA:                       'testData';

DATA_SPACE_DIAGRAMS:                        'diagrams';
DATA_SPACE_DIAGRAM:                         'diagram';

DATA_SPACE_ELEMENTS:                        'elements';

DATA_SPACE_EXECUTABLES:                     'executables';
DATA_SPACE_EXECUTABLE:                      'executable';

DATA_SPACE_SUPPORT_INFO:                    'supportInfo';
DATA_SPACE_SUPPORT_DOC_URL:                 'documentationUrl';

DATA_SPACE_SUPPORT_EMAIL:                   'Email';
DATA_SPACE_SUPPORT_EMAIL_ADDRESS:           'address';

DATA_SPACE_SUPPORT_COMBINED_INFO:           'Combined';
DATA_SPACE_SUPPORT_EMAILS:                  'emails';
DATA_SPACE_SUPPORT_WEBSITE:                 'website';
DATA_SPACE_SUPPORT_FAQ_URL:                 'faqUrl';
DATA_SPACE_SUPPORT_SUPPORT_URL:             'supportUrl';


// ------------------------------------ DEPRECATED --------------------------------------

DATA_SPACE_GROUP_ID:                        'groupId';
DATA_SPACE_ARTIFACT_ID:                     'artifactId';
DATA_SPACE_VERSION_ID:                      'versionId';

DATA_SPACE_FEATURED_DIAGRAMS:               'featuredDiagrams';