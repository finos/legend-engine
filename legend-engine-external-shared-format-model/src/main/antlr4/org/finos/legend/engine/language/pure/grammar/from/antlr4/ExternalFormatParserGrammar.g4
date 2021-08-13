parser grammar ExternalFormatParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ExternalFormatLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING
                                        | SCHEMA_SET | IMPORT | FORMAT | SCHEMAS
                                        | SCHEMA_ID | SCHEMA_LOCATION| SCHEMA_CONTENT
                                        | BINDING | SCHEMA_SET_REF | SCHEMA_ID_REF | CONTENT_TYPE
                                        | MODEL_INCLUDES | MODEL_EXCLUDES
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             imports
                                            (schemaSet|binding)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;

// -------------------------------------- SCHEMA SET -------------------------------------

schemaSet:                              SCHEMA_SET qualifiedName
                                            BRACE_OPEN
                                            ( format | schemas )*
                                            BRACE_CLOSE
;
format:                                 FORMAT COLON VALID_STRING SEMI_COLON
;
schemas:                                SCHEMAS COLON BRACKET_OPEN schema (COMMA schema)* BRACKET_CLOSE SEMI_COLON
;
schema:                                 BRACE_OPEN
                                            (schemaId|schemaLocation|schemaContent)*
                                        BRACE_CLOSE
;
schemaId:                               SCHEMA_ID COLON identifier SEMI_COLON
;
schemaLocation:                         SCHEMA_LOCATION COLON STRING SEMI_COLON
;
schemaContent:                          SCHEMA_CONTENT COLON STRING SEMI_COLON
;

// -------------------------------------- SCHEMA BINDING -------------------------------------

binding:                                BINDING qualifiedName
                                            BRACE_OPEN
                                            (schemaSetReference | schemaIdReference | contentType | modelIncludes | modelExcludes)*
                                            BRACE_CLOSE
;
schemaSetReference:                     SCHEMA_SET_REF COLON qualifiedName SEMI_COLON
;
schemaIdReference:                      SCHEMA_ID_REF COLON identifier SEMI_COLON
;
contentType:                            CONTENT_TYPE COLON STRING SEMI_COLON
;
modelIncludes:                          MODEL_INCLUDES COLON BRACKET_OPEN (qualifiedName (COMMA qualifiedName)*)? BRACKET_CLOSE SEMI_COLON
;
modelExcludes:                          MODEL_EXCLUDES COLON BRACKET_OPEN (qualifiedName (COMMA qualifiedName)*)? BRACKET_CLOSE SEMI_COLON
;
