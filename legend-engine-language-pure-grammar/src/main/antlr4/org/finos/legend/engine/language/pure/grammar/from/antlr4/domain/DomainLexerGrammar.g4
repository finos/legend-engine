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
