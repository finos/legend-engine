parser grammar OperationClassMappingParserGrammar;

import CoreParserGrammar;

options {
    tokenVocab = OperationClassMappingLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                     VALID_STRING | STRING
;


// -------------------------------------- DEFINITION -------------------------------------

operationClassMapping:          functionPath parameters (SEMI_COLON)?
                                EOF
;
parameters:                     PAREN_OPEN (identifier (COMMA identifier)*)? PAREN_CLOSE
;
functionPath:                   qualifiedName
;
