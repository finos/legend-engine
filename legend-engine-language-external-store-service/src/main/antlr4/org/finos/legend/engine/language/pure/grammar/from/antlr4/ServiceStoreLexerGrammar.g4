lexer grammar ServiceStoreLexerGrammar;

import M3LexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

SERVICE_STORE:                                   'ServiceStore';

DESCRIPTION:                                     'description';
SERVICE_GROUP:                                   'ServiceGroup';
SERVICE:                                         'Service';

PATH:                                            'path';

REQUEST_BODY:                                    'requestBody';
METHOD:                                          'method';
PARAMETERS:                                      'parameters';
RESPONSE:                                        'response';
SECURITY_SCHEME:                                 'security';

LOCATION:                                        'location';
STYLE:                                           'style';
EXPLODE:                                         'explode';
ENUM:                                            'enum';

// Mapping
SERVICE_MAPPING:                                 '~service';
PATH_MAPPING:                                    '~path';
PARAM_MAPPING:                                   '~paramMapping';
SERVICE_REFERENCE:                               '$service';


// -------------------------------------- BUILDING_BLOCK --------------------------------------

INVERTED_ARROW:                                      '<-';