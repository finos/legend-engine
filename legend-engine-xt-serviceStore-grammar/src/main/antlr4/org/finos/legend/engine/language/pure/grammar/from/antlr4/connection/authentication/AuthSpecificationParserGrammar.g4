parser grammar AuthSpecificationParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AuthSpecificationLexerGrammar;
}

identifier:                      VALID_STRING
;


oauthAuthentication:          OAUTH_AUTHENTICATION
                                            BRACE_OPEN
                                            (
                                                   grantType
                                                   | clientId
                                                   | clientSecret
                                                   |  authServerUrl
                                            )*
                                            BRACE_CLOSE
;

grantType:                                 GRANT_TYPE COLON STRING SEMI_COLON
;

clientId:                                   CLIENT_ID COLON STRING SEMI_COLON
;

clientSecret:                               CLIENT_SECRET_VAULT_REFERENCE COLON STRING SEMI_COLON
;

authServerUrl:                              AUTH_SERVER_URL COLON STRING SEMI_COLON
;

basicAuthentication:                        BASIC_AUTHENTICATION
                                            BRACE_OPEN
                                            (
                                                   username
                                                   | password
                                            )*
                                            BRACE_CLOSE
;

username:                                  USERNAME COLON STRING SEMI_COLON
;

password:                                  PASSWORD COLON STRING SEMI_COLON
;