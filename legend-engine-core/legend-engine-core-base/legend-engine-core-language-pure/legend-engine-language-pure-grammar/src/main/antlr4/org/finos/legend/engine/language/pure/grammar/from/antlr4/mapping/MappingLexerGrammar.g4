lexer grammar MappingLexerGrammar;

import M3LexerGrammar;

@lexer::members{
  static int lastTokenType=0;
public void emit(Token token) {
  super.emit(token);
  this.lastTokenType = token.getType();
}
}

// -------------------------------------- KEYWORD --------------------------------------
MAPPING:                                'Mapping';
IMPORT:                                 'import';
INCLUDE:                                'include';
TESTS:                                  'MappingTests';
EXTENDS:                                'extends';
INCLUDETYPE:                            'include '[a-z]([a-z])*' ';

//--------------------------------------- TEST ------------------------------------------
MAPPING_TESTABLE_FUNCTION:              'function';

MAPPING_TESTABLE_DATA:                  'data';
MAPPING_TESTABLE_ASSERT:                'assert';
MAPPING_TESTABLE_DOC:                   'doc';
MAPPING_TESTABLE_TYPE:                  'type';
MAPPING_TESTABLE_SUITES:                'testSuites';

//-------------------------------------- LEGACY_TEST ------------------------------------
MAPPING_TEST_ASSERTS:                   'asserts';
MAPPING_TESTS:                          'tests';
MAPPING_TESTS_QUERY:                    'query';

BRACE_OPEN:                             '{' {getVocabulary().getSymbolicName(this.lastTokenType).equals("COLON") || getVocabulary().getSymbolicName(this.lastTokenType).equals("BRACKET_OPEN")}?
                                        | '{' {pushMode (MAPPING_ISLAND_MODE);};


// -------------------------------------- ISLAND --------------------------------------

// NOTE: Since mapping can potentially support many drivers for class mapping (e.g. M2M)
// we have to split this up into multiple parsers. For this to happen, we need to use `island grammar` from ANTLR
// however, for this, we need a good token to start the ISLAND BLOCK. Historically, we used curly braces
// for mapping, which is not great in terms of consistency because curly braces are used as the standard opening
// indicator for things like Class, Enum, etc.
// So Mapping is the only element that opens with a parenthesis. That is why in `CoreLexer` we reserve a default
// island grammar opening token '#{' in order to standardize the behavior in the future

mode MAPPING_ISLAND_MODE;
MAPPING_ISLAND_BRACE_OPEN:              '{' -> pushMode (MAPPING_ISLAND_MODE);
MAPPING_ISLAND_BRACE_CLOSE:             '}' -> popMode;
MAPPING_ISLAND_CONTENT:                 (~[{}])+;
