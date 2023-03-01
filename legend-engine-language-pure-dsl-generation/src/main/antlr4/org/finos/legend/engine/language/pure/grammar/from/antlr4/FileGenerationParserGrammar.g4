parser grammar FileGenerationParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = FileGenerationLexerGrammar;
}


// -------------------------------------- BUILDING BLOCK -------------------------------------

identifier:                             VALID_STRING | STRING
                                        | FILE_GENERATION
                                        | SCHEMA_GENERATION
                                        | IMPORT
                                        | GENERATION_OUTPUT_PATH
                                        | SCOPE_ELEMENTS
                                        | SCHEMA_FORMAT
                                        | MODEL_INCLUDES
                                        | MODEL_EXCLUDES
                                        | SCHEMA_CONFIG
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             imports
                                            (elementDefinition)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
elementDefinition:                        (
                                          schemaGeneration | fileGeneration
                                          )

;


// -------------------------------------- DEPRECATED FILE ------------------------------------

fileGeneration:                         identifier qualifiedName
                                            BRACE_OPEN
                                                (scopeElements | generationOutputPath | configProperty)*
                                            BRACE_CLOSE
;

scopeElements:                          SCOPE_ELEMENTS COLON elementsArray SEMI_COLON
;
generationOutputPath:                   GENERATION_OUTPUT_PATH COLON STRING SEMI_COLON
;
elementsArray:                          BRACKET_OPEN
                                            (qualifiedName (COMMA qualifiedName)*)?
                                        BRACKET_CLOSE
;



// -------------------------------------- SCHEMA GENERATION -------------------------------------

schemaGeneration:                         SCHEMA_GENERATION qualifiedName
                                            BRACE_OPEN
                                                (formatType | modelIncludes | modelExcludes| config)*
                                            BRACE_CLOSE
;

formatType:                             SCHEMA_FORMAT COLON STRING SEMI_COLON
;
modelIncludes:                          MODEL_INCLUDES COLON BRACKET_OPEN (qualifiedName (COMMA qualifiedName)*)? BRACKET_CLOSE SEMI_COLON
;
modelExcludes:                          MODEL_EXCLUDES COLON BRACKET_OPEN (qualifiedName (COMMA qualifiedName)*)? BRACKET_CLOSE SEMI_COLON
;
config:                                 SCHEMA_CONFIG COLON
                                          BRACE_OPEN
                                            configValue
                                          BRACE_CLOSE
                                        SEMI_COLON
;
configValue:                             (configProperty)*
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

