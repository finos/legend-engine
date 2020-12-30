// Generated from C:/Users/lmaur/Documents/GS/legend-engine/legend-engine-language-pure-grammar/src/main/antlr4/org/finos/legend/engine/language/pure/grammar/from/antlr4/core\M3ParserGrammar.g4 by ANTLR 4.9
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class M3ParserGrammar extends Parser {
	static { RuntimeMetaData.checkVersion("4.9", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		VALID_STRING=1, STRING=2, ALL=3, LET=4, ALL_VERSIONS=5, ALL_VERSIONS_IN_RANGE=6, 
		PAREN_OPEN=7, PAREN_CLOSE=8, NEW_SYMBOL=9, LESS_THAN=10, PIPE=11, GREATER_THAN=12, 
		FILE_NAME=13, COLON=14, INTEGER=15, COMMA=16, FILE_NAME_END=17, AT=18, 
		TILDE=19, EQUAL=20, BRACKET_OPEN=21, BRACKET_CLOSE=22, LATEST_DATE=23, 
		DOT=24, PERCENT=25, SEMI_COLON=26, TEST_EQUAL=27, TEST_NOT_EQUAL=28, ARROW=29, 
		PATH_SEPARATOR=30, BRACE_OPEN=31, BRACE_CLOSE=32, DOLLAR=33, DATE=34, 
		PLUS=35, NOT=36, MINUS=37, FLOAT=38, DECIMAL=39, BOOLEAN=40, STAR=41, 
		DIVIDE=42, LESS_OR_EQUAL=43, GREATER_OR_EQUAL=44, AND=45, OR=46, NAVIGATION_PATH_BLOCK=47, 
		ISLAND_OPEN=48, ISLAND_START=49, ISLAND_BRACE_OPEN=50, ISLAND_CONTENT=51, 
		ISLAND_HASH=52, ISLAND_BRACE_CLOSE=53, ISLAND_END=54, DOT_DOT=55;
	public static final int
		RULE_identifier = 0, RULE_expression = 1, RULE_instance = 2, RULE_unitInstance = 3, 
		RULE_unitName = 4, RULE_instancePropertyAssignment = 5, RULE_instanceRightSide = 6, 
		RULE_instanceAtomicRightSideScalar = 7, RULE_instanceAtomicRightSideVector = 8, 
		RULE_instanceAtomicRightSide = 9, RULE_enumReference = 10, RULE_stereotypeReference = 11, 
		RULE_tagReference = 12, RULE_propertyReturnType = 13, RULE_codeBlock = 14, 
		RULE_programLine = 15, RULE_equalNotEqual = 16, RULE_combinedArithmeticOnly = 17, 
		RULE_expressionPart = 18, RULE_letExpression = 19, RULE_combinedExpression = 20, 
		RULE_expressionOrExpressionGroup = 21, RULE_expressionsArray = 22, RULE_propertyOrFunctionExpression = 23, 
		RULE_propertyExpression = 24, RULE_propertyBracketExpression = 25, RULE_functionExpression = 26, 
		RULE_functionExpressionLatestMilestoningDateParameter = 27, RULE_functionExpressionParameters = 28, 
		RULE_atomicExpression = 29, RULE_instanceReference = 30, RULE_lambdaFunction = 31, 
		RULE_variable = 32, RULE_allOrFunction = 33, RULE_allFunction = 34, RULE_allVersionsFunction = 35, 
		RULE_allVersionsInRangeFunction = 36, RULE_allFunctionWithMilestoning = 37, 
		RULE_buildMilestoningVariableExpression = 38, RULE_expressionInstance = 39, 
		RULE_expressionInstanceRightSide = 40, RULE_expressionInstanceAtomicRightSide = 41, 
		RULE_expressionInstanceParserPropertyAssignment = 42, RULE_sliceExpression = 43, 
		RULE_notExpression = 44, RULE_signedExpression = 45, RULE_lambdaPipe = 46, 
		RULE_lambdaParam = 47, RULE_lambdaParamType = 48, RULE_instanceLiteral = 49, 
		RULE_instanceLiteralToken = 50, RULE_unitInstanceLiteral = 51, RULE_arithmeticPart = 52, 
		RULE_booleanPart = 53, RULE_functionVariableExpression = 54, RULE_dsl = 55, 
		RULE_dslNavigationPath = 56, RULE_dslGraphFetch = 57, RULE_dslContent = 58, 
		RULE_type = 59, RULE_multiplicity = 60, RULE_fromMultiplicity = 61, RULE_toMultiplicity = 62, 
		RULE_functionTypePureType = 63, RULE_typeAndMultiplicityParameters = 64, 
		RULE_typeParametersWithContravarianceAndMultiplicityParameters = 65, RULE_typeParameters = 66, 
		RULE_typeParameter = 67, RULE_contravarianceTypeParameters = 68, RULE_contravarianceTypeParameter = 69, 
		RULE_multiplicityArguments = 70, RULE_multiplicityArgument = 71, RULE_typeArguments = 72, 
		RULE_multiplictyParameters = 73, RULE_qualifiedName = 74, RULE_packagePath = 75, 
		RULE_word = 76;
	private static String[] makeRuleNames() {
		return new String[] {
			"identifier", "expression", "instance", "unitInstance", "unitName", "instancePropertyAssignment", 
			"instanceRightSide", "instanceAtomicRightSideScalar", "instanceAtomicRightSideVector", 
			"instanceAtomicRightSide", "enumReference", "stereotypeReference", "tagReference", 
			"propertyReturnType", "codeBlock", "programLine", "equalNotEqual", "combinedArithmeticOnly", 
			"expressionPart", "letExpression", "combinedExpression", "expressionOrExpressionGroup", 
			"expressionsArray", "propertyOrFunctionExpression", "propertyExpression", 
			"propertyBracketExpression", "functionExpression", "functionExpressionLatestMilestoningDateParameter", 
			"functionExpressionParameters", "atomicExpression", "instanceReference", 
			"lambdaFunction", "variable", "allOrFunction", "allFunction", "allVersionsFunction", 
			"allVersionsInRangeFunction", "allFunctionWithMilestoning", "buildMilestoningVariableExpression", 
			"expressionInstance", "expressionInstanceRightSide", "expressionInstanceAtomicRightSide", 
			"expressionInstanceParserPropertyAssignment", "sliceExpression", "notExpression", 
			"signedExpression", "lambdaPipe", "lambdaParam", "lambdaParamType", "instanceLiteral", 
			"instanceLiteralToken", "unitInstanceLiteral", "arithmeticPart", "booleanPart", 
			"functionVariableExpression", "dsl", "dslNavigationPath", "dslGraphFetch", 
			"dslContent", "type", "multiplicity", "fromMultiplicity", "toMultiplicity", 
			"functionTypePureType", "typeAndMultiplicityParameters", "typeParametersWithContravarianceAndMultiplicityParameters", 
			"typeParameters", "typeParameter", "contravarianceTypeParameters", "contravarianceTypeParameter", 
			"multiplicityArguments", "multiplicityArgument", "typeArguments", "multiplictyParameters", 
			"qualifiedName", "packagePath", "word"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "VALID_STRING", "STRING", "ALL", "LET", "ALL_VERSIONS", "ALL_VERSIONS_IN_RANGE", 
			"PAREN_OPEN", "PAREN_CLOSE", "NEW_SYMBOL", "LESS_THAN", "PIPE", "GREATER_THAN", 
			"FILE_NAME", "COLON", "INTEGER", "COMMA", "FILE_NAME_END", "AT", "TILDE", 
			"EQUAL", "BRACKET_OPEN", "BRACKET_CLOSE", "LATEST_DATE", "DOT", "PERCENT", 
			"SEMI_COLON", "TEST_EQUAL", "TEST_NOT_EQUAL", "ARROW", "PATH_SEPARATOR", 
			"BRACE_OPEN", "BRACE_CLOSE", "DOLLAR", "DATE", "PLUS", "NOT", "MINUS", 
			"FLOAT", "DECIMAL", "BOOLEAN", "STAR", "DIVIDE", "LESS_OR_EQUAL", "GREATER_OR_EQUAL", 
			"AND", "OR", "NAVIGATION_PATH_BLOCK", "ISLAND_OPEN", "ISLAND_START", 
			"ISLAND_BRACE_OPEN", "ISLAND_CONTENT", "ISLAND_HASH", "ISLAND_BRACE_CLOSE", 
			"ISLAND_END", "DOT_DOT"
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

	@Override
	public String getGrammarFileName() { return "M3ParserGrammar.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public M3ParserGrammar(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class IdentifierContext extends ParserRuleContext {
		public TerminalNode VALID_STRING() { return getToken(M3ParserGrammar.VALID_STRING, 0); }
		public TerminalNode STRING() { return getToken(M3ParserGrammar.STRING, 0); }
		public TerminalNode ALL() { return getToken(M3ParserGrammar.ALL, 0); }
		public TerminalNode LET() { return getToken(M3ParserGrammar.LET, 0); }
		public TerminalNode ALL_VERSIONS() { return getToken(M3ParserGrammar.ALL_VERSIONS, 0); }
		public TerminalNode ALL_VERSIONS_IN_RANGE() { return getToken(M3ParserGrammar.ALL_VERSIONS_IN_RANGE, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(154);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionContext extends ParserRuleContext {
		public SliceExpressionContext sliceExpression() {
			return getRuleContext(SliceExpressionContext.class,0);
		}
		public AtomicExpressionContext atomicExpression() {
			return getRuleContext(AtomicExpressionContext.class,0);
		}
		public NotExpressionContext notExpression() {
			return getRuleContext(NotExpressionContext.class,0);
		}
		public SignedExpressionContext signedExpression() {
			return getRuleContext(SignedExpressionContext.class,0);
		}
		public ExpressionsArrayContext expressionsArray() {
			return getRuleContext(ExpressionsArrayContext.class,0);
		}
		public List<PropertyOrFunctionExpressionContext> propertyOrFunctionExpression() {
			return getRuleContexts(PropertyOrFunctionExpressionContext.class);
		}
		public PropertyOrFunctionExpressionContext propertyOrFunctionExpression(int i) {
			return getRuleContext(PropertyOrFunctionExpressionContext.class,i);
		}
		public EqualNotEqualContext equalNotEqual() {
			return getRuleContext(EqualNotEqualContext.class,0);
		}
		public TerminalNode PAREN_OPEN() { return getToken(M3ParserGrammar.PAREN_OPEN, 0); }
		public CombinedExpressionContext combinedExpression() {
			return getRuleContext(CombinedExpressionContext.class,0);
		}
		public TerminalNode PAREN_CLOSE() { return getToken(M3ParserGrammar.PAREN_CLOSE, 0); }
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_expression);
		try {
			int _alt;
			setState(176);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VALID_STRING:
			case STRING:
			case ALL:
			case LET:
			case ALL_VERSIONS:
			case ALL_VERSIONS_IN_RANGE:
			case NEW_SYMBOL:
			case PIPE:
			case INTEGER:
			case AT:
			case BRACKET_OPEN:
			case PATH_SEPARATOR:
			case BRACE_OPEN:
			case DOLLAR:
			case DATE:
			case PLUS:
			case NOT:
			case MINUS:
			case FLOAT:
			case DECIMAL:
			case BOOLEAN:
			case NAVIGATION_PATH_BLOCK:
			case ISLAND_OPEN:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(161);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
				case 1:
					{
					setState(156);
					sliceExpression();
					}
					break;
				case 2:
					{
					setState(157);
					atomicExpression();
					}
					break;
				case 3:
					{
					setState(158);
					notExpression();
					}
					break;
				case 4:
					{
					setState(159);
					signedExpression();
					}
					break;
				case 5:
					{
					setState(160);
					expressionsArray();
					}
					break;
				}
				{
				setState(166);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(163);
						propertyOrFunctionExpression();
						}
						} 
					}
					setState(168);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
				}
				setState(170);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
				case 1:
					{
					setState(169);
					equalNotEqual();
					}
					break;
				}
				}
				}
				}
				break;
			case PAREN_OPEN:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(172);
				match(PAREN_OPEN);
				setState(173);
				combinedExpression();
				setState(174);
				match(PAREN_CLOSE);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InstanceContext extends ParserRuleContext {
		public TerminalNode NEW_SYMBOL() { return getToken(M3ParserGrammar.NEW_SYMBOL, 0); }
		public List<QualifiedNameContext> qualifiedName() {
			return getRuleContexts(QualifiedNameContext.class);
		}
		public QualifiedNameContext qualifiedName(int i) {
			return getRuleContext(QualifiedNameContext.class,i);
		}
		public TerminalNode PAREN_OPEN() { return getToken(M3ParserGrammar.PAREN_OPEN, 0); }
		public TerminalNode PAREN_CLOSE() { return getToken(M3ParserGrammar.PAREN_CLOSE, 0); }
		public TerminalNode LESS_THAN() { return getToken(M3ParserGrammar.LESS_THAN, 0); }
		public TerminalNode GREATER_THAN() { return getToken(M3ParserGrammar.GREATER_THAN, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode FILE_NAME() { return getToken(M3ParserGrammar.FILE_NAME, 0); }
		public TerminalNode COLON() { return getToken(M3ParserGrammar.COLON, 0); }
		public List<TerminalNode> INTEGER() { return getTokens(M3ParserGrammar.INTEGER); }
		public TerminalNode INTEGER(int i) {
			return getToken(M3ParserGrammar.INTEGER, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public TerminalNode FILE_NAME_END() { return getToken(M3ParserGrammar.FILE_NAME_END, 0); }
		public TerminalNode AT() { return getToken(M3ParserGrammar.AT, 0); }
		public List<InstancePropertyAssignmentContext> instancePropertyAssignment() {
			return getRuleContexts(InstancePropertyAssignmentContext.class);
		}
		public InstancePropertyAssignmentContext instancePropertyAssignment(int i) {
			return getRuleContext(InstancePropertyAssignmentContext.class,i);
		}
		public TypeArgumentsContext typeArguments() {
			return getRuleContext(TypeArgumentsContext.class,0);
		}
		public TerminalNode PIPE() { return getToken(M3ParserGrammar.PIPE, 0); }
		public MultiplicityArgumentsContext multiplicityArguments() {
			return getRuleContext(MultiplicityArgumentsContext.class,0);
		}
		public InstanceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_instance; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterInstance(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitInstance(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitInstance(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InstanceContext instance() throws RecognitionException {
		InstanceContext _localctx = new InstanceContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_instance);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(178);
			match(NEW_SYMBOL);
			setState(179);
			qualifiedName();
			setState(189);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LESS_THAN) {
				{
				setState(180);
				match(LESS_THAN);
				setState(182);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE) | (1L << BRACE_OPEN))) != 0)) {
					{
					setState(181);
					typeArguments();
					}
				}

				setState(186);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PIPE) {
					{
					setState(184);
					match(PIPE);
					setState(185);
					multiplicityArguments();
					}
				}

				setState(188);
				match(GREATER_THAN);
				}
			}

			setState(192);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE))) != 0)) {
				{
				setState(191);
				identifier();
				}
			}

			setState(208);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FILE_NAME) {
				{
				setState(194);
				match(FILE_NAME);
				setState(195);
				match(COLON);
				setState(196);
				match(INTEGER);
				setState(197);
				match(COMMA);
				setState(198);
				match(INTEGER);
				setState(199);
				match(COMMA);
				setState(200);
				match(INTEGER);
				setState(201);
				match(COMMA);
				setState(202);
				match(INTEGER);
				setState(203);
				match(COMMA);
				setState(204);
				match(INTEGER);
				setState(205);
				match(COMMA);
				setState(206);
				match(INTEGER);
				setState(207);
				match(FILE_NAME_END);
				}
			}

			setState(212);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AT) {
				{
				setState(210);
				match(AT);
				setState(211);
				qualifiedName();
				}
			}

			setState(214);
			match(PAREN_OPEN);
			setState(223);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE))) != 0)) {
				{
				setState(215);
				instancePropertyAssignment();
				setState(220);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(216);
					match(COMMA);
					setState(217);
					instancePropertyAssignment();
					}
					}
					setState(222);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(225);
			match(PAREN_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UnitInstanceContext extends ParserRuleContext {
		public UnitInstanceLiteralContext unitInstanceLiteral() {
			return getRuleContext(UnitInstanceLiteralContext.class,0);
		}
		public UnitNameContext unitName() {
			return getRuleContext(UnitNameContext.class,0);
		}
		public UnitInstanceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unitInstance; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterUnitInstance(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitUnitInstance(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitUnitInstance(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnitInstanceContext unitInstance() throws RecognitionException {
		UnitInstanceContext _localctx = new UnitInstanceContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_unitInstance);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(227);
			unitInstanceLiteral();
			setState(228);
			unitName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UnitNameContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode TILDE() { return getToken(M3ParserGrammar.TILDE, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public UnitNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unitName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterUnitName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitUnitName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitUnitName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnitNameContext unitName() throws RecognitionException {
		UnitNameContext _localctx = new UnitNameContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_unitName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(230);
			qualifiedName();
			setState(231);
			match(TILDE);
			setState(232);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InstancePropertyAssignmentContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode EQUAL() { return getToken(M3ParserGrammar.EQUAL, 0); }
		public InstanceRightSideContext instanceRightSide() {
			return getRuleContext(InstanceRightSideContext.class,0);
		}
		public InstancePropertyAssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_instancePropertyAssignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterInstancePropertyAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitInstancePropertyAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitInstancePropertyAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InstancePropertyAssignmentContext instancePropertyAssignment() throws RecognitionException {
		InstancePropertyAssignmentContext _localctx = new InstancePropertyAssignmentContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_instancePropertyAssignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(234);
			identifier();
			setState(235);
			match(EQUAL);
			setState(236);
			instanceRightSide();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InstanceRightSideContext extends ParserRuleContext {
		public InstanceAtomicRightSideScalarContext instanceAtomicRightSideScalar() {
			return getRuleContext(InstanceAtomicRightSideScalarContext.class,0);
		}
		public InstanceAtomicRightSideVectorContext instanceAtomicRightSideVector() {
			return getRuleContext(InstanceAtomicRightSideVectorContext.class,0);
		}
		public InstanceRightSideContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_instanceRightSide; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterInstanceRightSide(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitInstanceRightSide(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitInstanceRightSide(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InstanceRightSideContext instanceRightSide() throws RecognitionException {
		InstanceRightSideContext _localctx = new InstanceRightSideContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_instanceRightSide);
		try {
			setState(240);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VALID_STRING:
			case STRING:
			case ALL:
			case LET:
			case ALL_VERSIONS:
			case ALL_VERSIONS_IN_RANGE:
			case NEW_SYMBOL:
			case INTEGER:
			case LATEST_DATE:
			case DATE:
			case PLUS:
			case MINUS:
			case FLOAT:
			case DECIMAL:
			case BOOLEAN:
				enterOuterAlt(_localctx, 1);
				{
				setState(238);
				instanceAtomicRightSideScalar();
				}
				break;
			case BRACKET_OPEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(239);
				instanceAtomicRightSideVector();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InstanceAtomicRightSideScalarContext extends ParserRuleContext {
		public InstanceAtomicRightSideContext instanceAtomicRightSide() {
			return getRuleContext(InstanceAtomicRightSideContext.class,0);
		}
		public InstanceAtomicRightSideScalarContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_instanceAtomicRightSideScalar; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterInstanceAtomicRightSideScalar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitInstanceAtomicRightSideScalar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitInstanceAtomicRightSideScalar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InstanceAtomicRightSideScalarContext instanceAtomicRightSideScalar() throws RecognitionException {
		InstanceAtomicRightSideScalarContext _localctx = new InstanceAtomicRightSideScalarContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_instanceAtomicRightSideScalar);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(242);
			instanceAtomicRightSide();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InstanceAtomicRightSideVectorContext extends ParserRuleContext {
		public TerminalNode BRACKET_OPEN() { return getToken(M3ParserGrammar.BRACKET_OPEN, 0); }
		public TerminalNode BRACKET_CLOSE() { return getToken(M3ParserGrammar.BRACKET_CLOSE, 0); }
		public List<InstanceAtomicRightSideContext> instanceAtomicRightSide() {
			return getRuleContexts(InstanceAtomicRightSideContext.class);
		}
		public InstanceAtomicRightSideContext instanceAtomicRightSide(int i) {
			return getRuleContext(InstanceAtomicRightSideContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public InstanceAtomicRightSideVectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_instanceAtomicRightSideVector; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterInstanceAtomicRightSideVector(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitInstanceAtomicRightSideVector(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitInstanceAtomicRightSideVector(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InstanceAtomicRightSideVectorContext instanceAtomicRightSideVector() throws RecognitionException {
		InstanceAtomicRightSideVectorContext _localctx = new InstanceAtomicRightSideVectorContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_instanceAtomicRightSideVector);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(244);
			match(BRACKET_OPEN);
			setState(253);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE) | (1L << NEW_SYMBOL) | (1L << INTEGER) | (1L << LATEST_DATE) | (1L << DATE) | (1L << PLUS) | (1L << MINUS) | (1L << FLOAT) | (1L << DECIMAL) | (1L << BOOLEAN))) != 0)) {
				{
				setState(245);
				instanceAtomicRightSide();
				setState(250);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(246);
					match(COMMA);
					setState(247);
					instanceAtomicRightSide();
					}
					}
					setState(252);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(255);
			match(BRACKET_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InstanceAtomicRightSideContext extends ParserRuleContext {
		public InstanceLiteralContext instanceLiteral() {
			return getRuleContext(InstanceLiteralContext.class,0);
		}
		public TerminalNode LATEST_DATE() { return getToken(M3ParserGrammar.LATEST_DATE, 0); }
		public InstanceContext instance() {
			return getRuleContext(InstanceContext.class,0);
		}
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public EnumReferenceContext enumReference() {
			return getRuleContext(EnumReferenceContext.class,0);
		}
		public StereotypeReferenceContext stereotypeReference() {
			return getRuleContext(StereotypeReferenceContext.class,0);
		}
		public TagReferenceContext tagReference() {
			return getRuleContext(TagReferenceContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public InstanceAtomicRightSideContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_instanceAtomicRightSide; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterInstanceAtomicRightSide(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitInstanceAtomicRightSide(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitInstanceAtomicRightSide(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InstanceAtomicRightSideContext instanceAtomicRightSide() throws RecognitionException {
		InstanceAtomicRightSideContext _localctx = new InstanceAtomicRightSideContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_instanceAtomicRightSide);
		try {
			setState(265);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(257);
				instanceLiteral();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(258);
				match(LATEST_DATE);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(259);
				instance();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(260);
				qualifiedName();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(261);
				enumReference();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(262);
				stereotypeReference();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(263);
				tagReference();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(264);
				identifier();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EnumReferenceContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode DOT() { return getToken(M3ParserGrammar.DOT, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public EnumReferenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumReference; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterEnumReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitEnumReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitEnumReference(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumReferenceContext enumReference() throws RecognitionException {
		EnumReferenceContext _localctx = new EnumReferenceContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_enumReference);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(267);
			qualifiedName();
			setState(268);
			match(DOT);
			setState(269);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StereotypeReferenceContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode AT() { return getToken(M3ParserGrammar.AT, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public StereotypeReferenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stereotypeReference; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterStereotypeReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitStereotypeReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitStereotypeReference(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StereotypeReferenceContext stereotypeReference() throws RecognitionException {
		StereotypeReferenceContext _localctx = new StereotypeReferenceContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_stereotypeReference);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
			qualifiedName();
			setState(272);
			match(AT);
			setState(273);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TagReferenceContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode PERCENT() { return getToken(M3ParserGrammar.PERCENT, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TagReferenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tagReference; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterTagReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitTagReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitTagReference(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TagReferenceContext tagReference() throws RecognitionException {
		TagReferenceContext _localctx = new TagReferenceContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_tagReference);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(275);
			qualifiedName();
			setState(276);
			match(PERCENT);
			setState(277);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyReturnTypeContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public MultiplicityContext multiplicity() {
			return getRuleContext(MultiplicityContext.class,0);
		}
		public PropertyReturnTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyReturnType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterPropertyReturnType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitPropertyReturnType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitPropertyReturnType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyReturnTypeContext propertyReturnType() throws RecognitionException {
		PropertyReturnTypeContext _localctx = new PropertyReturnTypeContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_propertyReturnType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(279);
			type();
			setState(280);
			multiplicity();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CodeBlockContext extends ParserRuleContext {
		public List<ProgramLineContext> programLine() {
			return getRuleContexts(ProgramLineContext.class);
		}
		public ProgramLineContext programLine(int i) {
			return getRuleContext(ProgramLineContext.class,i);
		}
		public List<TerminalNode> SEMI_COLON() { return getTokens(M3ParserGrammar.SEMI_COLON); }
		public TerminalNode SEMI_COLON(int i) {
			return getToken(M3ParserGrammar.SEMI_COLON, i);
		}
		public CodeBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_codeBlock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterCodeBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitCodeBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitCodeBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CodeBlockContext codeBlock() throws RecognitionException {
		CodeBlockContext _localctx = new CodeBlockContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_codeBlock);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(282);
			programLine();
			setState(292);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				{
				setState(283);
				match(SEMI_COLON);
				setState(289);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(284);
						programLine();
						setState(285);
						match(SEMI_COLON);
						}
						} 
					}
					setState(291);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProgramLineContext extends ParserRuleContext {
		public CombinedExpressionContext combinedExpression() {
			return getRuleContext(CombinedExpressionContext.class,0);
		}
		public LetExpressionContext letExpression() {
			return getRuleContext(LetExpressionContext.class,0);
		}
		public ProgramLineContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programLine; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterProgramLine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitProgramLine(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitProgramLine(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramLineContext programLine() throws RecognitionException {
		ProgramLineContext _localctx = new ProgramLineContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_programLine);
		try {
			setState(296);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(294);
				combinedExpression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(295);
				letExpression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EqualNotEqualContext extends ParserRuleContext {
		public CombinedArithmeticOnlyContext combinedArithmeticOnly() {
			return getRuleContext(CombinedArithmeticOnlyContext.class,0);
		}
		public TerminalNode TEST_EQUAL() { return getToken(M3ParserGrammar.TEST_EQUAL, 0); }
		public TerminalNode TEST_NOT_EQUAL() { return getToken(M3ParserGrammar.TEST_NOT_EQUAL, 0); }
		public EqualNotEqualContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_equalNotEqual; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterEqualNotEqual(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitEqualNotEqual(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitEqualNotEqual(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EqualNotEqualContext equalNotEqual() throws RecognitionException {
		EqualNotEqualContext _localctx = new EqualNotEqualContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_equalNotEqual);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(298);
			_la = _input.LA(1);
			if ( !(_la==TEST_EQUAL || _la==TEST_NOT_EQUAL) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(299);
			combinedArithmeticOnly();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CombinedArithmeticOnlyContext extends ParserRuleContext {
		public ExpressionOrExpressionGroupContext expressionOrExpressionGroup() {
			return getRuleContext(ExpressionOrExpressionGroupContext.class,0);
		}
		public List<ArithmeticPartContext> arithmeticPart() {
			return getRuleContexts(ArithmeticPartContext.class);
		}
		public ArithmeticPartContext arithmeticPart(int i) {
			return getRuleContext(ArithmeticPartContext.class,i);
		}
		public CombinedArithmeticOnlyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_combinedArithmeticOnly; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterCombinedArithmeticOnly(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitCombinedArithmeticOnly(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitCombinedArithmeticOnly(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CombinedArithmeticOnlyContext combinedArithmeticOnly() throws RecognitionException {
		CombinedArithmeticOnlyContext _localctx = new CombinedArithmeticOnlyContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_combinedArithmeticOnly);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(301);
			expressionOrExpressionGroup();
			setState(305);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(302);
					arithmeticPart();
					}
					} 
				}
				setState(307);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionPartContext extends ParserRuleContext {
		public BooleanPartContext booleanPart() {
			return getRuleContext(BooleanPartContext.class,0);
		}
		public ArithmeticPartContext arithmeticPart() {
			return getRuleContext(ArithmeticPartContext.class,0);
		}
		public ExpressionPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterExpressionPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitExpressionPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitExpressionPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionPartContext expressionPart() throws RecognitionException {
		ExpressionPartContext _localctx = new ExpressionPartContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_expressionPart);
		try {
			setState(310);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TEST_EQUAL:
			case TEST_NOT_EQUAL:
			case AND:
			case OR:
				enterOuterAlt(_localctx, 1);
				{
				setState(308);
				booleanPart();
				}
				break;
			case LESS_THAN:
			case GREATER_THAN:
			case PLUS:
			case MINUS:
			case STAR:
			case DIVIDE:
			case LESS_OR_EQUAL:
			case GREATER_OR_EQUAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(309);
				arithmeticPart();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LetExpressionContext extends ParserRuleContext {
		public TerminalNode LET() { return getToken(M3ParserGrammar.LET, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode EQUAL() { return getToken(M3ParserGrammar.EQUAL, 0); }
		public CombinedExpressionContext combinedExpression() {
			return getRuleContext(CombinedExpressionContext.class,0);
		}
		public LetExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_letExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterLetExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitLetExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitLetExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LetExpressionContext letExpression() throws RecognitionException {
		LetExpressionContext _localctx = new LetExpressionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_letExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(312);
			match(LET);
			setState(313);
			identifier();
			setState(314);
			match(EQUAL);
			setState(315);
			combinedExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CombinedExpressionContext extends ParserRuleContext {
		public ExpressionOrExpressionGroupContext expressionOrExpressionGroup() {
			return getRuleContext(ExpressionOrExpressionGroupContext.class,0);
		}
		public List<ExpressionPartContext> expressionPart() {
			return getRuleContexts(ExpressionPartContext.class);
		}
		public ExpressionPartContext expressionPart(int i) {
			return getRuleContext(ExpressionPartContext.class,i);
		}
		public CombinedExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_combinedExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterCombinedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitCombinedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitCombinedExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CombinedExpressionContext combinedExpression() throws RecognitionException {
		CombinedExpressionContext _localctx = new CombinedExpressionContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_combinedExpression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(317);
			expressionOrExpressionGroup();
			setState(321);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(318);
					expressionPart();
					}
					} 
				}
				setState(323);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionOrExpressionGroupContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ExpressionOrExpressionGroupContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionOrExpressionGroup; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterExpressionOrExpressionGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitExpressionOrExpressionGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitExpressionOrExpressionGroup(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionOrExpressionGroupContext expressionOrExpressionGroup() throws RecognitionException {
		ExpressionOrExpressionGroupContext _localctx = new ExpressionOrExpressionGroupContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_expressionOrExpressionGroup);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(324);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionsArrayContext extends ParserRuleContext {
		public TerminalNode BRACKET_OPEN() { return getToken(M3ParserGrammar.BRACKET_OPEN, 0); }
		public TerminalNode BRACKET_CLOSE() { return getToken(M3ParserGrammar.BRACKET_CLOSE, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public ExpressionsArrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionsArray; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterExpressionsArray(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitExpressionsArray(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitExpressionsArray(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionsArrayContext expressionsArray() throws RecognitionException {
		ExpressionsArrayContext _localctx = new ExpressionsArrayContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_expressionsArray);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(326);
			match(BRACKET_OPEN);
			setState(335);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE) | (1L << PAREN_OPEN) | (1L << NEW_SYMBOL) | (1L << PIPE) | (1L << INTEGER) | (1L << AT) | (1L << BRACKET_OPEN) | (1L << PATH_SEPARATOR) | (1L << BRACE_OPEN) | (1L << DOLLAR) | (1L << DATE) | (1L << PLUS) | (1L << NOT) | (1L << MINUS) | (1L << FLOAT) | (1L << DECIMAL) | (1L << BOOLEAN) | (1L << NAVIGATION_PATH_BLOCK) | (1L << ISLAND_OPEN))) != 0)) {
				{
				setState(327);
				expression();
				setState(332);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(328);
					match(COMMA);
					setState(329);
					expression();
					}
					}
					setState(334);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(337);
			match(BRACKET_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyOrFunctionExpressionContext extends ParserRuleContext {
		public PropertyExpressionContext propertyExpression() {
			return getRuleContext(PropertyExpressionContext.class,0);
		}
		public FunctionExpressionContext functionExpression() {
			return getRuleContext(FunctionExpressionContext.class,0);
		}
		public PropertyBracketExpressionContext propertyBracketExpression() {
			return getRuleContext(PropertyBracketExpressionContext.class,0);
		}
		public PropertyOrFunctionExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyOrFunctionExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterPropertyOrFunctionExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitPropertyOrFunctionExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitPropertyOrFunctionExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyOrFunctionExpressionContext propertyOrFunctionExpression() throws RecognitionException {
		PropertyOrFunctionExpressionContext _localctx = new PropertyOrFunctionExpressionContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_propertyOrFunctionExpression);
		try {
			setState(342);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(339);
				propertyExpression();
				}
				break;
			case ARROW:
				enterOuterAlt(_localctx, 2);
				{
				setState(340);
				functionExpression();
				}
				break;
			case BRACKET_OPEN:
				enterOuterAlt(_localctx, 3);
				{
				setState(341);
				propertyBracketExpression();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyExpressionContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(M3ParserGrammar.DOT, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public FunctionExpressionLatestMilestoningDateParameterContext functionExpressionLatestMilestoningDateParameter() {
			return getRuleContext(FunctionExpressionLatestMilestoningDateParameterContext.class,0);
		}
		public FunctionExpressionParametersContext functionExpressionParameters() {
			return getRuleContext(FunctionExpressionParametersContext.class,0);
		}
		public PropertyExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterPropertyExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitPropertyExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitPropertyExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyExpressionContext propertyExpression() throws RecognitionException {
		PropertyExpressionContext _localctx = new PropertyExpressionContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_propertyExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(344);
			match(DOT);
			setState(345);
			identifier();
			setState(348);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				{
				setState(346);
				functionExpressionLatestMilestoningDateParameter();
				}
				break;
			case 2:
				{
				setState(347);
				functionExpressionParameters();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyBracketExpressionContext extends ParserRuleContext {
		public TerminalNode BRACKET_OPEN() { return getToken(M3ParserGrammar.BRACKET_OPEN, 0); }
		public TerminalNode BRACKET_CLOSE() { return getToken(M3ParserGrammar.BRACKET_CLOSE, 0); }
		public TerminalNode STRING() { return getToken(M3ParserGrammar.STRING, 0); }
		public TerminalNode INTEGER() { return getToken(M3ParserGrammar.INTEGER, 0); }
		public PropertyBracketExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyBracketExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterPropertyBracketExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitPropertyBracketExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitPropertyBracketExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyBracketExpressionContext propertyBracketExpression() throws RecognitionException {
		PropertyBracketExpressionContext _localctx = new PropertyBracketExpressionContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_propertyBracketExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(350);
			match(BRACKET_OPEN);
			setState(351);
			_la = _input.LA(1);
			if ( !(_la==STRING || _la==INTEGER) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(352);
			match(BRACKET_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionExpressionContext extends ParserRuleContext {
		public List<TerminalNode> ARROW() { return getTokens(M3ParserGrammar.ARROW); }
		public TerminalNode ARROW(int i) {
			return getToken(M3ParserGrammar.ARROW, i);
		}
		public List<QualifiedNameContext> qualifiedName() {
			return getRuleContexts(QualifiedNameContext.class);
		}
		public QualifiedNameContext qualifiedName(int i) {
			return getRuleContext(QualifiedNameContext.class,i);
		}
		public List<FunctionExpressionParametersContext> functionExpressionParameters() {
			return getRuleContexts(FunctionExpressionParametersContext.class);
		}
		public FunctionExpressionParametersContext functionExpressionParameters(int i) {
			return getRuleContext(FunctionExpressionParametersContext.class,i);
		}
		public FunctionExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterFunctionExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitFunctionExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitFunctionExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionExpressionContext functionExpression() throws RecognitionException {
		FunctionExpressionContext _localctx = new FunctionExpressionContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_functionExpression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(354);
			match(ARROW);
			setState(355);
			qualifiedName();
			setState(356);
			functionExpressionParameters();
			setState(363);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(357);
					match(ARROW);
					setState(358);
					qualifiedName();
					setState(359);
					functionExpressionParameters();
					}
					} 
				}
				setState(365);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionExpressionLatestMilestoningDateParameterContext extends ParserRuleContext {
		public TerminalNode PAREN_OPEN() { return getToken(M3ParserGrammar.PAREN_OPEN, 0); }
		public List<TerminalNode> LATEST_DATE() { return getTokens(M3ParserGrammar.LATEST_DATE); }
		public TerminalNode LATEST_DATE(int i) {
			return getToken(M3ParserGrammar.LATEST_DATE, i);
		}
		public TerminalNode PAREN_CLOSE() { return getToken(M3ParserGrammar.PAREN_CLOSE, 0); }
		public TerminalNode COMMA() { return getToken(M3ParserGrammar.COMMA, 0); }
		public FunctionExpressionLatestMilestoningDateParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionExpressionLatestMilestoningDateParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterFunctionExpressionLatestMilestoningDateParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitFunctionExpressionLatestMilestoningDateParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitFunctionExpressionLatestMilestoningDateParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionExpressionLatestMilestoningDateParameterContext functionExpressionLatestMilestoningDateParameter() throws RecognitionException {
		FunctionExpressionLatestMilestoningDateParameterContext _localctx = new FunctionExpressionLatestMilestoningDateParameterContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_functionExpressionLatestMilestoningDateParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(366);
			match(PAREN_OPEN);
			setState(367);
			match(LATEST_DATE);
			setState(370);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(368);
				match(COMMA);
				setState(369);
				match(LATEST_DATE);
				}
			}

			setState(372);
			match(PAREN_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionExpressionParametersContext extends ParserRuleContext {
		public TerminalNode PAREN_OPEN() { return getToken(M3ParserGrammar.PAREN_OPEN, 0); }
		public TerminalNode PAREN_CLOSE() { return getToken(M3ParserGrammar.PAREN_CLOSE, 0); }
		public List<CombinedExpressionContext> combinedExpression() {
			return getRuleContexts(CombinedExpressionContext.class);
		}
		public CombinedExpressionContext combinedExpression(int i) {
			return getRuleContext(CombinedExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public FunctionExpressionParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionExpressionParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterFunctionExpressionParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitFunctionExpressionParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitFunctionExpressionParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionExpressionParametersContext functionExpressionParameters() throws RecognitionException {
		FunctionExpressionParametersContext _localctx = new FunctionExpressionParametersContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_functionExpressionParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(374);
			match(PAREN_OPEN);
			setState(383);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE) | (1L << PAREN_OPEN) | (1L << NEW_SYMBOL) | (1L << PIPE) | (1L << INTEGER) | (1L << AT) | (1L << BRACKET_OPEN) | (1L << PATH_SEPARATOR) | (1L << BRACE_OPEN) | (1L << DOLLAR) | (1L << DATE) | (1L << PLUS) | (1L << NOT) | (1L << MINUS) | (1L << FLOAT) | (1L << DECIMAL) | (1L << BOOLEAN) | (1L << NAVIGATION_PATH_BLOCK) | (1L << ISLAND_OPEN))) != 0)) {
				{
				setState(375);
				combinedExpression();
				setState(380);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(376);
					match(COMMA);
					setState(377);
					combinedExpression();
					}
					}
					setState(382);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(385);
			match(PAREN_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AtomicExpressionContext extends ParserRuleContext {
		public DslContext dsl() {
			return getRuleContext(DslContext.class,0);
		}
		public InstanceLiteralTokenContext instanceLiteralToken() {
			return getRuleContext(InstanceLiteralTokenContext.class,0);
		}
		public ExpressionInstanceContext expressionInstance() {
			return getRuleContext(ExpressionInstanceContext.class,0);
		}
		public UnitInstanceContext unitInstance() {
			return getRuleContext(UnitInstanceContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode AT() { return getToken(M3ParserGrammar.AT, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public LambdaPipeContext lambdaPipe() {
			return getRuleContext(LambdaPipeContext.class,0);
		}
		public LambdaFunctionContext lambdaFunction() {
			return getRuleContext(LambdaFunctionContext.class,0);
		}
		public InstanceReferenceContext instanceReference() {
			return getRuleContext(InstanceReferenceContext.class,0);
		}
		public LambdaParamContext lambdaParam() {
			return getRuleContext(LambdaParamContext.class,0);
		}
		public AtomicExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atomicExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterAtomicExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitAtomicExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitAtomicExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AtomicExpressionContext atomicExpression() throws RecognitionException {
		AtomicExpressionContext _localctx = new AtomicExpressionContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_atomicExpression);
		try {
			setState(400);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(387);
				dsl();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(388);
				instanceLiteralToken();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(389);
				expressionInstance();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(390);
				unitInstance();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(391);
				variable();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				{
				setState(392);
				match(AT);
				setState(393);
				type();
				}
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(394);
				lambdaPipe();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(395);
				lambdaFunction();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(396);
				instanceReference();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				{
				setState(397);
				lambdaParam();
				setState(398);
				lambdaPipe();
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InstanceReferenceContext extends ParserRuleContext {
		public TerminalNode PATH_SEPARATOR() { return getToken(M3ParserGrammar.PATH_SEPARATOR, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public UnitNameContext unitName() {
			return getRuleContext(UnitNameContext.class,0);
		}
		public AllOrFunctionContext allOrFunction() {
			return getRuleContext(AllOrFunctionContext.class,0);
		}
		public InstanceReferenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_instanceReference; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterInstanceReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitInstanceReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitInstanceReference(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InstanceReferenceContext instanceReference() throws RecognitionException {
		InstanceReferenceContext _localctx = new InstanceReferenceContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_instanceReference);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(405);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
			case 1:
				{
				setState(402);
				match(PATH_SEPARATOR);
				}
				break;
			case 2:
				{
				setState(403);
				qualifiedName();
				}
				break;
			case 3:
				{
				setState(404);
				unitName();
				}
				break;
			}
			setState(408);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				{
				setState(407);
				allOrFunction();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LambdaFunctionContext extends ParserRuleContext {
		public TerminalNode BRACE_OPEN() { return getToken(M3ParserGrammar.BRACE_OPEN, 0); }
		public LambdaPipeContext lambdaPipe() {
			return getRuleContext(LambdaPipeContext.class,0);
		}
		public TerminalNode BRACE_CLOSE() { return getToken(M3ParserGrammar.BRACE_CLOSE, 0); }
		public List<LambdaParamContext> lambdaParam() {
			return getRuleContexts(LambdaParamContext.class);
		}
		public LambdaParamContext lambdaParam(int i) {
			return getRuleContext(LambdaParamContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public LambdaFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambdaFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterLambdaFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitLambdaFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitLambdaFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LambdaFunctionContext lambdaFunction() throws RecognitionException {
		LambdaFunctionContext _localctx = new LambdaFunctionContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_lambdaFunction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(410);
			match(BRACE_OPEN);
			setState(419);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE))) != 0)) {
				{
				setState(411);
				lambdaParam();
				setState(416);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(412);
					match(COMMA);
					setState(413);
					lambdaParam();
					}
					}
					setState(418);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(421);
			lambdaPipe();
			setState(422);
			match(BRACE_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableContext extends ParserRuleContext {
		public TerminalNode DOLLAR() { return getToken(M3ParserGrammar.DOLLAR, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(424);
			match(DOLLAR);
			setState(425);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AllOrFunctionContext extends ParserRuleContext {
		public AllFunctionContext allFunction() {
			return getRuleContext(AllFunctionContext.class,0);
		}
		public AllVersionsFunctionContext allVersionsFunction() {
			return getRuleContext(AllVersionsFunctionContext.class,0);
		}
		public AllVersionsInRangeFunctionContext allVersionsInRangeFunction() {
			return getRuleContext(AllVersionsInRangeFunctionContext.class,0);
		}
		public AllFunctionWithMilestoningContext allFunctionWithMilestoning() {
			return getRuleContext(AllFunctionWithMilestoningContext.class,0);
		}
		public FunctionExpressionParametersContext functionExpressionParameters() {
			return getRuleContext(FunctionExpressionParametersContext.class,0);
		}
		public AllOrFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_allOrFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterAllOrFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitAllOrFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitAllOrFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AllOrFunctionContext allOrFunction() throws RecognitionException {
		AllOrFunctionContext _localctx = new AllOrFunctionContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_allOrFunction);
		try {
			setState(432);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(427);
				allFunction();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(428);
				allVersionsFunction();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(429);
				allVersionsInRangeFunction();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(430);
				allFunctionWithMilestoning();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(431);
				functionExpressionParameters();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AllFunctionContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(M3ParserGrammar.DOT, 0); }
		public TerminalNode ALL() { return getToken(M3ParserGrammar.ALL, 0); }
		public TerminalNode PAREN_OPEN() { return getToken(M3ParserGrammar.PAREN_OPEN, 0); }
		public TerminalNode PAREN_CLOSE() { return getToken(M3ParserGrammar.PAREN_CLOSE, 0); }
		public AllFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_allFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterAllFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitAllFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitAllFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AllFunctionContext allFunction() throws RecognitionException {
		AllFunctionContext _localctx = new AllFunctionContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_allFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(434);
			match(DOT);
			setState(435);
			match(ALL);
			setState(436);
			match(PAREN_OPEN);
			setState(437);
			match(PAREN_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AllVersionsFunctionContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(M3ParserGrammar.DOT, 0); }
		public TerminalNode ALL_VERSIONS() { return getToken(M3ParserGrammar.ALL_VERSIONS, 0); }
		public TerminalNode PAREN_OPEN() { return getToken(M3ParserGrammar.PAREN_OPEN, 0); }
		public TerminalNode PAREN_CLOSE() { return getToken(M3ParserGrammar.PAREN_CLOSE, 0); }
		public AllVersionsFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_allVersionsFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterAllVersionsFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitAllVersionsFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitAllVersionsFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AllVersionsFunctionContext allVersionsFunction() throws RecognitionException {
		AllVersionsFunctionContext _localctx = new AllVersionsFunctionContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_allVersionsFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(439);
			match(DOT);
			setState(440);
			match(ALL_VERSIONS);
			setState(441);
			match(PAREN_OPEN);
			setState(442);
			match(PAREN_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AllVersionsInRangeFunctionContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(M3ParserGrammar.DOT, 0); }
		public TerminalNode ALL_VERSIONS_IN_RANGE() { return getToken(M3ParserGrammar.ALL_VERSIONS_IN_RANGE, 0); }
		public TerminalNode PAREN_OPEN() { return getToken(M3ParserGrammar.PAREN_OPEN, 0); }
		public List<BuildMilestoningVariableExpressionContext> buildMilestoningVariableExpression() {
			return getRuleContexts(BuildMilestoningVariableExpressionContext.class);
		}
		public BuildMilestoningVariableExpressionContext buildMilestoningVariableExpression(int i) {
			return getRuleContext(BuildMilestoningVariableExpressionContext.class,i);
		}
		public TerminalNode COMMA() { return getToken(M3ParserGrammar.COMMA, 0); }
		public TerminalNode PAREN_CLOSE() { return getToken(M3ParserGrammar.PAREN_CLOSE, 0); }
		public AllVersionsInRangeFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_allVersionsInRangeFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterAllVersionsInRangeFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitAllVersionsInRangeFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitAllVersionsInRangeFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AllVersionsInRangeFunctionContext allVersionsInRangeFunction() throws RecognitionException {
		AllVersionsInRangeFunctionContext _localctx = new AllVersionsInRangeFunctionContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_allVersionsInRangeFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(444);
			match(DOT);
			setState(445);
			match(ALL_VERSIONS_IN_RANGE);
			setState(446);
			match(PAREN_OPEN);
			setState(447);
			buildMilestoningVariableExpression();
			setState(448);
			match(COMMA);
			setState(449);
			buildMilestoningVariableExpression();
			setState(450);
			match(PAREN_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AllFunctionWithMilestoningContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(M3ParserGrammar.DOT, 0); }
		public TerminalNode ALL() { return getToken(M3ParserGrammar.ALL, 0); }
		public TerminalNode PAREN_OPEN() { return getToken(M3ParserGrammar.PAREN_OPEN, 0); }
		public List<BuildMilestoningVariableExpressionContext> buildMilestoningVariableExpression() {
			return getRuleContexts(BuildMilestoningVariableExpressionContext.class);
		}
		public BuildMilestoningVariableExpressionContext buildMilestoningVariableExpression(int i) {
			return getRuleContext(BuildMilestoningVariableExpressionContext.class,i);
		}
		public TerminalNode PAREN_CLOSE() { return getToken(M3ParserGrammar.PAREN_CLOSE, 0); }
		public TerminalNode COMMA() { return getToken(M3ParserGrammar.COMMA, 0); }
		public AllFunctionWithMilestoningContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_allFunctionWithMilestoning; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterAllFunctionWithMilestoning(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitAllFunctionWithMilestoning(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitAllFunctionWithMilestoning(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AllFunctionWithMilestoningContext allFunctionWithMilestoning() throws RecognitionException {
		AllFunctionWithMilestoningContext _localctx = new AllFunctionWithMilestoningContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_allFunctionWithMilestoning);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(452);
			match(DOT);
			setState(453);
			match(ALL);
			setState(454);
			match(PAREN_OPEN);
			setState(455);
			buildMilestoningVariableExpression();
			setState(458);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(456);
				match(COMMA);
				setState(457);
				buildMilestoningVariableExpression();
				}
			}

			setState(460);
			match(PAREN_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BuildMilestoningVariableExpressionContext extends ParserRuleContext {
		public TerminalNode LATEST_DATE() { return getToken(M3ParserGrammar.LATEST_DATE, 0); }
		public TerminalNode DATE() { return getToken(M3ParserGrammar.DATE, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public BuildMilestoningVariableExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_buildMilestoningVariableExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterBuildMilestoningVariableExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitBuildMilestoningVariableExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitBuildMilestoningVariableExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BuildMilestoningVariableExpressionContext buildMilestoningVariableExpression() throws RecognitionException {
		BuildMilestoningVariableExpressionContext _localctx = new BuildMilestoningVariableExpressionContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_buildMilestoningVariableExpression);
		try {
			setState(465);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LATEST_DATE:
				enterOuterAlt(_localctx, 1);
				{
				setState(462);
				match(LATEST_DATE);
				}
				break;
			case DATE:
				enterOuterAlt(_localctx, 2);
				{
				setState(463);
				match(DATE);
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 3);
				{
				setState(464);
				variable();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionInstanceContext extends ParserRuleContext {
		public TerminalNode NEW_SYMBOL() { return getToken(M3ParserGrammar.NEW_SYMBOL, 0); }
		public TerminalNode PAREN_OPEN() { return getToken(M3ParserGrammar.PAREN_OPEN, 0); }
		public TerminalNode PAREN_CLOSE() { return getToken(M3ParserGrammar.PAREN_CLOSE, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode LESS_THAN() { return getToken(M3ParserGrammar.LESS_THAN, 0); }
		public TerminalNode GREATER_THAN() { return getToken(M3ParserGrammar.GREATER_THAN, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public List<ExpressionInstanceParserPropertyAssignmentContext> expressionInstanceParserPropertyAssignment() {
			return getRuleContexts(ExpressionInstanceParserPropertyAssignmentContext.class);
		}
		public ExpressionInstanceParserPropertyAssignmentContext expressionInstanceParserPropertyAssignment(int i) {
			return getRuleContext(ExpressionInstanceParserPropertyAssignmentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public TypeArgumentsContext typeArguments() {
			return getRuleContext(TypeArgumentsContext.class,0);
		}
		public TerminalNode PIPE() { return getToken(M3ParserGrammar.PIPE, 0); }
		public MultiplicityArgumentsContext multiplicityArguments() {
			return getRuleContext(MultiplicityArgumentsContext.class,0);
		}
		public ExpressionInstanceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionInstance; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterExpressionInstance(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitExpressionInstance(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitExpressionInstance(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionInstanceContext expressionInstance() throws RecognitionException {
		ExpressionInstanceContext _localctx = new ExpressionInstanceContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_expressionInstance);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(467);
			match(NEW_SYMBOL);
			setState(470);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOLLAR:
				{
				setState(468);
				variable();
				}
				break;
			case VALID_STRING:
			case STRING:
			case ALL:
			case LET:
			case ALL_VERSIONS:
			case ALL_VERSIONS_IN_RANGE:
				{
				setState(469);
				qualifiedName();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(481);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LESS_THAN) {
				{
				setState(472);
				match(LESS_THAN);
				setState(474);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE) | (1L << BRACE_OPEN))) != 0)) {
					{
					setState(473);
					typeArguments();
					}
				}

				setState(478);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PIPE) {
					{
					setState(476);
					match(PIPE);
					setState(477);
					multiplicityArguments();
					}
				}

				setState(480);
				match(GREATER_THAN);
				}
			}

			setState(484);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE))) != 0)) {
				{
				setState(483);
				identifier();
				}
			}

			setState(486);
			match(PAREN_OPEN);
			setState(488);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE))) != 0)) {
				{
				setState(487);
				expressionInstanceParserPropertyAssignment();
				}
			}

			setState(494);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(490);
				match(COMMA);
				setState(491);
				expressionInstanceParserPropertyAssignment();
				}
				}
				setState(496);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(497);
			match(PAREN_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionInstanceRightSideContext extends ParserRuleContext {
		public ExpressionInstanceAtomicRightSideContext expressionInstanceAtomicRightSide() {
			return getRuleContext(ExpressionInstanceAtomicRightSideContext.class,0);
		}
		public ExpressionInstanceRightSideContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionInstanceRightSide; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterExpressionInstanceRightSide(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitExpressionInstanceRightSide(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitExpressionInstanceRightSide(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionInstanceRightSideContext expressionInstanceRightSide() throws RecognitionException {
		ExpressionInstanceRightSideContext _localctx = new ExpressionInstanceRightSideContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_expressionInstanceRightSide);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(499);
			expressionInstanceAtomicRightSide();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionInstanceAtomicRightSideContext extends ParserRuleContext {
		public CombinedExpressionContext combinedExpression() {
			return getRuleContext(CombinedExpressionContext.class,0);
		}
		public ExpressionInstanceContext expressionInstance() {
			return getRuleContext(ExpressionInstanceContext.class,0);
		}
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public ExpressionInstanceAtomicRightSideContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionInstanceAtomicRightSide; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterExpressionInstanceAtomicRightSide(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitExpressionInstanceAtomicRightSide(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitExpressionInstanceAtomicRightSide(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionInstanceAtomicRightSideContext expressionInstanceAtomicRightSide() throws RecognitionException {
		ExpressionInstanceAtomicRightSideContext _localctx = new ExpressionInstanceAtomicRightSideContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_expressionInstanceAtomicRightSide);
		try {
			setState(504);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,45,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(501);
				combinedExpression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(502);
				expressionInstance();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(503);
				qualifiedName();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionInstanceParserPropertyAssignmentContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode EQUAL() { return getToken(M3ParserGrammar.EQUAL, 0); }
		public ExpressionInstanceRightSideContext expressionInstanceRightSide() {
			return getRuleContext(ExpressionInstanceRightSideContext.class,0);
		}
		public List<TerminalNode> DOT() { return getTokens(M3ParserGrammar.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(M3ParserGrammar.DOT, i);
		}
		public TerminalNode PLUS() { return getToken(M3ParserGrammar.PLUS, 0); }
		public ExpressionInstanceParserPropertyAssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionInstanceParserPropertyAssignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterExpressionInstanceParserPropertyAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitExpressionInstanceParserPropertyAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitExpressionInstanceParserPropertyAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionInstanceParserPropertyAssignmentContext expressionInstanceParserPropertyAssignment() throws RecognitionException {
		ExpressionInstanceParserPropertyAssignmentContext _localctx = new ExpressionInstanceParserPropertyAssignmentContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_expressionInstanceParserPropertyAssignment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(506);
			identifier();
			setState(511);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(507);
				match(DOT);
				setState(508);
				identifier();
				}
				}
				setState(513);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(515);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PLUS) {
				{
				setState(514);
				match(PLUS);
				}
			}

			setState(517);
			match(EQUAL);
			setState(518);
			expressionInstanceRightSide();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SliceExpressionContext extends ParserRuleContext {
		public TerminalNode BRACKET_OPEN() { return getToken(M3ParserGrammar.BRACKET_OPEN, 0); }
		public TerminalNode BRACKET_CLOSE() { return getToken(M3ParserGrammar.BRACKET_CLOSE, 0); }
		public List<TerminalNode> COLON() { return getTokens(M3ParserGrammar.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(M3ParserGrammar.COLON, i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public SliceExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sliceExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterSliceExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitSliceExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitSliceExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SliceExpressionContext sliceExpression() throws RecognitionException {
		SliceExpressionContext _localctx = new SliceExpressionContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_sliceExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(520);
			match(BRACKET_OPEN);
			setState(533);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
			case 1:
				{
				{
				setState(521);
				match(COLON);
				setState(522);
				expression();
				}
				}
				break;
			case 2:
				{
				{
				setState(523);
				expression();
				setState(524);
				match(COLON);
				setState(525);
				expression();
				}
				}
				break;
			case 3:
				{
				{
				setState(527);
				expression();
				setState(528);
				match(COLON);
				setState(529);
				expression();
				setState(530);
				match(COLON);
				setState(531);
				expression();
				}
				}
				break;
			}
			setState(535);
			match(BRACKET_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NotExpressionContext extends ParserRuleContext {
		public TerminalNode NOT() { return getToken(M3ParserGrammar.NOT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public NotExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_notExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterNotExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitNotExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitNotExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NotExpressionContext notExpression() throws RecognitionException {
		NotExpressionContext _localctx = new NotExpressionContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_notExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(537);
			match(NOT);
			setState(538);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SignedExpressionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(M3ParserGrammar.MINUS, 0); }
		public TerminalNode PLUS() { return getToken(M3ParserGrammar.PLUS, 0); }
		public SignedExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_signedExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterSignedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitSignedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitSignedExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SignedExpressionContext signedExpression() throws RecognitionException {
		SignedExpressionContext _localctx = new SignedExpressionContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_signedExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(540);
			_la = _input.LA(1);
			if ( !(_la==PLUS || _la==MINUS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(541);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LambdaPipeContext extends ParserRuleContext {
		public TerminalNode PIPE() { return getToken(M3ParserGrammar.PIPE, 0); }
		public CodeBlockContext codeBlock() {
			return getRuleContext(CodeBlockContext.class,0);
		}
		public LambdaPipeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambdaPipe; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterLambdaPipe(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitLambdaPipe(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitLambdaPipe(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LambdaPipeContext lambdaPipe() throws RecognitionException {
		LambdaPipeContext _localctx = new LambdaPipeContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_lambdaPipe);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(543);
			match(PIPE);
			setState(544);
			codeBlock();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LambdaParamContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public LambdaParamTypeContext lambdaParamType() {
			return getRuleContext(LambdaParamTypeContext.class,0);
		}
		public LambdaParamContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambdaParam; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterLambdaParam(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitLambdaParam(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitLambdaParam(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LambdaParamContext lambdaParam() throws RecognitionException {
		LambdaParamContext _localctx = new LambdaParamContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_lambdaParam);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(546);
			identifier();
			setState(548);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(547);
				lambdaParamType();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LambdaParamTypeContext extends ParserRuleContext {
		public TerminalNode COLON() { return getToken(M3ParserGrammar.COLON, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public MultiplicityContext multiplicity() {
			return getRuleContext(MultiplicityContext.class,0);
		}
		public LambdaParamTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambdaParamType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterLambdaParamType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitLambdaParamType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitLambdaParamType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LambdaParamTypeContext lambdaParamType() throws RecognitionException {
		LambdaParamTypeContext _localctx = new LambdaParamTypeContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_lambdaParamType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(550);
			match(COLON);
			setState(551);
			type();
			setState(552);
			multiplicity();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InstanceLiteralContext extends ParserRuleContext {
		public InstanceLiteralTokenContext instanceLiteralToken() {
			return getRuleContext(InstanceLiteralTokenContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(M3ParserGrammar.MINUS, 0); }
		public TerminalNode INTEGER() { return getToken(M3ParserGrammar.INTEGER, 0); }
		public TerminalNode FLOAT() { return getToken(M3ParserGrammar.FLOAT, 0); }
		public TerminalNode DECIMAL() { return getToken(M3ParserGrammar.DECIMAL, 0); }
		public TerminalNode PLUS() { return getToken(M3ParserGrammar.PLUS, 0); }
		public InstanceLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_instanceLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterInstanceLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitInstanceLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitInstanceLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InstanceLiteralContext instanceLiteral() throws RecognitionException {
		InstanceLiteralContext _localctx = new InstanceLiteralContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_instanceLiteral);
		try {
			setState(567);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,50,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(554);
				instanceLiteralToken();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(555);
				match(MINUS);
				setState(556);
				match(INTEGER);
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				{
				setState(557);
				match(MINUS);
				setState(558);
				match(FLOAT);
				}
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				{
				setState(559);
				match(MINUS);
				setState(560);
				match(DECIMAL);
				}
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				{
				setState(561);
				match(PLUS);
				setState(562);
				match(INTEGER);
				}
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				{
				setState(563);
				match(PLUS);
				setState(564);
				match(FLOAT);
				}
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				{
				setState(565);
				match(PLUS);
				setState(566);
				match(DECIMAL);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InstanceLiteralTokenContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(M3ParserGrammar.STRING, 0); }
		public TerminalNode INTEGER() { return getToken(M3ParserGrammar.INTEGER, 0); }
		public TerminalNode FLOAT() { return getToken(M3ParserGrammar.FLOAT, 0); }
		public TerminalNode DECIMAL() { return getToken(M3ParserGrammar.DECIMAL, 0); }
		public TerminalNode DATE() { return getToken(M3ParserGrammar.DATE, 0); }
		public TerminalNode BOOLEAN() { return getToken(M3ParserGrammar.BOOLEAN, 0); }
		public InstanceLiteralTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_instanceLiteralToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterInstanceLiteralToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitInstanceLiteralToken(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitInstanceLiteralToken(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InstanceLiteralTokenContext instanceLiteralToken() throws RecognitionException {
		InstanceLiteralTokenContext _localctx = new InstanceLiteralTokenContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_instanceLiteralToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(569);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STRING) | (1L << INTEGER) | (1L << DATE) | (1L << FLOAT) | (1L << DECIMAL) | (1L << BOOLEAN))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UnitInstanceLiteralContext extends ParserRuleContext {
		public TerminalNode INTEGER() { return getToken(M3ParserGrammar.INTEGER, 0); }
		public TerminalNode MINUS() { return getToken(M3ParserGrammar.MINUS, 0); }
		public TerminalNode FLOAT() { return getToken(M3ParserGrammar.FLOAT, 0); }
		public TerminalNode DECIMAL() { return getToken(M3ParserGrammar.DECIMAL, 0); }
		public TerminalNode PLUS() { return getToken(M3ParserGrammar.PLUS, 0); }
		public UnitInstanceLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unitInstanceLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterUnitInstanceLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitUnitInstanceLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitUnitInstanceLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnitInstanceLiteralContext unitInstanceLiteral() throws RecognitionException {
		UnitInstanceLiteralContext _localctx = new UnitInstanceLiteralContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_unitInstanceLiteral);
		int _la;
		try {
			setState(589);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,54,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(572);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(571);
					match(MINUS);
					}
				}

				setState(574);
				match(INTEGER);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(576);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(575);
					match(MINUS);
					}
				}

				setState(578);
				match(FLOAT);
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				{
				setState(580);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(579);
					match(MINUS);
					}
				}

				setState(582);
				match(DECIMAL);
				}
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				{
				setState(583);
				match(PLUS);
				setState(584);
				match(INTEGER);
				}
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				{
				setState(585);
				match(PLUS);
				setState(586);
				match(FLOAT);
				}
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				{
				setState(587);
				match(PLUS);
				setState(588);
				match(DECIMAL);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArithmeticPartContext extends ParserRuleContext {
		public List<TerminalNode> PLUS() { return getTokens(M3ParserGrammar.PLUS); }
		public TerminalNode PLUS(int i) {
			return getToken(M3ParserGrammar.PLUS, i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> STAR() { return getTokens(M3ParserGrammar.STAR); }
		public TerminalNode STAR(int i) {
			return getToken(M3ParserGrammar.STAR, i);
		}
		public List<TerminalNode> MINUS() { return getTokens(M3ParserGrammar.MINUS); }
		public TerminalNode MINUS(int i) {
			return getToken(M3ParserGrammar.MINUS, i);
		}
		public List<TerminalNode> DIVIDE() { return getTokens(M3ParserGrammar.DIVIDE); }
		public TerminalNode DIVIDE(int i) {
			return getToken(M3ParserGrammar.DIVIDE, i);
		}
		public TerminalNode LESS_THAN() { return getToken(M3ParserGrammar.LESS_THAN, 0); }
		public TerminalNode LESS_OR_EQUAL() { return getToken(M3ParserGrammar.LESS_OR_EQUAL, 0); }
		public TerminalNode GREATER_THAN() { return getToken(M3ParserGrammar.GREATER_THAN, 0); }
		public TerminalNode GREATER_OR_EQUAL() { return getToken(M3ParserGrammar.GREATER_OR_EQUAL, 0); }
		public ArithmeticPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmeticPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterArithmeticPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitArithmeticPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitArithmeticPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArithmeticPartContext arithmeticPart() throws RecognitionException {
		ArithmeticPartContext _localctx = new ArithmeticPartContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_arithmeticPart);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(635);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PLUS:
				{
				setState(591);
				match(PLUS);
				setState(592);
				expression();
				setState(597);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,55,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(593);
						match(PLUS);
						setState(594);
						expression();
						}
						} 
					}
					setState(599);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,55,_ctx);
				}
				}
				break;
			case STAR:
				{
				{
				setState(600);
				match(STAR);
				setState(601);
				expression();
				setState(606);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,56,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(602);
						match(STAR);
						setState(603);
						expression();
						}
						} 
					}
					setState(608);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,56,_ctx);
				}
				}
				}
				break;
			case MINUS:
				{
				{
				setState(609);
				match(MINUS);
				setState(610);
				expression();
				setState(615);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,57,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(611);
						match(MINUS);
						setState(612);
						expression();
						}
						} 
					}
					setState(617);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,57,_ctx);
				}
				}
				}
				break;
			case DIVIDE:
				{
				{
				setState(618);
				match(DIVIDE);
				setState(619);
				expression();
				setState(624);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,58,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(620);
						match(DIVIDE);
						setState(621);
						expression();
						}
						} 
					}
					setState(626);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,58,_ctx);
				}
				}
				}
				break;
			case LESS_THAN:
				{
				{
				setState(627);
				match(LESS_THAN);
				setState(628);
				expression();
				}
				}
				break;
			case LESS_OR_EQUAL:
				{
				{
				setState(629);
				match(LESS_OR_EQUAL);
				setState(630);
				expression();
				}
				}
				break;
			case GREATER_THAN:
				{
				{
				setState(631);
				match(GREATER_THAN);
				setState(632);
				expression();
				}
				}
				break;
			case GREATER_OR_EQUAL:
				{
				{
				setState(633);
				match(GREATER_OR_EQUAL);
				setState(634);
				expression();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BooleanPartContext extends ParserRuleContext {
		public TerminalNode AND() { return getToken(M3ParserGrammar.AND, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode OR() { return getToken(M3ParserGrammar.OR, 0); }
		public EqualNotEqualContext equalNotEqual() {
			return getRuleContext(EqualNotEqualContext.class,0);
		}
		public BooleanPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterBooleanPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitBooleanPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitBooleanPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanPartContext booleanPart() throws RecognitionException {
		BooleanPartContext _localctx = new BooleanPartContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_booleanPart);
		try {
			setState(642);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AND:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(637);
				match(AND);
				setState(638);
				expression();
				}
				}
				break;
			case OR:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(639);
				match(OR);
				setState(640);
				expression();
				}
				}
				break;
			case TEST_EQUAL:
			case TEST_NOT_EQUAL:
				enterOuterAlt(_localctx, 3);
				{
				setState(641);
				equalNotEqual();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionVariableExpressionContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(M3ParserGrammar.COLON, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public MultiplicityContext multiplicity() {
			return getRuleContext(MultiplicityContext.class,0);
		}
		public FunctionVariableExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionVariableExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterFunctionVariableExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitFunctionVariableExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitFunctionVariableExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionVariableExpressionContext functionVariableExpression() throws RecognitionException {
		FunctionVariableExpressionContext _localctx = new FunctionVariableExpressionContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_functionVariableExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(644);
			identifier();
			setState(645);
			match(COLON);
			setState(646);
			type();
			setState(647);
			multiplicity();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DslContext extends ParserRuleContext {
		public DslGraphFetchContext dslGraphFetch() {
			return getRuleContext(DslGraphFetchContext.class,0);
		}
		public DslNavigationPathContext dslNavigationPath() {
			return getRuleContext(DslNavigationPathContext.class,0);
		}
		public DslContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dsl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterDsl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitDsl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitDsl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DslContext dsl() throws RecognitionException {
		DslContext _localctx = new DslContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_dsl);
		try {
			setState(651);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ISLAND_OPEN:
				enterOuterAlt(_localctx, 1);
				{
				setState(649);
				dslGraphFetch();
				}
				break;
			case NAVIGATION_PATH_BLOCK:
				enterOuterAlt(_localctx, 2);
				{
				setState(650);
				dslNavigationPath();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DslNavigationPathContext extends ParserRuleContext {
		public TerminalNode NAVIGATION_PATH_BLOCK() { return getToken(M3ParserGrammar.NAVIGATION_PATH_BLOCK, 0); }
		public DslNavigationPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dslNavigationPath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterDslNavigationPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitDslNavigationPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitDslNavigationPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DslNavigationPathContext dslNavigationPath() throws RecognitionException {
		DslNavigationPathContext _localctx = new DslNavigationPathContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_dslNavigationPath);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(653);
			match(NAVIGATION_PATH_BLOCK);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DslGraphFetchContext extends ParserRuleContext {
		public TerminalNode ISLAND_OPEN() { return getToken(M3ParserGrammar.ISLAND_OPEN, 0); }
		public List<DslContentContext> dslContent() {
			return getRuleContexts(DslContentContext.class);
		}
		public DslContentContext dslContent(int i) {
			return getRuleContext(DslContentContext.class,i);
		}
		public DslGraphFetchContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dslGraphFetch; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterDslGraphFetch(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitDslGraphFetch(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitDslGraphFetch(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DslGraphFetchContext dslGraphFetch() throws RecognitionException {
		DslGraphFetchContext _localctx = new DslGraphFetchContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_dslGraphFetch);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(655);
			match(ISLAND_OPEN);
			setState(659);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ISLAND_START) | (1L << ISLAND_BRACE_OPEN) | (1L << ISLAND_CONTENT) | (1L << ISLAND_HASH) | (1L << ISLAND_BRACE_CLOSE) | (1L << ISLAND_END))) != 0)) {
				{
				{
				setState(656);
				dslContent();
				}
				}
				setState(661);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DslContentContext extends ParserRuleContext {
		public TerminalNode ISLAND_START() { return getToken(M3ParserGrammar.ISLAND_START, 0); }
		public TerminalNode ISLAND_BRACE_OPEN() { return getToken(M3ParserGrammar.ISLAND_BRACE_OPEN, 0); }
		public TerminalNode ISLAND_CONTENT() { return getToken(M3ParserGrammar.ISLAND_CONTENT, 0); }
		public TerminalNode ISLAND_HASH() { return getToken(M3ParserGrammar.ISLAND_HASH, 0); }
		public TerminalNode ISLAND_BRACE_CLOSE() { return getToken(M3ParserGrammar.ISLAND_BRACE_CLOSE, 0); }
		public TerminalNode ISLAND_END() { return getToken(M3ParserGrammar.ISLAND_END, 0); }
		public DslContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dslContent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterDslContent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitDslContent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitDslContent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DslContentContext dslContent() throws RecognitionException {
		DslContentContext _localctx = new DslContentContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_dslContent);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(662);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ISLAND_START) | (1L << ISLAND_BRACE_OPEN) | (1L << ISLAND_CONTENT) | (1L << ISLAND_HASH) | (1L << ISLAND_BRACE_CLOSE) | (1L << ISLAND_END))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode LESS_THAN() { return getToken(M3ParserGrammar.LESS_THAN, 0); }
		public TerminalNode GREATER_THAN() { return getToken(M3ParserGrammar.GREATER_THAN, 0); }
		public TypeArgumentsContext typeArguments() {
			return getRuleContext(TypeArgumentsContext.class,0);
		}
		public TerminalNode PIPE() { return getToken(M3ParserGrammar.PIPE, 0); }
		public MultiplicityArgumentsContext multiplicityArguments() {
			return getRuleContext(MultiplicityArgumentsContext.class,0);
		}
		public TerminalNode BRACE_OPEN() { return getToken(M3ParserGrammar.BRACE_OPEN, 0); }
		public TerminalNode ARROW() { return getToken(M3ParserGrammar.ARROW, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public MultiplicityContext multiplicity() {
			return getRuleContext(MultiplicityContext.class,0);
		}
		public TerminalNode BRACE_CLOSE() { return getToken(M3ParserGrammar.BRACE_CLOSE, 0); }
		public List<FunctionTypePureTypeContext> functionTypePureType() {
			return getRuleContexts(FunctionTypePureTypeContext.class);
		}
		public FunctionTypePureTypeContext functionTypePureType(int i) {
			return getRuleContext(FunctionTypePureTypeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public UnitNameContext unitName() {
			return getRuleContext(UnitNameContext.class,0);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_type);
		int _la;
		try {
			setState(693);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,68,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(664);
				qualifiedName();
				setState(674);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,65,_ctx) ) {
				case 1:
					{
					setState(665);
					match(LESS_THAN);
					setState(667);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE) | (1L << BRACE_OPEN))) != 0)) {
						{
						setState(666);
						typeArguments();
						}
					}

					setState(671);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==PIPE) {
						{
						setState(669);
						match(PIPE);
						setState(670);
						multiplicityArguments();
						}
					}

					setState(673);
					match(GREATER_THAN);
					}
					break;
				}
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(676);
				match(BRACE_OPEN);
				setState(678);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALID_STRING) | (1L << STRING) | (1L << ALL) | (1L << LET) | (1L << ALL_VERSIONS) | (1L << ALL_VERSIONS_IN_RANGE) | (1L << BRACE_OPEN))) != 0)) {
					{
					setState(677);
					functionTypePureType();
					}
				}

				setState(684);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(680);
					match(COMMA);
					setState(681);
					functionTypePureType();
					}
					}
					setState(686);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(687);
				match(ARROW);
				setState(688);
				type();
				setState(689);
				multiplicity();
				setState(690);
				match(BRACE_CLOSE);
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(692);
				unitName();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultiplicityContext extends ParserRuleContext {
		public TerminalNode BRACKET_OPEN() { return getToken(M3ParserGrammar.BRACKET_OPEN, 0); }
		public MultiplicityArgumentContext multiplicityArgument() {
			return getRuleContext(MultiplicityArgumentContext.class,0);
		}
		public TerminalNode BRACKET_CLOSE() { return getToken(M3ParserGrammar.BRACKET_CLOSE, 0); }
		public MultiplicityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiplicity; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterMultiplicity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitMultiplicity(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitMultiplicity(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiplicityContext multiplicity() throws RecognitionException {
		MultiplicityContext _localctx = new MultiplicityContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_multiplicity);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(695);
			match(BRACKET_OPEN);
			setState(696);
			multiplicityArgument();
			setState(697);
			match(BRACKET_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FromMultiplicityContext extends ParserRuleContext {
		public TerminalNode INTEGER() { return getToken(M3ParserGrammar.INTEGER, 0); }
		public FromMultiplicityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromMultiplicity; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterFromMultiplicity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitFromMultiplicity(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitFromMultiplicity(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromMultiplicityContext fromMultiplicity() throws RecognitionException {
		FromMultiplicityContext _localctx = new FromMultiplicityContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_fromMultiplicity);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(699);
			match(INTEGER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ToMultiplicityContext extends ParserRuleContext {
		public TerminalNode INTEGER() { return getToken(M3ParserGrammar.INTEGER, 0); }
		public TerminalNode STAR() { return getToken(M3ParserGrammar.STAR, 0); }
		public ToMultiplicityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_toMultiplicity; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterToMultiplicity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitToMultiplicity(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitToMultiplicity(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ToMultiplicityContext toMultiplicity() throws RecognitionException {
		ToMultiplicityContext _localctx = new ToMultiplicityContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_toMultiplicity);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(701);
			_la = _input.LA(1);
			if ( !(_la==INTEGER || _la==STAR) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionTypePureTypeContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public MultiplicityContext multiplicity() {
			return getRuleContext(MultiplicityContext.class,0);
		}
		public FunctionTypePureTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionTypePureType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterFunctionTypePureType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitFunctionTypePureType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitFunctionTypePureType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionTypePureTypeContext functionTypePureType() throws RecognitionException {
		FunctionTypePureTypeContext _localctx = new FunctionTypePureTypeContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_functionTypePureType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(703);
			type();
			setState(704);
			multiplicity();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeAndMultiplicityParametersContext extends ParserRuleContext {
		public TerminalNode LESS_THAN() { return getToken(M3ParserGrammar.LESS_THAN, 0); }
		public TerminalNode GREATER_THAN() { return getToken(M3ParserGrammar.GREATER_THAN, 0); }
		public MultiplictyParametersContext multiplictyParameters() {
			return getRuleContext(MultiplictyParametersContext.class,0);
		}
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public TypeAndMultiplicityParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeAndMultiplicityParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterTypeAndMultiplicityParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitTypeAndMultiplicityParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitTypeAndMultiplicityParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeAndMultiplicityParametersContext typeAndMultiplicityParameters() throws RecognitionException {
		TypeAndMultiplicityParametersContext _localctx = new TypeAndMultiplicityParametersContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_typeAndMultiplicityParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(706);
			match(LESS_THAN);
			setState(712);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VALID_STRING:
			case STRING:
			case ALL:
			case LET:
			case ALL_VERSIONS:
			case ALL_VERSIONS_IN_RANGE:
				{
				{
				setState(707);
				typeParameters();
				setState(709);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PIPE) {
					{
					setState(708);
					multiplictyParameters();
					}
				}

				}
				}
				break;
			case PIPE:
				{
				setState(711);
				multiplictyParameters();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(714);
			match(GREATER_THAN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeParametersWithContravarianceAndMultiplicityParametersContext extends ParserRuleContext {
		public TerminalNode LESS_THAN() { return getToken(M3ParserGrammar.LESS_THAN, 0); }
		public TerminalNode GREATER_THAN() { return getToken(M3ParserGrammar.GREATER_THAN, 0); }
		public MultiplictyParametersContext multiplictyParameters() {
			return getRuleContext(MultiplictyParametersContext.class,0);
		}
		public ContravarianceTypeParametersContext contravarianceTypeParameters() {
			return getRuleContext(ContravarianceTypeParametersContext.class,0);
		}
		public TypeParametersWithContravarianceAndMultiplicityParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParametersWithContravarianceAndMultiplicityParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterTypeParametersWithContravarianceAndMultiplicityParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitTypeParametersWithContravarianceAndMultiplicityParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitTypeParametersWithContravarianceAndMultiplicityParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParametersWithContravarianceAndMultiplicityParametersContext typeParametersWithContravarianceAndMultiplicityParameters() throws RecognitionException {
		TypeParametersWithContravarianceAndMultiplicityParametersContext _localctx = new TypeParametersWithContravarianceAndMultiplicityParametersContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_typeParametersWithContravarianceAndMultiplicityParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(716);
			match(LESS_THAN);
			setState(722);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VALID_STRING:
			case STRING:
			case ALL:
			case LET:
			case ALL_VERSIONS:
			case ALL_VERSIONS_IN_RANGE:
			case MINUS:
				{
				{
				setState(717);
				contravarianceTypeParameters();
				setState(719);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PIPE) {
					{
					setState(718);
					multiplictyParameters();
					}
				}

				}
				}
				break;
			case PIPE:
				{
				setState(721);
				multiplictyParameters();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(724);
			match(GREATER_THAN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeParametersContext extends ParserRuleContext {
		public List<TypeParameterContext> typeParameter() {
			return getRuleContexts(TypeParameterContext.class);
		}
		public TypeParameterContext typeParameter(int i) {
			return getRuleContext(TypeParameterContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public TypeParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterTypeParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitTypeParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitTypeParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParametersContext typeParameters() throws RecognitionException {
		TypeParametersContext _localctx = new TypeParametersContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_typeParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(726);
			typeParameter();
			setState(731);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(727);
				match(COMMA);
				setState(728);
				typeParameter();
				}
				}
				setState(733);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeParameterContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TypeParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterTypeParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitTypeParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitTypeParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParameterContext typeParameter() throws RecognitionException {
		TypeParameterContext _localctx = new TypeParameterContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_typeParameter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(734);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ContravarianceTypeParametersContext extends ParserRuleContext {
		public List<ContravarianceTypeParameterContext> contravarianceTypeParameter() {
			return getRuleContexts(ContravarianceTypeParameterContext.class);
		}
		public ContravarianceTypeParameterContext contravarianceTypeParameter(int i) {
			return getRuleContext(ContravarianceTypeParameterContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public ContravarianceTypeParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_contravarianceTypeParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterContravarianceTypeParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitContravarianceTypeParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitContravarianceTypeParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ContravarianceTypeParametersContext contravarianceTypeParameters() throws RecognitionException {
		ContravarianceTypeParametersContext _localctx = new ContravarianceTypeParametersContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_contravarianceTypeParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(736);
			contravarianceTypeParameter();
			setState(741);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(737);
				match(COMMA);
				setState(738);
				contravarianceTypeParameter();
				}
				}
				setState(743);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ContravarianceTypeParameterContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(M3ParserGrammar.MINUS, 0); }
		public ContravarianceTypeParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_contravarianceTypeParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterContravarianceTypeParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitContravarianceTypeParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitContravarianceTypeParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ContravarianceTypeParameterContext contravarianceTypeParameter() throws RecognitionException {
		ContravarianceTypeParameterContext _localctx = new ContravarianceTypeParameterContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_contravarianceTypeParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(745);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(744);
				match(MINUS);
				}
			}

			setState(747);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultiplicityArgumentsContext extends ParserRuleContext {
		public List<MultiplicityArgumentContext> multiplicityArgument() {
			return getRuleContexts(MultiplicityArgumentContext.class);
		}
		public MultiplicityArgumentContext multiplicityArgument(int i) {
			return getRuleContext(MultiplicityArgumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public MultiplicityArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiplicityArguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterMultiplicityArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitMultiplicityArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitMultiplicityArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiplicityArgumentsContext multiplicityArguments() throws RecognitionException {
		MultiplicityArgumentsContext _localctx = new MultiplicityArgumentsContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_multiplicityArguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(749);
			multiplicityArgument();
			setState(754);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(750);
				match(COMMA);
				setState(751);
				multiplicityArgument();
				}
				}
				setState(756);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultiplicityArgumentContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ToMultiplicityContext toMultiplicity() {
			return getRuleContext(ToMultiplicityContext.class,0);
		}
		public FromMultiplicityContext fromMultiplicity() {
			return getRuleContext(FromMultiplicityContext.class,0);
		}
		public TerminalNode DOT_DOT() { return getToken(M3ParserGrammar.DOT_DOT, 0); }
		public MultiplicityArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiplicityArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterMultiplicityArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitMultiplicityArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitMultiplicityArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiplicityArgumentContext multiplicityArgument() throws RecognitionException {
		MultiplicityArgumentContext _localctx = new MultiplicityArgumentContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_multiplicityArgument);
		try {
			setState(764);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VALID_STRING:
			case STRING:
			case ALL:
			case LET:
			case ALL_VERSIONS:
			case ALL_VERSIONS_IN_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(757);
				identifier();
				}
				break;
			case INTEGER:
			case STAR:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(761);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,77,_ctx) ) {
				case 1:
					{
					setState(758);
					fromMultiplicity();
					setState(759);
					match(DOT_DOT);
					}
					break;
				}
				setState(763);
				toMultiplicity();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeArgumentsContext extends ParserRuleContext {
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public TypeArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeArguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterTypeArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitTypeArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitTypeArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeArgumentsContext typeArguments() throws RecognitionException {
		TypeArgumentsContext _localctx = new TypeArgumentsContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_typeArguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(766);
			type();
			setState(771);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(767);
				match(COMMA);
				setState(768);
				type();
				}
				}
				setState(773);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultiplictyParametersContext extends ParserRuleContext {
		public TerminalNode PIPE() { return getToken(M3ParserGrammar.PIPE, 0); }
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(M3ParserGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(M3ParserGrammar.COMMA, i);
		}
		public MultiplictyParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiplictyParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterMultiplictyParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitMultiplictyParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitMultiplictyParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiplictyParametersContext multiplictyParameters() throws RecognitionException {
		MultiplictyParametersContext _localctx = new MultiplictyParametersContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_multiplictyParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(774);
			match(PIPE);
			setState(775);
			identifier();
			setState(780);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(776);
				match(COMMA);
				setState(777);
				identifier();
				}
				}
				setState(782);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QualifiedNameContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public PackagePathContext packagePath() {
			return getRuleContext(PackagePathContext.class,0);
		}
		public TerminalNode PATH_SEPARATOR() { return getToken(M3ParserGrammar.PATH_SEPARATOR, 0); }
		public QualifiedNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterQualifiedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitQualifiedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitQualifiedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameContext qualifiedName() throws RecognitionException {
		QualifiedNameContext _localctx = new QualifiedNameContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_qualifiedName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(786);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
			case 1:
				{
				setState(783);
				packagePath();
				setState(784);
				match(PATH_SEPARATOR);
				}
				break;
			}
			setState(788);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PackagePathContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> PATH_SEPARATOR() { return getTokens(M3ParserGrammar.PATH_SEPARATOR); }
		public TerminalNode PATH_SEPARATOR(int i) {
			return getToken(M3ParserGrammar.PATH_SEPARATOR, i);
		}
		public PackagePathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_packagePath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterPackagePath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitPackagePath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitPackagePath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PackagePathContext packagePath() throws RecognitionException {
		PackagePathContext _localctx = new PackagePathContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_packagePath);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(790);
			identifier();
			setState(795);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,82,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(791);
					match(PATH_SEPARATOR);
					setState(792);
					identifier();
					}
					} 
				}
				setState(797);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,82,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WordContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode BOOLEAN() { return getToken(M3ParserGrammar.BOOLEAN, 0); }
		public TerminalNode INTEGER() { return getToken(M3ParserGrammar.INTEGER, 0); }
		public WordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_word; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).enterWord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof M3ParserGrammarListener ) ((M3ParserGrammarListener)listener).exitWord(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof M3ParserGrammarVisitor ) return ((M3ParserGrammarVisitor<? extends T>)visitor).visitWord(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WordContext word() throws RecognitionException {
		WordContext _localctx = new WordContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_word);
		try {
			setState(801);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VALID_STRING:
			case STRING:
			case ALL:
			case LET:
			case ALL_VERSIONS:
			case ALL_VERSIONS_IN_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(798);
				identifier();
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 2);
				{
				setState(799);
				match(BOOLEAN);
				}
				break;
			case INTEGER:
				enterOuterAlt(_localctx, 3);
				{
				setState(800);
				match(INTEGER);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\39\u0326\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\3\2\3\2\3\3\3\3\3\3\3\3\3\3\5\3\u00a4"+
		"\n\3\3\3\7\3\u00a7\n\3\f\3\16\3\u00aa\13\3\3\3\5\3\u00ad\n\3\3\3\3\3\3"+
		"\3\3\3\5\3\u00b3\n\3\3\4\3\4\3\4\3\4\5\4\u00b9\n\4\3\4\3\4\5\4\u00bd\n"+
		"\4\3\4\5\4\u00c0\n\4\3\4\5\4\u00c3\n\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4"+
		"\3\4\3\4\3\4\3\4\3\4\3\4\5\4\u00d3\n\4\3\4\3\4\5\4\u00d7\n\4\3\4\3\4\3"+
		"\4\3\4\7\4\u00dd\n\4\f\4\16\4\u00e0\13\4\5\4\u00e2\n\4\3\4\3\4\3\5\3\5"+
		"\3\5\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3\b\5\b\u00f3\n\b\3\t\3\t\3\n"+
		"\3\n\3\n\3\n\7\n\u00fb\n\n\f\n\16\n\u00fe\13\n\5\n\u0100\n\n\3\n\3\n\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u010c\n\13\3\f\3\f\3\f\3\f"+
		"\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\20\3\20\3\20\3\20"+
		"\3\20\7\20\u0122\n\20\f\20\16\20\u0125\13\20\5\20\u0127\n\20\3\21\3\21"+
		"\5\21\u012b\n\21\3\22\3\22\3\22\3\23\3\23\7\23\u0132\n\23\f\23\16\23\u0135"+
		"\13\23\3\24\3\24\5\24\u0139\n\24\3\25\3\25\3\25\3\25\3\25\3\26\3\26\7"+
		"\26\u0142\n\26\f\26\16\26\u0145\13\26\3\27\3\27\3\30\3\30\3\30\3\30\7"+
		"\30\u014d\n\30\f\30\16\30\u0150\13\30\5\30\u0152\n\30\3\30\3\30\3\31\3"+
		"\31\3\31\5\31\u0159\n\31\3\32\3\32\3\32\3\32\5\32\u015f\n\32\3\33\3\33"+
		"\3\33\3\33\3\34\3\34\3\34\3\34\3\34\3\34\3\34\7\34\u016c\n\34\f\34\16"+
		"\34\u016f\13\34\3\35\3\35\3\35\3\35\5\35\u0175\n\35\3\35\3\35\3\36\3\36"+
		"\3\36\3\36\7\36\u017d\n\36\f\36\16\36\u0180\13\36\5\36\u0182\n\36\3\36"+
		"\3\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\5\37\u0193\n\37\3 \3 \3 \5 \u0198\n \3 \5 \u019b\n \3!\3!\3!\3!\7!\u01a1"+
		"\n!\f!\16!\u01a4\13!\5!\u01a6\n!\3!\3!\3!\3\"\3\"\3\"\3#\3#\3#\3#\3#\5"+
		"#\u01b3\n#\3$\3$\3$\3$\3$\3%\3%\3%\3%\3%\3&\3&\3&\3&\3&\3&\3&\3&\3\'\3"+
		"\'\3\'\3\'\3\'\3\'\5\'\u01cd\n\'\3\'\3\'\3(\3(\3(\5(\u01d4\n(\3)\3)\3"+
		")\5)\u01d9\n)\3)\3)\5)\u01dd\n)\3)\3)\5)\u01e1\n)\3)\5)\u01e4\n)\3)\5"+
		")\u01e7\n)\3)\3)\5)\u01eb\n)\3)\3)\7)\u01ef\n)\f)\16)\u01f2\13)\3)\3)"+
		"\3*\3*\3+\3+\3+\5+\u01fb\n+\3,\3,\3,\7,\u0200\n,\f,\16,\u0203\13,\3,\5"+
		",\u0206\n,\3,\3,\3,\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\5-\u0218\n"+
		"-\3-\3-\3.\3.\3.\3/\3/\3/\3\60\3\60\3\60\3\61\3\61\5\61\u0227\n\61\3\62"+
		"\3\62\3\62\3\62\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63"+
		"\3\63\3\63\5\63\u023a\n\63\3\64\3\64\3\65\5\65\u023f\n\65\3\65\3\65\5"+
		"\65\u0243\n\65\3\65\3\65\5\65\u0247\n\65\3\65\3\65\3\65\3\65\3\65\3\65"+
		"\3\65\5\65\u0250\n\65\3\66\3\66\3\66\3\66\7\66\u0256\n\66\f\66\16\66\u0259"+
		"\13\66\3\66\3\66\3\66\3\66\7\66\u025f\n\66\f\66\16\66\u0262\13\66\3\66"+
		"\3\66\3\66\3\66\7\66\u0268\n\66\f\66\16\66\u026b\13\66\3\66\3\66\3\66"+
		"\3\66\7\66\u0271\n\66\f\66\16\66\u0274\13\66\3\66\3\66\3\66\3\66\3\66"+
		"\3\66\3\66\3\66\5\66\u027e\n\66\3\67\3\67\3\67\3\67\3\67\5\67\u0285\n"+
		"\67\38\38\38\38\38\39\39\59\u028e\n9\3:\3:\3;\3;\7;\u0294\n;\f;\16;\u0297"+
		"\13;\3<\3<\3=\3=\3=\5=\u029e\n=\3=\3=\5=\u02a2\n=\3=\5=\u02a5\n=\3=\3"+
		"=\5=\u02a9\n=\3=\3=\7=\u02ad\n=\f=\16=\u02b0\13=\3=\3=\3=\3=\3=\3=\5="+
		"\u02b8\n=\3>\3>\3>\3>\3?\3?\3@\3@\3A\3A\3A\3B\3B\3B\5B\u02c8\nB\3B\5B"+
		"\u02cb\nB\3B\3B\3C\3C\3C\5C\u02d2\nC\3C\5C\u02d5\nC\3C\3C\3D\3D\3D\7D"+
		"\u02dc\nD\fD\16D\u02df\13D\3E\3E\3F\3F\3F\7F\u02e6\nF\fF\16F\u02e9\13"+
		"F\3G\5G\u02ec\nG\3G\3G\3H\3H\3H\7H\u02f3\nH\fH\16H\u02f6\13H\3I\3I\3I"+
		"\3I\5I\u02fc\nI\3I\5I\u02ff\nI\3J\3J\3J\7J\u0304\nJ\fJ\16J\u0307\13J\3"+
		"K\3K\3K\3K\7K\u030d\nK\fK\16K\u0310\13K\3L\3L\3L\5L\u0315\nL\3L\3L\3M"+
		"\3M\3M\7M\u031c\nM\fM\16M\u031f\13M\3N\3N\3N\5N\u0324\nN\3N\2\2O\2\4\6"+
		"\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRT"+
		"VXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e"+
		"\u0090\u0092\u0094\u0096\u0098\u009a\2\t\3\2\3\b\3\2\35\36\4\2\4\4\21"+
		"\21\4\2%%\'\'\6\2\4\4\21\21$$(*\3\2\638\4\2\21\21++\2\u0358\2\u009c\3"+
		"\2\2\2\4\u00b2\3\2\2\2\6\u00b4\3\2\2\2\b\u00e5\3\2\2\2\n\u00e8\3\2\2\2"+
		"\f\u00ec\3\2\2\2\16\u00f2\3\2\2\2\20\u00f4\3\2\2\2\22\u00f6\3\2\2\2\24"+
		"\u010b\3\2\2\2\26\u010d\3\2\2\2\30\u0111\3\2\2\2\32\u0115\3\2\2\2\34\u0119"+
		"\3\2\2\2\36\u011c\3\2\2\2 \u012a\3\2\2\2\"\u012c\3\2\2\2$\u012f\3\2\2"+
		"\2&\u0138\3\2\2\2(\u013a\3\2\2\2*\u013f\3\2\2\2,\u0146\3\2\2\2.\u0148"+
		"\3\2\2\2\60\u0158\3\2\2\2\62\u015a\3\2\2\2\64\u0160\3\2\2\2\66\u0164\3"+
		"\2\2\28\u0170\3\2\2\2:\u0178\3\2\2\2<\u0192\3\2\2\2>\u0197\3\2\2\2@\u019c"+
		"\3\2\2\2B\u01aa\3\2\2\2D\u01b2\3\2\2\2F\u01b4\3\2\2\2H\u01b9\3\2\2\2J"+
		"\u01be\3\2\2\2L\u01c6\3\2\2\2N\u01d3\3\2\2\2P\u01d5\3\2\2\2R\u01f5\3\2"+
		"\2\2T\u01fa\3\2\2\2V\u01fc\3\2\2\2X\u020a\3\2\2\2Z\u021b\3\2\2\2\\\u021e"+
		"\3\2\2\2^\u0221\3\2\2\2`\u0224\3\2\2\2b\u0228\3\2\2\2d\u0239\3\2\2\2f"+
		"\u023b\3\2\2\2h\u024f\3\2\2\2j\u027d\3\2\2\2l\u0284\3\2\2\2n\u0286\3\2"+
		"\2\2p\u028d\3\2\2\2r\u028f\3\2\2\2t\u0291\3\2\2\2v\u0298\3\2\2\2x\u02b7"+
		"\3\2\2\2z\u02b9\3\2\2\2|\u02bd\3\2\2\2~\u02bf\3\2\2\2\u0080\u02c1\3\2"+
		"\2\2\u0082\u02c4\3\2\2\2\u0084\u02ce\3\2\2\2\u0086\u02d8\3\2\2\2\u0088"+
		"\u02e0\3\2\2\2\u008a\u02e2\3\2\2\2\u008c\u02eb\3\2\2\2\u008e\u02ef\3\2"+
		"\2\2\u0090\u02fe\3\2\2\2\u0092\u0300\3\2\2\2\u0094\u0308\3\2\2\2\u0096"+
		"\u0314\3\2\2\2\u0098\u0318\3\2\2\2\u009a\u0323\3\2\2\2\u009c\u009d\t\2"+
		"\2\2\u009d\3\3\2\2\2\u009e\u00a4\5X-\2\u009f\u00a4\5<\37\2\u00a0\u00a4"+
		"\5Z.\2\u00a1\u00a4\5\\/\2\u00a2\u00a4\5.\30\2\u00a3\u009e\3\2\2\2\u00a3"+
		"\u009f\3\2\2\2\u00a3\u00a0\3\2\2\2\u00a3\u00a1\3\2\2\2\u00a3\u00a2\3\2"+
		"\2\2\u00a4\u00a8\3\2\2\2\u00a5\u00a7\5\60\31\2\u00a6\u00a5\3\2\2\2\u00a7"+
		"\u00aa\3\2\2\2\u00a8\u00a6\3\2\2\2\u00a8\u00a9\3\2\2\2\u00a9\u00ac\3\2"+
		"\2\2\u00aa\u00a8\3\2\2\2\u00ab\u00ad\5\"\22\2\u00ac\u00ab\3\2\2\2\u00ac"+
		"\u00ad\3\2\2\2\u00ad\u00b3\3\2\2\2\u00ae\u00af\7\t\2\2\u00af\u00b0\5*"+
		"\26\2\u00b0\u00b1\7\n\2\2\u00b1\u00b3\3\2\2\2\u00b2\u00a3\3\2\2\2\u00b2"+
		"\u00ae\3\2\2\2\u00b3\5\3\2\2\2\u00b4\u00b5\7\13\2\2\u00b5\u00bf\5\u0096"+
		"L\2\u00b6\u00b8\7\f\2\2\u00b7\u00b9\5\u0092J\2\u00b8\u00b7\3\2\2\2\u00b8"+
		"\u00b9\3\2\2\2\u00b9\u00bc\3\2\2\2\u00ba\u00bb\7\r\2\2\u00bb\u00bd\5\u008e"+
		"H\2\u00bc\u00ba\3\2\2\2\u00bc\u00bd\3\2\2\2\u00bd\u00be\3\2\2\2\u00be"+
		"\u00c0\7\16\2\2\u00bf\u00b6\3\2\2\2\u00bf\u00c0\3\2\2\2\u00c0\u00c2\3"+
		"\2\2\2\u00c1\u00c3\5\2\2\2\u00c2\u00c1\3\2\2\2\u00c2\u00c3\3\2\2\2\u00c3"+
		"\u00d2\3\2\2\2\u00c4\u00c5\7\17\2\2\u00c5\u00c6\7\20\2\2\u00c6\u00c7\7"+
		"\21\2\2\u00c7\u00c8\7\22\2\2\u00c8\u00c9\7\21\2\2\u00c9\u00ca\7\22\2\2"+
		"\u00ca\u00cb\7\21\2\2\u00cb\u00cc\7\22\2\2\u00cc\u00cd\7\21\2\2\u00cd"+
		"\u00ce\7\22\2\2\u00ce\u00cf\7\21\2\2\u00cf\u00d0\7\22\2\2\u00d0\u00d1"+
		"\7\21\2\2\u00d1\u00d3\7\23\2\2\u00d2\u00c4\3\2\2\2\u00d2\u00d3\3\2\2\2"+
		"\u00d3\u00d6\3\2\2\2\u00d4\u00d5\7\24\2\2\u00d5\u00d7\5\u0096L\2\u00d6"+
		"\u00d4\3\2\2\2\u00d6\u00d7\3\2\2\2\u00d7\u00d8\3\2\2\2\u00d8\u00e1\7\t"+
		"\2\2\u00d9\u00de\5\f\7\2\u00da\u00db\7\22\2\2\u00db\u00dd\5\f\7\2\u00dc"+
		"\u00da\3\2\2\2\u00dd\u00e0\3\2\2\2\u00de\u00dc\3\2\2\2\u00de\u00df\3\2"+
		"\2\2\u00df\u00e2\3\2\2\2\u00e0\u00de\3\2\2\2\u00e1\u00d9\3\2\2\2\u00e1"+
		"\u00e2\3\2\2\2\u00e2\u00e3\3\2\2\2\u00e3\u00e4\7\n\2\2\u00e4\7\3\2\2\2"+
		"\u00e5\u00e6\5h\65\2\u00e6\u00e7\5\n\6\2\u00e7\t\3\2\2\2\u00e8\u00e9\5"+
		"\u0096L\2\u00e9\u00ea\7\25\2\2\u00ea\u00eb\5\2\2\2\u00eb\13\3\2\2\2\u00ec"+
		"\u00ed\5\2\2\2\u00ed\u00ee\7\26\2\2\u00ee\u00ef\5\16\b\2\u00ef\r\3\2\2"+
		"\2\u00f0\u00f3\5\20\t\2\u00f1\u00f3\5\22\n\2\u00f2\u00f0\3\2\2\2\u00f2"+
		"\u00f1\3\2\2\2\u00f3\17\3\2\2\2\u00f4\u00f5\5\24\13\2\u00f5\21\3\2\2\2"+
		"\u00f6\u00ff\7\27\2\2\u00f7\u00fc\5\24\13\2\u00f8\u00f9\7\22\2\2\u00f9"+
		"\u00fb\5\24\13\2\u00fa\u00f8\3\2\2\2\u00fb\u00fe\3\2\2\2\u00fc\u00fa\3"+
		"\2\2\2\u00fc\u00fd\3\2\2\2\u00fd\u0100\3\2\2\2\u00fe\u00fc\3\2\2\2\u00ff"+
		"\u00f7\3\2\2\2\u00ff\u0100\3\2\2\2\u0100\u0101\3\2\2\2\u0101\u0102\7\30"+
		"\2\2\u0102\23\3\2\2\2\u0103\u010c\5d\63\2\u0104\u010c\7\31\2\2\u0105\u010c"+
		"\5\6\4\2\u0106\u010c\5\u0096L\2\u0107\u010c\5\26\f\2\u0108\u010c\5\30"+
		"\r\2\u0109\u010c\5\32\16\2\u010a\u010c\5\2\2\2\u010b\u0103\3\2\2\2\u010b"+
		"\u0104\3\2\2\2\u010b\u0105\3\2\2\2\u010b\u0106\3\2\2\2\u010b\u0107\3\2"+
		"\2\2\u010b\u0108\3\2\2\2\u010b\u0109\3\2\2\2\u010b\u010a\3\2\2\2\u010c"+
		"\25\3\2\2\2\u010d\u010e\5\u0096L\2\u010e\u010f\7\32\2\2\u010f\u0110\5"+
		"\2\2\2\u0110\27\3\2\2\2\u0111\u0112\5\u0096L\2\u0112\u0113\7\24\2\2\u0113"+
		"\u0114\5\2\2\2\u0114\31\3\2\2\2\u0115\u0116\5\u0096L\2\u0116\u0117\7\33"+
		"\2\2\u0117\u0118\5\2\2\2\u0118\33\3\2\2\2\u0119\u011a\5x=\2\u011a\u011b"+
		"\5z>\2\u011b\35\3\2\2\2\u011c\u0126\5 \21\2\u011d\u0123\7\34\2\2\u011e"+
		"\u011f\5 \21\2\u011f\u0120\7\34\2\2\u0120\u0122\3\2\2\2\u0121\u011e\3"+
		"\2\2\2\u0122\u0125\3\2\2\2\u0123\u0121\3\2\2\2\u0123\u0124\3\2\2\2\u0124"+
		"\u0127\3\2\2\2\u0125\u0123\3\2\2\2\u0126\u011d\3\2\2\2\u0126\u0127\3\2"+
		"\2\2\u0127\37\3\2\2\2\u0128\u012b\5*\26\2\u0129\u012b\5(\25\2\u012a\u0128"+
		"\3\2\2\2\u012a\u0129\3\2\2\2\u012b!\3\2\2\2\u012c\u012d\t\3\2\2\u012d"+
		"\u012e\5$\23\2\u012e#\3\2\2\2\u012f\u0133\5,\27\2\u0130\u0132\5j\66\2"+
		"\u0131\u0130\3\2\2\2\u0132\u0135\3\2\2\2\u0133\u0131\3\2\2\2\u0133\u0134"+
		"\3\2\2\2\u0134%\3\2\2\2\u0135\u0133\3\2\2\2\u0136\u0139\5l\67\2\u0137"+
		"\u0139\5j\66\2\u0138\u0136\3\2\2\2\u0138\u0137\3\2\2\2\u0139\'\3\2\2\2"+
		"\u013a\u013b\7\6\2\2\u013b\u013c\5\2\2\2\u013c\u013d\7\26\2\2\u013d\u013e"+
		"\5*\26\2\u013e)\3\2\2\2\u013f\u0143\5,\27\2\u0140\u0142\5&\24\2\u0141"+
		"\u0140\3\2\2\2\u0142\u0145\3\2\2\2\u0143\u0141\3\2\2\2\u0143\u0144\3\2"+
		"\2\2\u0144+\3\2\2\2\u0145\u0143\3\2\2\2\u0146\u0147\5\4\3\2\u0147-\3\2"+
		"\2\2\u0148\u0151\7\27\2\2\u0149\u014e\5\4\3\2\u014a\u014b\7\22\2\2\u014b"+
		"\u014d\5\4\3\2\u014c\u014a\3\2\2\2\u014d\u0150\3\2\2\2\u014e\u014c\3\2"+
		"\2\2\u014e\u014f\3\2\2\2\u014f\u0152\3\2\2\2\u0150\u014e\3\2\2\2\u0151"+
		"\u0149\3\2\2\2\u0151\u0152\3\2\2\2\u0152\u0153\3\2\2\2\u0153\u0154\7\30"+
		"\2\2\u0154/\3\2\2\2\u0155\u0159\5\62\32\2\u0156\u0159\5\66\34\2\u0157"+
		"\u0159\5\64\33\2\u0158\u0155\3\2\2\2\u0158\u0156\3\2\2\2\u0158\u0157\3"+
		"\2\2\2\u0159\61\3\2\2\2\u015a\u015b\7\32\2\2\u015b\u015e\5\2\2\2\u015c"+
		"\u015f\58\35\2\u015d\u015f\5:\36\2\u015e\u015c\3\2\2\2\u015e\u015d\3\2"+
		"\2\2\u015e\u015f\3\2\2\2\u015f\63\3\2\2\2\u0160\u0161\7\27\2\2\u0161\u0162"+
		"\t\4\2\2\u0162\u0163\7\30\2\2\u0163\65\3\2\2\2\u0164\u0165\7\37\2\2\u0165"+
		"\u0166\5\u0096L\2\u0166\u016d\5:\36\2\u0167\u0168\7\37\2\2\u0168\u0169"+
		"\5\u0096L\2\u0169\u016a\5:\36\2\u016a\u016c\3\2\2\2\u016b\u0167\3\2\2"+
		"\2\u016c\u016f\3\2\2\2\u016d\u016b\3\2\2\2\u016d\u016e\3\2\2\2\u016e\67"+
		"\3\2\2\2\u016f\u016d\3\2\2\2\u0170\u0171\7\t\2\2\u0171\u0174\7\31\2\2"+
		"\u0172\u0173\7\22\2\2\u0173\u0175\7\31\2\2\u0174\u0172\3\2\2\2\u0174\u0175"+
		"\3\2\2\2\u0175\u0176\3\2\2\2\u0176\u0177\7\n\2\2\u01779\3\2\2\2\u0178"+
		"\u0181\7\t\2\2\u0179\u017e\5*\26\2\u017a\u017b\7\22\2\2\u017b\u017d\5"+
		"*\26\2\u017c\u017a\3\2\2\2\u017d\u0180\3\2\2\2\u017e\u017c\3\2\2\2\u017e"+
		"\u017f\3\2\2\2\u017f\u0182\3\2\2\2\u0180\u017e\3\2\2\2\u0181\u0179\3\2"+
		"\2\2\u0181\u0182\3\2\2\2\u0182\u0183\3\2\2\2\u0183\u0184\7\n\2\2\u0184"+
		";\3\2\2\2\u0185\u0193\5p9\2\u0186\u0193\5f\64\2\u0187\u0193\5P)\2\u0188"+
		"\u0193\5\b\5\2\u0189\u0193\5B\"\2\u018a\u018b\7\24\2\2\u018b\u0193\5x"+
		"=\2\u018c\u0193\5^\60\2\u018d\u0193\5@!\2\u018e\u0193\5> \2\u018f\u0190"+
		"\5`\61\2\u0190\u0191\5^\60\2\u0191\u0193\3\2\2\2\u0192\u0185\3\2\2\2\u0192"+
		"\u0186\3\2\2\2\u0192\u0187\3\2\2\2\u0192\u0188\3\2\2\2\u0192\u0189\3\2"+
		"\2\2\u0192\u018a\3\2\2\2\u0192\u018c\3\2\2\2\u0192\u018d\3\2\2\2\u0192"+
		"\u018e\3\2\2\2\u0192\u018f\3\2\2\2\u0193=\3\2\2\2\u0194\u0198\7 \2\2\u0195"+
		"\u0198\5\u0096L\2\u0196\u0198\5\n\6\2\u0197\u0194\3\2\2\2\u0197\u0195"+
		"\3\2\2\2\u0197\u0196\3\2\2\2\u0198\u019a\3\2\2\2\u0199\u019b\5D#\2\u019a"+
		"\u0199\3\2\2\2\u019a\u019b\3\2\2\2\u019b?\3\2\2\2\u019c\u01a5\7!\2\2\u019d"+
		"\u01a2\5`\61\2\u019e\u019f\7\22\2\2\u019f\u01a1\5`\61\2\u01a0\u019e\3"+
		"\2\2\2\u01a1\u01a4\3\2\2\2\u01a2\u01a0\3\2\2\2\u01a2\u01a3\3\2\2\2\u01a3"+
		"\u01a6\3\2\2\2\u01a4\u01a2\3\2\2\2\u01a5\u019d\3\2\2\2\u01a5\u01a6\3\2"+
		"\2\2\u01a6\u01a7\3\2\2\2\u01a7\u01a8\5^\60\2\u01a8\u01a9\7\"\2\2\u01a9"+
		"A\3\2\2\2\u01aa\u01ab\7#\2\2\u01ab\u01ac\5\2\2\2\u01acC\3\2\2\2\u01ad"+
		"\u01b3\5F$\2\u01ae\u01b3\5H%\2\u01af\u01b3\5J&\2\u01b0\u01b3\5L\'\2\u01b1"+
		"\u01b3\5:\36\2\u01b2\u01ad\3\2\2\2\u01b2\u01ae\3\2\2\2\u01b2\u01af\3\2"+
		"\2\2\u01b2\u01b0\3\2\2\2\u01b2\u01b1\3\2\2\2\u01b3E\3\2\2\2\u01b4\u01b5"+
		"\7\32\2\2\u01b5\u01b6\7\5\2\2\u01b6\u01b7\7\t\2\2\u01b7\u01b8\7\n\2\2"+
		"\u01b8G\3\2\2\2\u01b9\u01ba\7\32\2\2\u01ba\u01bb\7\7\2\2\u01bb\u01bc\7"+
		"\t\2\2\u01bc\u01bd\7\n\2\2\u01bdI\3\2\2\2\u01be\u01bf\7\32\2\2\u01bf\u01c0"+
		"\7\b\2\2\u01c0\u01c1\7\t\2\2\u01c1\u01c2\5N(\2\u01c2\u01c3\7\22\2\2\u01c3"+
		"\u01c4\5N(\2\u01c4\u01c5\7\n\2\2\u01c5K\3\2\2\2\u01c6\u01c7\7\32\2\2\u01c7"+
		"\u01c8\7\5\2\2\u01c8\u01c9\7\t\2\2\u01c9\u01cc\5N(\2\u01ca\u01cb\7\22"+
		"\2\2\u01cb\u01cd\5N(\2\u01cc\u01ca\3\2\2\2\u01cc\u01cd\3\2\2\2\u01cd\u01ce"+
		"\3\2\2\2\u01ce\u01cf\7\n\2\2\u01cfM\3\2\2\2\u01d0\u01d4\7\31\2\2\u01d1"+
		"\u01d4\7$\2\2\u01d2\u01d4\5B\"\2\u01d3\u01d0\3\2\2\2\u01d3\u01d1\3\2\2"+
		"\2\u01d3\u01d2\3\2\2\2\u01d4O\3\2\2\2\u01d5\u01d8\7\13\2\2\u01d6\u01d9"+
		"\5B\"\2\u01d7\u01d9\5\u0096L\2\u01d8\u01d6\3\2\2\2\u01d8\u01d7\3\2\2\2"+
		"\u01d9\u01e3\3\2\2\2\u01da\u01dc\7\f\2\2\u01db\u01dd\5\u0092J\2\u01dc"+
		"\u01db\3\2\2\2\u01dc\u01dd\3\2\2\2\u01dd\u01e0\3\2\2\2\u01de\u01df\7\r"+
		"\2\2\u01df\u01e1\5\u008eH\2\u01e0\u01de\3\2\2\2\u01e0\u01e1\3\2\2\2\u01e1"+
		"\u01e2\3\2\2\2\u01e2\u01e4\7\16\2\2\u01e3\u01da\3\2\2\2\u01e3\u01e4\3"+
		"\2\2\2\u01e4\u01e6\3\2\2\2\u01e5\u01e7\5\2\2\2\u01e6\u01e5\3\2\2\2\u01e6"+
		"\u01e7\3\2\2\2\u01e7\u01e8\3\2\2\2\u01e8\u01ea\7\t\2\2\u01e9\u01eb\5V"+
		",\2\u01ea\u01e9\3\2\2\2\u01ea\u01eb\3\2\2\2\u01eb\u01f0\3\2\2\2\u01ec"+
		"\u01ed\7\22\2\2\u01ed\u01ef\5V,\2\u01ee\u01ec\3\2\2\2\u01ef\u01f2\3\2"+
		"\2\2\u01f0\u01ee\3\2\2\2\u01f0\u01f1\3\2\2\2\u01f1\u01f3\3\2\2\2\u01f2"+
		"\u01f0\3\2\2\2\u01f3\u01f4\7\n\2\2\u01f4Q\3\2\2\2\u01f5\u01f6\5T+\2\u01f6"+
		"S\3\2\2\2\u01f7\u01fb\5*\26\2\u01f8\u01fb\5P)\2\u01f9\u01fb\5\u0096L\2"+
		"\u01fa\u01f7\3\2\2\2\u01fa\u01f8\3\2\2\2\u01fa\u01f9\3\2\2\2\u01fbU\3"+
		"\2\2\2\u01fc\u0201\5\2\2\2\u01fd\u01fe\7\32\2\2\u01fe\u0200\5\2\2\2\u01ff"+
		"\u01fd\3\2\2\2\u0200\u0203\3\2\2\2\u0201\u01ff\3\2\2\2\u0201\u0202\3\2"+
		"\2\2\u0202\u0205\3\2\2\2\u0203\u0201\3\2\2\2\u0204\u0206\7%\2\2\u0205"+
		"\u0204\3\2\2\2\u0205\u0206\3\2\2\2\u0206\u0207\3\2\2\2\u0207\u0208\7\26"+
		"\2\2\u0208\u0209\5R*\2\u0209W\3\2\2\2\u020a\u0217\7\27\2\2\u020b\u020c"+
		"\7\20\2\2\u020c\u0218\5\4\3\2\u020d\u020e\5\4\3\2\u020e\u020f\7\20\2\2"+
		"\u020f\u0210\5\4\3\2\u0210\u0218\3\2\2\2\u0211\u0212\5\4\3\2\u0212\u0213"+
		"\7\20\2\2\u0213\u0214\5\4\3\2\u0214\u0215\7\20\2\2\u0215\u0216\5\4\3\2"+
		"\u0216\u0218\3\2\2\2\u0217\u020b\3\2\2\2\u0217\u020d\3\2\2\2\u0217\u0211"+
		"\3\2\2\2\u0218\u0219\3\2\2\2\u0219\u021a\7\30\2\2\u021aY\3\2\2\2\u021b"+
		"\u021c\7&\2\2\u021c\u021d\5\4\3\2\u021d[\3\2\2\2\u021e\u021f\t\5\2\2\u021f"+
		"\u0220\5\4\3\2\u0220]\3\2\2\2\u0221\u0222\7\r\2\2\u0222\u0223\5\36\20"+
		"\2\u0223_\3\2\2\2\u0224\u0226\5\2\2\2\u0225\u0227\5b\62\2\u0226\u0225"+
		"\3\2\2\2\u0226\u0227\3\2\2\2\u0227a\3\2\2\2\u0228\u0229\7\20\2\2\u0229"+
		"\u022a\5x=\2\u022a\u022b\5z>\2\u022bc\3\2\2\2\u022c\u023a\5f\64\2\u022d"+
		"\u022e\7\'\2\2\u022e\u023a\7\21\2\2\u022f\u0230\7\'\2\2\u0230\u023a\7"+
		"(\2\2\u0231\u0232\7\'\2\2\u0232\u023a\7)\2\2\u0233\u0234\7%\2\2\u0234"+
		"\u023a\7\21\2\2\u0235\u0236\7%\2\2\u0236\u023a\7(\2\2\u0237\u0238\7%\2"+
		"\2\u0238\u023a\7)\2\2\u0239\u022c\3\2\2\2\u0239\u022d\3\2\2\2\u0239\u022f"+
		"\3\2\2\2\u0239\u0231\3\2\2\2\u0239\u0233\3\2\2\2\u0239\u0235\3\2\2\2\u0239"+
		"\u0237\3\2\2\2\u023ae\3\2\2\2\u023b\u023c\t\6\2\2\u023cg\3\2\2\2\u023d"+
		"\u023f\7\'\2\2\u023e\u023d\3\2\2\2\u023e\u023f\3\2\2\2\u023f\u0240\3\2"+
		"\2\2\u0240\u0250\7\21\2\2\u0241\u0243\7\'\2\2\u0242\u0241\3\2\2\2\u0242"+
		"\u0243\3\2\2\2\u0243\u0244\3\2\2\2\u0244\u0250\7(\2\2\u0245\u0247\7\'"+
		"\2\2\u0246\u0245\3\2\2\2\u0246\u0247\3\2\2\2\u0247\u0248\3\2\2\2\u0248"+
		"\u0250\7)\2\2\u0249\u024a\7%\2\2\u024a\u0250\7\21\2\2\u024b\u024c\7%\2"+
		"\2\u024c\u0250\7(\2\2\u024d\u024e\7%\2\2\u024e\u0250\7)\2\2\u024f\u023e"+
		"\3\2\2\2\u024f\u0242\3\2\2\2\u024f\u0246\3\2\2\2\u024f\u0249\3\2\2\2\u024f"+
		"\u024b\3\2\2\2\u024f\u024d\3\2\2\2\u0250i\3\2\2\2\u0251\u0252\7%\2\2\u0252"+
		"\u0257\5\4\3\2\u0253\u0254\7%\2\2\u0254\u0256\5\4\3\2\u0255\u0253\3\2"+
		"\2\2\u0256\u0259\3\2\2\2\u0257\u0255\3\2\2\2\u0257\u0258\3\2\2\2\u0258"+
		"\u027e\3\2\2\2\u0259\u0257\3\2\2\2\u025a\u025b\7+\2\2\u025b\u0260\5\4"+
		"\3\2\u025c\u025d\7+\2\2\u025d\u025f\5\4\3\2\u025e\u025c\3\2\2\2\u025f"+
		"\u0262\3\2\2\2\u0260\u025e\3\2\2\2\u0260\u0261\3\2\2\2\u0261\u027e\3\2"+
		"\2\2\u0262\u0260\3\2\2\2\u0263\u0264\7\'\2\2\u0264\u0269\5\4\3\2\u0265"+
		"\u0266\7\'\2\2\u0266\u0268\5\4\3\2\u0267\u0265\3\2\2\2\u0268\u026b\3\2"+
		"\2\2\u0269\u0267\3\2\2\2\u0269\u026a\3\2\2\2\u026a\u027e\3\2\2\2\u026b"+
		"\u0269\3\2\2\2\u026c\u026d\7,\2\2\u026d\u0272\5\4\3\2\u026e\u026f\7,\2"+
		"\2\u026f\u0271\5\4\3\2\u0270\u026e\3\2\2\2\u0271\u0274\3\2\2\2\u0272\u0270"+
		"\3\2\2\2\u0272\u0273\3\2\2\2\u0273\u027e\3\2\2\2\u0274\u0272\3\2\2\2\u0275"+
		"\u0276\7\f\2\2\u0276\u027e\5\4\3\2\u0277\u0278\7-\2\2\u0278\u027e\5\4"+
		"\3\2\u0279\u027a\7\16\2\2\u027a\u027e\5\4\3\2\u027b\u027c\7.\2\2\u027c"+
		"\u027e\5\4\3\2\u027d\u0251\3\2\2\2\u027d\u025a\3\2\2\2\u027d\u0263\3\2"+
		"\2\2\u027d\u026c\3\2\2\2\u027d\u0275\3\2\2\2\u027d\u0277\3\2\2\2\u027d"+
		"\u0279\3\2\2\2\u027d\u027b\3\2\2\2\u027ek\3\2\2\2\u027f\u0280\7/\2\2\u0280"+
		"\u0285\5\4\3\2\u0281\u0282\7\60\2\2\u0282\u0285\5\4\3\2\u0283\u0285\5"+
		"\"\22\2\u0284\u027f\3\2\2\2\u0284\u0281\3\2\2\2\u0284\u0283\3\2\2\2\u0285"+
		"m\3\2\2\2\u0286\u0287\5\2\2\2\u0287\u0288\7\20\2\2\u0288\u0289\5x=\2\u0289"+
		"\u028a\5z>\2\u028ao\3\2\2\2\u028b\u028e\5t;\2\u028c\u028e\5r:\2\u028d"+
		"\u028b\3\2\2\2\u028d\u028c\3\2\2\2\u028eq\3\2\2\2\u028f\u0290\7\61\2\2"+
		"\u0290s\3\2\2\2\u0291\u0295\7\62\2\2\u0292\u0294\5v<\2\u0293\u0292\3\2"+
		"\2\2\u0294\u0297\3\2\2\2\u0295\u0293\3\2\2\2\u0295\u0296\3\2\2\2\u0296"+
		"u\3\2\2\2\u0297\u0295\3\2\2\2\u0298\u0299\t\7\2\2\u0299w\3\2\2\2\u029a"+
		"\u02a4\5\u0096L\2\u029b\u029d\7\f\2\2\u029c\u029e\5\u0092J\2\u029d\u029c"+
		"\3\2\2\2\u029d\u029e\3\2\2\2\u029e\u02a1\3\2\2\2\u029f\u02a0\7\r\2\2\u02a0"+
		"\u02a2\5\u008eH\2\u02a1\u029f\3\2\2\2\u02a1\u02a2\3\2\2\2\u02a2\u02a3"+
		"\3\2\2\2\u02a3\u02a5\7\16\2\2\u02a4\u029b\3\2\2\2\u02a4\u02a5\3\2\2\2"+
		"\u02a5\u02b8\3\2\2\2\u02a6\u02a8\7!\2\2\u02a7\u02a9\5\u0080A\2\u02a8\u02a7"+
		"\3\2\2\2\u02a8\u02a9\3\2\2\2\u02a9\u02ae\3\2\2\2\u02aa\u02ab\7\22\2\2"+
		"\u02ab\u02ad\5\u0080A\2\u02ac\u02aa\3\2\2\2\u02ad\u02b0\3\2\2\2\u02ae"+
		"\u02ac\3\2\2\2\u02ae\u02af\3\2\2\2\u02af\u02b1\3\2\2\2\u02b0\u02ae\3\2"+
		"\2\2\u02b1\u02b2\7\37\2\2\u02b2\u02b3\5x=\2\u02b3\u02b4\5z>\2\u02b4\u02b5"+
		"\7\"\2\2\u02b5\u02b8\3\2\2\2\u02b6\u02b8\5\n\6\2\u02b7\u029a\3\2\2\2\u02b7"+
		"\u02a6\3\2\2\2\u02b7\u02b6\3\2\2\2\u02b8y\3\2\2\2\u02b9\u02ba\7\27\2\2"+
		"\u02ba\u02bb\5\u0090I\2\u02bb\u02bc\7\30\2\2\u02bc{\3\2\2\2\u02bd\u02be"+
		"\7\21\2\2\u02be}\3\2\2\2\u02bf\u02c0\t\b\2\2\u02c0\177\3\2\2\2\u02c1\u02c2"+
		"\5x=\2\u02c2\u02c3\5z>\2\u02c3\u0081\3\2\2\2\u02c4\u02ca\7\f\2\2\u02c5"+
		"\u02c7\5\u0086D\2\u02c6\u02c8\5\u0094K\2\u02c7\u02c6\3\2\2\2\u02c7\u02c8"+
		"\3\2\2\2\u02c8\u02cb\3\2\2\2\u02c9\u02cb\5\u0094K\2\u02ca\u02c5\3\2\2"+
		"\2\u02ca\u02c9\3\2\2\2\u02cb\u02cc\3\2\2\2\u02cc\u02cd\7\16\2\2\u02cd"+
		"\u0083\3\2\2\2\u02ce\u02d4\7\f\2\2\u02cf\u02d1\5\u008aF\2\u02d0\u02d2"+
		"\5\u0094K\2\u02d1\u02d0\3\2\2\2\u02d1\u02d2\3\2\2\2\u02d2\u02d5\3\2\2"+
		"\2\u02d3\u02d5\5\u0094K\2\u02d4\u02cf\3\2\2\2\u02d4\u02d3\3\2\2\2\u02d5"+
		"\u02d6\3\2\2\2\u02d6\u02d7\7\16\2\2\u02d7\u0085\3\2\2\2\u02d8\u02dd\5"+
		"\u0088E\2\u02d9\u02da\7\22\2\2\u02da\u02dc\5\u0088E\2\u02db\u02d9\3\2"+
		"\2\2\u02dc\u02df\3\2\2\2\u02dd\u02db\3\2\2\2\u02dd\u02de\3\2\2\2\u02de"+
		"\u0087\3\2\2\2\u02df\u02dd\3\2\2\2\u02e0\u02e1\5\2\2\2\u02e1\u0089\3\2"+
		"\2\2\u02e2\u02e7\5\u008cG\2\u02e3\u02e4\7\22\2\2\u02e4\u02e6\5\u008cG"+
		"\2\u02e5\u02e3\3\2\2\2\u02e6\u02e9\3\2\2\2\u02e7\u02e5\3\2\2\2\u02e7\u02e8"+
		"\3\2\2\2\u02e8\u008b\3\2\2\2\u02e9\u02e7\3\2\2\2\u02ea\u02ec\7\'\2\2\u02eb"+
		"\u02ea\3\2\2\2\u02eb\u02ec\3\2\2\2\u02ec\u02ed\3\2\2\2\u02ed\u02ee\5\2"+
		"\2\2\u02ee\u008d\3\2\2\2\u02ef\u02f4\5\u0090I\2\u02f0\u02f1\7\22\2\2\u02f1"+
		"\u02f3\5\u0090I\2\u02f2\u02f0\3\2\2\2\u02f3\u02f6\3\2\2\2\u02f4\u02f2"+
		"\3\2\2\2\u02f4\u02f5\3\2\2\2\u02f5\u008f\3\2\2\2\u02f6\u02f4\3\2\2\2\u02f7"+
		"\u02ff\5\2\2\2\u02f8\u02f9\5|?\2\u02f9\u02fa\79\2\2\u02fa\u02fc\3\2\2"+
		"\2\u02fb\u02f8\3\2\2\2\u02fb\u02fc\3\2\2\2\u02fc\u02fd\3\2\2\2\u02fd\u02ff"+
		"\5~@\2\u02fe\u02f7\3\2\2\2\u02fe\u02fb\3\2\2\2\u02ff\u0091\3\2\2\2\u0300"+
		"\u0305\5x=\2\u0301\u0302\7\22\2\2\u0302\u0304\5x=\2\u0303\u0301\3\2\2"+
		"\2\u0304\u0307\3\2\2\2\u0305\u0303\3\2\2\2\u0305\u0306\3\2\2\2\u0306\u0093"+
		"\3\2\2\2\u0307\u0305\3\2\2\2\u0308\u0309\7\r\2\2\u0309\u030e\5\2\2\2\u030a"+
		"\u030b\7\22\2\2\u030b\u030d\5\2\2\2\u030c\u030a\3\2\2\2\u030d\u0310\3"+
		"\2\2\2\u030e\u030c\3\2\2\2\u030e\u030f\3\2\2\2\u030f\u0095\3\2\2\2\u0310"+
		"\u030e\3\2\2\2\u0311\u0312\5\u0098M\2\u0312\u0313\7 \2\2\u0313\u0315\3"+
		"\2\2\2\u0314\u0311\3\2\2\2\u0314\u0315\3\2\2\2\u0315\u0316\3\2\2\2\u0316"+
		"\u0317\5\2\2\2\u0317\u0097\3\2\2\2\u0318\u031d\5\2\2\2\u0319\u031a\7 "+
		"\2\2\u031a\u031c\5\2\2\2\u031b\u0319\3\2\2\2\u031c\u031f\3\2\2\2\u031d"+
		"\u031b\3\2\2\2\u031d\u031e\3\2\2\2\u031e\u0099\3\2\2\2\u031f\u031d\3\2"+
		"\2\2\u0320\u0324\5\2\2\2\u0321\u0324\7*\2\2\u0322\u0324\7\21\2\2\u0323"+
		"\u0320\3\2\2\2\u0323\u0321\3\2\2\2\u0323\u0322\3\2\2\2\u0324\u009b\3\2"+
		"\2\2V\u00a3\u00a8\u00ac\u00b2\u00b8\u00bc\u00bf\u00c2\u00d2\u00d6\u00de"+
		"\u00e1\u00f2\u00fc\u00ff\u010b\u0123\u0126\u012a\u0133\u0138\u0143\u014e"+
		"\u0151\u0158\u015e\u016d\u0174\u017e\u0181\u0192\u0197\u019a\u01a2\u01a5"+
		"\u01b2\u01cc\u01d3\u01d8\u01dc\u01e0\u01e3\u01e6\u01ea\u01f0\u01fa\u0201"+
		"\u0205\u0217\u0226\u0239\u023e\u0242\u0246\u024f\u0257\u0260\u0269\u0272"+
		"\u027d\u0284\u028d\u0295\u029d\u02a1\u02a4\u02a8\u02ae\u02b7\u02c7\u02ca"+
		"\u02d1\u02d4\u02dd\u02e7\u02eb\u02f4\u02fb\u02fe\u0305\u030e\u0314\u031d"+
		"\u0323";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}