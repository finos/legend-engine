lexer grammar ServiceStoreConnectionLexerGrammar;

import CoreLexerGrammar;

@lexer::members{
  static int lastTokenType=0;
public void emit(Token token) {
  super.emit(token);
  this.lastTokenType = token.getType();
}
}

// -------------------------------------- KEYWORD --------------------------------------

STORE:                                  'store';
BASE_URL:                               'baseUrl';
AUTH_SPECS:                             'auth';

// -------------------------------------- ISLAND ---------------------------------------
BRACE_OPEN:                    '{' {getVocabulary().getSymbolicName(this.lastTokenType).equals("COLON")}?
                               | '{' {pushMode (AUTH_SPECIFICATION_ISLAND_MODE);};


mode AUTH_SPECIFICATION_ISLAND_MODE;
AUTH_SPECIFICATION_ISLAND_OPEN: '{' -> pushMode (AUTH_SPECIFICATION_ISLAND_MODE);
AUTH_SPECIFICATION_ISLAND_CLOSE: '}' -> popMode;
AUTH_SPECIFICATION_CONTENT: (~[{}])+;