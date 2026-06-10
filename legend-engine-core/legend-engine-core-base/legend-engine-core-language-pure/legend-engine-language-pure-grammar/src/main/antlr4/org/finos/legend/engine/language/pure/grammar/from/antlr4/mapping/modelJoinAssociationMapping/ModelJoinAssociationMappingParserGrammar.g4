parser grammar ModelJoinAssociationMappingParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = ModelJoinAssociationMappingLexerGrammar;
}

// -------------------------------------- DEFINITION -------------------------------------

modelJoinAssociationMapping:                    combinedExpression EOF
;
