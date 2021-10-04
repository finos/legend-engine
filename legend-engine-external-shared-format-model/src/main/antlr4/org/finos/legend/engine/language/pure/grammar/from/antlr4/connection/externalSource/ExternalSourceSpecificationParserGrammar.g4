parser grammar ExternalSourceSpecificationParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ExternalSourceSpecificationLexerGrammar;
}

identifier:                      VALID_STRING
;

// ----------------------------- EXTERNAL FORMAT CONNECTION EXTERNAL SOURCE SPEC -----------------------------

urlStreamExternalSourceSpecification:           URL_STREAM_ESP
                                                BRACE_OPEN
                                                    urlStreamUrl*
                                                BRACE_CLOSE
;
urlStreamUrl:                               URL COLON STRING SEMI_COLON
;
