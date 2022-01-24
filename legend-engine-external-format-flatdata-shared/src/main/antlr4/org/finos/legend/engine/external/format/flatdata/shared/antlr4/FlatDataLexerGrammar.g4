lexer grammar FlatDataLexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

SECTION:                            'section';
RECORD:                             'Record';
FORMAT:                             'format';
TIME_ZONE:                          'timeZone';
TRUE_STRING:                        'trueString';
FALSE_STRING:                       'falseString';
OPTIONAL:                           'optional';
RECORD_DATA_TYPE:                   'STRING' | 'INTEGER' | 'BOOLEAN'| 'FLOAT' | 'DECIMAL' | 'DATE' | 'DATETIME';

// ---------------------------------- BUILDING BLOCK --------------------------------------

ADDRESS:                            '{'(Letter | Digit)+'}';

// -------------------------------- FROM CORE GRAMMAR -------------------------------------
// Deliberately copied so that the FlatData sub-grammar of ExternalFormat can parse
// independently of the main compiler modules.

STRING:                                     String;
BRACE_OPEN:                                 '{';
BRACE_CLOSE:                                '}';
BRACKET_OPEN:                               '[';
BRACKET_CLOSE:                              ']';
PAREN_OPEN:                                 '(';
PAREN_CLOSE:                                ')';
COMMA:                                      ',';
EQUAL:                                      '=';
DOT:                                        '.';
COLON:                                      ':';
SEMI_COLON:                                 ';';

WHITESPACE:                                 Whitespace      -> skip;

INTEGER:                                    Integer;
VALID_STRING:                               ValidString;

fragment Whitespace:                    [ \r\t\n]+
;
fragment Letter:                        [A-Za-z]
;
fragment Digit:                         [0-9]
;
fragment HexDigit:                      [0-9a-fA-F]
;
fragment String:                        ('\'' ( EscSeq | ~['\r\n\\] )*  '\'' )
;
fragment UnicodeEsc:	                'u' (HexDigit (HexDigit (HexDigit HexDigit?)?)?)?
;
fragment Esc:                           '\\'
;
fragment EscSeq:	                    Esc
                        		        (
                        		            [btnfr"'\\]	// The standard escaped character set such as tab, newline, etc.
		                                    | UnicodeEsc	// A Unicode escape sequence
		                                    | .				// Invalid escape character
		                                    | EOF			// Incomplete at EOF
		                                )
;
fragment Integer:                       (Digit)+
;
fragment ValidString:                   (Letter | Digit | '_' ) (Letter | Digit | '_' | '$')*
;
