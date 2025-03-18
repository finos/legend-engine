parser grammar FunctionJarParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = FunctionJarLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                             VALID_STRING | STRING
                                        | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE | TO_BYTES_FUNCTION      // from M3Parser
                                        | STEREOTYPES | TAGS
                                        | FUNCTIONJAR | IMPORT
                                        | FUNCTIONJAR_OWNERSHIP | FUNCTIONJAR_DOCUMENTATION
                                        | FUNCTIONJAR_FUNCTION | FUNCTIONJAR_ACTIVATION
                                        | FUNCTIONJAR_OWNERSHIP_DEPLOYMENT | FUNCTIONJAR_OWNERSHIP_DEPLOYMENT_IDENTIFIER
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                             imports
                                            (functionJar)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
functionJar:                                FUNCTIONJAR stereotypes? taggedValues? qualifiedName
                                            BRACE_OPEN
                                                (
                                                    functionJarOwnership
                                                    | functionJarDocumentation
                                                    | functionJarFunc
                                                    | functionJarActivationConfiguration
                                                )*
                                            BRACE_CLOSE
;

actionBody:                             ISLAND_OPEN actionValue
;
actionValue:                            (ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_BRACE_CLOSE | ISLAND_END)*
;
actionType:                             VALID_STRING
;
stereotypes:                            LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN
;
stereotype:                             qualifiedName DOT identifier
;
taggedValues:                           BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE
;
taggedValue:                            qualifiedName DOT identifier EQUAL STRING
;

functionJarActivationConfiguration:         FUNCTIONJAR_ACTIVATION COLON qualifiedName SEMI_COLON
;

functionJarOwnership:                          FUNCTIONJAR_OWNERSHIP COLON deployment SEMI_COLON
;

deployment:                             FUNCTIONJAR_OWNERSHIP_DEPLOYMENT
                                            BRACE_OPEN
                                                FUNCTIONJAR_OWNERSHIP_DEPLOYMENT_IDENTIFIER COLON STRING
                                            BRACE_CLOSE
;


functionJarDocumentation:                   FUNCTIONJAR_DOCUMENTATION COLON STRING SEMI_COLON
;

// -------------------------------------- EXECUTION --------------------------------------

functionJarFunc:                            FUNCTIONJAR_FUNCTION COLON functionIdentifier SEMI_COLON
;
