// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.SubTypeGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.AnalyticsExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.TypeParameter;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.*;
import org.finos.legend.pure.m3.navigation.relation._RelationType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HelperValueSpecificationBuilder
{
    public static LambdaFunction<?> buildLambda(Lambda lambda, CompileContext context)
    {
        return buildLambda(lambda.body, lambda.parameters, context);
    }

    public static LambdaFunction<?> buildLambda(List<ValueSpecification> expressions, List<Variable> parameters, CompileContext context)
    {
        return buildLambdaWithContext(expressions, parameters, context, new ProcessingContext("build Lambda"));
    }

    public static LambdaFunction<?> buildLambdaWithContext(Lambda lambda, CompileContext context, ProcessingContext ctx)
    {
        return buildLambdaWithContext(lambda.body, lambda.parameters, context, ctx);
    }

    public static LambdaFunction<?> buildLambdaWithContext(List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> expressions, List<Variable> parameters, CompileContext context, ProcessingContext ctx)
    {
        return buildLambdaWithContext("", expressions, parameters, context, ctx);
    }

    public static LambdaFunction<?> buildLambdaWithContext(String lambdaId, List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> expressions, List<Variable> parameters, CompileContext context, ProcessingContext ctx)
    {
        ctx.push("new lambda");
        ctx.addVariableLevel();
        MutableList<VariableExpression> pureParameters = ListIterate.collect(parameters, p -> (VariableExpression) p.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), ctx)));
        if (parameters.size() != 0 && !parameters.get(0).name.equals("v_automap"))
        {
            if (ctx.milestoningDatePropagationContext.size() == 0)
            {
                ctx.pushMilestoningDatePropagationContext(new MilestoningDatePropagationContext(null, null));
            }
            else
            {
                ctx.pushMilestoningDatePropagationContext(ctx.peekMilestoningDatePropagationContext());
            }
        }
        MutableList<String> openVariables = Lists.mutable.empty();
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> valueSpecifications = ListIterate.collect(expressions, p -> p.accept(new ValueSpecificationBuilder(context, openVariables, ctx)));

        // Remove Lambda parameters from openVariables
        MutableList<String> cleanedOpenVariables = openVariables.distinct();
        ListIterate.forEach(pureParameters, p -> ctx.flushVariable(p._name()));
        cleanedOpenVariables.removeAll(pureParameters.collect(VariableExpressionAccessor::_name));

        // Remove let variables
        List<String> lets = valueSpecifications.collect(v -> v instanceof FunctionExpression && "letFunction".equals(((FunctionExpression) v)._functionName()) ? ((InstanceValue) ((FunctionExpression) v)._parametersValues().getFirst())._values().getFirst().toString() : "");
        cleanedOpenVariables.removeAll(lets);

        GenericType functionType = PureModel.buildFunctionType(pureParameters, valueSpecifications.getLast()._genericType(), valueSpecifications.getLast()._multiplicity(), context.pureModel);
        ctx.removeLastVariableLevel();
        ctx.pop();
        if (parameters.size() != 0 && !parameters.get(0).name.equals("v_automap") && ctx.milestoningDatePropagationContext.size() != 0)
        {
            ctx.popMilestoningDatePropagationContext();
        }

        LambdaFunction lambda = new Root_meta_pure_metamodel_function_LambdaFunction_Impl<>(lambdaId)
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(context.pureModel.getType("meta::pure::metamodel::function::LambdaFunction"))._typeArguments(FastList.newListWith(functionType)))
                ._openVariables(cleanedOpenVariables)
                ._expressionSequence(valueSpecifications);

        return context.getCompilerExtensions().getExtraLambdaPostProcessors().stream()
                .reduce(lambda, (originalLambda, processor) -> processor.value(originalLambda, context, ctx), (p1, p2) -> p1);
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification processProperty(CompileContext context, MutableList<String> openVariables, ProcessingContext processingContext, List<ValueSpecification> parameters, String property, SourceInformation sourceInformation)
    {
        // for X.property. X is the first parameter
        processingContext.push("Processing property " + property);
        ValueSpecification firstArgument = parameters.get(0);
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> processedParameters = ListIterate.collect(parameters, p -> p.accept(new ValueSpecificationBuilder(context, openVariables, processingContext)));

        GenericType genericType;
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity multiplicity;
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification inferredVariable;
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification result;

        if (firstArgument instanceof Enum // Only for backward compatibility!
                || (processedParameters.get(0)._genericType()._rawType().equals(context.pureModel.getType("meta::pure::metamodel::type::Enumeration"))))
        {
            CString enumValue = new CString(property);
            context.resolveEnumValue(((PackageableElementPtr) firstArgument).fullPath, enumValue.value, firstArgument.sourceInformation, sourceInformation); // validation to make sure the enum value can be found
            AppliedFunction extractEnum = new AppliedFunction();
            extractEnum.function = "extractEnumValue";
            extractEnum.parameters = Lists.mutable.of(firstArgument, enumValue);
            result = extractEnum.accept(new ValueSpecificationBuilder(context, openVariables, processingContext));
        }
        else
        {
            if (firstArgument instanceof Variable)
            {
                inferredVariable = processingContext.getInferredVariable(((Variable) firstArgument).name);
            }
            else
            {
                inferredVariable = processedParameters.get(0);
            }

            Type inferredType = inferredVariable._genericType()._rawType();

            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> foundFunction;
            if (inferredType instanceof RelationType)
            {
                foundFunction = ((RelationType<?>) inferredType)._columns().detect(c -> c._name().equals(property));
                if (foundFunction == null)
                {
                    throw new EngineException("The column '" + property + "' can't be found in the relation " + _RelationType.print(inferredType, context.pureModel.getExecutionSupport().getProcessorSupport()), sourceInformation, EngineErrorType.COMPILATION);
                }
                genericType = (foundFunction._classifierGenericType()._typeArguments()).getLast();
                multiplicity = context.pureModel.getMultiplicity("one");
            }
            else
            {
                if (inferredType instanceof PrimitiveType)
                {
                    throw new EngineException("The property '" + property + "' can't be accessed on primitive types. Inferred primitive type is " + inferredType, sourceInformation, EngineErrorType.COMPILATION);
                }

                if (!(inferredType instanceof Class)) //is an enum
                {
                    inferredType = context.pureModel.getType("meta::pure::metamodel::type::Enum");
                }

                foundFunction = findProperty(context, (Class<?>) inferredType, parameters, property, sourceInformation);

                if (foundFunction instanceof Property)
                {
                    genericType = (foundFunction._classifierGenericType()._typeArguments()).getLast();
                    multiplicity = ((Property<?, ?>) foundFunction)._multiplicity();
                }
                else if (foundFunction instanceof QualifiedProperty)
                {
                    FunctionType fType = (FunctionType) foundFunction._classifierGenericType()._typeArguments().getFirst()._rawType();
                    genericType = fType._returnType();
                    multiplicity = fType._returnMultiplicity();
                }
                else
                {
                    throw new UnsupportedOperationException("Unhandled property: " + foundFunction);
                }

                if (genericType._typeParameter() != null)
                {
                    RichIterable<? extends Pair<? extends TypeParameter, ? extends GenericType>> zip = ((Class<?>) inferredType)._typeParameters().zip(inferredVariable._genericType()._typeArguments());
                    for (Pair<? extends TypeParameter, ? extends GenericType> p : zip)
                    {
                        if (p.getOne()._name().equals(genericType._typeParameter()._name()))
                        {
                            genericType = p.getTwo();
                            break;
                        }
                    }
                }

                if (multiplicity._multiplicityParameter() != null)
                {
                    RichIterable<? extends Pair<? extends InstanceValue, ? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity>> zip = ((Class<?>) inferredType)._multiplicityParameters().zip(inferredVariable._genericType()._multiplicityArguments());
                    for (Pair<? extends InstanceValue, ? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity> p : zip)
                    {
                        if (p.getOne()._values().getAny().equals(multiplicity._multiplicityParameter()))
                        {
                            multiplicity = p.getTwo();
                            break;
                        }
                    }
                }
            }

            if (!inferredVariable._multiplicity().getName().equals("PureOne")) // autoMap
            {
                processingContext.push("Building Automap for  " + property);

                List<ValueSpecification> localParameters = Lists.mutable.ofAll(parameters);
                final String automapName = "v_automap";

                Lambda automapLambda = new Lambda();
                AppliedProperty appliedProperty = new AppliedProperty();
                appliedProperty.property = property;
                Variable automapvar = new Variable();
                Variable automaLambdaparam = new Variable();
                automapvar.name = automapName;
                appliedProperty.parameters = Lists.mutable.of(automapvar);
                appliedProperty.sourceInformation = sourceInformation;
                if (!localParameters.isEmpty())
                {
                    localParameters.remove(0);
                }
                appliedProperty.parameters.addAll(localParameters);
                automaLambdaparam.name = automapName;
                automaLambdaparam._class = new PackageableElementPointer(PackageableElementType.CLASS, HelperModelBuilder.getElementFullPath((PackageableElement) inferredVariable._genericType()._rawType(), context.pureModel.getExecutionSupport()));
                automaLambdaparam.multiplicity = Multiplicity.PURE_ONE;
                automapLambda.body = Lists.mutable.of(appliedProperty);

                List<Variable> lambdaParams = new FastList<>();
                lambdaParams.add(automaLambdaparam);
                automapLambda.parameters = lambdaParams;
                List<ValueSpecification> newParams = Lists.mutable.of(parameters.get(0), automapLambda);
                MilestoningDatePropagationHelper.updateMilestoningPropagationContextWhileReprocessingFunctionExpression(processingContext);
                result = context.buildFunctionExpression("map", null, newParams, openVariables, sourceInformation, processingContext).getOne();
                processingContext.pop();
            }
            else
            {
                result = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("", SourceInformationHelper.toM3SourceInformation(sourceInformation), context.pureModel.getClass("meta::pure::metamodel::valuespecification::SimpleFunctionExpression"))
                        ._func(foundFunction)
                        ._propertyName(new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))._values(Lists.fixedSize.of(foundFunction.getName())))
                        ._genericType(genericType)
                        ._multiplicity(multiplicity)
                        ._parametersValues(processedParameters);

                if (foundFunction instanceof AbstractProperty)
                {
                    if (MilestoningDatePropagationHelper.isGeneratedMilestonedQualifiedPropertyWithMissingDates((AbstractProperty<?>) foundFunction, context, parameters.size()))
                    {
                        MilestoningDatePropagationHelper.updateFunctionExpressionWithMilestoningDateParams((FunctionExpression) result, (AbstractProperty<?>) foundFunction, sourceInformation, processingContext);
                    }
                    MilestoningDatePropagationHelper.updateMilestoningContext((AbstractProperty<?>) foundFunction, (ListIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification>) ((SimpleFunctionExpression) result)._parametersValues(), processingContext, context);
                }
            }
        }
        processingContext.pop();
        return result;
    }

    private static AbstractProperty<?> findProperty(CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> aClass, List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> parameters, String name, SourceInformation sourceInformation)
    {
        try
        {
            return HelperModelBuilder.getAppliedProperty(context, aClass, java.util.Optional.of(parameters), name, sourceInformation);
        }
        catch (Exception e)
        {
            return HelperModelBuilder.getOwnedAppliedProperty(context, aClass, name, sourceInformation, context.pureModel.getExecutionSupport());
        }
    }

    public static Root_meta_pure_runtime_ExecutionContext processExecutionContext(ExecutionContext executionContext, CompileContext context)
    {
        if (executionContext instanceof BaseExecutionContext)
        {
            BaseExecutionContext baseExecutionContext = (BaseExecutionContext) executionContext;
            return new Root_meta_pure_runtime_ExecutionContext_Impl(" ")._queryTimeOutInSeconds(baseExecutionContext.queryTimeOutInSeconds)._enableConstraints(baseExecutionContext.enableConstraints);
        }
        if (executionContext instanceof AnalyticsExecutionContext)
        {
            AnalyticsExecutionContext analyticsExecutionContext = (AnalyticsExecutionContext) executionContext;
            LambdaFunction<?> lambda = (LambdaFunction<?>) ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue) analyticsExecutionContext.toFlowSetFunction.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), new ProcessingContext(""))))._values().getFirst();
            return new Root_meta_pure_router_analytics_AnalyticsExecutionContext_Impl("", null, context.pureModel.getClass("meta::pure::router::analytics::AnalyticsExecutionContext"))
                    ._queryTimeOutInSeconds(analyticsExecutionContext.queryTimeOutInSeconds)
                    ._enableConstraints(analyticsExecutionContext.enableConstraints)
                    ._useAnalytics(analyticsExecutionContext.useAnalytics)
                    ._toFlowSetFunction(lambda);

        }
        return context.getCompilerExtensions().getExtraExecutionContextProcessors().stream()
                .map(processor -> processor.value(executionContext, context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported execution context type '" + executionContext.getClass() + "'"));
    }

    public static GraphFetchTree buildGraphFetchTree(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.GraphFetchTree graphFetchTree, CompileContext context, Class<?> parentClass, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        if (graphFetchTree instanceof PropertyGraphFetchTree)
        {
            return buildPropertyGraphFetchTree((PropertyGraphFetchTree) graphFetchTree, context, parentClass, openVariables, processingContext);
        }
        else if (graphFetchTree instanceof RootGraphFetchTree)
        {
            return buildRootGraphFetchTree((RootGraphFetchTree) graphFetchTree, context, parentClass, openVariables, processingContext);
        }
        throw new UnsupportedOperationException("Unsupported graph fetch tree node type '" + graphFetchTree.getClass().getSimpleName() + "'");
    }

    private static GraphFetchTree buildPropertyGraphFetchTree(PropertyGraphFetchTree propertyGraphFetchTree, CompileContext context, Class<?> parentClass, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        AbstractProperty<?> property;
        MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> pureParameters = Lists.mutable.empty();

        Variable thisVariable = new Variable("this", HelperModelBuilder.getElementFullPath(parentClass, context.pureModel.getExecutionSupport()), new Multiplicity(1, 1));
        MutableList<ValueSpecification> originalParams = Lists.mutable.<ValueSpecification>with(thisVariable).withAll(propertyGraphFetchTree.parameters);
        property = HelperModelBuilder.getAppliedProperty(context, parentClass, Optional.of(originalParams), propertyGraphFetchTree.property, propertyGraphFetchTree.sourceInformation);
        processingContext.push("PropertyTree");
        processingContext.addInferredVariables("this", HelperModelBuilder.createThisVariableForClass(context, HelperModelBuilder.getElementFullPath(parentClass, context.pureModel.getExecutionSupport())));
        MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> originalPureParameters = ListIterate.collect(originalParams, x -> x.accept(new ValueSpecificationBuilder(context, openVariables, processingContext)));
        processingContext.flushVariable("this");

        if (MilestoningDatePropagationHelper.isGeneratedMilestonedQualifiedPropertyWithMissingDates(property, context, originalPureParameters.size()))   // add count for property owner parameter
        {
            pureParameters = MilestoningDatePropagationHelper.getMilestoningDatesFromMilestoningContext(property, originalPureParameters, processingContext);
        }
        else
        {
            pureParameters = originalPureParameters;
        }
        MilestoningDatePropagationHelper.updateMilestoningContext(property, pureParameters, processingContext, context);
        processingContext.pop();

        pureParameters.remove(0);        //remove this variable before putting in property tree
        Class<?> subType = propertyGraphFetchTree.subType == null ? null : context.resolveClass(propertyGraphFetchTree.subType, propertyGraphFetchTree.sourceInformation);
        Type returnType = subType == null ? property._genericType()._rawType() : subType;

        ListIterable<GraphFetchTree> children = ListIterate.collect(propertyGraphFetchTree.subTrees, subTree -> buildGraphFetchTree(subTree, context, (Class<?>) returnType, openVariables, processingContext));
        return new Root_meta_pure_graphFetch_PropertyGraphFetchTree_Impl("", SourceInformationHelper.toM3SourceInformation(propertyGraphFetchTree.sourceInformation), context.pureModel.getClass("meta::pure::graphFetch::PropertyGraphFetchTree"))
                ._property(property)
                ._parameters(pureParameters)
                ._alias(propertyGraphFetchTree.alias)
                ._subType(subType)
                ._subTrees(children);
    }

    private static GraphFetchTree buildRootGraphFetchTree(RootGraphFetchTree rootGraphFetchTree, CompileContext context, Class<?> parentClass, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        HashSet<String> subTypeClasses = new HashSet<String>();
        HashSet<String> propertieIdentifiersAtRootLevel = new HashSet<String>();
        for (org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.GraphFetchTree propertyGraphFetchTree : rootGraphFetchTree.subTrees)
        {
            propertieIdentifiersAtRootLevel.add(getPropertyIdentifier((PropertyGraphFetchTree) propertyGraphFetchTree));
        }
        for (SubTypeGraphFetchTree subTypeGraphFetchTree : rootGraphFetchTree.subTypeTrees)
        {
            if (!subTypeClasses.add(subTypeGraphFetchTree.subTypeClass))
            {
                throw new EngineException("There are multiple subTypeTrees having subType " + subTypeGraphFetchTree.subTypeClass + ", Only one is allowed", subTypeGraphFetchTree.sourceInformation, EngineErrorType.COMPILATION);
            }
            for (org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.GraphFetchTree propertyGraphFetchTree : subTypeGraphFetchTree.subTrees)
            {
                String propertyIdentifier = getPropertyIdentifier((PropertyGraphFetchTree) propertyGraphFetchTree);
                if (propertieIdentifiersAtRootLevel.contains(propertyIdentifier))
                {
                    throw new EngineException("Property \"" + propertyIdentifier + "\" is present at root level hence should not be specified at subType level", subTypeGraphFetchTree.sourceInformation, EngineErrorType.COMPILATION);
                }
            }
        }
        Class<?> _class = context.resolveClass(rootGraphFetchTree._class, rootGraphFetchTree.sourceInformation);
        ListIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree> children = ListIterate.collect(rootGraphFetchTree.subTrees, subTree -> buildGraphFetchTree(subTree, context, _class, openVariables, processingContext));
        ListIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.SubTypeGraphFetchTree> subTypeTrees = ListIterate.collect(rootGraphFetchTree.subTypeTrees, subTypeTree -> buildSubTypeGraphFetchTree((SubTypeGraphFetchTree) subTypeTree, context, _class, openVariables, processingContext));
        Class<?> classifier = context.pureModel.getClass("meta::pure::graphFetch::RootGraphFetchTree");
        GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                ._rawType(classifier)
                ._typeArguments(Lists.fixedSize.of(context.pureModel.getGenericType(_class)));
        return new Root_meta_pure_graphFetch_RootGraphFetchTree_Impl<>("", SourceInformationHelper.toM3SourceInformation(rootGraphFetchTree.sourceInformation), classifier)
                ._class(_class)
                ._classifierGenericType(genericType)
                ._subTrees(children)
                ._subTypeTrees(subTypeTrees);
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.SubTypeGraphFetchTree buildSubTypeGraphFetchTree(SubTypeGraphFetchTree subTypeGraphFetchTree, CompileContext context, Class<?> parentClass, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        Class<?> subTypeClass = context.resolveClass(subTypeGraphFetchTree.subTypeClass, subTypeGraphFetchTree.sourceInformation);

        if (!org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(subTypeClass, parentClass, context.pureModel.getExecutionSupport().getProcessorSupport()))
        {
            throw new EngineException("The type " + subTypeClass.getName() + " is not a subtype of " + parentClass.getName(), subTypeGraphFetchTree.sourceInformation, EngineErrorType.COMPILATION);
        }
        ListIterable<GraphFetchTree> children = ListIterate.collect(subTypeGraphFetchTree.subTrees, subTree -> buildGraphFetchTree(subTree, context, subTypeClass, openVariables, processingContext));
        return new Root_meta_pure_graphFetch_SubTypeGraphFetchTree_Impl("", SourceInformationHelper.toM3SourceInformation(subTypeGraphFetchTree.sourceInformation), context.pureModel.getClass("meta::pure::graphFetch::SubTypeGraphFetchTree"))
                ._subTypeClass(subTypeClass)
                ._subTrees(children);
    }

    private static String getPropertyIdentifier(PropertyGraphFetchTree propertyGraphFetchTree)
    {
        return propertyGraphFetchTree.alias != null ? propertyGraphFetchTree.alias : propertyGraphFetchTree.property;
    }
}
