parser grammar SecuritySchemeParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = SecuritySchemeLexerGrammar;
}

identifier:                                 VALID_STRING | STRING
;

// ----------------------------- SERVICE STORE SECURITY SCHEME-----------------------------

httpSecurityScheme:                         HTTP_SECURITY_SCHEME
                                            BRACE_OPEN
                                            (
                                                scheme
                                                | bearerFormat
                                            )*
                                            BRACE_CLOSE
;

scheme:                                     SCHEME COLON identifier SEMI_COLON
;

bearerFormat:                               BEARER_FORMAT COLON STRING SEMI_COLON
;

apiKeySecurityScheme:                       API_KEY_SECURITY_SCHEME
                                            BRACE_OPEN
                                            (
                                                location
                                                | keyname
                                            )*
                                            BRACE_CLOSE
;

location:                                   LOCATION COLON identifier SEMI_COLON
;
keyname:                                    KEYNAME COLON STRING SEMI_COLON
;
