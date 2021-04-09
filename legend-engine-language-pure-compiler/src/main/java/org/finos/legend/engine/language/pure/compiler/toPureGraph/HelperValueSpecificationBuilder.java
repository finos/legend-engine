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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.AnalyticsExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.pure.generated.Root_meta_pure_graphFetch_PropertyGraphFetchTree_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_graphFetch_RootGraphFetchTree_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_LambdaFunction_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_router_analytics_AnalyticsExecutionContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpressionAccessor;

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
        MutableList<String> openVariables = Lists.mutable.empty();
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> valueSpecifications = ListIterate.collect(expressions, p -> p.accept(new ValueSpecificationBuilder(context, openVariables, ctx)));

        // Remove Lambda parameters from openVariables
        MutableList<String> cleanedOpenVariables = openVariables.distinct();
        ListIterate.forEach(pureParameters, p -> ctx.flushVariable(p._name()));
        cleanedOpenVariables.removeAll(pureParameters.collect(VariableExpressionAccessor::_name));

        // Remove let variables
        List<String> lets = valueSpecifications.collect(v -> v instanceof FunctionExpression && "letFunction".equals(((FunctionExpression) v)._functionName()) ? ((InstanceValue) ((FunctionExpression) v)._parametersValues().getFirst())._values().getFirst().toString() : "");
        cleanedOpenVariables.removeAll(lets);

        GenericType functionType = PureModel.buildFunctionType(pureParameters, valueSpecifications.getLast()._genericType(), valueSpecifications.getLast()._multiplicity());
        ctx.removeLastVariableLevel();
        ctx.pop();
        return new Root_meta_pure_metamodel_function_LambdaFunction_Impl<>(lambdaId)
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(context.pureModel.getType("meta::pure::metamodel::function::LambdaFunction"))._typeArguments(FastList.newListWith(functionType)))
                ._openVariables(cleanedOpenVariables)
                ._expressionSequence(valueSpecifications);
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification processProperty(CompileContext context, MutableList<String> openVariables, ProcessingContext processingContext, List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> parameters, String property, SourceInformation sourceInformation)
    {
        // for X.property. X is the first parameter
        processingContext.push("Processing property " + property);
        org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification firstParameter = parameters.get(0);

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType genericType;
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity multiplicity;
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification inferredVariable;
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification result;

        if (firstParameter instanceof org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum)
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity m = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity();
            m.lowerBound = 1;
            m.setUpperBound(1);
            CString enumValue = new CString();
            enumValue.values = Lists.mutable.of(property);
            enumValue.multiplicity = m;
            context.resolveEnumValue(((Enum) firstParameter).fullPath, enumValue.values.get(0), ((Enum) firstParameter).sourceInformation, sourceInformation); // validation to make sure the enum value can be found
            AppliedFunction extractEnum = new AppliedFunction();
            extractEnum.function = "extractEnumValue";
            extractEnum.parameters = Lists.mutable.of(firstParameter, enumValue);
            result = extractEnum.accept(new ValueSpecificationBuilder(context, openVariables, processingContext));
        }
        else
        {
            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> processedParameters = ListIterate.collect(parameters, p -> p.accept(new ValueSpecificationBuilder(context, openVariables, processingContext)));

            if (firstParameter instanceof Variable)
            {
                inferredVariable = processingContext.getInferredVariable(((Variable) firstParameter).name);
            }
            else
            {
                inferredVariable = processedParameters.get(0);
            }

            Type inferredType = inferredVariable._genericType()._rawType();

            if (!(inferredType instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class)) //is an enum
            {
                inferredType = context.pureModel.getType("meta::pure::metamodel::type::Enum");
            }

            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty<?> foundProperty = findProperty(context, (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) inferredType, parameters, property, sourceInformation);

            if (foundProperty instanceof Property)
            {
                genericType = (foundProperty._classifierGenericType()._typeArguments()).getLast();
                multiplicity = foundProperty._multiplicity();
            }
            else if (foundProperty instanceof QualifiedProperty)
            {
                FunctionType fType = (FunctionType) foundProperty._classifierGenericType()._typeArguments().getFirst()._rawType();
                genericType = fType._returnType();
                multiplicity = fType._returnMultiplicity();
            }
            else
            {
                throw new UnsupportedOperationException("Unhandled property: " + foundProperty);
            }

            // FIXME: remove this when we cleanup the extension method designed specifically for flatdata
            Type finalInferredType = inferredType;
            LazyIterate.flatCollect(context.getCompilerExtensions().getExtensions(), CompilerExtension::DEPRECATED_getExtraInferredTypeProcessors).forEach(processor -> processor.value(
                    finalInferredType, firstParameter, context, processingContext, parameters, property, genericType, processedParameters
            ));

            if (!inferredVariable._multiplicity().getName().equals("PureOne")) // autoMap
            {
                processingContext.push("Building Automap for  " + property);

                org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity m = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity();
                m.lowerBound = 1;
                m.setUpperBound(1);
                List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> localParameters = Lists.mutable.ofAll(parameters);
                final String automapName = "v_automap";

                Lambda automapLambda = new Lambda();
                AppliedProperty appliedProperty = new AppliedProperty();
                appliedProperty.property = property;
                Variable automapvar = new Variable();
                Variable automaLambdaparam = new Variable();
                automapvar.name = automapName;
                appliedProperty.parameters = Lists.mutable.of(automapvar);
                if (!localParameters.isEmpty())
                {
                    localParameters.remove(0);
                }
                appliedProperty.parameters.addAll(localParameters);
                automaLambdaparam.name = automapName;
                automaLambdaparam._class = HelperModelBuilder.getElementFullPath(inferredVariable._genericType()._rawType(), context.pureModel.getExecutionSupport());
                automaLambdaparam.multiplicity = m;
                automapLambda.body = Lists.mutable.of(appliedProperty);

                List<Variable> lambdaParams = new FastList<>();
                lambdaParams.add(automaLambdaparam);
                automapLambda.parameters = lambdaParams;
                List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> newParams = Lists.mutable.of(parameters.get(0), automapLambda);
                result = context.buildFunctionExpression("map", null, newParams, openVariables, null, processingContext).getOne();
                processingContext.pop();
            }
            else
            {
                result = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                        ._func(foundProperty)
                        ._propertyName(new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")._values(Lists.fixedSize.of(foundProperty.getName())))
                        ._genericType(genericType)
                        ._multiplicity(multiplicity)
                        ._parametersValues(processedParameters);
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

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext processExecutionContext(ExecutionContext executionContext, CompileContext context)
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
            return new Root_meta_pure_router_analytics_AnalyticsExecutionContext_Impl("")
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

    public static GraphFetchTree buildGraphFetchTree(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.GraphFetchTree graphFetchTree, CompileContext context, Class<?> parentClass, MutableList<String> openVariables, ProcessingContext processingContext)
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
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> pureParameters = Lists.mutable.empty();
        if (!propertyGraphFetchTree.parameters.isEmpty())
        {
            Variable thisVariable = new Variable("this", HelperModelBuilder.getElementFullPath(parentClass, context.pureModel.getExecutionSupport()), new Multiplicity(1, 1));
            property = HelperModelBuilder.getAppliedProperty(context, parentClass, Optional.of(Lists.mutable.<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification>with(thisVariable).withAll(propertyGraphFetchTree.parameters)), propertyGraphFetchTree.property);

            processingContext.push("PropertyTree");
            processingContext.addInferredVariables("this", HelperModelBuilder.createThisVariableForClass(context, HelperModelBuilder.getElementFullPath(parentClass, context.pureModel.getExecutionSupport())));
            pureParameters = ListIterate.collect(propertyGraphFetchTree.parameters, x -> x.accept(new ValueSpecificationBuilder(context, openVariables, processingContext)));
            processingContext.flushVariable("this");
            processingContext.pop();
        }
        else
        {
            property = HelperModelBuilder.getAppliedProperty(context, parentClass, Optional.empty(), propertyGraphFetchTree.property, propertyGraphFetchTree.sourceInformation);
        }
        Class<?> subType = propertyGraphFetchTree.subType == null ? null : context.resolveClass(propertyGraphFetchTree.subType, propertyGraphFetchTree.sourceInformation);
        Type returnType = subType == null ? property._genericType()._rawType() : subType;

        ListIterable<GraphFetchTree> children = ListIterate.collect(propertyGraphFetchTree.subTrees, subTree -> buildGraphFetchTree(subTree, context, (Class<?>) returnType, openVariables, processingContext));
        return new Root_meta_pure_graphFetch_PropertyGraphFetchTree_Impl("")
                ._property(property)
                ._parameters(pureParameters)
                ._alias(propertyGraphFetchTree.alias)
                ._subType(subType)
                ._subTrees(children);
    }

    private static GraphFetchTree buildRootGraphFetchTree(RootGraphFetchTree rootGraphFetchTree, CompileContext context, Class<?> parentClass, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        Class<?> _class = context.resolveClass(rootGraphFetchTree._class, rootGraphFetchTree.sourceInformation);
        ListIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree> children = ListIterate.collect(rootGraphFetchTree.subTrees, subTree -> buildGraphFetchTree(subTree, context, _class, openVariables, processingContext));
        return new Root_meta_pure_graphFetch_RootGraphFetchTree_Impl<>("")
                ._class(_class)
                ._subTrees(children);
    }
}