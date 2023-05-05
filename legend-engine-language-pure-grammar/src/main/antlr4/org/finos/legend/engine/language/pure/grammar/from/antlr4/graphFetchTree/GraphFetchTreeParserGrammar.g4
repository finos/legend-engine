parser grammar GraphFetchTreeParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = GraphFetchTreeLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                         VALID_STRING | STRING
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                         qualifiedName graphDefinition EOF
;
graphDefinition:                    BRACE_OPEN
                                        graphPaths
                                    BRACE_CLOSE
;
graphPaths:                         graphPath (COMMA graphPath)*
;
graphPath:                          alias? identifier propertyParameters? subtype? graphDefinition?
;
alias:                              STRING COLON
;
propertyParameters:                 PAREN_OPEN
                                        (parameter (COMMA parameter)*)?
                                    PAREN_CLOSE
;
subtype:                            SUBTYPE_START qualifiedName PAREN_CLOSE
;
parameter:                          scalarParameter | collectionParameter
;
scalarParameter:                    LATEST_DATE | instanceLiteral | variable | enumReference
;
collectionParameter:                BRACKET_OPEN
                                        (scalarParameter (COMMA scalarParameter)*)?
                                    BRACKET_CLOSE
;
instanceLiteral:                    instanceLiteralToken | (MINUS INTEGER) | (MINUS FLOAT) | (MINUS DECIMAL) | (PLUS INTEGER) | (PLUS FLOAT) | (PLUS DECIMAL)
;
instanceLiteralToken:               STRING | INTEGER | FLOAT | DECIMAL | DATE | BOOLEAN
;
variable:                           DOLLAR identifier
;
enumReference:                      qualifiedName DOT identifier
;
