parser grammar NavigationParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = NavigationLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                     VALID_STRING | STRING
                                | VALID_STRING_TYPE
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                     DIVIDE genericType (propertyWithParameters)* (name)?
                                EOF
;


// -------------------------------------- PROPERTY PATH -----------------------------------

propertyWithParameters:         DIVIDE VALID_STRING (PAREN_OPEN (parameter (COMMA parameter)*)? PAREN_CLOSE)?
;
parameter:                      scalar | collection
;
collection:                     BRACKET_OPEN (scalar (COMMA scalar)*)? BRACKET_CLOSE
;
scalar:                         atomic | enumStub
;
enumStub :                      VALID_STRING DOT VALID_STRING
;
atomic:                         BOOLEAN | INTEGER | FLOAT | STRING | DATE | LATEST_DATE
;
name:                           NOT VALID_STRING
;
genericType:                    path? identifier
;
path:                           (identifier PATH_SEPARATOR)+
;