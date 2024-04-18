parser grammar EnumerationMappingParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = EnumerationMappingLexerGrammar;
}


// ---------------------------------- IDENTIFIER --------------------------------------

identifier:                     VALID_STRING | STRING
;


// ---------------------------------- DEFINITION --------------------------------------

enumerationMapping:             (enumSingleEntryMapping (COMMA enumSingleEntryMapping)*)?
                                EOF
;
enumSingleEntryMapping:         enumName COLON (enumSourceValue | enumMultipleSourceValue)
;
enumMultipleSourceValue:        BRACKET_OPEN enumSourceValue (COMMA enumSourceValue)* BRACKET_CLOSE
;
enumName:                       identifier
;
enumSourceValue:                (STRING | INTEGER | enumReference)
;
enumReference:                  qualifiedName DOT identifier
;
