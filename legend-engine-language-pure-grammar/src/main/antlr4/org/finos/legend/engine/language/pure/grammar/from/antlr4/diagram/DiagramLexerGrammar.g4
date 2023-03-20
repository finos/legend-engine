lexer grammar DiagramLexerGrammar;

import CoreLexerGrammar;

fragment ValidDiagramViewId:    (Letter | Digit | '_' | '-' )+
;

// -------------------------------------- KEYWORD --------------------------------------

DIAGRAM:                        'Diagram';
IMPORT:                         'import';

CLASS_VIEW:                     'classView';
CLASS:                          'class';
POSITION:                       'position';
RECTANGLE:                      'rectangle';
HIDE_PROPERTIES:                'hideProperties';
HIDE_TAGGED_VALUE:              'hideTaggedValue';
HIDE_STEREOTYPE:                'hideStereotype';

PROPERTY_VIEW:                  'propertyView';
GENERALIZATION_VIEW:            'generalizationView';
PROPERTY:                       'property';
POINTS:                         'points';
SOURCE:                         'source';
TARGET:                         'target';


// ---------------------------------- BUILDING BLOCK --------------------------------------

FLOAT:                          ('+' | '-')? Float;
INTEGER:                        ('+' | '-')? Integer;
BOOLEAN:                        Boolean;
VALID_STRING:                   ValidString;
VALID_DIAGRAM_VIEW_ID:          ValidDiagramViewId;
