// Generated from C:/Users/lmaur/Documents/GS/legend-engine/legend-engine-language-pure-grammar/src/main/antlr4/org/finos/legend/engine/language/pure/grammar/from/antlr4/core\M3ParserGrammar.g4 by ANTLR 4.9
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link M3ParserGrammar}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface M3ParserGrammarVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(M3ParserGrammar.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(M3ParserGrammar.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#instance}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstance(M3ParserGrammar.InstanceContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#unitInstance}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitInstance(M3ParserGrammar.UnitInstanceContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#unitName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitName(M3ParserGrammar.UnitNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#instancePropertyAssignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstancePropertyAssignment(M3ParserGrammar.InstancePropertyAssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#instanceRightSide}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstanceRightSide(M3ParserGrammar.InstanceRightSideContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#instanceAtomicRightSideScalar}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstanceAtomicRightSideScalar(M3ParserGrammar.InstanceAtomicRightSideScalarContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#instanceAtomicRightSideVector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstanceAtomicRightSideVector(M3ParserGrammar.InstanceAtomicRightSideVectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#instanceAtomicRightSide}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstanceAtomicRightSide(M3ParserGrammar.InstanceAtomicRightSideContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#enumReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumReference(M3ParserGrammar.EnumReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#stereotypeReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStereotypeReference(M3ParserGrammar.StereotypeReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#tagReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTagReference(M3ParserGrammar.TagReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#propertyReturnType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyReturnType(M3ParserGrammar.PropertyReturnTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#codeBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCodeBlock(M3ParserGrammar.CodeBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#programLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgramLine(M3ParserGrammar.ProgramLineContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#equalNotEqual}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualNotEqual(M3ParserGrammar.EqualNotEqualContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#combinedArithmeticOnly}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCombinedArithmeticOnly(M3ParserGrammar.CombinedArithmeticOnlyContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#expressionPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionPart(M3ParserGrammar.ExpressionPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#letExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLetExpression(M3ParserGrammar.LetExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#combinedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCombinedExpression(M3ParserGrammar.CombinedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#expressionOrExpressionGroup}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionOrExpressionGroup(M3ParserGrammar.ExpressionOrExpressionGroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#expressionsArray}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionsArray(M3ParserGrammar.ExpressionsArrayContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#propertyOrFunctionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyOrFunctionExpression(M3ParserGrammar.PropertyOrFunctionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#propertyExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyExpression(M3ParserGrammar.PropertyExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#propertyBracketExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyBracketExpression(M3ParserGrammar.PropertyBracketExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#functionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionExpression(M3ParserGrammar.FunctionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#functionExpressionLatestMilestoningDateParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionExpressionLatestMilestoningDateParameter(M3ParserGrammar.FunctionExpressionLatestMilestoningDateParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#functionExpressionParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionExpressionParameters(M3ParserGrammar.FunctionExpressionParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#atomicExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtomicExpression(M3ParserGrammar.AtomicExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#instanceReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstanceReference(M3ParserGrammar.InstanceReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#lambdaFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaFunction(M3ParserGrammar.LambdaFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(M3ParserGrammar.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#allOrFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllOrFunction(M3ParserGrammar.AllOrFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#allFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllFunction(M3ParserGrammar.AllFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#allVersionsFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllVersionsFunction(M3ParserGrammar.AllVersionsFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#allVersionsInRangeFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllVersionsInRangeFunction(M3ParserGrammar.AllVersionsInRangeFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#allFunctionWithMilestoning}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllFunctionWithMilestoning(M3ParserGrammar.AllFunctionWithMilestoningContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#buildMilestoningVariableExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBuildMilestoningVariableExpression(M3ParserGrammar.BuildMilestoningVariableExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#expressionInstance}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionInstance(M3ParserGrammar.ExpressionInstanceContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#expressionInstanceRightSide}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionInstanceRightSide(M3ParserGrammar.ExpressionInstanceRightSideContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#expressionInstanceAtomicRightSide}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionInstanceAtomicRightSide(M3ParserGrammar.ExpressionInstanceAtomicRightSideContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#expressionInstanceParserPropertyAssignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionInstanceParserPropertyAssignment(M3ParserGrammar.ExpressionInstanceParserPropertyAssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#sliceExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSliceExpression(M3ParserGrammar.SliceExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#notExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpression(M3ParserGrammar.NotExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#signedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignedExpression(M3ParserGrammar.SignedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#lambdaPipe}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaPipe(M3ParserGrammar.LambdaPipeContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#lambdaParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaParam(M3ParserGrammar.LambdaParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#lambdaParamType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaParamType(M3ParserGrammar.LambdaParamTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#instanceLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstanceLiteral(M3ParserGrammar.InstanceLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#instanceLiteralToken}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstanceLiteralToken(M3ParserGrammar.InstanceLiteralTokenContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#unitInstanceLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitInstanceLiteral(M3ParserGrammar.UnitInstanceLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#arithmeticPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticPart(M3ParserGrammar.ArithmeticPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#booleanPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanPart(M3ParserGrammar.BooleanPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#functionVariableExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionVariableExpression(M3ParserGrammar.FunctionVariableExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#dsl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDsl(M3ParserGrammar.DslContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#dslNavigationPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDslNavigationPath(M3ParserGrammar.DslNavigationPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#dslGraphFetch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDslGraphFetch(M3ParserGrammar.DslGraphFetchContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#dslContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDslContent(M3ParserGrammar.DslContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(M3ParserGrammar.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#multiplicity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicity(M3ParserGrammar.MultiplicityContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#fromMultiplicity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromMultiplicity(M3ParserGrammar.FromMultiplicityContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#toMultiplicity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToMultiplicity(M3ParserGrammar.ToMultiplicityContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#functionTypePureType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTypePureType(M3ParserGrammar.FunctionTypePureTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#typeAndMultiplicityParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAndMultiplicityParameters(M3ParserGrammar.TypeAndMultiplicityParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#typeParametersWithContravarianceAndMultiplicityParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParametersWithContravarianceAndMultiplicityParameters(M3ParserGrammar.TypeParametersWithContravarianceAndMultiplicityParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#typeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameters(M3ParserGrammar.TypeParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(M3ParserGrammar.TypeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#contravarianceTypeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContravarianceTypeParameters(M3ParserGrammar.ContravarianceTypeParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#contravarianceTypeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContravarianceTypeParameter(M3ParserGrammar.ContravarianceTypeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#multiplicityArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicityArguments(M3ParserGrammar.MultiplicityArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#multiplicityArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicityArgument(M3ParserGrammar.MultiplicityArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#typeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArguments(M3ParserGrammar.TypeArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#multiplictyParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplictyParameters(M3ParserGrammar.MultiplictyParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(M3ParserGrammar.QualifiedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#packagePath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackagePath(M3ParserGrammar.PackagePathContext ctx);
	/**
	 * Visit a parse tree produced by {@link M3ParserGrammar#word}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWord(M3ParserGrammar.WordContext ctx);
}