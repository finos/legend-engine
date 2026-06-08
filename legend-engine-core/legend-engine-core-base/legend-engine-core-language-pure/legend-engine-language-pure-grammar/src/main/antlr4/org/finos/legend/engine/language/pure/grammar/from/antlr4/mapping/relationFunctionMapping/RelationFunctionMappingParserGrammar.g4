parser grammar RelationFunctionMappingParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = RelationFunctionMappingLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------------

identifier:                                 VALID_STRING | STRING | RELATION_FUNC | RELATION_PRIMARY_KEY | ALL | LET | ALL_VERSIONS
                                            | ALL_VERSIONS_IN_RANGE | TO_BYTES_FUNCTION
                                            | BINDING | ENUMERATION_MAPPING | INLINE
;

// -------------------------------------- RELATION FUNCTION MAPPING --------------------------------------

relationFunctionMapping:                    RELATION_FUNC functionIdentifier
                                            primaryKey?
                                            (singlePropertyMapping (COMMA singlePropertyMapping)*)?
                                            EOF
;

primaryKey:                                 RELATION_PRIMARY_KEY COLON (identifier | BRACKET_OPEN identifier (COMMA identifier)* BRACKET_CLOSE)
;

singlePropertyMapping:                      singleLocalPropertyMapping | singleNonLocalPropertyMapping
;

singleLocalPropertyMapping:                 PLUS qualifiedName COLON type multiplicity relationFunctionPropertyMapping
;

singleNonLocalPropertyMapping:              qualifiedName
                                            (
                                                relationFunctionPropertyMapping
                                                | relationFunctionEmbeddedPropertyMapping
                                                | inlineRelationFunctionEmbeddedPropertyMapping
                                            )
;

relationFunctionPropertyMapping:            COLON (transformer)? identifier
;

transformer:                                bindingTransformer | enumTransformer
;

bindingTransformer:                         BINDING qualifiedName COLON
;

enumTransformer:                            ENUMERATION_MAPPING identifier COLON
;

// -------------------------------------- EMBEDDED PROPERTY MAPPING --------------------------------------

relationFunctionEmbeddedPropertyMapping:            PAREN_OPEN
                                                    (
                                                        singlePropertyMapping (COMMA singlePropertyMapping)*
                                                    )?
                                                    PAREN_CLOSE
;

inlineRelationFunctionEmbeddedPropertyMapping:      PAREN_OPEN PAREN_CLOSE INLINE BRACKET_OPEN identifier BRACKET_CLOSE
;