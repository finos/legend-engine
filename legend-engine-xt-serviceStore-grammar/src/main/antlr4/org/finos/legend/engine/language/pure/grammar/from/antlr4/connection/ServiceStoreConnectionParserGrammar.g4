parser grammar ServiceStoreConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ServiceStoreConnectionLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING | STORE | BASE_URL
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             (connectionStore | authentication | baseUrl)*
                                        EOF
;
connectionStore:                        STORE COLON qualifiedName SEMI_COLON
;

authentication:                         AUTH_SPECS COLON
                                        BRACKET_OPEN
                                                ( authSpecificationObject ( COMMA authSpecificationObject )* )?
                                        BRACKET_CLOSE
                                        SEMI_COLON
;

authSpecificationObject:     qualifiedName COLON singleAuthSpecification
;

singleAuthSpecification:     authSpecificationType (authSpecification)?
;

authSpecificationType:       VALID_STRING
;

authSpecification:           BRACE_OPEN (authSpecificationValue)*
;

authSpecificationValue:      AUTH_SPECIFICATION_ISLAND_OPEN | AUTH_SPECIFICATION_CONTENT | AUTH_SPECIFICATION_ISLAND_CLOSE
;

baseUrl:                                BASE_URL COLON identifier SEMI_COLON
;
