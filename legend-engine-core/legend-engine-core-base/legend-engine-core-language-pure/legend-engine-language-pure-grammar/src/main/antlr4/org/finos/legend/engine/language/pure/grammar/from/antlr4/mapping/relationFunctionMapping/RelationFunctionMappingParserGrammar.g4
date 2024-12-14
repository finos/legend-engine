parser grammar RelationFunctionMappingParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = RelationFunctionMappingLexerGrammar;
}

// -------------------------------------- RELATION MAPPING --------------------------------------

relationMapping:                            RELATION_FUNC qualifiedName
                                            (singlePropertyMapping (COMMA singlePropertyMapping)*)?
                                            EOF
;

singlePropertyMapping:                      ((PLUS qualifiedName COLON type multiplicity) | qualifiedName) COLON identifier
;

