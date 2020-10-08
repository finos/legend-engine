parser grammar PureInstanceClassMappingParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = PureInstanceClassMappingLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                                     VALID_STRING | STRING
                                                | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE      // from M3Parser
                                                | ENUMERATION_MAPPING
;


// -------------------------------------- DEFINITION -------------------------------------

// NOTE: line endings are not standard here, for example: mapping source and filter don't end with semi colon
// and the list of mapping lines end with comma (except the last one)
// this is done as an effort to be consistent with legacy grammar, we might want to review this decision in the future

pureInstanceClassMapping:                       (mappingSrc | mappingFilter)*
                                                (propertyMapping (COMMA propertyMapping)*)?
                                                EOF
;
mappingSrc:                                     MAPPING_SRC qualifiedName
;
mappingFilter:                                  MAPPING_FILTER combinedExpression
;
propertyMapping:                                ((PLUS qualifiedName COLON type multiplicity) | qualifiedName (sourceAndTargetMappingId)?) STAR? COLON (ENUMERATION_MAPPING identifier COLON)? combinedExpression
;
sourceAndTargetMappingId:                       BRACKET_OPEN sourceId (COMMA targetId)? BRACKET_CLOSE
;
sourceId:                                       qualifiedName
;
targetId:                                       qualifiedName
;
