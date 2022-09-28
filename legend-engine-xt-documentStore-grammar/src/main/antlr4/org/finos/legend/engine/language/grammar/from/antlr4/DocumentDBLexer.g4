lexer grammar DocumentDBLexer;

import CoreLexerGrammar;

AND : 'and' ;
OR : 'or' ;
CONSTRAINT : 'constraint' ;
PROCESSING_MILESTONING : 'processing';
DATABASE : 'Database';

INCLUDE : 'include' ;
DOC_EQUAL: 'eq';
DOC_NOTEQUAL : 'neq';
DOC_GT : 'gt';
DOC_GTE : 'gte';
DOC_IN : 'in';
DOC_NOTIN : 'nin';
DOC_LT : 'lt';
DOC_LTE : 'lte';

JOIN : 'Join' ;
MAINCOLLECTIONCMD : '~mainCollection';
COLLECTION : 'Collection' ;
DOCUMENTFRAGMENT : 'DocumentFragment';

PRIMARYKEY : 'PRIMARY KEY';
PRIMARYKEYCMD : '~primaryKey';
PARTIALKEY : 'PARTIAL KEY';
PARTIALKEYCMD : '~partialKey';


INTEGER: ('+' | '-')? (Digit)+;
FLOAT: ('+' | '-')? (Float)+;
QUOTED_STRING:   ('"' ( EscSeq | ~["\r\n] )*  '"' ) ;

VIEW : 'View' ;

//mode ISLAND_BLOCK;
//INNER_CURLY_BRACKET_OPEN : '{' -> pushMode (ISLAND_BLOCK);
//CONTENT: (~[{}])+;
//INNER_CURLY_BRACKET_CLOSE: '}' -> popMode;

