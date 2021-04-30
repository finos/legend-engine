lexer grammar RelationalDatabaseConnectionLexerGrammar;

import CoreLexerGrammar;


// -------------------------------------- KEYWORD --------------------------------------

STORE:                                  'store';
TYPE:                                   'type';
RELATIONAL_DATASOURCE_SPEC:             'specification';
RELATIONAL_AUTH_STRATEGY:               'auth';
RELATIONAL_POST_PROCESSORS:             'postProcessors';

DB_TIMEZONE:                            'timezone';
QUOTE_IDENTIFIERS:                      'quoteIdentifiers';

BRACE_OPEN:                             '{' -> pushMode(SPECIFICATION_ISLAND_MODE);

mode SPECIFICATION_ISLAND_MODE;
SPECIFICATION_BRACE_OPEN: '{' -> pushMode (SPECIFICATION_ISLAND_MODE);
SPECIFICATION_BRACE_CLOSE: '}' -> popMode;
SPECIFICATION_CONTENT: (~[{}])+;