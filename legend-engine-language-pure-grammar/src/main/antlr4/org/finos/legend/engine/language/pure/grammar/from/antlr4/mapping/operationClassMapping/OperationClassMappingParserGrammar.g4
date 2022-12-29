parser grammar OperationClassMappingParserGrammar;

import M3ParserGrammar;

options {
    tokenVocab = OperationClassMappingLexerGrammar;
}



// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                                     VALID_STRING | STRING
                                                | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE
                                                | BYTE_STREAM_FUNCTION      // from M3Parser
;

// -------------------------------------- DEFINITION -------------------------------------

operationClassMapping:          functionPath (parameters | mergeParameters) (SEMI_COLON)?
                                EOF
;
parameters:                     PAREN_OPEN (identifier (COMMA identifier)*)? PAREN_CLOSE
;
functionPath:                   qualifiedName
;

validationLambda:      combinedExpression
;

mergeParameters:         PAREN_OPEN setParameter COMMA validationLambda PAREN_CLOSE
;

setParameter:       BRACKET_OPEN (identifier (COMMA identifier)*)? BRACKET_CLOSE
;

