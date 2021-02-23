parser grammar XStoreAssociationMappingParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = XStoreAssociationMappingLexerGrammar;
}

// -------------------------------------- DEFINITION -------------------------------------

xStoreAssociationMapping:                       (xStorePropertyMapping (COMMA xStorePropertyMapping)*)?
                                                EOF
;
xStorePropertyMapping:                          qualifiedName (sourceAndTargetMappingId)?  COLON combinedExpression
;
sourceAndTargetMappingId:                       BRACKET_OPEN sourceId COMMA targetId BRACKET_CLOSE
;
sourceId:                                       word
;
targetId:                                       word
;