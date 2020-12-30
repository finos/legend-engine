// Generated from C:/Users/lmaur/Documents/GS/legend-engine/legend-engine-language-pure-grammar/src/main/antlr4/org/finos/legend/engine/language/pure/grammar/from/antlr4/core\M3ParserGrammar.g4 by ANTLR 4.9
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link M3ParserGrammar}.
 */
public interface M3ParserGrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(M3ParserGrammar.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(M3ParserGrammar.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(M3ParserGrammar.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(M3ParserGrammar.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#instance}.
	 * @param ctx the parse tree
	 */
	void enterInstance(M3ParserGrammar.InstanceContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#instance}.
	 * @param ctx the parse tree
	 */
	void exitInstance(M3ParserGrammar.InstanceContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#unitInstance}.
	 * @param ctx the parse tree
	 */
	void enterUnitInstance(M3ParserGrammar.UnitInstanceContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#unitInstance}.
	 * @param ctx the parse tree
	 */
	void exitUnitInstance(M3ParserGrammar.UnitInstanceContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#unitName}.
	 * @param ctx the parse tree
	 */
	void enterUnitName(M3ParserGrammar.UnitNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#unitName}.
	 * @param ctx the parse tree
	 */
	void exitUnitName(M3ParserGrammar.UnitNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#instancePropertyAssignment}.
	 * @param ctx the parse tree
	 */
	void enterInstancePropertyAssignment(M3ParserGrammar.InstancePropertyAssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#instancePropertyAssignment}.
	 * @param ctx the parse tree
	 */
	void exitInstancePropertyAssignment(M3ParserGrammar.InstancePropertyAssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#instanceRightSide}.
	 * @param ctx the parse tree
	 */
	void enterInstanceRightSide(M3ParserGrammar.InstanceRightSideContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#instanceRightSide}.
	 * @param ctx the parse tree
	 */
	void exitInstanceRightSide(M3ParserGrammar.InstanceRightSideContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#instanceAtomicRightSideScalar}.
	 * @param ctx the parse tree
	 */
	void enterInstanceAtomicRightSideScalar(M3ParserGrammar.InstanceAtomicRightSideScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#instanceAtomicRightSideScalar}.
	 * @param ctx the parse tree
	 */
	void exitInstanceAtomicRightSideScalar(M3ParserGrammar.InstanceAtomicRightSideScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#instanceAtomicRightSideVector}.
	 * @param ctx the parse tree
	 */
	void enterInstanceAtomicRightSideVector(M3ParserGrammar.InstanceAtomicRightSideVectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#instanceAtomicRightSideVector}.
	 * @param ctx the parse tree
	 */
	void exitInstanceAtomicRightSideVector(M3ParserGrammar.InstanceAtomicRightSideVectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#instanceAtomicRightSide}.
	 * @param ctx the parse tree
	 */
	void enterInstanceAtomicRightSide(M3ParserGrammar.InstanceAtomicRightSideContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#instanceAtomicRightSide}.
	 * @param ctx the parse tree
	 */
	void exitInstanceAtomicRightSide(M3ParserGrammar.InstanceAtomicRightSideContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#enumReference}.
	 * @param ctx the parse tree
	 */
	void enterEnumReference(M3ParserGrammar.EnumReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#enumReference}.
	 * @param ctx the parse tree
	 */
	void exitEnumReference(M3ParserGrammar.EnumReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#stereotypeReference}.
	 * @param ctx the parse tree
	 */
	void enterStereotypeReference(M3ParserGrammar.StereotypeReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#stereotypeReference}.
	 * @param ctx the parse tree
	 */
	void exitStereotypeReference(M3ParserGrammar.StereotypeReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#tagReference}.
	 * @param ctx the parse tree
	 */
	void enterTagReference(M3ParserGrammar.TagReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#tagReference}.
	 * @param ctx the parse tree
	 */
	void exitTagReference(M3ParserGrammar.TagReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#propertyReturnType}.
	 * @param ctx the parse tree
	 */
	void enterPropertyReturnType(M3ParserGrammar.PropertyReturnTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#propertyReturnType}.
	 * @param ctx the parse tree
	 */
	void exitPropertyReturnType(M3ParserGrammar.PropertyReturnTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#codeBlock}.
	 * @param ctx the parse tree
	 */
	void enterCodeBlock(M3ParserGrammar.CodeBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#codeBlock}.
	 * @param ctx the parse tree
	 */
	void exitCodeBlock(M3ParserGrammar.CodeBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#programLine}.
	 * @param ctx the parse tree
	 */
	void enterProgramLine(M3ParserGrammar.ProgramLineContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#programLine}.
	 * @param ctx the parse tree
	 */
	void exitProgramLine(M3ParserGrammar.ProgramLineContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#equalNotEqual}.
	 * @param ctx the parse tree
	 */
	void enterEqualNotEqual(M3ParserGrammar.EqualNotEqualContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#equalNotEqual}.
	 * @param ctx the parse tree
	 */
	void exitEqualNotEqual(M3ParserGrammar.EqualNotEqualContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#combinedArithmeticOnly}.
	 * @param ctx the parse tree
	 */
	void enterCombinedArithmeticOnly(M3ParserGrammar.CombinedArithmeticOnlyContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#combinedArithmeticOnly}.
	 * @param ctx the parse tree
	 */
	void exitCombinedArithmeticOnly(M3ParserGrammar.CombinedArithmeticOnlyContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#expressionPart}.
	 * @param ctx the parse tree
	 */
	void enterExpressionPart(M3ParserGrammar.ExpressionPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#expressionPart}.
	 * @param ctx the parse tree
	 */
	void exitExpressionPart(M3ParserGrammar.ExpressionPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#letExpression}.
	 * @param ctx the parse tree
	 */
	void enterLetExpression(M3ParserGrammar.LetExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#letExpression}.
	 * @param ctx the parse tree
	 */
	void exitLetExpression(M3ParserGrammar.LetExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#combinedExpression}.
	 * @param ctx the parse tree
	 */
	void enterCombinedExpression(M3ParserGrammar.CombinedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#combinedExpression}.
	 * @param ctx the parse tree
	 */
	void exitCombinedExpression(M3ParserGrammar.CombinedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#expressionOrExpressionGroup}.
	 * @param ctx the parse tree
	 */
	void enterExpressionOrExpressionGroup(M3ParserGrammar.ExpressionOrExpressionGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#expressionOrExpressionGroup}.
	 * @param ctx the parse tree
	 */
	void exitExpressionOrExpressionGroup(M3ParserGrammar.ExpressionOrExpressionGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#expressionsArray}.
	 * @param ctx the parse tree
	 */
	void enterExpressionsArray(M3ParserGrammar.ExpressionsArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#expressionsArray}.
	 * @param ctx the parse tree
	 */
	void exitExpressionsArray(M3ParserGrammar.ExpressionsArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#propertyOrFunctionExpression}.
	 * @param ctx the parse tree
	 */
	void enterPropertyOrFunctionExpression(M3ParserGrammar.PropertyOrFunctionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#propertyOrFunctionExpression}.
	 * @param ctx the parse tree
	 */
	void exitPropertyOrFunctionExpression(M3ParserGrammar.PropertyOrFunctionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#propertyExpression}.
	 * @param ctx the parse tree
	 */
	void enterPropertyExpression(M3ParserGrammar.PropertyExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#propertyExpression}.
	 * @param ctx the parse tree
	 */
	void exitPropertyExpression(M3ParserGrammar.PropertyExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#propertyBracketExpression}.
	 * @param ctx the parse tree
	 */
	void enterPropertyBracketExpression(M3ParserGrammar.PropertyBracketExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#propertyBracketExpression}.
	 * @param ctx the parse tree
	 */
	void exitPropertyBracketExpression(M3ParserGrammar.PropertyBracketExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#functionExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionExpression(M3ParserGrammar.FunctionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#functionExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionExpression(M3ParserGrammar.FunctionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#functionExpressionLatestMilestoningDateParameter}.
	 * @param ctx the parse tree
	 */
	void enterFunctionExpressionLatestMilestoningDateParameter(M3ParserGrammar.FunctionExpressionLatestMilestoningDateParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#functionExpressionLatestMilestoningDateParameter}.
	 * @param ctx the parse tree
	 */
	void exitFunctionExpressionLatestMilestoningDateParameter(M3ParserGrammar.FunctionExpressionLatestMilestoningDateParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#functionExpressionParameters}.
	 * @param ctx the parse tree
	 */
	void enterFunctionExpressionParameters(M3ParserGrammar.FunctionExpressionParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#functionExpressionParameters}.
	 * @param ctx the parse tree
	 */
	void exitFunctionExpressionParameters(M3ParserGrammar.FunctionExpressionParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#atomicExpression}.
	 * @param ctx the parse tree
	 */
	void enterAtomicExpression(M3ParserGrammar.AtomicExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#atomicExpression}.
	 * @param ctx the parse tree
	 */
	void exitAtomicExpression(M3ParserGrammar.AtomicExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#instanceReference}.
	 * @param ctx the parse tree
	 */
	void enterInstanceReference(M3ParserGrammar.InstanceReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#instanceReference}.
	 * @param ctx the parse tree
	 */
	void exitInstanceReference(M3ParserGrammar.InstanceReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#lambdaFunction}.
	 * @param ctx the parse tree
	 */
	void enterLambdaFunction(M3ParserGrammar.LambdaFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#lambdaFunction}.
	 * @param ctx the parse tree
	 */
	void exitLambdaFunction(M3ParserGrammar.LambdaFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(M3ParserGrammar.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(M3ParserGrammar.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#allOrFunction}.
	 * @param ctx the parse tree
	 */
	void enterAllOrFunction(M3ParserGrammar.AllOrFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#allOrFunction}.
	 * @param ctx the parse tree
	 */
	void exitAllOrFunction(M3ParserGrammar.AllOrFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#allFunction}.
	 * @param ctx the parse tree
	 */
	void enterAllFunction(M3ParserGrammar.AllFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#allFunction}.
	 * @param ctx the parse tree
	 */
	void exitAllFunction(M3ParserGrammar.AllFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#allVersionsFunction}.
	 * @param ctx the parse tree
	 */
	void enterAllVersionsFunction(M3ParserGrammar.AllVersionsFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#allVersionsFunction}.
	 * @param ctx the parse tree
	 */
	void exitAllVersionsFunction(M3ParserGrammar.AllVersionsFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#allVersionsInRangeFunction}.
	 * @param ctx the parse tree
	 */
	void enterAllVersionsInRangeFunction(M3ParserGrammar.AllVersionsInRangeFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#allVersionsInRangeFunction}.
	 * @param ctx the parse tree
	 */
	void exitAllVersionsInRangeFunction(M3ParserGrammar.AllVersionsInRangeFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#allFunctionWithMilestoning}.
	 * @param ctx the parse tree
	 */
	void enterAllFunctionWithMilestoning(M3ParserGrammar.AllFunctionWithMilestoningContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#allFunctionWithMilestoning}.
	 * @param ctx the parse tree
	 */
	void exitAllFunctionWithMilestoning(M3ParserGrammar.AllFunctionWithMilestoningContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#buildMilestoningVariableExpression}.
	 * @param ctx the parse tree
	 */
	void enterBuildMilestoningVariableExpression(M3ParserGrammar.BuildMilestoningVariableExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#buildMilestoningVariableExpression}.
	 * @param ctx the parse tree
	 */
	void exitBuildMilestoningVariableExpression(M3ParserGrammar.BuildMilestoningVariableExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#expressionInstance}.
	 * @param ctx the parse tree
	 */
	void enterExpressionInstance(M3ParserGrammar.ExpressionInstanceContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#expressionInstance}.
	 * @param ctx the parse tree
	 */
	void exitExpressionInstance(M3ParserGrammar.ExpressionInstanceContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#expressionInstanceRightSide}.
	 * @param ctx the parse tree
	 */
	void enterExpressionInstanceRightSide(M3ParserGrammar.ExpressionInstanceRightSideContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#expressionInstanceRightSide}.
	 * @param ctx the parse tree
	 */
	void exitExpressionInstanceRightSide(M3ParserGrammar.ExpressionInstanceRightSideContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#expressionInstanceAtomicRightSide}.
	 * @param ctx the parse tree
	 */
	void enterExpressionInstanceAtomicRightSide(M3ParserGrammar.ExpressionInstanceAtomicRightSideContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#expressionInstanceAtomicRightSide}.
	 * @param ctx the parse tree
	 */
	void exitExpressionInstanceAtomicRightSide(M3ParserGrammar.ExpressionInstanceAtomicRightSideContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#expressionInstanceParserPropertyAssignment}.
	 * @param ctx the parse tree
	 */
	void enterExpressionInstanceParserPropertyAssignment(M3ParserGrammar.ExpressionInstanceParserPropertyAssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#expressionInstanceParserPropertyAssignment}.
	 * @param ctx the parse tree
	 */
	void exitExpressionInstanceParserPropertyAssignment(M3ParserGrammar.ExpressionInstanceParserPropertyAssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#sliceExpression}.
	 * @param ctx the parse tree
	 */
	void enterSliceExpression(M3ParserGrammar.SliceExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#sliceExpression}.
	 * @param ctx the parse tree
	 */
	void exitSliceExpression(M3ParserGrammar.SliceExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#notExpression}.
	 * @param ctx the parse tree
	 */
	void enterNotExpression(M3ParserGrammar.NotExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#notExpression}.
	 * @param ctx the parse tree
	 */
	void exitNotExpression(M3ParserGrammar.NotExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#signedExpression}.
	 * @param ctx the parse tree
	 */
	void enterSignedExpression(M3ParserGrammar.SignedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#signedExpression}.
	 * @param ctx the parse tree
	 */
	void exitSignedExpression(M3ParserGrammar.SignedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#lambdaPipe}.
	 * @param ctx the parse tree
	 */
	void enterLambdaPipe(M3ParserGrammar.LambdaPipeContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#lambdaPipe}.
	 * @param ctx the parse tree
	 */
	void exitLambdaPipe(M3ParserGrammar.LambdaPipeContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#lambdaParam}.
	 * @param ctx the parse tree
	 */
	void enterLambdaParam(M3ParserGrammar.LambdaParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#lambdaParam}.
	 * @param ctx the parse tree
	 */
	void exitLambdaParam(M3ParserGrammar.LambdaParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#lambdaParamType}.
	 * @param ctx the parse tree
	 */
	void enterLambdaParamType(M3ParserGrammar.LambdaParamTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#lambdaParamType}.
	 * @param ctx the parse tree
	 */
	void exitLambdaParamType(M3ParserGrammar.LambdaParamTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#instanceLiteral}.
	 * @param ctx the parse tree
	 */
	void enterInstanceLiteral(M3ParserGrammar.InstanceLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#instanceLiteral}.
	 * @param ctx the parse tree
	 */
	void exitInstanceLiteral(M3ParserGrammar.InstanceLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#instanceLiteralToken}.
	 * @param ctx the parse tree
	 */
	void enterInstanceLiteralToken(M3ParserGrammar.InstanceLiteralTokenContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#instanceLiteralToken}.
	 * @param ctx the parse tree
	 */
	void exitInstanceLiteralToken(M3ParserGrammar.InstanceLiteralTokenContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#unitInstanceLiteral}.
	 * @param ctx the parse tree
	 */
	void enterUnitInstanceLiteral(M3ParserGrammar.UnitInstanceLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#unitInstanceLiteral}.
	 * @param ctx the parse tree
	 */
	void exitUnitInstanceLiteral(M3ParserGrammar.UnitInstanceLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#arithmeticPart}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticPart(M3ParserGrammar.ArithmeticPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#arithmeticPart}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticPart(M3ParserGrammar.ArithmeticPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#booleanPart}.
	 * @param ctx the parse tree
	 */
	void enterBooleanPart(M3ParserGrammar.BooleanPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#booleanPart}.
	 * @param ctx the parse tree
	 */
	void exitBooleanPart(M3ParserGrammar.BooleanPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#functionVariableExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionVariableExpression(M3ParserGrammar.FunctionVariableExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#functionVariableExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionVariableExpression(M3ParserGrammar.FunctionVariableExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#dsl}.
	 * @param ctx the parse tree
	 */
	void enterDsl(M3ParserGrammar.DslContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#dsl}.
	 * @param ctx the parse tree
	 */
	void exitDsl(M3ParserGrammar.DslContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#dslNavigationPath}.
	 * @param ctx the parse tree
	 */
	void enterDslNavigationPath(M3ParserGrammar.DslNavigationPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#dslNavigationPath}.
	 * @param ctx the parse tree
	 */
	void exitDslNavigationPath(M3ParserGrammar.DslNavigationPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#dslGraphFetch}.
	 * @param ctx the parse tree
	 */
	void enterDslGraphFetch(M3ParserGrammar.DslGraphFetchContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#dslGraphFetch}.
	 * @param ctx the parse tree
	 */
	void exitDslGraphFetch(M3ParserGrammar.DslGraphFetchContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#dslContent}.
	 * @param ctx the parse tree
	 */
	void enterDslContent(M3ParserGrammar.DslContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#dslContent}.
	 * @param ctx the parse tree
	 */
	void exitDslContent(M3ParserGrammar.DslContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#type}.
	 * @param ctx the parse tree
	 */
	void enterType(M3ParserGrammar.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#type}.
	 * @param ctx the parse tree
	 */
	void exitType(M3ParserGrammar.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#multiplicity}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicity(M3ParserGrammar.MultiplicityContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#multiplicity}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicity(M3ParserGrammar.MultiplicityContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#fromMultiplicity}.
	 * @param ctx the parse tree
	 */
	void enterFromMultiplicity(M3ParserGrammar.FromMultiplicityContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#fromMultiplicity}.
	 * @param ctx the parse tree
	 */
	void exitFromMultiplicity(M3ParserGrammar.FromMultiplicityContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#toMultiplicity}.
	 * @param ctx the parse tree
	 */
	void enterToMultiplicity(M3ParserGrammar.ToMultiplicityContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#toMultiplicity}.
	 * @param ctx the parse tree
	 */
	void exitToMultiplicity(M3ParserGrammar.ToMultiplicityContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#functionTypePureType}.
	 * @param ctx the parse tree
	 */
	void enterFunctionTypePureType(M3ParserGrammar.FunctionTypePureTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#functionTypePureType}.
	 * @param ctx the parse tree
	 */
	void exitFunctionTypePureType(M3ParserGrammar.FunctionTypePureTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#typeAndMultiplicityParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeAndMultiplicityParameters(M3ParserGrammar.TypeAndMultiplicityParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#typeAndMultiplicityParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeAndMultiplicityParameters(M3ParserGrammar.TypeAndMultiplicityParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#typeParametersWithContravarianceAndMultiplicityParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParametersWithContravarianceAndMultiplicityParameters(M3ParserGrammar.TypeParametersWithContravarianceAndMultiplicityParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#typeParametersWithContravarianceAndMultiplicityParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParametersWithContravarianceAndMultiplicityParameters(M3ParserGrammar.TypeParametersWithContravarianceAndMultiplicityParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#typeParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameters(M3ParserGrammar.TypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#typeParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameters(M3ParserGrammar.TypeParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(M3ParserGrammar.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(M3ParserGrammar.TypeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#contravarianceTypeParameters}.
	 * @param ctx the parse tree
	 */
	void enterContravarianceTypeParameters(M3ParserGrammar.ContravarianceTypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#contravarianceTypeParameters}.
	 * @param ctx the parse tree
	 */
	void exitContravarianceTypeParameters(M3ParserGrammar.ContravarianceTypeParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#contravarianceTypeParameter}.
	 * @param ctx the parse tree
	 */
	void enterContravarianceTypeParameter(M3ParserGrammar.ContravarianceTypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#contravarianceTypeParameter}.
	 * @param ctx the parse tree
	 */
	void exitContravarianceTypeParameter(M3ParserGrammar.ContravarianceTypeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#multiplicityArguments}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicityArguments(M3ParserGrammar.MultiplicityArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#multiplicityArguments}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicityArguments(M3ParserGrammar.MultiplicityArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#multiplicityArgument}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicityArgument(M3ParserGrammar.MultiplicityArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#multiplicityArgument}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicityArgument(M3ParserGrammar.MultiplicityArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#typeArguments}.
	 * @param ctx the parse tree
	 */
	void enterTypeArguments(M3ParserGrammar.TypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#typeArguments}.
	 * @param ctx the parse tree
	 */
	void exitTypeArguments(M3ParserGrammar.TypeArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#multiplictyParameters}.
	 * @param ctx the parse tree
	 */
	void enterMultiplictyParameters(M3ParserGrammar.MultiplictyParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#multiplictyParameters}.
	 * @param ctx the parse tree
	 */
	void exitMultiplictyParameters(M3ParserGrammar.MultiplictyParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(M3ParserGrammar.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(M3ParserGrammar.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#packagePath}.
	 * @param ctx the parse tree
	 */
	void enterPackagePath(M3ParserGrammar.PackagePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#packagePath}.
	 * @param ctx the parse tree
	 */
	void exitPackagePath(M3ParserGrammar.PackagePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link M3ParserGrammar#word}.
	 * @param ctx the parse tree
	 */
	void enterWord(M3ParserGrammar.WordContext ctx);
	/**
	 * Exit a parse tree produced by {@link M3ParserGrammar#word}.
	 * @param ctx the parse tree
	 */
	void exitWord(M3ParserGrammar.WordContext ctx);
}