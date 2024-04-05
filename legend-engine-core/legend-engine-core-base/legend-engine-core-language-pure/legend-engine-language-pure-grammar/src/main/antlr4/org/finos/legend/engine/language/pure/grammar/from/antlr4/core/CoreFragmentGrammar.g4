lexer grammar CoreFragmentGrammar;


// ---------------------------------- BUILDING BLOCK --------------------------------------

fragment True:                          'true'
;
fragment False:                         'false'
;
fragment Letter:                        [A-Za-z]
;
fragment Digit:                         [0-9]
;
fragment HexDigit:                      [0-9a-fA-F]
;


// ------------------------------------ WHITE SPACE ------------------------------------

fragment Whitespace:                    [ \r\t\n]+
;


// -------------------------------------- COMMENT --------------------------------------

fragment Comment:                       '/*' .*? '*/'
;
fragment LineComment:                   '//' ~[\r\n]*
;


// -------------------------------------- ESCAPE -----------------------------------------

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
fragment EscAny:	                    Esc .
;


// -------------------------------------- SPECIFICS --------------------------------------

fragment TimeZone:                      (('+' | '-')(Digit)(Digit)(Digit)(Digit))
;
fragment ValidString:                   (Letter | Digit | '_' ) (Letter | Digit | '_' | '$')*
;
fragment FileName:                      '?[' (Letter | Digit | '_' | '.' | '/')+
;
fragment FileNameEnd:                   ']?'
;
fragment Assign:                        ([ \r\t\n])* '='
;
fragment PathSeparator:                 '::'
;


// ---------------------------------------- TYPE ------------------------------------------

fragment String:                        ('\'' ( EscSeq | ~['\r\n\\] )*  '\'' )
;
fragment Boolean:                       True | False
;
fragment Integer:                       (Digit)+
;
fragment Float:                         (Digit)* '.' (Digit)+ ( ('e' | 'E') ('+' | '-')? (Digit)+)? ('f' | 'F')?
;
fragment Decimal:                       ((Digit)* '.' (Digit)+ | (Digit)+) ( ('e' | 'E') ('+' | '-')? (Digit)+)? ('d' | 'D')
;
fragment Date:                          '%' ('-')? (Digit)+ ('-'(Digit)+ ('-'(Digit)+ ('T' DateTime TimeZone?)?)?)?
;
fragment DateTime:                      (Digit)+ (':'(Digit)+ (':'(Digit)+ ('.'(Digit)+)?)?)?
;
fragment StrictTime:                     '%' (Digit)+ (':'(Digit)+ (':'(Digit)+ ('.'(Digit)+)?)?)?
;


// --------------------------------------- INVALID -------------------------------------------

// Add a rule for INVALID token to the very end of the lexer grammar to make sure your lexer handles all
// input successfully in order to boost performance
// See https://github.com/antlr/antlr4/issues/1540#issuecomment-268738030

fragment Invalid:                       .
;