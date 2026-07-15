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


CONSTRAINT_OWNER:                           '~owner'            [ \t\r\n]* ':';
CONSTRAINT_EXTERNAL_ID:                     '~externalId'       [ \t\r\n]* ':';
CONSTRAINT_FUNCTION:                        '~function'         [ \t\r\n]* ':';
CONSTRAINT_MESSAGE:                         '~message'          [ \t\r\n]* ':';
CONSTRAINT_ENFORCEMENT:                     '~enforcementLevel' [ \t\r\n]* ':';

CONSTRAINT_ENFORCEMENT_LEVEL_ERROR:         'Error';
CONSTRAINT_ENFORCEMENT_LEVEL_WARN:          'Warn';

NATIVE:                                     'native';

PROJECTS:                                   'projects';
AS:                                         'as';

AGGREGATION_TYPE_COMPOSITE:                 'composite';
AGGREGATION_TYPE_SHARED:                    'shared';
AGGREGATION_TYPE_NONE:                      'none';