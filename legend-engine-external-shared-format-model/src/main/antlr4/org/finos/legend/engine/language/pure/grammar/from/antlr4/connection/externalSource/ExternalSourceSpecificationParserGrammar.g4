parser grammar ExternalSourceSpecificationParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ExternalSourceSpecificationLexerGrammar;
}

identifier:                      VALID_STRING | STRING | URL_STREAM_ESP | URL | PARAMETER_ESP | NAME
;

// ----------------------------- EXTERNAL FORMAT CONNECTION EXTERNAL SOURCE SPEC -----------------------------

urlStreamExternalSourceSpecification:           URL_STREAM_ESP
                                                BRACE_OPEN
                                                    urlStreamUrl*
                                                BRACE_CLOSE
;
urlStreamUrl:                               URL COLON STRING SEMI_COLON
;

parameterExternalSourceSpecification:           PARAMETER_ESP
                                                BRACE_OPEN
                                                    parameterName*
                                                BRACE_CLOSE
;
parameterName:                                  NAME COLON STRING SEMI_COLON
;
