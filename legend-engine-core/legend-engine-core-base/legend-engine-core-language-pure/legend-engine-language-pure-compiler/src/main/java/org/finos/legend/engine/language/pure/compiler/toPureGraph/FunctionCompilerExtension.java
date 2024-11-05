// Copyright 2024 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.UserDefinedFunctionHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.TypeAndMultiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_ConcreteFunctionDefinition_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.slf4j.Logger;

import java.util.List;

public class FunctionCompilerExtension implements CompilerExtension
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FunctionCompilerExtension.class);

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Function");
    }

    @Override
    public CompilerExtension build()
    {
        return new FunctionCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        Function.class,
                        Lists.fixedSize.with(DataElement.class, Class.class, Association.class, Mapping.class, Binding.class),
                        this::functionFirstPass,
                        (Function function, CompileContext context) ->
                        {
                        },
                        this::functionThirdPass
                )
        );
    }

    private PackageableElement functionFirstPass(Function function, CompileContext context)
    {
        // NOTE: in the protocol, we still store the function name as is, but in the function index, we will store the function based on its function signature
        String functionSignature = HelperModelBuilder.getSignature(function);
        String functionFullName = context.pureModel.buildPackageString(function._package, functionSignature);
        String functionName = context.pureModel.buildPackageString(function._package, HelperModelBuilder.getFunctionNameWithoutSignature(function));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> targetFunc = new Root_meta_pure_metamodel_function_ConcreteFunctionDefinition_Impl<>(functionSignature, SourceInformationHelper.toM3SourceInformation(function.sourceInformation), null);

        ProcessingContext ctx = new ProcessingContext("Function '" + functionFullName + "' First Pass");

        context.pureModel.setNameAndPackage(targetFunc, functionSignature, function._package, function.sourceInformation)
                ._functionName(functionName) // function name to be used in the handler map -> meta::pure::functions::date::isAfterDay
                ._classifierGenericType(context.newGenericType(context.pureModel.getType("meta::pure::metamodel::function::ConcreteFunctionDefinition"), PureModel.buildFunctionType(ListIterate.collect(function.parameters, p -> (VariableExpression) p.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), ctx))), context.newGenericType(function.returnGenericType), context.pureModel.getMultiplicity(function.returnMultiplicity), context.pureModel)))
                ._stereotypes(ListIterate.collect(function.stereotypes, context::resolveStereotype))
                ._taggedValues(ListIterate.collect(function.taggedValues, context::newTaggedValue));
        HelperModelBuilder.processFunctionConstraints(function, context, targetFunc, ctx);

        context.pureModel.handlers.register(new UserDefinedFunctionHandler(context.pureModel, functionFullName, targetFunc,
                ps -> new TypeAndMultiplicity(context.newGenericType(function.returnGenericType), context.pureModel.getMultiplicity(function.returnMultiplicity)),
                ps ->
                {
                    List<ValueSpecification> vs = ListIterate.collect(function.parameters, p -> p.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), ctx)));
                    if (ps.size() == function.parameters.size())
                    {
                        int size = ps.size();
                        // TODO clean up the check....
                        try
                        {
                            for (int i = 0; i < size; i++)
                            {
                                HelperModelBuilder.checkCompatibility(context, ps.get(i)._genericType()._rawType(), ps.get(i)._multiplicity(), vs.get(i)._genericType()._rawType(), vs.get(i)._multiplicity(), "Error in function '" + functionFullName + "'", function.body.get(function.body.size() - 1).sourceInformation);
                            }
                        }
                        catch (Exception e)
                        {
                            return false;
                        }
                        return true;
                    }
                    return false;
                }));
        return targetFunc;
    }

    private void functionThirdPass(Function function, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> targetFunc = context.pureModel.getConcreteFunctionDefinition(function);
        String packageString = context.pureModel.buildPackageString(function._package, targetFunc._name());
        ProcessingContext ctx = new ProcessingContext("Function '" + packageString + "' Third Pass");
        MutableList<ValueSpecification> body;
        try
        {
            function.parameters.forEach(p -> p.accept(new ValueSpecificationBuilder(context, org.eclipse.collections.api.factory.Lists.mutable.empty(), ctx)));
            body = ListIterate.collect(function.body, expression -> expression.accept(new ValueSpecificationBuilder(context, org.eclipse.collections.api.factory.Lists.mutable.empty(), ctx)));
        }
        catch (Exception e)
        {
            LOGGER.warn(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.GRAPH_EXPRESSION_ERROR, "Can't build function '" + packageString + "' - stack: " + ctx.getStack()).toString());
            throw e;
        }
        FunctionType fType = ((FunctionType) targetFunc._classifierGenericType()._typeArguments().getFirst()._rawType());
        HelperModelBuilder.checkCompatibility(context, body.getLast()._genericType()._rawType(), body.getLast()._multiplicity(), fType._returnType()._rawType(), fType._returnMultiplicity(), "Error in function '" + packageString + "'", function.body.get(function.body.size() - 1).sourceInformation);
        ctx.pop();
        targetFunc._expressionSequence(body);
        HelperFunctionBuilder.processFunctionSuites(function, targetFunc, context, ctx);
    }
}
