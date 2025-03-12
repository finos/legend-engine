lexer grammar DataProductLexerGrammar;

import M3LexerGrammar;


// ------------------------------------ KEYWORD --------------------------------------

DATA_PRODUCT:                                 'DataProduct';

STEREOTYPES:                                'stereotypes';
TAGS:                                       'tags';

// generic tokens
DATA_PRODUCT__NAME:                           'name';
DATA_PRODUCT__TITLE:                          'title';
DATA_PRODUCT__DESCRIPTION:                    'description';
DATA_PRODUCT__TEMPLATE_QUERY:                 'query';
DATA_PRODUCT__EXECUTABLE__ID:                 'id';
DATA_PRODUCT__EXECUTION_CONTEXT_KEY:          'executionContextKey';

DATA_PRODUCT_EXECUTION_CONTEXTS:              'executionContexts';
DATA_PRODUCT_DEFAULT_EXECUTION_CONTEXT:       'defaultExecutionContext';

DATA_PRODUCT_MAPPING:                         'mapping';
DATA_PRODUCT_DEFAULT_RUNTIME:                 'defaultRuntime';
DATA_PRODUCT_TEST_DATA:                       'testData';

DATA_PRODUCT_DIAGRAMS:                        'diagrams';
DATA_PRODUCT_DIAGRAM:                         'diagram';

DATA_PRODUCT_ELEMENTS:                        'elements';

DATA_PRODUCT_EXECUTABLES:                     'executables';
DATA_PRODUCT_EXECUTABLE:                      'executable';

DATA_PRODUCT_SUPPORT_INFO:                    'supportInfo';
DATA_PRODUCT_SUPPORT_DOC_URL:                 'documentationUrl';

DATA_PRODUCT_SUPPORT_EMAIL:                   'Email';
DATA_PRODUCT_SUPPORT_EMAIL_ADDRESS:           'address';

DATA_PRODUCT_SUPPORT_COMBINED_INFO:           'Combined';
DATA_PRODUCT_SUPPORT_EMAILS:                  'emails';
DATA_PRODUCT_SUPPORT_WEBSITE:                 'website';
DATA_PRODUCT_SUPPORT_FAQ_URL:                 'faqUrl';
DATA_PRODUCT_SUPPORT_SUPPORT_URL:             'supportUrl';


// ------------------------------------ DEPRECATED --------------------------------------

DATA_PRODUCT_GROUP_ID:                        'groupId';
DATA_PRODUCT_ARTIFACT_ID:                     'artifactId';
DATA_PRODUCT_VERSION_ID:                      'versionId';

DATA_PRODUCT_FEATURED_DIAGRAMS:               'featuredDiagrams';
