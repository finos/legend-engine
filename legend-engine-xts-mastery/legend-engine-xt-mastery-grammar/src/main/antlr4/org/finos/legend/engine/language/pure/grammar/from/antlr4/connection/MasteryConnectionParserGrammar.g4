parser grammar MasteryConnectionParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = MasteryConnectionLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING | FTP_CONNECTION
                                            | SFTP_CONNECTION | HTTP_CONNECTION | KAFKA_CONNECTION
;
// -------------------------------------- DEFINITION --------------------------------------

definition:                                 imports
                                                (
                                                  ftpConnection
                                                   | httpConnection
                                                   | kafkaConnection
                                               )
                                            EOF
;
imports:                                    (importStatement)*
;
importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;

// -------------------------------------- FTP_CONNECTION --------------------------------------
ftpConnection:                                  (
                                                    host
                                                    | port
                                                    | secure
                                                    | authentication
                                                )*
;
secure:                                     SECURE COLON booleanValue SEMI_COLON
;

// -------------------------------------- HTTP_CONNECTION --------------------------------------
httpConnection:                                (
                                                    url
                                                    | authentication
                                                    | proxy
                                                )*
;
url:                                           URL COLON STRING SEMI_COLON
;
proxy:                                         PROXY COLON
                                                BRACE_OPEN
                                                 (
                                                     host
                                                     | port
                                                     | authentication
                                                 )*
                                                 BRACE_CLOSE SEMI_COLON
;
// -------------------------------------- KAFKA_CONNECTION --------------------------------------
kafkaConnection:                              (
                                                    topicName
                                                    | topicUrls
                                                    | authentication
                                                )*
;
topicName:                                     TOPIC_NAME COLON STRING SEMI_COLON
;
topicUrls:                                     TOPIC_URLS COLON
                                                BRACKET_OPEN
                                                  (
                                                     STRING
                                                      (
                                                          COMMA
                                                          STRING
                                                      )*
                                                  )
                                                  BRACKET_CLOSE SEMI_COLON
;


// -------------------------------------- COMMON --------------------------------------

host:                                       HOST COLON STRING SEMI_COLON
;
authentication:                             AUTHENTICATION COLON islandSpecification SEMI_COLON
;
port:                                       PORT COLON INTEGER SEMI_COLON
;
booleanValue:                               TRUE | FALSE
;

// -------------------------------------- ISLAND SPECIFICATION --------------------------------------
islandSpecification:                        islandType (islandValue)?
;
islandType:                                 identifier
;
islandValue:                                ISLAND_OPEN (islandValueContent)* ISLAND_END
;
islandValueContent:                         ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_BRACE_CLOSE | ISLAND_START | ISLAND_END
;