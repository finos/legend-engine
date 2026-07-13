parser grammar RelationFunctionMappingParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = RelationFunctionMappingLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------------

identifier:                                 VALID_STRING | STRING | RELATION_FUNC | RELATION_SRC | RELATION_PRIMARY_KEY | ALL | LET | ALL_VERSIONS
                                            | ALL_VERSIONS_IN_RANGE | TO_BYTES_FUNCTION
                                            | BINDING | ENUMERATION_MAPPING | INLINE
;

// -------------------------------------- RELATION FUNCTION MAPPING --------------------------------------

relationFunctionMapping:                    relationSource
                                            primaryKey?
                                            (singlePropertyMapping (COMMA singlePropertyMapping)*)?
                                            EOF
;

// ~func references an existing Pure function by descriptor or qualified name.
// ~src takes an inline zero-arg Pure expression that evaluates to a Relation —
// the walker wraps it in a synthetic `{ <expr>}` lambda so the rest of the
// pipeline can treat both forms uniformly.
relationSource:                             RELATION_FUNC functionIdentifier
                                            | RELATION_SRC combinedExpression
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

// Property RHS — bare `columnName` (legacy, lowered to `$src.<col>`) or a full
// Pure expression over `$src`. The bare-column form is matched by `identifier`
// alone (a single identifier token), so anything more complex (starting with
// `$`, containing operators / function calls) falls through to combinedExpression.
relationFunctionPropertyMapping:            COLON (transformer)? (identifier | combinedExpression)
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

