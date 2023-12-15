lexer grammar DomainLexerGrammar;

import M3LexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

IMPORT:                                     'import';

CLASS:                                      'Class';
ASSOCIATION:                                'Association';
PROFILE:                                    'Profile';
ENUM:                                       'Enum';
MEASURE:                                    'Measure';
FUNCTION:                                   'function';

EXTENDS:                                    'extends';

STEREOTYPES:                                'stereotypes';
TAGS:                                       'tags';

CONSTRAINT_OWNER:                           '~owner';
CONSTRAINT_EXTERNAL_ID:                     '~externalId';
CONSTRAINT_FUNCTION:                        '~function';
CONSTRAINT_MESSAGE:                         '~message';
CONSTRAINT_ENFORCEMENT:                     '~enforcementLevel';
CONSTRAINT_ENFORCEMENT_LEVEL_ERROR:         'Error';
CONSTRAINT_ENFORCEMENT_LEVEL_WARN:          'Warn';

NATIVE:                                     'native';

PROJECTS:                                   'projects';
AS:                                         'as';

AGGREGATION_TYPE_COMPOSITE:                 'composite';
AGGREGATION_TYPE_SHARED:                    'shared';
AGGREGATION_TYPE_NONE:                      'none';

FUNCTION_TEST_DATA:                         'data';
FUNCTION_SUITE_TESTS:                       'tests';
FUNCTION_TEST_PARAMETERS:                   'parameters';
FUNCTION_TEST_ASSERTS:                      'asserts';
FUNCTION_TEST_DATA_STORE:                   'store';
