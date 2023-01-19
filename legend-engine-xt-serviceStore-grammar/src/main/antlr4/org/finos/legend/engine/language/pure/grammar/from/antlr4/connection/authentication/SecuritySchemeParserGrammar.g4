parser grammar SecuritySchemeParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = SecuritySchemeLexerGrammar;
}

identifier:                      VALID_STRING
;

// ----------------------------- SERVICE STORE SECURITY SCHEME-----------------------------

httpSecurityScheme:                         SIMPLE_HTTP_SECURITY_SCHEME
                                            BRACE_OPEN
                                                   ( scheme )*
                                            BRACE_CLOSE
;

scheme:                                     SCHEME COLON STRING SEMI_COLON
;

apiKeySecurityScheme:                       API_KEY_SECURITY_SCHEME
                                            BRACE_OPEN
                                                (
                                                    location
                                                    | keyname
                                                )*
                                            BRACE_CLOSE
;

location:                                   LOCATION COLON STRING SEMI_COLON
;
keyname:                                    KEYNAME COLON STRING SEMI_COLON
;
