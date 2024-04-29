/*
 * Licensed to Crate.io GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

lexer grammar SqlBaseLexer;

options {
   superClass = AbstractSqlBaseLexer;
}

/*
 * Antlr does not generate an import statement for SqlBaseLexer to be able to extend AbstractSqlBaseLexer.
 * seems like a known issue? https://github.com/antlr/antlr4/issues/3124
 * The workaround is found from: https://github.com/tunnelvisionlabs/antlr4ts/issues/483
 */
@lexer::header {
import org.finos.legend.engine.language.sql.grammar.from.AbstractSqlBaseLexer;
}

AUTHORIZATION: A U T H O R I Z A T I O N;
SELECT: S E L E C T;
FROM: F R O M ;
TO: T O;
AS: A S;
AT: A T;
ALL: A L L;
ANY: A N Y;
SOME: S O M E;
DEALLOCATE: D E A L L O C A T E;
DIRECTORY: D I R E C T O R Y;
DISTINCT: D I S T I N C T;
WHERE: W H E R E;
GROUP: G R O U P;
BY: B Y;
ORDER: O R D E R;
HAVING: H A V I N G;
LIMIT: L I M I T ;
OFFSET: O F F S E T;
OR: O R;
AND: A N D;
IN: I N;
NOT: N O T;
EXISTS: E X I S T S;
BETWEEN: B E T W E E N;
LIKE: L I K E;
ILIKE: I L I K E;
IS: I S;
NULL: N U L L ;
TRUE: T R U E;
FALSE: F A L S E;
IGNORE: I G N O R E;
RESPECT: R E S P E C T;
NULLS: N U L L S;
FETCH: F E T C H;
FIRST: F I R S T;
LAST: L A S T;
NEXT: N E X T;
ESCAPE: E S C A P E;
ASC: A S C;
DESC: D E S C;
SUBSTRING: S U B S T R I N G;
TRIM: T R I M;
LEADING: L E A D I N G;
TRAILING: T R A I L I N G;
BOTH: B O T H;
FOR: F O R;
TIME: T I M E;
ZONE: Z O N E;
YEAR: Y E A R;
MONTH: M O N T H;
DAY: D A Y;
HOUR: H O U R;
MINUTE: M I N U T E;
SECOND: S E C O N D;
CURRENT_DATE: C U R R E N T '_' D A T E;
CURRENT_TIME: C U R R E N T '_' T I M E;
CURRENT_TIMESTAMP: C U R R E N T '_' T I M E S T A M P;
CURRENT_SCHEMA: C U R R E N T '_' S C H E M A;
CURRENT_USER: C U R R E N T '_' U S E R;
SESSION_USER: S E S S I O N '_' U S E R;
EXTRACT: E X T R A C T;
CASE: C A S E;
WHEN: W H E N;
THEN: T H E N ;
ELSE: E L S E;
END: E N D;
IF: I F;
INTERVAL: I N T E R V A L;
JOIN: J O I N;
CROSS: C R O S S;
OUTER: O U T E R;
INNER: I N N E R;
LEFT: L E F T;
RIGHT: R I G H T;
FULL: F U L L;
NATURAL: N A T U R A L;
USING: U S I N G;
ON: O N;
OVER: O V E R;
WINDOW: W I N D O W;
PARTITION: P A R T I T I O N;
PROMOTE: P R O M O T E;
RANGE: R A N G E;
ROWS: R O W S;
UNBOUNDED: U N B O U N D E D;
PRECEDING: P R E C E D I N G;
FOLLOWING: F O L L O W I N G;
CURRENT: C U R R E N T;
ROW: R O W;
WITH: W I T H;
WITHOUT: W I T H O U T;
RECURSIVE: R E C U R S I V E;
CREATE: C R E A T E;
BLOB: B L O B;
TABLE: T A B L E;
SWAP: S W A P;
GC: G C;
DANGLING: D A N G L I N G;
ARTIFACTS: A R T I F A C T S;
DECOMMISSION: D E C O M M I S S I O N;
CLUSTER: C L U S T E R;
REPOSITORY: R E P O S I T O R Y;
SNAPSHOT: S N A P S H O T;
ALTER: A L T E R;
KILL: K I L L;
ONLY: O N L Y;

ADD: A D D;
COLUMN: C O L U M N;

OPEN: O P E N;
CLOSE: C L O S E;

RENAME: R E N A M E;

REROUTE: R E R O U T E;
MOVE: M O V E;
SHARD: S H A R D;
ALLOCATE: A L L O C A T E;
REPLICA: R E P L I C A;
CANCEL: C A N C E L;
RETRY: R E T R Y;
FAILED: F A I L E D;

BOOLEAN: B O O L E A N;
BYTE: B Y T E;
SHORT: S H O R T;
INTEGER: I N T E G E R;
INT: I N T;
LONG: L O N G;
FLOAT: F L O A T;
DOUBLE: D O U B L E;
PRECISION: P R E C I S I O N;
TIMESTAMP: T I M E S T A M P;
IP: I P;
CHARACTER: C H A R A C T E R;
CHAR_SPECIAL: '"CHAR"';
VARYING: V A R Y I N G;
OBJECT: O B J E C T;
STRING_TYPE: S T R I N G;
GEO_POINT: G E O '_' P O I N T;
GEO_SHAPE: G E O '_' S H A P E;
GLOBAL : G L O B A L;
SESSION : S E S S I O N;
LOCAL : L O C A L;
LICENSE : L I C E N S E;

BEGIN: B E G I N;
START: S T A R T;
COMMIT: C O M M I T;
WORK: W O R K;
TRANSACTION: T R A N S A C T I O N;
TRANSACTION_ISOLATION: T R A N S A C T I O N '_' I S O L A T I O N;
CHARACTERISTICS: C H A R A C T E R I S T I C S;
ISOLATION: I S O L A T I O N;
LEVEL: L E V E L;
SERIALIZABLE: S E R I A L I Z A B L E;
REPEATABLE: R E P E A T A B L E;
COMMITTED: C O M M I T T E D;
UNCOMMITTED: U N C O M M I T T E D;
READ: R E A D;
WRITE: W R I T E;
DEFERRABLE: D E F E R R A B L E;

RETURNS: R E T U R N S;
CALLED: C A L L E D;
REPLACE: R E P L A C E;
FUNCTION: F U N C T I O N;
LANGUAGE: L A N G U A G E;
INPUT: I N P U T;

ANALYZE: A N A L Y Z E;
DISCARD: D I S C A R D;
PLANS: P L A N S;
SEQUENCES: S E Q U E N C E S;
TEMPORARY: T E M P O R A R Y;
TEMP: T E M P;
CONSTRAINT: C O N S T R A I N T;
CHECK: C H E C K;
DESCRIBE: D E S C R I B E;
EXPLAIN: E X P L A I N;
FORMAT: F O R M A T;
TYPE: T Y P E;
TEXT: T E X T;
GRAPHVIZ: G R A P H V I Z;
LOGICAL: L O G I C A L;
DISTRIBUTED: D I S T R I B U T E D;
CAST: C A S T;
TRY_CAST: T R Y '_' C A S T;
SHOW: S H O W;
TABLES: T A B L E S;
SCHEMAS: S C H E M A S;
CATALOGS: C A T A L O G S;
COLUMNS: C O L U M N S;
PARTITIONS: P A R T I T I O N S;
FUNCTIONS: F U N C T I O N S;
MATERIALIZED: M A T E R I A L I Z E D;
VIEW: V I E W;
OPTIMIZE: O P T I M I Z E;
REFRESH: R E F R E S H;
RESTORE: R E S T O R E;
DROP: D R O P;
ALIAS: A L I A S;
UNION: U N I O N;
EXCEPT: E X C E P T;
INTERSECT: I N T E R S E C T;
SYSTEM: S Y S T E M;
BERNOULLI: B E R N O U L L I;
TABLESAMPLE: T A B L E S A M P L E;
STRATIFY: S T R A T I F Y;
INSERT: I N S E R T;
INTO: I N T O;
VALUES: V A L U E S;
DELETE: D E L E T E;
UPDATE: U P D A T E;
KEY: K E Y;
DUPLICATE: D U P L I C A T E;
CONFLICT: C O N F L I C T;
DO: D O;
NOTHING: N O T H I N G;
SET: S E T;
RESET: R E S E T;
DEFAULT: D E F A U L T;
COPY: C O P Y;
CLUSTERED: C L U S T E R E D;
SHARDS: S H A R D S;
PRIMARY_KEY: P R I M A R Y ' ' K E Y;
OFF: O F F;
FULLTEXT: F U L L T E X T;
FILTER: F I L T E R;
PLAIN: P L A I N;
INDEX: I N D E X;
STORAGE: S T O R A G E;
RETURNING: R E T U R N I N G;

DYNAMIC: D Y N A M I C;
STRICT: S T R I C T;
IGNORED: I G N O R E D;

ARRAY: A R R A Y;

ANALYZER: A N A L Y Z E R;
EXTENDS: E X T E N D S;
TOKENIZER: T O K E N I Z E R;
TOKEN_FILTERS: T O K E N '_' F I L T E R S;
CHAR_FILTERS: C H A R '_' F I L T E R S;

PARTITIONED: P A R T I T I O N E D;
PREPARE: P R E P A R E;

TRANSIENT: T R A N S I E N T;
PERSISTENT: P E R S I S T E N T;

MATCH: M A T C H;

GENERATED: G E N E R A T E D;
ALWAYS: A L W A Y S;

USER: U S E R ;
GRANT: G R A N T;
DENY: D E N Y;
REVOKE: R E V O K E;
PRIVILEGES: P R I V I L E G E S;
SCHEMA: S C H E M A;

RETURN: R E T U R N;
SUMMARY: S U M M A R Y;

METADATA: M E T A D A T A;

PUBLICATION: P U B L I C A T I O N;
SUBSCRIPTION: S U B S C R I P T I O N;
CONNECTION: C O N N E C T I O N;
ENABLE: E N A B L E;
DISABLE: D I S A B L E;

DECLARE: D E C L A R E;
CURSOR: C U R S O R;
ASENSITIVE: A S E N S I T I V E;
INSENSITIVE: I N S E N S I T I V E;
BINARY: B I N A R Y;
NO: N O;
SCROLL: S C R O L L;
HOLD: H O L D;
ABSOLUTE: A B S O L U T E;
FORWARD: F O R W A R D;
BACKWARD: B A C K W A R D;
RELATIVE: R E L A T I V E;
PRIOR: P R I O R;
WITHIN: W I T H I N;

EQ  : '=';
NEQ : '<>' | '!=';
LT  : '<';
LTE : '<=';
GT  : '>';
GTE : '>=';
LLT  : '<<';
REGEX_MATCH: '~';
REGEX_NO_MATCH: '!~';
REGEX_MATCH_CI: '~*';
REGEX_NO_MATCH_CI: '!~*';
OP_LIKE: '~~';
OP_ILIKE: '~~*';
OP_NOT_LIKE: '!~~';
OP_NOT_ILIKE: '!~~*';

PLUS: '+';
MINUS: '-';
ASTERISK: '*';
SLASH: '/';
PERCENT: '%';
CARET: '^';
CONCAT: '||';
CAST_OPERATOR: '::';
SEMICOLON: ';';
COLON: ':';
COMMA: ',';
DOT: '.';
OPEN_ROUND_BRACKET: '(';
CLOSE_ROUND_BRACKET: ')';
OPEN_CURLY_BRACKET: '{';
CLOSE_CURLY_BRACKET: '}';
OPEN_SQUARE_BRACKET: '[';
CLOSE_SQUARE_BRACKET: ']';
EMPTY_SQUARE_BRACKET: '[]';
QUESTION: '?';
DOLLAR: '$';
BITWISE_AND: '&';
BITWISE_OR: '|';
BITWISE_XOR: '#';

STRING
    : '\'' ( ~'\'' | '\'\'' )* '\''
    ;

ESCAPED_STRING
    : E '\'' ( ~'\'' | '\'\'' | '\\\'' )* '\''
    ;

BIT_STRING
    : B '\'' ([0-1])* '\''
    ;


INTEGER_VALUE
    : DIGIT+
    ;

DECIMAL_VALUE
    : DIGIT+ '.' DIGIT*
    | '.' DIGIT+
    | DIGIT+ ('.' DIGIT*)? EXPONENT
    | '.' DIGIT+ EXPONENT
    ;

IDENTIFIER
    : (LETTER | '_') (LETTER | DIGIT | '_' | '@')*
    ;

DIGIT_IDENTIFIER
    : DIGIT (LETTER | DIGIT | '_' | '@')+
    ;

QUOTED_IDENTIFIER
    : '"' ( ~'"' | '""' )* '"'
    ;

BACKQUOTED_IDENTIFIER
    : '`' ( ~'`' | '``' )* '`'
    ;

BEGIN_DOLLAR_QUOTED_STRING
   : '$' TAG? '$'
   {pushTag();} -> pushMode (DollarQuotedStringMode)
   ;

fragment TAG
    : IDENTIFIER
    ;

fragment EXPONENT
    : E [+-]? DIGIT+
    ;

fragment DIGIT
    : [0-9]
    ;

fragment LETTER
    : [A-Za-z]
    ;

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];


COMMENT
    : ('--' ~[\r\n]* '\r'? '\n'? | '/*' .*? '*/') -> channel(HIDDEN)
    ;

WS
    : [ \r\n\t]+ -> channel(HIDDEN)
    ;

UNRECOGNIZED
    : .
    ;

mode DollarQuotedStringMode;
DOLLAR_QUOTED_STRING_BODY
   : ~ '$'+
   // | '$'([0-9])+
   // this alternative improves the efficiency of handling $ characters within a dollar-quoted string which are
   // not part of the ending tag.
   | '$' ~ '$'*
   ;

END_DOLLAR_QUOTED_STRING
   : ('$' TAG? '$')
   {isTag()}?
   {popTag();} -> popMode
   ;