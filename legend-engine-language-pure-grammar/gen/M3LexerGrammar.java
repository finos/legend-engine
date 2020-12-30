// Generated from C:/Users/lmaur/Documents/GS/legend-engine/legend-engine-language-pure-grammar/src/main/antlr4/org/finos/legend/engine/language/pure/grammar/from/antlr4/core\M3LexerGrammar.g4 by ANTLR 4.9
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class M3LexerGrammar extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		ALL=1, LET=2, ALL_VERSIONS=3, ALL_VERSIONS_IN_RANGE=4, NAVIGATION_PATH_BLOCK=5, 
		WHITESPACE=6, COMMENT=7, LINE_COMMENT=8, ISLAND_OPEN=9, STRING=10, BOOLEAN=11, 
		TRUE=12, FALSE=13, INTEGER=14, FLOAT=15, DECIMAL=16, DATE=17, LATEST_DATE=18, 
		FILE_NAME=19, FILE_NAME_END=20, PATH_SEPARATOR=21, TIMEZONE=22, AND=23, 
		OR=24, NOT=25, COMMA=26, EQUAL=27, TEST_EQUAL=28, TEST_NOT_EQUAL=29, PERCENT=30, 
		ARROW=31, BRACE_OPEN=32, BRACE_CLOSE=33, BRACKET_OPEN=34, BRACKET_CLOSE=35, 
		PAREN_OPEN=36, PAREN_CLOSE=37, COLON=38, DOT=39, DOLLAR=40, DOT_DOT=41, 
		SEMI_COLON=42, NEW_SYMBOL=43, PIPE=44, TILDE=45, AT=46, PLUS=47, STAR=48, 
		MINUS=49, DIVIDE=50, LESS_THAN=51, LESS_OR_EQUAL=52, GREATER_THAN=53, 
		GREATER_OR_EQUAL=54, VALID_STRING=55, INVALID=56, ISLAND_START=57, ISLAND_END=58, 
		ISLAND_HASH=59, ISLAND_BRACE_OPEN=60, ISLAND_BRACE_CLOSE=61, ISLAND_CONTENT=62;
	public static final int
		ISLAND_MODE=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "ISLAND_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"ALL", "LET", "ALL_VERSIONS", "ALL_VERSIONS_IN_RANGE", "NAVIGATION_PATH_BLOCK", 
			"WHITESPACE", "COMMENT", "LINE_COMMENT", "ISLAND_OPEN", "STRING", "BOOLEAN", 
			"TRUE", "FALSE", "INTEGER", "FLOAT", "DECIMAL", "DATE", "LATEST_DATE", 
			"FILE_NAME", "FILE_NAME_END", "PATH_SEPARATOR", "TIMEZONE", "AND", "OR", 
			"NOT", "COMMA", "EQUAL", "TEST_EQUAL", "TEST_NOT_EQUAL", "PERCENT", "ARROW", 
			"BRACE_OPEN", "BRACE_CLOSE", "BRACKET_OPEN", "BRACKET_CLOSE", "PAREN_OPEN", 
			"PAREN_CLOSE", "COLON", "DOT", "DOLLAR", "DOT_DOT", "SEMI_COLON", "NEW_SYMBOL", 
			"PIPE", "TILDE", "AT", "PLUS", "STAR", "MINUS", "DIVIDE", "LESS_THAN", 
			"LESS_OR_EQUAL", "GREATER_THAN", "GREATER_OR_EQUAL", "VALID_STRING", 
			"INVALID", "True", "False", "Letter", "Digit", "HexDigit", "Whitespace", 
			"Comment", "LineComment", "UnicodeEsc", "Esc", "EscSeq", "EscAny", "TimeZone", 
			"ValidString", "FileName", "FileNameEnd", "Assign", "PathSeparator", 
			"String", "Boolean", "Integer", "Float", "Decimal", "Date", "DateTime", 
			"Invalid", "ISLAND_START", "ISLAND_END", "ISLAND_HASH", "ISLAND_BRACE_OPEN", 
			"ISLAND_BRACE_CLOSE", "ISLAND_CONTENT"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'all'", "'let'", "'allVersions'", "'allVersionsInRange'", null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			"'%latest'", null, null, null, null, "'&&'", "'||'", "'!'", "','", "'='", 
			"'=='", "'!='", "'%'", "'->'", null, null, "'['", "']'", "'('", "')'", 
			"':'", "'.'", "'$'", "'..'", "';'", "'^'", "'|'", "'~'", "'@'", "'+'", 
			"'*'", "'-'", "'/'", "'<'", "'<='", "'>'", "'>='", null, null, null, 
			"'}#'", "'#'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "ALL", "LET", "ALL_VERSIONS", "ALL_VERSIONS_IN_RANGE", "NAVIGATION_PATH_BLOCK", 
			"WHITESPACE", "COMMENT", "LINE_COMMENT", "ISLAND_OPEN", "STRING", "BOOLEAN", 
			"TRUE", "FALSE", "INTEGER", "FLOAT", "DECIMAL", "DATE", "LATEST_DATE", 
			"FILE_NAME", "FILE_NAME_END", "PATH_SEPARATOR", "TIMEZONE", "AND", "OR", 
			"NOT", "COMMA", "EQUAL", "TEST_EQUAL", "TEST_NOT_EQUAL", "PERCENT", "ARROW", 
			"BRACE_OPEN", "BRACE_CLOSE", "BRACKET_OPEN", "BRACKET_CLOSE", "PAREN_OPEN", 
			"PAREN_CLOSE", "COLON", "DOT", "DOLLAR", "DOT_DOT", "SEMI_COLON", "NEW_SYMBOL", 
			"PIPE", "TILDE", "AT", "PLUS", "STAR", "MINUS", "DIVIDE", "LESS_THAN", 
			"LESS_OR_EQUAL", "GREATER_THAN", "GREATER_OR_EQUAL", "VALID_STRING", 
			"INVALID", "ISLAND_START", "ISLAND_END", "ISLAND_HASH", "ISLAND_BRACE_OPEN", 
			"ISLAND_BRACE_CLOSE", "ISLAND_CONTENT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public M3LexerGrammar(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "M3LexerGrammar.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2@\u0273\b\1\b\1\4"+
		"\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n"+
		"\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t"+
		"=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4"+
		"I\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\t"+
		"T\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\4\3"+
		"\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\7\6\u00e0"+
		"\n\6\f\6\16\6\u00e3\13\6\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\t\3"+
		"\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17"+
		"\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\30\3\31\3\31"+
		"\3\31\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\35\3\36\3\36\3\36\3\37"+
		"\3\37\3 \3 \3 \3!\3!\3\"\3\"\3#\3#\3$\3$\3%\3%\3&\3&\3\'\3\'\3(\3(\3)"+
		"\3)\3*\3*\3*\3+\3+\3,\3,\3-\3-\3.\3.\3/\3/\3\60\3\60\3\61\3\61\3\62\3"+
		"\62\3\63\3\63\3\64\3\64\3\65\3\65\3\65\3\66\3\66\3\67\3\67\3\67\38\38"+
		"\39\39\3:\3:\3:\3:\3:\3;\3;\3;\3;\3;\3;\3<\3<\3=\3=\3>\3>\3?\6?\u0176"+
		"\n?\r?\16?\u0177\3@\3@\3@\3@\7@\u017e\n@\f@\16@\u0181\13@\3@\3@\3@\3A"+
		"\3A\3A\3A\7A\u018a\nA\fA\16A\u018d\13A\3B\3B\3B\3B\3B\5B\u0194\nB\5B\u0196"+
		"\nB\5B\u0198\nB\5B\u019a\nB\3C\3C\3D\3D\3D\3D\3D\5D\u01a3\nD\3E\3E\3E"+
		"\3F\3F\3F\3F\3F\3F\3G\3G\3G\5G\u01b1\nG\3G\3G\3G\7G\u01b6\nG\fG\16G\u01b9"+
		"\13G\3H\3H\3H\3H\3H\3H\6H\u01c1\nH\rH\16H\u01c2\3I\3I\3I\3J\7J\u01c9\n"+
		"J\fJ\16J\u01cc\13J\3J\3J\3K\3K\3K\3L\3L\3L\7L\u01d6\nL\fL\16L\u01d9\13"+
		"L\3L\3L\3M\3M\5M\u01df\nM\3N\6N\u01e2\nN\rN\16N\u01e3\3O\7O\u01e7\nO\f"+
		"O\16O\u01ea\13O\3O\3O\6O\u01ee\nO\rO\16O\u01ef\3O\3O\5O\u01f4\nO\3O\6"+
		"O\u01f7\nO\rO\16O\u01f8\5O\u01fb\nO\3O\5O\u01fe\nO\3P\7P\u0201\nP\fP\16"+
		"P\u0204\13P\3P\3P\6P\u0208\nP\rP\16P\u0209\3P\6P\u020d\nP\rP\16P\u020e"+
		"\5P\u0211\nP\3P\3P\5P\u0215\nP\3P\6P\u0218\nP\rP\16P\u0219\5P\u021c\n"+
		"P\3P\3P\3Q\3Q\5Q\u0222\nQ\3Q\6Q\u0225\nQ\rQ\16Q\u0226\3Q\3Q\6Q\u022b\n"+
		"Q\rQ\16Q\u022c\3Q\3Q\6Q\u0231\nQ\rQ\16Q\u0232\3Q\3Q\3Q\5Q\u0238\nQ\5Q"+
		"\u023a\nQ\5Q\u023c\nQ\5Q\u023e\nQ\3R\6R\u0241\nR\rR\16R\u0242\3R\3R\6"+
		"R\u0247\nR\rR\16R\u0248\3R\3R\6R\u024d\nR\rR\16R\u024e\3R\3R\6R\u0253"+
		"\nR\rR\16R\u0254\5R\u0257\nR\5R\u0259\nR\5R\u025b\nR\3S\3S\3T\3T\3T\3"+
		"T\3T\3U\3U\3U\3U\3U\3V\3V\3W\3W\3X\3X\3Y\6Y\u0270\nY\rY\16Y\u0271\3\u017f"+
		"\2Z\4\3\6\4\b\5\n\6\f\7\16\b\20\t\22\n\24\13\26\f\30\r\32\16\34\17\36"+
		"\20 \21\"\22$\23&\24(\25*\26,\27.\30\60\31\62\32\64\33\66\348\35:\36<"+
		"\37> @!B\"D#F$H%J&L\'N(P)R*T+V,X-Z.\\/^\60`\61b\62d\63f\64h\65j\66l\67"+
		"n8p9r:t\2v\2x\2z\2|\2~\2\u0080\2\u0082\2\u0084\2\u0086\2\u0088\2\u008a"+
		"\2\u008c\2\u008e\2\u0090\2\u0092\2\u0094\2\u0096\2\u0098\2\u009a\2\u009c"+
		"\2\u009e\2\u00a0\2\u00a2\2\u00a4\2\u00a6\2\u00a8;\u00aa<\u00ac=\u00ae"+
		">\u00b0?\u00b2@\4\2\3\21\3\2%%\4\2C\\c|\3\2\62;\5\2\62;CHch\5\2\13\f\17"+
		"\17\"\"\4\2\f\f\17\17\n\2$$))^^ddhhppttvv\4\2--//\4\2&&aa\4\2\60\61aa"+
		"\6\2\f\f\17\17))^^\4\2GGgg\4\2HHhh\4\2FFff\5\2%%}}\177\177\2\u028c\2\4"+
		"\3\2\2\2\2\6\3\2\2\2\2\b\3\2\2\2\2\n\3\2\2\2\2\f\3\2\2\2\2\16\3\2\2\2"+
		"\2\20\3\2\2\2\2\22\3\2\2\2\2\24\3\2\2\2\2\26\3\2\2\2\2\30\3\2\2\2\2\32"+
		"\3\2\2\2\2\34\3\2\2\2\2\36\3\2\2\2\2 \3\2\2\2\2\"\3\2\2\2\2$\3\2\2\2\2"+
		"&\3\2\2\2\2(\3\2\2\2\2*\3\2\2\2\2,\3\2\2\2\2.\3\2\2\2\2\60\3\2\2\2\2\62"+
		"\3\2\2\2\2\64\3\2\2\2\2\66\3\2\2\2\28\3\2\2\2\2:\3\2\2\2\2<\3\2\2\2\2"+
		">\3\2\2\2\2@\3\2\2\2\2B\3\2\2\2\2D\3\2\2\2\2F\3\2\2\2\2H\3\2\2\2\2J\3"+
		"\2\2\2\2L\3\2\2\2\2N\3\2\2\2\2P\3\2\2\2\2R\3\2\2\2\2T\3\2\2\2\2V\3\2\2"+
		"\2\2X\3\2\2\2\2Z\3\2\2\2\2\\\3\2\2\2\2^\3\2\2\2\2`\3\2\2\2\2b\3\2\2\2"+
		"\2d\3\2\2\2\2f\3\2\2\2\2h\3\2\2\2\2j\3\2\2\2\2l\3\2\2\2\2n\3\2\2\2\2p"+
		"\3\2\2\2\2r\3\2\2\2\3\u00a8\3\2\2\2\3\u00aa\3\2\2\2\3\u00ac\3\2\2\2\3"+
		"\u00ae\3\2\2\2\3\u00b0\3\2\2\2\3\u00b2\3\2\2\2\4\u00b4\3\2\2\2\6\u00b8"+
		"\3\2\2\2\b\u00bc\3\2\2\2\n\u00c8\3\2\2\2\f\u00db\3\2\2\2\16\u00e6\3\2"+
		"\2\2\20\u00ea\3\2\2\2\22\u00ee\3\2\2\2\24\u00f2\3\2\2\2\26\u00f7\3\2\2"+
		"\2\30\u00f9\3\2\2\2\32\u00fb\3\2\2\2\34\u00fd\3\2\2\2\36\u00ff\3\2\2\2"+
		" \u0101\3\2\2\2\"\u0103\3\2\2\2$\u0105\3\2\2\2&\u0107\3\2\2\2(\u010f\3"+
		"\2\2\2*\u0111\3\2\2\2,\u0113\3\2\2\2.\u0115\3\2\2\2\60\u0117\3\2\2\2\62"+
		"\u011a\3\2\2\2\64\u011d\3\2\2\2\66\u011f\3\2\2\28\u0121\3\2\2\2:\u0123"+
		"\3\2\2\2<\u0126\3\2\2\2>\u0129\3\2\2\2@\u012b\3\2\2\2B\u012e\3\2\2\2D"+
		"\u0130\3\2\2\2F\u0132\3\2\2\2H\u0134\3\2\2\2J\u0136\3\2\2\2L\u0138\3\2"+
		"\2\2N\u013a\3\2\2\2P\u013c\3\2\2\2R\u013e\3\2\2\2T\u0140\3\2\2\2V\u0143"+
		"\3\2\2\2X\u0145\3\2\2\2Z\u0147\3\2\2\2\\\u0149\3\2\2\2^\u014b\3\2\2\2"+
		"`\u014d\3\2\2\2b\u014f\3\2\2\2d\u0151\3\2\2\2f\u0153\3\2\2\2h\u0155\3"+
		"\2\2\2j\u0157\3\2\2\2l\u015a\3\2\2\2n\u015c\3\2\2\2p\u015f\3\2\2\2r\u0161"+
		"\3\2\2\2t\u0163\3\2\2\2v\u0168\3\2\2\2x\u016e\3\2\2\2z\u0170\3\2\2\2|"+
		"\u0172\3\2\2\2~\u0175\3\2\2\2\u0080\u0179\3\2\2\2\u0082\u0185\3\2\2\2"+
		"\u0084\u018e\3\2\2\2\u0086\u019b\3\2\2\2\u0088\u019d\3\2\2\2\u008a\u01a4"+
		"\3\2\2\2\u008c\u01a7\3\2\2\2\u008e\u01b0\3\2\2\2\u0090\u01ba\3\2\2\2\u0092"+
		"\u01c4\3\2\2\2\u0094\u01ca\3\2\2\2\u0096\u01cf\3\2\2\2\u0098\u01d2\3\2"+
		"\2\2\u009a\u01de\3\2\2\2\u009c\u01e1\3\2\2\2\u009e\u01e8\3\2\2\2\u00a0"+
		"\u0210\3\2\2\2\u00a2\u021f\3\2\2\2\u00a4\u0240\3\2\2\2\u00a6\u025c\3\2"+
		"\2\2\u00a8\u025e\3\2\2\2\u00aa\u0263\3\2\2\2\u00ac\u0268\3\2\2\2\u00ae"+
		"\u026a\3\2\2\2\u00b0\u026c\3\2\2\2\u00b2\u026f\3\2\2\2\u00b4\u00b5\7c"+
		"\2\2\u00b5\u00b6\7n\2\2\u00b6\u00b7\7n\2\2\u00b7\5\3\2\2\2\u00b8\u00b9"+
		"\7n\2\2\u00b9\u00ba\7g\2\2\u00ba\u00bb\7v\2\2\u00bb\7\3\2\2\2\u00bc\u00bd"+
		"\7c\2\2\u00bd\u00be\7n\2\2\u00be\u00bf\7n\2\2\u00bf\u00c0\7X\2\2\u00c0"+
		"\u00c1\7g\2\2\u00c1\u00c2\7t\2\2\u00c2\u00c3\7u\2\2\u00c3\u00c4\7k\2\2"+
		"\u00c4\u00c5\7q\2\2\u00c5\u00c6\7p\2\2\u00c6\u00c7\7u\2\2\u00c7\t\3\2"+
		"\2\2\u00c8\u00c9\7c\2\2\u00c9\u00ca\7n\2\2\u00ca\u00cb\7n\2\2\u00cb\u00cc"+
		"\7X\2\2\u00cc\u00cd\7g\2\2\u00cd\u00ce\7t\2\2\u00ce\u00cf\7u\2\2\u00cf"+
		"\u00d0\7k\2\2\u00d0\u00d1\7q\2\2\u00d1\u00d2\7p\2\2\u00d2\u00d3\7u\2\2"+
		"\u00d3\u00d4\7K\2\2\u00d4\u00d5\7p\2\2\u00d5\u00d6\7T\2\2\u00d6\u00d7"+
		"\7c\2\2\u00d7\u00d8\7p\2\2\u00d8\u00d9\7i\2\2\u00d9\u00da\7g\2\2\u00da"+
		"\13\3\2\2\2\u00db\u00dc\7%\2\2\u00dc\u00dd\7\61\2\2\u00dd\u00e1\3\2\2"+
		"\2\u00de\u00e0\n\2\2\2\u00df\u00de\3\2\2\2\u00e0\u00e3\3\2\2\2\u00e1\u00df"+
		"\3\2\2\2\u00e1\u00e2\3\2\2\2\u00e2\u00e4\3\2\2\2\u00e3\u00e1\3\2\2\2\u00e4"+
		"\u00e5\7%\2\2\u00e5\r\3\2\2\2\u00e6\u00e7\5~?\2\u00e7\u00e8\3\2\2\2\u00e8"+
		"\u00e9\b\7\2\2\u00e9\17\3\2\2\2\u00ea\u00eb\5\u0080@\2\u00eb\u00ec\3\2"+
		"\2\2\u00ec\u00ed\b\b\2\2\u00ed\21\3\2\2\2\u00ee\u00ef\5\u0082A\2\u00ef"+
		"\u00f0\3\2\2\2\u00f0\u00f1\b\t\2\2\u00f1\23\3\2\2\2\u00f2\u00f3\7%\2\2"+
		"\u00f3\u00f4\7}\2\2\u00f4\u00f5\3\2\2\2\u00f5\u00f6\b\n\3\2\u00f6\25\3"+
		"\2\2\2\u00f7\u00f8\5\u0098L\2\u00f8\27\3\2\2\2\u00f9\u00fa\5\u009aM\2"+
		"\u00fa\31\3\2\2\2\u00fb\u00fc\5t:\2\u00fc\33\3\2\2\2\u00fd\u00fe\5v;\2"+
		"\u00fe\35\3\2\2\2\u00ff\u0100\5\u009cN\2\u0100\37\3\2\2\2\u0101\u0102"+
		"\5\u009eO\2\u0102!\3\2\2\2\u0103\u0104\5\u00a0P\2\u0104#\3\2\2\2\u0105"+
		"\u0106\5\u00a2Q\2\u0106%\3\2\2\2\u0107\u0108\7\'\2\2\u0108\u0109\7n\2"+
		"\2\u0109\u010a\7c\2\2\u010a\u010b\7v\2\2\u010b\u010c\7g\2\2\u010c\u010d"+
		"\7u\2\2\u010d\u010e\7v\2\2\u010e\'\3\2\2\2\u010f\u0110\5\u0090H\2\u0110"+
		")\3\2\2\2\u0111\u0112\5\u0092I\2\u0112+\3\2\2\2\u0113\u0114\5\u0096K\2"+
		"\u0114-\3\2\2\2\u0115\u0116\5\u008cF\2\u0116/\3\2\2\2\u0117\u0118\7(\2"+
		"\2\u0118\u0119\7(\2\2\u0119\61\3\2\2\2\u011a\u011b\7~\2\2\u011b\u011c"+
		"\7~\2\2\u011c\63\3\2\2\2\u011d\u011e\7#\2\2\u011e\65\3\2\2\2\u011f\u0120"+
		"\7.\2\2\u0120\67\3\2\2\2\u0121\u0122\7?\2\2\u01229\3\2\2\2\u0123\u0124"+
		"\7?\2\2\u0124\u0125\7?\2\2\u0125;\3\2\2\2\u0126\u0127\7#\2\2\u0127\u0128"+
		"\7?\2\2\u0128=\3\2\2\2\u0129\u012a\7\'\2\2\u012a?\3\2\2\2\u012b\u012c"+
		"\7/\2\2\u012c\u012d\7@\2\2\u012dA\3\2\2\2\u012e\u012f\7}\2\2\u012fC\3"+
		"\2\2\2\u0130\u0131\7\177\2\2\u0131E\3\2\2\2\u0132\u0133\7]\2\2\u0133G"+
		"\3\2\2\2\u0134\u0135\7_\2\2\u0135I\3\2\2\2\u0136\u0137\7*\2\2\u0137K\3"+
		"\2\2\2\u0138\u0139\7+\2\2\u0139M\3\2\2\2\u013a\u013b\7<\2\2\u013bO\3\2"+
		"\2\2\u013c\u013d\7\60\2\2\u013dQ\3\2\2\2\u013e\u013f\7&\2\2\u013fS\3\2"+
		"\2\2\u0140\u0141\7\60\2\2\u0141\u0142\7\60\2\2\u0142U\3\2\2\2\u0143\u0144"+
		"\7=\2\2\u0144W\3\2\2\2\u0145\u0146\7`\2\2\u0146Y\3\2\2\2\u0147\u0148\7"+
		"~\2\2\u0148[\3\2\2\2\u0149\u014a\7\u0080\2\2\u014a]\3\2\2\2\u014b\u014c"+
		"\7B\2\2\u014c_\3\2\2\2\u014d\u014e\7-\2\2\u014ea\3\2\2\2\u014f\u0150\7"+
		",\2\2\u0150c\3\2\2\2\u0151\u0152\7/\2\2\u0152e\3\2\2\2\u0153\u0154\7\61"+
		"\2\2\u0154g\3\2\2\2\u0155\u0156\7>\2\2\u0156i\3\2\2\2\u0157\u0158\7>\2"+
		"\2\u0158\u0159\7?\2\2\u0159k\3\2\2\2\u015a\u015b\7@\2\2\u015bm\3\2\2\2"+
		"\u015c\u015d\7@\2\2\u015d\u015e\7?\2\2\u015eo\3\2\2\2\u015f\u0160\5\u008e"+
		"G\2\u0160q\3\2\2\2\u0161\u0162\5\u00a6S\2\u0162s\3\2\2\2\u0163\u0164\7"+
		"v\2\2\u0164\u0165\7t\2\2\u0165\u0166\7w\2\2\u0166\u0167\7g\2\2\u0167u"+
		"\3\2\2\2\u0168\u0169\7h\2\2\u0169\u016a\7c\2\2\u016a\u016b\7n\2\2\u016b"+
		"\u016c\7u\2\2\u016c\u016d\7g\2\2\u016dw\3\2\2\2\u016e\u016f\t\3\2\2\u016f"+
		"y\3\2\2\2\u0170\u0171\t\4\2\2\u0171{\3\2\2\2\u0172\u0173\t\5\2\2\u0173"+
		"}\3\2\2\2\u0174\u0176\t\6\2\2\u0175\u0174\3\2\2\2\u0176\u0177\3\2\2\2"+
		"\u0177\u0175\3\2\2\2\u0177\u0178\3\2\2\2\u0178\177\3\2\2\2\u0179\u017a"+
		"\7\61\2\2\u017a\u017b\7,\2\2\u017b\u017f\3\2\2\2\u017c\u017e\13\2\2\2"+
		"\u017d\u017c\3\2\2\2\u017e\u0181\3\2\2\2\u017f\u0180\3\2\2\2\u017f\u017d"+
		"\3\2\2\2\u0180\u0182\3\2\2\2\u0181\u017f\3\2\2\2\u0182\u0183\7,\2\2\u0183"+
		"\u0184\7\61\2\2\u0184\u0081\3\2\2\2\u0185\u0186\7\61\2\2\u0186\u0187\7"+
		"\61\2\2\u0187\u018b\3\2\2\2\u0188\u018a\n\7\2\2\u0189\u0188\3\2\2\2\u018a"+
		"\u018d\3\2\2\2\u018b\u0189\3\2\2\2\u018b\u018c\3\2\2\2\u018c\u0083\3\2"+
		"\2\2\u018d\u018b\3\2\2\2\u018e\u0199\7w\2\2\u018f\u0197\5|>\2\u0190\u0195"+
		"\5|>\2\u0191\u0193\5|>\2\u0192\u0194\5|>\2\u0193\u0192\3\2\2\2\u0193\u0194"+
		"\3\2\2\2\u0194\u0196\3\2\2\2\u0195\u0191\3\2\2\2\u0195\u0196\3\2\2\2\u0196"+
		"\u0198\3\2\2\2\u0197\u0190\3\2\2\2\u0197\u0198\3\2\2\2\u0198\u019a\3\2"+
		"\2\2\u0199\u018f\3\2\2\2\u0199\u019a\3\2\2\2\u019a\u0085\3\2\2\2\u019b"+
		"\u019c\7^\2\2\u019c\u0087\3\2\2\2\u019d\u01a2\5\u0086C\2\u019e\u01a3\t"+
		"\b\2\2\u019f\u01a3\5\u0084B\2\u01a0\u01a3\13\2\2\2\u01a1\u01a3\7\2\2\3"+
		"\u01a2\u019e\3\2\2\2\u01a2\u019f\3\2\2\2\u01a2\u01a0\3\2\2\2\u01a2\u01a1"+
		"\3\2\2\2\u01a3\u0089\3\2\2\2\u01a4\u01a5\5\u0086C\2\u01a5\u01a6\13\2\2"+
		"\2\u01a6\u008b\3\2\2\2\u01a7\u01a8\t\t\2\2\u01a8\u01a9\5z=\2\u01a9\u01aa"+
		"\5z=\2\u01aa\u01ab\5z=\2\u01ab\u01ac\5z=\2\u01ac\u008d\3\2\2\2\u01ad\u01b1"+
		"\5x<\2\u01ae\u01b1\5z=\2\u01af\u01b1\7a\2\2\u01b0\u01ad\3\2\2\2\u01b0"+
		"\u01ae\3\2\2\2\u01b0\u01af\3\2\2\2\u01b1\u01b7\3\2\2\2\u01b2\u01b6\5x"+
		"<\2\u01b3\u01b6\5z=\2\u01b4\u01b6\t\n\2\2\u01b5\u01b2\3\2\2\2\u01b5\u01b3"+
		"\3\2\2\2\u01b5\u01b4\3\2\2\2\u01b6\u01b9\3\2\2\2\u01b7\u01b5\3\2\2\2\u01b7"+
		"\u01b8\3\2\2\2\u01b8\u008f\3\2\2\2\u01b9\u01b7\3\2\2\2\u01ba\u01bb\7A"+
		"\2\2\u01bb\u01bc\7]\2\2\u01bc\u01c0\3\2\2\2\u01bd\u01c1\5x<\2\u01be\u01c1"+
		"\5z=\2\u01bf\u01c1\t\13\2\2\u01c0\u01bd\3\2\2\2\u01c0\u01be\3\2\2\2\u01c0"+
		"\u01bf\3\2\2\2\u01c1\u01c2\3\2\2\2\u01c2\u01c0\3\2\2\2\u01c2\u01c3\3\2"+
		"\2\2\u01c3\u0091\3\2\2\2\u01c4\u01c5\7_\2\2\u01c5\u01c6\7A\2\2\u01c6\u0093"+
		"\3\2\2\2\u01c7\u01c9\t\6\2\2\u01c8\u01c7\3\2\2\2\u01c9\u01cc\3\2\2\2\u01ca"+
		"\u01c8\3\2\2\2\u01ca\u01cb\3\2\2\2\u01cb\u01cd\3\2\2\2\u01cc\u01ca\3\2"+
		"\2\2\u01cd\u01ce\7?\2\2\u01ce\u0095\3\2\2\2\u01cf\u01d0\7<\2\2\u01d0\u01d1"+
		"\7<\2\2\u01d1\u0097\3\2\2\2\u01d2\u01d7\7)\2\2\u01d3\u01d6\5\u0088D\2"+
		"\u01d4\u01d6\n\f\2\2\u01d5\u01d3\3\2\2\2\u01d5\u01d4\3\2\2\2\u01d6\u01d9"+
		"\3\2\2\2\u01d7\u01d5\3\2\2\2\u01d7\u01d8\3\2\2\2\u01d8\u01da\3\2\2\2\u01d9"+
		"\u01d7\3\2\2\2\u01da\u01db\7)\2\2\u01db\u0099\3\2\2\2\u01dc\u01df\5t:"+
		"\2\u01dd\u01df\5v;\2\u01de\u01dc\3\2\2\2\u01de\u01dd\3\2\2\2\u01df\u009b"+
		"\3\2\2\2\u01e0\u01e2\5z=\2\u01e1\u01e0\3\2\2\2\u01e2\u01e3\3\2\2\2\u01e3"+
		"\u01e1\3\2\2\2\u01e3\u01e4\3\2\2\2\u01e4\u009d\3\2\2\2\u01e5\u01e7\5z"+
		"=\2\u01e6\u01e5\3\2\2\2\u01e7\u01ea\3\2\2\2\u01e8\u01e6\3\2\2\2\u01e8"+
		"\u01e9\3\2\2\2\u01e9\u01eb\3\2\2\2\u01ea\u01e8\3\2\2\2\u01eb\u01ed\7\60"+
		"\2\2\u01ec\u01ee\5z=\2\u01ed\u01ec\3\2\2\2\u01ee\u01ef\3\2\2\2\u01ef\u01ed"+
		"\3\2\2\2\u01ef\u01f0\3\2\2\2\u01f0\u01fa\3\2\2\2\u01f1\u01f3\t\r\2\2\u01f2"+
		"\u01f4\t\t\2\2\u01f3\u01f2\3\2\2\2\u01f3\u01f4\3\2\2\2\u01f4\u01f6\3\2"+
		"\2\2\u01f5\u01f7\5z=\2\u01f6\u01f5\3\2\2\2\u01f7\u01f8\3\2\2\2\u01f8\u01f6"+
		"\3\2\2\2\u01f8\u01f9\3\2\2\2\u01f9\u01fb\3\2\2\2\u01fa\u01f1\3\2\2\2\u01fa"+
		"\u01fb\3\2\2\2\u01fb\u01fd\3\2\2\2\u01fc\u01fe\t\16\2\2\u01fd\u01fc\3"+
		"\2\2\2\u01fd\u01fe\3\2\2\2\u01fe\u009f\3\2\2\2\u01ff\u0201\5z=\2\u0200"+
		"\u01ff\3\2\2\2\u0201\u0204\3\2\2\2\u0202\u0200\3\2\2\2\u0202\u0203\3\2"+
		"\2\2\u0203\u0205\3\2\2\2\u0204\u0202\3\2\2\2\u0205\u0207\7\60\2\2\u0206"+
		"\u0208\5z=\2\u0207\u0206\3\2\2\2\u0208\u0209\3\2\2\2\u0209\u0207\3\2\2"+
		"\2\u0209\u020a\3\2\2\2\u020a\u0211\3\2\2\2\u020b\u020d\5z=\2\u020c\u020b"+
		"\3\2\2\2\u020d\u020e\3\2\2\2\u020e\u020c\3\2\2\2\u020e\u020f\3\2\2\2\u020f"+
		"\u0211\3\2\2\2\u0210\u0202\3\2\2\2\u0210\u020c\3\2\2\2\u0211\u021b\3\2"+
		"\2\2\u0212\u0214\t\r\2\2\u0213\u0215\t\t\2\2\u0214\u0213\3\2\2\2\u0214"+
		"\u0215\3\2\2\2\u0215\u0217\3\2\2\2\u0216\u0218\5z=\2\u0217\u0216\3\2\2"+
		"\2\u0218\u0219\3\2\2\2\u0219\u0217\3\2\2\2\u0219\u021a\3\2\2\2\u021a\u021c"+
		"\3\2\2\2\u021b\u0212\3\2\2\2\u021b\u021c\3\2\2\2\u021c\u021d\3\2\2\2\u021d"+
		"\u021e\t\17\2\2\u021e\u00a1\3\2\2\2\u021f\u0221\7\'\2\2\u0220\u0222\7"+
		"/\2\2\u0221\u0220\3\2\2\2\u0221\u0222\3\2\2\2\u0222\u0224\3\2\2\2\u0223"+
		"\u0225\5z=\2\u0224\u0223\3\2\2\2\u0225\u0226\3\2\2\2\u0226\u0224\3\2\2"+
		"\2\u0226\u0227\3\2\2\2\u0227\u023d\3\2\2\2\u0228\u022a\7/\2\2\u0229\u022b"+
		"\5z=\2\u022a\u0229\3\2\2\2\u022b\u022c\3\2\2\2\u022c\u022a\3\2\2\2\u022c"+
		"\u022d\3\2\2\2\u022d\u023b\3\2\2\2\u022e\u0230\7/\2\2\u022f\u0231\5z="+
		"\2\u0230\u022f\3\2\2\2\u0231\u0232\3\2\2\2\u0232\u0230\3\2\2\2\u0232\u0233"+
		"\3\2\2\2\u0233\u0239\3\2\2\2\u0234\u0235\7V\2\2\u0235\u0237\5\u00a4R\2"+
		"\u0236\u0238\5\u008cF\2\u0237\u0236\3\2\2\2\u0237\u0238\3\2\2\2\u0238"+
		"\u023a\3\2\2\2\u0239\u0234\3\2\2\2\u0239\u023a\3\2\2\2\u023a\u023c\3\2"+
		"\2\2\u023b\u022e\3\2\2\2\u023b\u023c\3\2\2\2\u023c\u023e\3\2\2\2\u023d"+
		"\u0228\3\2\2\2\u023d\u023e\3\2\2\2\u023e\u00a3\3\2\2\2\u023f\u0241\5z"+
		"=\2\u0240\u023f\3\2\2\2\u0241\u0242\3\2\2\2\u0242\u0240\3\2\2\2\u0242"+
		"\u0243\3\2\2\2\u0243\u025a\3\2\2\2\u0244\u0246\7<\2\2\u0245\u0247\5z="+
		"\2\u0246\u0245\3\2\2\2\u0247\u0248\3\2\2\2\u0248\u0246\3\2\2\2\u0248\u0249"+
		"\3\2\2\2\u0249\u0258\3\2\2\2\u024a\u024c\7<\2\2\u024b\u024d\5z=\2\u024c"+
		"\u024b\3\2\2\2\u024d\u024e\3\2\2\2\u024e\u024c\3\2\2\2\u024e\u024f\3\2"+
		"\2\2\u024f\u0256\3\2\2\2\u0250\u0252\7\60\2\2\u0251\u0253\5z=\2\u0252"+
		"\u0251\3\2\2\2\u0253\u0254\3\2\2\2\u0254\u0252\3\2\2\2\u0254\u0255\3\2"+
		"\2\2\u0255\u0257\3\2\2\2\u0256\u0250\3\2\2\2\u0256\u0257\3\2\2\2\u0257"+
		"\u0259\3\2\2\2\u0258\u024a\3\2\2\2\u0258\u0259\3\2\2\2\u0259\u025b\3\2"+
		"\2\2\u025a\u0244\3\2\2\2\u025a\u025b\3\2\2\2\u025b\u00a5\3\2\2\2\u025c"+
		"\u025d\13\2\2\2\u025d\u00a7\3\2\2\2\u025e\u025f\7%\2\2\u025f\u0260\7}"+
		"\2\2\u0260\u0261\3\2\2\2\u0261\u0262\bT\3\2\u0262\u00a9\3\2\2\2\u0263"+
		"\u0264\7\177\2\2\u0264\u0265\7%\2\2\u0265\u0266\3\2\2\2\u0266\u0267\b"+
		"U\4\2\u0267\u00ab\3\2\2\2\u0268\u0269\7%\2\2\u0269\u00ad\3\2\2\2\u026a"+
		"\u026b\7}\2\2\u026b\u00af\3\2\2\2\u026c\u026d\7\177\2\2\u026d\u00b1\3"+
		"\2\2\2\u026e\u0270\n\20\2\2\u026f\u026e\3\2\2\2\u0270\u0271\3\2\2\2\u0271"+
		"\u026f\3\2\2\2\u0271\u0272\3\2\2\2\u0272\u00b3\3\2\2\2\64\2\3\u00e1\u0177"+
		"\u017f\u018b\u0193\u0195\u0197\u0199\u01a2\u01b0\u01b5\u01b7\u01c0\u01c2"+
		"\u01ca\u01d5\u01d7\u01de\u01e3\u01e8\u01ef\u01f3\u01f8\u01fa\u01fd\u0202"+
		"\u0209\u020e\u0210\u0214\u0219\u021b\u0221\u0226\u022c\u0232\u0237\u0239"+
		"\u023b\u023d\u0242\u0248\u024e\u0254\u0256\u0258\u025a\u0271\5\b\2\2\7"+
		"\3\2\6\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}