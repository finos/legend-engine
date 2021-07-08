lexer grammar DataSourceSpecificationLexerGrammar;

import CoreLexerGrammar;

STATIC_DSP:                                 'Static';
EMBEDDED_H2_DSP:                            'EmbeddedH2';
EMBEDDED_H2_DSP_AUTO_SERVER_MODE:           'autoServerMode';
LOCAL_H2_DSP:                               'LocalH2';
LOCAL_H2_DSP_TEST_DATA_SETUP_CSV:           'testDataSetupCSV';
LOCAL_H2_DSP_TEST_DATA_SETUP_SQLS:          'testDataSetupSqls';
HOST:                                       'host';
PORT:                                       'port';
DIRECTORY:                                  'directory';
NAME:                                       'name';
BRACKET_OPEN:                               '[';
BRACKET_CLOSE:                              ']';


SNOWFLAKE:                                  'Snowflake';
ACCOUNT:                                    'account';
WAREHOUSE:                                  'warehouse';
REGION:                                     'region';
CLOUDTYPE:                                  'cloudType';
QUOTED_IDENTIFIERS_IGNORE_CASE:             'quotedIdentifiersIgnoreCase';

DELTALAKE:                                  'DeltaLake';
SHARD:                                      'shard';
HTTP_PATH:                                  'httpPath';