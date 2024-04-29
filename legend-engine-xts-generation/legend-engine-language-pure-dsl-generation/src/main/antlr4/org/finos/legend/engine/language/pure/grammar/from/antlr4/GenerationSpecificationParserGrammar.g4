parser grammar GenerationSpecificationParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = GenerationSpecificationLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                             VALID_STRING | STRING
                                        | IMPORT
                                        | GENERATION_ELEMENT | GENERATION_NODES | FILE_GENERATIONS
                                        | GENERATION_SPECIFICATION | NODE_ID
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                             imports
                                            (generationSpecification)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
generationSpecification:                GENERATION_SPECIFICATION qualifiedName
                                            BRACE_OPEN
                                                (generationNodes
                                                | fileGenerations)*
                                            BRACE_CLOSE
;
fileGenerations:                        FILE_GENERATIONS COLON fileGenerationsValues SEMI_COLON
;
fileGenerationsValues:                  BRACKET_OPEN
                                            (qualifiedName (COMMA qualifiedName)*)?
                                        BRACKET_CLOSE
;
generationNodes:                        GENERATION_NODES COLON generationNodesValues SEMI_COLON
;
generationNodesValues:                  BRACKET_OPEN
                                            (generationNode (COMMA generationNode)*)?
                                        BRACKET_CLOSE
;
generationNode:                         BRACE_OPEN
                                            (nodeId)?
                                            generationElement
                                        BRACE_CLOSE
;
generationElement:                      GENERATION_ELEMENT COLON qualifiedName SEMI_COLON
;
nodeId:                                 NODE_ID COLON STRING SEMI_COLON
;
