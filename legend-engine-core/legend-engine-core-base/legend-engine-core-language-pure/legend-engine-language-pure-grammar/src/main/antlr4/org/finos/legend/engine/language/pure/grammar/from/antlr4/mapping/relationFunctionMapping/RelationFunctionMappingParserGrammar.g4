parser grammar RelationFunctionMappingParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = RelationFunctionMappingLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------------

identifier:                                 VALID_STRING | STRING | RELATION_FUNC | BINDING | ALL | LET | ALL_VERSIONS 
                                            | ALL_VERSIONS_IN_RANGE | TO_BYTES_FUNCTION
;

// -------------------------------------- RELATION FUNCTION MAPPING --------------------------------------

relationFunctionMapping:                    RELATION_FUNC functionIdentifier
                                            (singlePropertyMapping (COMMA singlePropertyMapping)*)?
                                            EOF
;

singlePropertyMapping:                      singleLocalPropertyMapping | singleNonLocalPropertyMapping
;

singleLocalPropertyMapping:                 PLUS qualifiedName COLON type multiplicity relationFunctionPropertyMapping
;

singleNonLocalPropertyMapping:              qualifiedName relationFunctionPropertyMapping
;

relationFunctionPropertyMapping:            COLON (transformer)? identifier
;

transformer:                                bindingTransformer
;

bindingTransformer:                         BINDING qualifiedName COLON
;
