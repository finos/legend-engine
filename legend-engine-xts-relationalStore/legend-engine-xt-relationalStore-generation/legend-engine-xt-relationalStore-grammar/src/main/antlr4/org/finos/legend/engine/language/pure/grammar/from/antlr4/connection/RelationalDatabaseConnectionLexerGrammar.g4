lexer grammar RelationalDatabaseConnectionLexerGrammar;

import CoreLexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

STORE:                                  'store';
TYPE:                                   'type';
MODE:                                   'mode';
RELATIONAL_DATASOURCE_SPEC:             'specification';
RELATIONAL_AUTH_STRATEGY:               'auth';
RELATIONAL_POST_PROCESSORS:             'postProcessors';
QUERY_TIMEOUT:                          'queryTimeOutInSeconds';

DB_TIMEZONE:                            'timezone';
TIMEZONE:                                TimeZone;

QUOTE_IDENTIFIERS:                      'quoteIdentifiers';

QUERY_GENERATION_CONFIGS:               'queryGenerationConfigs';

BRACE_OPEN:                             '{' -> pushMode(SPECIFICATION_ISLAND_MODE);

mode SPECIFICATION_ISLAND_MODE;
SPECIFICATION_BRACE_OPEN: '{' -> pushMode (SPECIFICATION_ISLAND_MODE);
SPECIFICATION_BRACE_CLOSE: '}' -> popMode;
SPECIFICATION_CONTENT: (~[{}])+;