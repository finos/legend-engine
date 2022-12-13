parser grammar PostProcessorParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = PostProcessorLexerGrammar;
}

identifier:                      VALID_STRING
;

// -------------------------------------- MAPPER -------------------------------------
mapperPostProcessor:             identifier
                                 BRACE_OPEN
                                    mappers
                                    (
                                        order
                                    )*
                                 BRACE_CLOSE
;

order:                           ORDER COLON INTEGER SEMI_COLON
;

mappers:                         MAPPERS COLON BRACKET_OPEN
                                    (mapper (COMMA mapper)*)?
                                 BRACKET_CLOSE SEMI_COLON
;

mapper:                          (tableMapper | schemaMapper)
;


tableMapper:                     TABLE BRACE_OPEN
                                 (
                                    mapperFrom | mapperTo | schemaFrom | schemaTo
                                 )* BRACE_CLOSE
;

schemaFrom:                      SCHEMA_FROM COLON STRING SEMI_COLON
;

schemaTo:                        SCHEMA_TO COLON STRING SEMI_COLON
;

mapperFrom:                      FROM COLON STRING SEMI_COLON
;

mapperTo:                        TO COLON STRING SEMI_COLON
;

schemaMapper:                    SCHEMA BRACE_OPEN
                                 (
                                    mapperFrom | mapperTo
                                 )* BRACE_CLOSE
;