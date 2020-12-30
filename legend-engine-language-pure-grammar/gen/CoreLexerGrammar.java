// Generated from C:/Users/lmaur/Documents/GS/legend-engine/legend-engine-language-pure-grammar/src/main/antlr4/org/finos/legend/engine/language/pure/grammar/from/antlr4/core\CoreLexerGrammar.g4 by ANTLR 4.9
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class CoreLexerGrammar extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WHITESPACE=1, COMMENT=2, LINE_COMMENT=3, ISLAND_OPEN=4, STRING=5, BOOLEAN=6, 
		TRUE=7, FALSE=8, INTEGER=9, FLOAT=10, DECIMAL=11, DATE=12, LATEST_DATE=13, 
		FILE_NAME=14, FILE_NAME_END=15, PATH_SEPARATOR=16, TIMEZONE=17, AND=18, 
		OR=19, NOT=20, COMMA=21, EQUAL=22, TEST_EQUAL=23, TEST_NOT_EQUAL=24, PERCENT=25, 
		ARROW=26, BRACE_OPEN=27, BRACE_CLOSE=28, BRACKET_OPEN=29, BRACKET_CLOSE=30, 
		PAREN_OPEN=31, PAREN_CLOSE=32, COLON=33, DOT=34, DOLLAR=35, DOT_DOT=36, 
		SEMI_COLON=37, NEW_SYMBOL=38, PIPE=39, TILDE=40, AT=41, PLUS=42, STAR=43, 
		MINUS=44, DIVIDE=45, LESS_THAN=46, LESS_OR_EQUAL=47, GREATER_THAN=48, 
		GREATER_OR_EQUAL=49, VALID_STRING=50, INVALID=51, ISLAND_START=52, ISLAND_END=53, 
		ISLAND_HASH=54, ISLAND_BRACE_OPEN=55, ISLAND_BRACE_CLOSE=56, ISLAND_CONTENT=57;
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
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, "'%latest'", null, null, null, null, "'&&'", "'||'", "'!'", "','", 
			"'='", "'=='", "'!='", "'%'", "'->'", null, null, "'['", "']'", "'('", 
			"')'", "':'", "'.'", "'$'", "'..'", "';'", "'^'", "'|'", "'~'", "'@'", 
			"'+'", "'*'", "'-'", "'/'", "'<'", "'<='", "'>'", "'>='", null, null, 
			null, "'}#'", "'#'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WHITESPACE", "COMMENT", "LINE_COMMENT", "ISLAND_OPEN", "STRING", 
			"BOOLEAN", "TRUE", "FALSE", "INTEGER", "FLOAT", "DECIMAL", "DATE", "LATEST_DATE", 
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


	public CoreLexerGrammar(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "CoreLexerGrammar.g4"; }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2;\u0237\b\1\b\1\4"+
		"\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n"+
		"\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t"+
		"=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4"+
		"I\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\t"+
		"T\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5"+
		"\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22"+
		"\3\22\3\23\3\23\3\23\3\24\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30"+
		"\3\30\3\30\3\31\3\31\3\31\3\32\3\32\3\33\3\33\3\33\3\34\3\34\3\35\3\35"+
		"\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3\"\3\"\3#\3#\3$\3$\3%\3%\3%\3&\3&\3"+
		"\'\3\'\3(\3(\3)\3)\3*\3*\3+\3+\3,\3,\3-\3-\3.\3.\3/\3/\3\60\3\60\3\60"+
		"\3\61\3\61\3\62\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3\65\3\65\3\65\3\65"+
		"\3\66\3\66\3\66\3\66\3\66\3\66\3\67\3\67\38\38\39\39\3:\6:\u013a\n:\r"+
		":\16:\u013b\3;\3;\3;\3;\7;\u0142\n;\f;\16;\u0145\13;\3;\3;\3;\3<\3<\3"+
		"<\3<\7<\u014e\n<\f<\16<\u0151\13<\3=\3=\3=\3=\3=\5=\u0158\n=\5=\u015a"+
		"\n=\5=\u015c\n=\5=\u015e\n=\3>\3>\3?\3?\3?\3?\3?\5?\u0167\n?\3@\3@\3@"+
		"\3A\3A\3A\3A\3A\3A\3B\3B\3B\5B\u0175\nB\3B\3B\3B\7B\u017a\nB\fB\16B\u017d"+
		"\13B\3C\3C\3C\3C\3C\3C\6C\u0185\nC\rC\16C\u0186\3D\3D\3D\3E\7E\u018d\n"+
		"E\fE\16E\u0190\13E\3E\3E\3F\3F\3F\3G\3G\3G\7G\u019a\nG\fG\16G\u019d\13"+
		"G\3G\3G\3H\3H\5H\u01a3\nH\3I\6I\u01a6\nI\rI\16I\u01a7\3J\7J\u01ab\nJ\f"+
		"J\16J\u01ae\13J\3J\3J\6J\u01b2\nJ\rJ\16J\u01b3\3J\3J\5J\u01b8\nJ\3J\6"+
		"J\u01bb\nJ\rJ\16J\u01bc\5J\u01bf\nJ\3J\5J\u01c2\nJ\3K\7K\u01c5\nK\fK\16"+
		"K\u01c8\13K\3K\3K\6K\u01cc\nK\rK\16K\u01cd\3K\6K\u01d1\nK\rK\16K\u01d2"+
		"\5K\u01d5\nK\3K\3K\5K\u01d9\nK\3K\6K\u01dc\nK\rK\16K\u01dd\5K\u01e0\n"+
		"K\3K\3K\3L\3L\5L\u01e6\nL\3L\6L\u01e9\nL\rL\16L\u01ea\3L\3L\6L\u01ef\n"+
		"L\rL\16L\u01f0\3L\3L\6L\u01f5\nL\rL\16L\u01f6\3L\3L\3L\5L\u01fc\nL\5L"+
		"\u01fe\nL\5L\u0200\nL\5L\u0202\nL\3M\6M\u0205\nM\rM\16M\u0206\3M\3M\6"+
		"M\u020b\nM\rM\16M\u020c\3M\3M\6M\u0211\nM\rM\16M\u0212\3M\3M\6M\u0217"+
		"\nM\rM\16M\u0218\5M\u021b\nM\5M\u021d\nM\5M\u021f\nM\3N\3N\3O\3O\3O\3"+
		"O\3O\3P\3P\3P\3P\3P\3Q\3Q\3R\3R\3S\3S\3T\6T\u0234\nT\rT\16T\u0235\3\u0143"+
		"\2U\4\3\6\4\b\5\n\6\f\7\16\b\20\t\22\n\24\13\26\f\30\r\32\16\34\17\36"+
		"\20 \21\"\22$\23&\24(\25*\26,\27.\30\60\31\62\32\64\33\66\348\35:\36<"+
		"\37> @!B\"D#F$H%J&L\'N(P)R*T+V,X-Z.\\/^\60`\61b\62d\63f\64h\65j\2l\2n"+
		"\2p\2r\2t\2v\2x\2z\2|\2~\2\u0080\2\u0082\2\u0084\2\u0086\2\u0088\2\u008a"+
		"\2\u008c\2\u008e\2\u0090\2\u0092\2\u0094\2\u0096\2\u0098\2\u009a\2\u009c"+
		"\2\u009e\66\u00a0\67\u00a28\u00a49\u00a6:\u00a8;\4\2\3\20\4\2C\\c|\3\2"+
		"\62;\5\2\62;CHch\5\2\13\f\17\17\"\"\4\2\f\f\17\17\n\2$$))^^ddhhppttvv"+
		"\4\2--//\4\2&&aa\4\2\60\61aa\6\2\f\f\17\17))^^\4\2GGgg\4\2HHhh\4\2FFf"+
		"f\5\2%%}}\177\177\2\u024f\2\4\3\2\2\2\2\6\3\2\2\2\2\b\3\2\2\2\2\n\3\2"+
		"\2\2\2\f\3\2\2\2\2\16\3\2\2\2\2\20\3\2\2\2\2\22\3\2\2\2\2\24\3\2\2\2\2"+
		"\26\3\2\2\2\2\30\3\2\2\2\2\32\3\2\2\2\2\34\3\2\2\2\2\36\3\2\2\2\2 \3\2"+
		"\2\2\2\"\3\2\2\2\2$\3\2\2\2\2&\3\2\2\2\2(\3\2\2\2\2*\3\2\2\2\2,\3\2\2"+
		"\2\2.\3\2\2\2\2\60\3\2\2\2\2\62\3\2\2\2\2\64\3\2\2\2\2\66\3\2\2\2\28\3"+
		"\2\2\2\2:\3\2\2\2\2<\3\2\2\2\2>\3\2\2\2\2@\3\2\2\2\2B\3\2\2\2\2D\3\2\2"+
		"\2\2F\3\2\2\2\2H\3\2\2\2\2J\3\2\2\2\2L\3\2\2\2\2N\3\2\2\2\2P\3\2\2\2\2"+
		"R\3\2\2\2\2T\3\2\2\2\2V\3\2\2\2\2X\3\2\2\2\2Z\3\2\2\2\2\\\3\2\2\2\2^\3"+
		"\2\2\2\2`\3\2\2\2\2b\3\2\2\2\2d\3\2\2\2\2f\3\2\2\2\2h\3\2\2\2\3\u009e"+
		"\3\2\2\2\3\u00a0\3\2\2\2\3\u00a2\3\2\2\2\3\u00a4\3\2\2\2\3\u00a6\3\2\2"+
		"\2\3\u00a8\3\2\2\2\4\u00aa\3\2\2\2\6\u00ae\3\2\2\2\b\u00b2\3\2\2\2\n\u00b6"+
		"\3\2\2\2\f\u00bb\3\2\2\2\16\u00bd\3\2\2\2\20\u00bf\3\2\2\2\22\u00c1\3"+
		"\2\2\2\24\u00c3\3\2\2\2\26\u00c5\3\2\2\2\30\u00c7\3\2\2\2\32\u00c9\3\2"+
		"\2\2\34\u00cb\3\2\2\2\36\u00d3\3\2\2\2 \u00d5\3\2\2\2\"\u00d7\3\2\2\2"+
		"$\u00d9\3\2\2\2&\u00db\3\2\2\2(\u00de\3\2\2\2*\u00e1\3\2\2\2,\u00e3\3"+
		"\2\2\2.\u00e5\3\2\2\2\60\u00e7\3\2\2\2\62\u00ea\3\2\2\2\64\u00ed\3\2\2"+
		"\2\66\u00ef\3\2\2\28\u00f2\3\2\2\2:\u00f4\3\2\2\2<\u00f6\3\2\2\2>\u00f8"+
		"\3\2\2\2@\u00fa\3\2\2\2B\u00fc\3\2\2\2D\u00fe\3\2\2\2F\u0100\3\2\2\2H"+
		"\u0102\3\2\2\2J\u0104\3\2\2\2L\u0107\3\2\2\2N\u0109\3\2\2\2P\u010b\3\2"+
		"\2\2R\u010d\3\2\2\2T\u010f\3\2\2\2V\u0111\3\2\2\2X\u0113\3\2\2\2Z\u0115"+
		"\3\2\2\2\\\u0117\3\2\2\2^\u0119\3\2\2\2`\u011b\3\2\2\2b\u011e\3\2\2\2"+
		"d\u0120\3\2\2\2f\u0123\3\2\2\2h\u0125\3\2\2\2j\u0127\3\2\2\2l\u012c\3"+
		"\2\2\2n\u0132\3\2\2\2p\u0134\3\2\2\2r\u0136\3\2\2\2t\u0139\3\2\2\2v\u013d"+
		"\3\2\2\2x\u0149\3\2\2\2z\u0152\3\2\2\2|\u015f\3\2\2\2~\u0161\3\2\2\2\u0080"+
		"\u0168\3\2\2\2\u0082\u016b\3\2\2\2\u0084\u0174\3\2\2\2\u0086\u017e\3\2"+
		"\2\2\u0088\u0188\3\2\2\2\u008a\u018e\3\2\2\2\u008c\u0193\3\2\2\2\u008e"+
		"\u0196\3\2\2\2\u0090\u01a2\3\2\2\2\u0092\u01a5\3\2\2\2\u0094\u01ac\3\2"+
		"\2\2\u0096\u01d4\3\2\2\2\u0098\u01e3\3\2\2\2\u009a\u0204\3\2\2\2\u009c"+
		"\u0220\3\2\2\2\u009e\u0222\3\2\2\2\u00a0\u0227\3\2\2\2\u00a2\u022c\3\2"+
		"\2\2\u00a4\u022e\3\2\2\2\u00a6\u0230\3\2\2\2\u00a8\u0233\3\2\2\2\u00aa"+
		"\u00ab\5t:\2\u00ab\u00ac\3\2\2\2\u00ac\u00ad\b\2\2\2\u00ad\5\3\2\2\2\u00ae"+
		"\u00af\5v;\2\u00af\u00b0\3\2\2\2\u00b0\u00b1\b\3\2\2\u00b1\7\3\2\2\2\u00b2"+
		"\u00b3\5x<\2\u00b3\u00b4\3\2\2\2\u00b4\u00b5\b\4\2\2\u00b5\t\3\2\2\2\u00b6"+
		"\u00b7\7%\2\2\u00b7\u00b8\7}\2\2\u00b8\u00b9\3\2\2\2\u00b9\u00ba\b\5\3"+
		"\2\u00ba\13\3\2\2\2\u00bb\u00bc\5\u008eG\2\u00bc\r\3\2\2\2\u00bd\u00be"+
		"\5\u0090H\2\u00be\17\3\2\2\2\u00bf\u00c0\5j\65\2\u00c0\21\3\2\2\2\u00c1"+
		"\u00c2\5l\66\2\u00c2\23\3\2\2\2\u00c3\u00c4\5\u0092I\2\u00c4\25\3\2\2"+
		"\2\u00c5\u00c6\5\u0094J\2\u00c6\27\3\2\2\2\u00c7\u00c8\5\u0096K\2\u00c8"+
		"\31\3\2\2\2\u00c9\u00ca\5\u0098L\2\u00ca\33\3\2\2\2\u00cb\u00cc\7\'\2"+
		"\2\u00cc\u00cd\7n\2\2\u00cd\u00ce\7c\2\2\u00ce\u00cf\7v\2\2\u00cf\u00d0"+
		"\7g\2\2\u00d0\u00d1\7u\2\2\u00d1\u00d2\7v\2\2\u00d2\35\3\2\2\2\u00d3\u00d4"+
		"\5\u0086C\2\u00d4\37\3\2\2\2\u00d5\u00d6\5\u0088D\2\u00d6!\3\2\2\2\u00d7"+
		"\u00d8\5\u008cF\2\u00d8#\3\2\2\2\u00d9\u00da\5\u0082A\2\u00da%\3\2\2\2"+
		"\u00db\u00dc\7(\2\2\u00dc\u00dd\7(\2\2\u00dd\'\3\2\2\2\u00de\u00df\7~"+
		"\2\2\u00df\u00e0\7~\2\2\u00e0)\3\2\2\2\u00e1\u00e2\7#\2\2\u00e2+\3\2\2"+
		"\2\u00e3\u00e4\7.\2\2\u00e4-\3\2\2\2\u00e5\u00e6\7?\2\2\u00e6/\3\2\2\2"+
		"\u00e7\u00e8\7?\2\2\u00e8\u00e9\7?\2\2\u00e9\61\3\2\2\2\u00ea\u00eb\7"+
		"#\2\2\u00eb\u00ec\7?\2\2\u00ec\63\3\2\2\2\u00ed\u00ee\7\'\2\2\u00ee\65"+
		"\3\2\2\2\u00ef\u00f0\7/\2\2\u00f0\u00f1\7@\2\2\u00f1\67\3\2\2\2\u00f2"+
		"\u00f3\7}\2\2\u00f39\3\2\2\2\u00f4\u00f5\7\177\2\2\u00f5;\3\2\2\2\u00f6"+
		"\u00f7\7]\2\2\u00f7=\3\2\2\2\u00f8\u00f9\7_\2\2\u00f9?\3\2\2\2\u00fa\u00fb"+
		"\7*\2\2\u00fbA\3\2\2\2\u00fc\u00fd\7+\2\2\u00fdC\3\2\2\2\u00fe\u00ff\7"+
		"<\2\2\u00ffE\3\2\2\2\u0100\u0101\7\60\2\2\u0101G\3\2\2\2\u0102\u0103\7"+
		"&\2\2\u0103I\3\2\2\2\u0104\u0105\7\60\2\2\u0105\u0106\7\60\2\2\u0106K"+
		"\3\2\2\2\u0107\u0108\7=\2\2\u0108M\3\2\2\2\u0109\u010a\7`\2\2\u010aO\3"+
		"\2\2\2\u010b\u010c\7~\2\2\u010cQ\3\2\2\2\u010d\u010e\7\u0080\2\2\u010e"+
		"S\3\2\2\2\u010f\u0110\7B\2\2\u0110U\3\2\2\2\u0111\u0112\7-\2\2\u0112W"+
		"\3\2\2\2\u0113\u0114\7,\2\2\u0114Y\3\2\2\2\u0115\u0116\7/\2\2\u0116[\3"+
		"\2\2\2\u0117\u0118\7\61\2\2\u0118]\3\2\2\2\u0119\u011a\7>\2\2\u011a_\3"+
		"\2\2\2\u011b\u011c\7>\2\2\u011c\u011d\7?\2\2\u011da\3\2\2\2\u011e\u011f"+
		"\7@\2\2\u011fc\3\2\2\2\u0120\u0121\7@\2\2\u0121\u0122\7?\2\2\u0122e\3"+
		"\2\2\2\u0123\u0124\5\u0084B\2\u0124g\3\2\2\2\u0125\u0126\5\u009cN\2\u0126"+
		"i\3\2\2\2\u0127\u0128\7v\2\2\u0128\u0129\7t\2\2\u0129\u012a\7w\2\2\u012a"+
		"\u012b\7g\2\2\u012bk\3\2\2\2\u012c\u012d\7h\2\2\u012d\u012e\7c\2\2\u012e"+
		"\u012f\7n\2\2\u012f\u0130\7u\2\2\u0130\u0131\7g\2\2\u0131m\3\2\2\2\u0132"+
		"\u0133\t\2\2\2\u0133o\3\2\2\2\u0134\u0135\t\3\2\2\u0135q\3\2\2\2\u0136"+
		"\u0137\t\4\2\2\u0137s\3\2\2\2\u0138\u013a\t\5\2\2\u0139\u0138\3\2\2\2"+
		"\u013a\u013b\3\2\2\2\u013b\u0139\3\2\2\2\u013b\u013c\3\2\2\2\u013cu\3"+
		"\2\2\2\u013d\u013e\7\61\2\2\u013e\u013f\7,\2\2\u013f\u0143\3\2\2\2\u0140"+
		"\u0142\13\2\2\2\u0141\u0140\3\2\2\2\u0142\u0145\3\2\2\2\u0143\u0144\3"+
		"\2\2\2\u0143\u0141\3\2\2\2\u0144\u0146\3\2\2\2\u0145\u0143\3\2\2\2\u0146"+
		"\u0147\7,\2\2\u0147\u0148\7\61\2\2\u0148w\3\2\2\2\u0149\u014a\7\61\2\2"+
		"\u014a\u014b\7\61\2\2\u014b\u014f\3\2\2\2\u014c\u014e\n\6\2\2\u014d\u014c"+
		"\3\2\2\2\u014e\u0151\3\2\2\2\u014f\u014d\3\2\2\2\u014f\u0150\3\2\2\2\u0150"+
		"y\3\2\2\2\u0151\u014f\3\2\2\2\u0152\u015d\7w\2\2\u0153\u015b\5r9\2\u0154"+
		"\u0159\5r9\2\u0155\u0157\5r9\2\u0156\u0158\5r9\2\u0157\u0156\3\2\2\2\u0157"+
		"\u0158\3\2\2\2\u0158\u015a\3\2\2\2\u0159\u0155\3\2\2\2\u0159\u015a\3\2"+
		"\2\2\u015a\u015c\3\2\2\2\u015b\u0154\3\2\2\2\u015b\u015c\3\2\2\2\u015c"+
		"\u015e\3\2\2\2\u015d\u0153\3\2\2\2\u015d\u015e\3\2\2\2\u015e{\3\2\2\2"+
		"\u015f\u0160\7^\2\2\u0160}\3\2\2\2\u0161\u0166\5|>\2\u0162\u0167\t\7\2"+
		"\2\u0163\u0167\5z=\2\u0164\u0167\13\2\2\2\u0165\u0167\7\2\2\3\u0166\u0162"+
		"\3\2\2\2\u0166\u0163\3\2\2\2\u0166\u0164\3\2\2\2\u0166\u0165\3\2\2\2\u0167"+
		"\177\3\2\2\2\u0168\u0169\5|>\2\u0169\u016a\13\2\2\2\u016a\u0081\3\2\2"+
		"\2\u016b\u016c\t\b\2\2\u016c\u016d\5p8\2\u016d\u016e\5p8\2\u016e\u016f"+
		"\5p8\2\u016f\u0170\5p8\2\u0170\u0083\3\2\2\2\u0171\u0175\5n\67\2\u0172"+
		"\u0175\5p8\2\u0173\u0175\7a\2\2\u0174\u0171\3\2\2\2\u0174\u0172\3\2\2"+
		"\2\u0174\u0173\3\2\2\2\u0175\u017b\3\2\2\2\u0176\u017a\5n\67\2\u0177\u017a"+
		"\5p8\2\u0178\u017a\t\t\2\2\u0179\u0176\3\2\2\2\u0179\u0177\3\2\2\2\u0179"+
		"\u0178\3\2\2\2\u017a\u017d\3\2\2\2\u017b\u0179\3\2\2\2\u017b\u017c\3\2"+
		"\2\2\u017c\u0085\3\2\2\2\u017d\u017b\3\2\2\2\u017e\u017f\7A\2\2\u017f"+
		"\u0180\7]\2\2\u0180\u0184\3\2\2\2\u0181\u0185\5n\67\2\u0182\u0185\5p8"+
		"\2\u0183\u0185\t\n\2\2\u0184\u0181\3\2\2\2\u0184\u0182\3\2\2\2\u0184\u0183"+
		"\3\2\2\2\u0185\u0186\3\2\2\2\u0186\u0184\3\2\2\2\u0186\u0187\3\2\2\2\u0187"+
		"\u0087\3\2\2\2\u0188\u0189\7_\2\2\u0189\u018a\7A\2\2\u018a\u0089\3\2\2"+
		"\2\u018b\u018d\t\5\2\2\u018c\u018b\3\2\2\2\u018d\u0190\3\2\2\2\u018e\u018c"+
		"\3\2\2\2\u018e\u018f\3\2\2\2\u018f\u0191\3\2\2\2\u0190\u018e\3\2\2\2\u0191"+
		"\u0192\7?\2\2\u0192\u008b\3\2\2\2\u0193\u0194\7<\2\2\u0194\u0195\7<\2"+
		"\2\u0195\u008d\3\2\2\2\u0196\u019b\7)\2\2\u0197\u019a\5~?\2\u0198\u019a"+
		"\n\13\2\2\u0199\u0197\3\2\2\2\u0199\u0198\3\2\2\2\u019a\u019d\3\2\2\2"+
		"\u019b\u0199\3\2\2\2\u019b\u019c\3\2\2\2\u019c\u019e\3\2\2\2\u019d\u019b"+
		"\3\2\2\2\u019e\u019f\7)\2\2\u019f\u008f\3\2\2\2\u01a0\u01a3\5j\65\2\u01a1"+
		"\u01a3\5l\66\2\u01a2\u01a0\3\2\2\2\u01a2\u01a1\3\2\2\2\u01a3\u0091\3\2"+
		"\2\2\u01a4\u01a6\5p8\2\u01a5\u01a4\3\2\2\2\u01a6\u01a7\3\2\2\2\u01a7\u01a5"+
		"\3\2\2\2\u01a7\u01a8\3\2\2\2\u01a8\u0093\3\2\2\2\u01a9\u01ab\5p8\2\u01aa"+
		"\u01a9\3\2\2\2\u01ab\u01ae\3\2\2\2\u01ac\u01aa\3\2\2\2\u01ac\u01ad\3\2"+
		"\2\2\u01ad\u01af\3\2\2\2\u01ae\u01ac\3\2\2\2\u01af\u01b1\7\60\2\2\u01b0"+
		"\u01b2\5p8\2\u01b1\u01b0\3\2\2\2\u01b2\u01b3\3\2\2\2\u01b3\u01b1\3\2\2"+
		"\2\u01b3\u01b4\3\2\2\2\u01b4\u01be\3\2\2\2\u01b5\u01b7\t\f\2\2\u01b6\u01b8"+
		"\t\b\2\2\u01b7\u01b6\3\2\2\2\u01b7\u01b8\3\2\2\2\u01b8\u01ba\3\2\2\2\u01b9"+
		"\u01bb\5p8\2\u01ba\u01b9\3\2\2\2\u01bb\u01bc\3\2\2\2\u01bc\u01ba\3\2\2"+
		"\2\u01bc\u01bd\3\2\2\2\u01bd\u01bf\3\2\2\2\u01be\u01b5\3\2\2\2\u01be\u01bf"+
		"\3\2\2\2\u01bf\u01c1\3\2\2\2\u01c0\u01c2\t\r\2\2\u01c1\u01c0\3\2\2\2\u01c1"+
		"\u01c2\3\2\2\2\u01c2\u0095\3\2\2\2\u01c3\u01c5\5p8\2\u01c4\u01c3\3\2\2"+
		"\2\u01c5\u01c8\3\2\2\2\u01c6\u01c4\3\2\2\2\u01c6\u01c7\3\2\2\2\u01c7\u01c9"+
		"\3\2\2\2\u01c8\u01c6\3\2\2\2\u01c9\u01cb\7\60\2\2\u01ca\u01cc\5p8\2\u01cb"+
		"\u01ca\3\2\2\2\u01cc\u01cd\3\2\2\2\u01cd\u01cb\3\2\2\2\u01cd\u01ce\3\2"+
		"\2\2\u01ce\u01d5\3\2\2\2\u01cf\u01d1\5p8\2\u01d0\u01cf\3\2\2\2\u01d1\u01d2"+
		"\3\2\2\2\u01d2\u01d0\3\2\2\2\u01d2\u01d3\3\2\2\2\u01d3\u01d5\3\2\2\2\u01d4"+
		"\u01c6\3\2\2\2\u01d4\u01d0\3\2\2\2\u01d5\u01df\3\2\2\2\u01d6\u01d8\t\f"+
		"\2\2\u01d7\u01d9\t\b\2\2\u01d8\u01d7\3\2\2\2\u01d8\u01d9\3\2\2\2\u01d9"+
		"\u01db\3\2\2\2\u01da\u01dc\5p8\2\u01db\u01da\3\2\2\2\u01dc\u01dd\3\2\2"+
		"\2\u01dd\u01db\3\2\2\2\u01dd\u01de\3\2\2\2\u01de\u01e0\3\2\2\2\u01df\u01d6"+
		"\3\2\2\2\u01df\u01e0\3\2\2\2\u01e0\u01e1\3\2\2\2\u01e1\u01e2\t\16\2\2"+
		"\u01e2\u0097\3\2\2\2\u01e3\u01e5\7\'\2\2\u01e4\u01e6\7/\2\2\u01e5\u01e4"+
		"\3\2\2\2\u01e5\u01e6\3\2\2\2\u01e6\u01e8\3\2\2\2\u01e7\u01e9\5p8\2\u01e8"+
		"\u01e7\3\2\2\2\u01e9\u01ea\3\2\2\2\u01ea\u01e8\3\2\2\2\u01ea\u01eb\3\2"+
		"\2\2\u01eb\u0201\3\2\2\2\u01ec\u01ee\7/\2\2\u01ed\u01ef\5p8\2\u01ee\u01ed"+
		"\3\2\2\2\u01ef\u01f0\3\2\2\2\u01f0\u01ee\3\2\2\2\u01f0\u01f1\3\2\2\2\u01f1"+
		"\u01ff\3\2\2\2\u01f2\u01f4\7/\2\2\u01f3\u01f5\5p8\2\u01f4\u01f3\3\2\2"+
		"\2\u01f5\u01f6\3\2\2\2\u01f6\u01f4\3\2\2\2\u01f6\u01f7\3\2\2\2\u01f7\u01fd"+
		"\3\2\2\2\u01f8\u01f9\7V\2\2\u01f9\u01fb\5\u009aM\2\u01fa\u01fc\5\u0082"+
		"A\2\u01fb\u01fa\3\2\2\2\u01fb\u01fc\3\2\2\2\u01fc\u01fe\3\2\2\2\u01fd"+
		"\u01f8\3\2\2\2\u01fd\u01fe\3\2\2\2\u01fe\u0200\3\2\2\2\u01ff\u01f2\3\2"+
		"\2\2\u01ff\u0200\3\2\2\2\u0200\u0202\3\2\2\2\u0201\u01ec\3\2\2\2\u0201"+
		"\u0202\3\2\2\2\u0202\u0099\3\2\2\2\u0203\u0205\5p8\2\u0204\u0203\3\2\2"+
		"\2\u0205\u0206\3\2\2\2\u0206\u0204\3\2\2\2\u0206\u0207\3\2\2\2\u0207\u021e"+
		"\3\2\2\2\u0208\u020a\7<\2\2\u0209\u020b\5p8\2\u020a\u0209\3\2\2\2\u020b"+
		"\u020c\3\2\2\2\u020c\u020a\3\2\2\2\u020c\u020d\3\2\2\2\u020d\u021c\3\2"+
		"\2\2\u020e\u0210\7<\2\2\u020f\u0211\5p8\2\u0210\u020f\3\2\2\2\u0211\u0212"+
		"\3\2\2\2\u0212\u0210\3\2\2\2\u0212\u0213\3\2\2\2\u0213\u021a\3\2\2\2\u0214"+
		"\u0216\7\60\2\2\u0215\u0217\5p8\2\u0216\u0215\3\2\2\2\u0217\u0218\3\2"+
		"\2\2\u0218\u0216\3\2\2\2\u0218\u0219\3\2\2\2\u0219\u021b\3\2\2\2\u021a"+
		"\u0214\3\2\2\2\u021a\u021b\3\2\2\2\u021b\u021d\3\2\2\2\u021c\u020e\3\2"+
		"\2\2\u021c\u021d\3\2\2\2\u021d\u021f\3\2\2\2\u021e\u0208\3\2\2\2\u021e"+
		"\u021f\3\2\2\2\u021f\u009b\3\2\2\2\u0220\u0221\13\2\2\2\u0221\u009d\3"+
		"\2\2\2\u0222\u0223\7%\2\2\u0223\u0224\7}\2\2\u0224\u0225\3\2\2\2\u0225"+
		"\u0226\bO\3\2\u0226\u009f\3\2\2\2\u0227\u0228\7\177\2\2\u0228\u0229\7"+
		"%\2\2\u0229\u022a\3\2\2\2\u022a\u022b\bP\4\2\u022b\u00a1\3\2\2\2\u022c"+
		"\u022d\7%\2\2\u022d\u00a3\3\2\2\2\u022e\u022f\7}\2\2\u022f\u00a5\3\2\2"+
		"\2\u0230\u0231\7\177\2\2\u0231\u00a7\3\2\2\2\u0232\u0234\n\17\2\2\u0233"+
		"\u0232\3\2\2\2\u0234\u0235\3\2\2\2\u0235\u0233\3\2\2\2\u0235\u0236\3\2"+
		"\2\2\u0236\u00a9\3\2\2\2\63\2\3\u013b\u0143\u014f\u0157\u0159\u015b\u015d"+
		"\u0166\u0174\u0179\u017b\u0184\u0186\u018e\u0199\u019b\u01a2\u01a7\u01ac"+
		"\u01b3\u01b7\u01bc\u01be\u01c1\u01c6\u01cd\u01d2\u01d4\u01d8\u01dd\u01df"+
		"\u01e5\u01ea\u01f0\u01f6\u01fb\u01fd\u01ff\u0201\u0206\u020c\u0212\u0218"+
		"\u021a\u021c\u021e\u0235\5\b\2\2\7\3\2\6\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}