lexer grammar ServiceStoreLexerGrammar;

import M3LexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

SERVICE_STORE:                                   'ServiceStore';

DESCRIPTION:                                     'description';
SERVICE_GROUP:                                   'ServiceGroup';
SERVICE:                                         'Service';
SECURITY_SCHEMES:                                'SecuritySchemes';

PATH:                                            'path';

REQUEST_BODY:                                    'requestBody';
METHOD:                                          'method';
PARAMETERS:                                      'parameters';
RESPONSE:                                        'response';
SECURITY_SCHEME:                                 'security';

ALLOW_RESERVED:                                  'allowReserved';
REQUIRED:                                        'required';
LOCATION:                                        'location';
STYLE:                                           'style';
EXPLODE:                                         'explode';
ENUM:                                            'enum';

// Mapping
SERVICE_MAPPING:                                 '~service';
PATH_OFFSET:                                     '~path';
REQUEST:                                         '~request';
BODY:                                            'body';
SERVICE_REFERENCE:                               '$service';

// TODO: TO BE REMOVED
PARAM_MAPPING:                                   '~paramMapping';
// -------------------------------------- BUILDING_BLOCK --------------------------------------

INVERTED_ARROW:                                  '<-';
QUOTED_STRING:                                   ('"' ( EscSeq | ~["\r\n] )*  '"');



// -------------------------------------- ISLAND ---------------------------------------
BRACE_OPEN:                    '{' -> pushMode (SECURITY_SCHEME_ISLAND_MODE);


mode SECURITY_SCHEME_ISLAND_MODE;
SECURITY_SCHEME_ISLAND_OPEN: '{' -> pushMode (SECURITY_SCHEME_ISLAND_MODE);
SECURITY_SCHEME_ISLAND_CLOSE: '}' -> popMode;
SECURITY_SCHEME_CONTENT: (~[{}])+;