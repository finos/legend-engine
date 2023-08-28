lexer grammar MasteryConnectionLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// Common
IMPORT:                                     'import';
TRUE:                                       'true';
FALSE:                                      'false';

// Connection
CONNECTION:                                      'Connection';
FTP_CONNECTION:                                  'FTPConnection';
SFTP_CONNECTION:                                 'SFTPConnection';
HTTP_CONNECTION:                                 'HTTPConnection';
KAFKA_CONNECTION:                                'KafkaConnection';
PROXY:                                           'proxy';
HOST:                                            'host';
PORT:                                            'port';
URL:                                             'url';
TOPIC_URLS:                                      'topicUrls';
TOPIC_NAME:                                      'topicName';
SECURE:                                          'secure';

// Authentication
AUTHENTICATION:                                   'authentication';
CREDENTIAL:                                       'credential';