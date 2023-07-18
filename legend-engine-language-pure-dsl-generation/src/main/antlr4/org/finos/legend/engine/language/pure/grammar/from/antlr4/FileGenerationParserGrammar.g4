parser grammar FileGenerationParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = FileGenerationLexerGrammar;
}


// -------------------------------------- BUILDING BLOCK -------------------------------------

identifier:                             VALID_STRING | STRING
                                        | FILE_GENERATION
                                        | IMPORT
                                        | GENERATION_OUTPUT_PATH
                                        | SCOPE_ELEMENTS
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             imports
                                            (fileGeneration)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
fileGeneration:                         identifier qualifiedName
                                            BRACE_OPEN
                                                (scopeElements | generationOutputPath | configProperty)*
                                            BRACE_CLOSE
;


// -------------------------------------- SCOPE ------------------------------------

scopeElements:                          SCOPE_ELEMENTS COLON elementsArray SEMI_COLON
;
generationOutputPath:                   GENERATION_OUTPUT_PATH COLON STRING SEMI_COLON
;
elementsArray:                          BRACKET_OPEN
                                            (qualifiedName (COMMA qualifiedName)*)?
                                        BRACKET_CLOSE
;


// -------------------------------------- CONFIG ------------------------------------

configProperty:                         configPropertyName COLON configPropertyValue SEMI_COLON
;
configPropertyName:                     identifier (DOT identifier)*
;
configPropertyValue:                    INTEGER | STRING | BOOLEAN | stringArray | integerArray | configMap
;
stringArray:                            BRACKET_OPEN
                                            (STRING (COMMA STRING)*)?
                                        BRACKET_CLOSE
;
integerArray:                           BRACKET_OPEN
                                            (INTEGER (COMMA INTEGER)*)?
                                        BRACKET_CLOSE
;
configMap:                              BRACE_OPEN
                                           (configProperty)*
                                        BRACE_CLOSE
;
