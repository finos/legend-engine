parser grammar DiagramParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DiagramLexerGrammar;
}


// ---------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING
                                        | DIAGRAM | IMPORT
                                        | CLASS_VIEW | PROPERTY_VIEW | GENERALIZATION_VIEW
                                        | CLASS | HIDE_PROPERTIES | HIDE_TAGGED_VALUE | HIDE_STEREOTYPE | POSITION | RECTANGLE
                                        | PROPERTY | POINTS | SOURCE | TARGET
;


// ---------------------------------- DEFINITION -------------------------------------

definition:                             imports
                                            (diagram)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
diagram:                                DIAGRAM qualifiedName
                                            BRACE_OPEN
                                                (classView | propertyView | generalizationView)*
                                            BRACE_CLOSE
;


// ---------------------------------- CLASS VIEW (NODE) -------------------------------------

classView:                              CLASS_VIEW viewId
                                            BRACE_OPEN
                                                (
                                                    classViewClassProp
                                                    | classViewPositionProp
                                                    | classViewRectangleProp
                                                    | classViewHidePropertiesProp
                                                    | classViewHideTaggedValueProp
                                                    | classViewHideStereotypeProp
                                                )*
                                            BRACE_CLOSE
;
classViewClassProp:                     CLASS COLON qualifiedName SEMI_COLON
;
classViewPositionProp:                  POSITION COLON numberPair SEMI_COLON
;
classViewRectangleProp:                 RECTANGLE COLON numberPair SEMI_COLON
;
classViewHidePropertiesProp:            HIDE_PROPERTIES COLON BOOLEAN SEMI_COLON
;
classViewHideTaggedValueProp:           HIDE_TAGGED_VALUE COLON BOOLEAN SEMI_COLON
;
classViewHideStereotypeProp:            HIDE_STEREOTYPE COLON BOOLEAN SEMI_COLON
;


// ---------------------------------- RELATIONSHIP VIEW (EDGE) -------------------------------------

propertyView:                           PROPERTY_VIEW
                                            BRACE_OPEN
                                                (
                                                    propertyHolderViewPropertyProp
                                                    | relationshipViewSourceProp
                                                    | relationshipViewTargetProp
                                                    | relationshipViewPointsProp
                                                )*
                                            BRACE_CLOSE
;
generalizationView:                     GENERALIZATION_VIEW
                                            BRACE_OPEN
                                                (
                                                    relationshipViewSourceProp
                                                    | relationshipViewTargetProp
                                                    | relationshipViewPointsProp
                                                )*
                                            BRACE_CLOSE
;
relationshipViewPointsProp:             POINTS COLON BRACKET_OPEN ( numberPair (COMMA numberPair)* )? BRACKET_CLOSE SEMI_COLON
;
relationshipViewSourceProp:             SOURCE COLON viewId SEMI_COLON
;
relationshipViewTargetProp:             TARGET COLON viewId SEMI_COLON
;
propertyHolderViewPropertyProp:         PROPERTY COLON qualifiedName DOT identifier SEMI_COLON
;
numberPair:                             PAREN_OPEN number COMMA number PAREN_CLOSE
;


// -------------------------------------- BUILDING BLOCK -------------------------------------

viewId:                                 identifier | VALID_DIAGRAM_VIEW_ID
;
number:                                 FLOAT | INTEGER
;
