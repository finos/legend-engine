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

package org.finos.legend.engine.repl.autocomplete.handlers;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.repl.autocomplete.Completer;
import org.finos.legend.engine.repl.autocomplete.CompletionItem;
import org.finos.legend.engine.repl.autocomplete.FunctionHandler;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;

import static org.finos.legend.engine.repl.autocomplete.Completer.proposeColumnNamesForEditColSpec;
import static org.finos.legend.engine.repl.autocomplete.handlers.ExtendHandler.updateColSpecs;

public class PivotHandler extends FunctionHandler
{
    @Override
    public String functionName()
    {
        return "pivot";
    }

    @Override
    public MutableList<CompletionItem> proposedParameters(AppliedFunction currentFunc, GenericType leftType, PureModel pureModel, Completer completer, ProcessingContext processingContext, ValueSpecification currentVS)
    {
        return proposeColumnNamesForEditColSpec(currentFunc, leftType);
    }

    @Override
    public void handleFunctionAppliedParameters(AppliedFunction currentFunc, GenericType leftType, ProcessingContext processingContext, PureModel pureModel)
    {
        if (currentFunc.parameters.size() > 2 && currentFunc.parameters.get(2) instanceof ClassInstance)
        {
            Object aggExpressions = ((ClassInstance) currentFunc.parameters.get(2)).value;
            updateColSpecs(aggExpressions, leftType, processingContext, pureModel);

            try
            {
                if (aggExpressions instanceof ColSpec)
                {
                    processSecondLambda((ColSpec) aggExpressions, processingContext, pureModel);
                }
                if (aggExpressions instanceof ColSpecArray)
                {
                    ListIterate.forEach(((ColSpecArray) aggExpressions).colSpecs, x -> processSecondLambda(x, processingContext, pureModel));
                }
            }
            catch (Exception e)
            {
                // Do nothing
                // The first Lambda may not be complete yet which would throw a compilation exception, which is fine
            }
        }
    }

    private void processSecondLambda(ColSpec colSpec, ProcessingContext processingContext, PureModel pureModel)
    {
        InstanceValue iv = (InstanceValue) colSpec.function1.accept(new ValueSpecificationBuilder(new CompileContext.Builder(pureModel).build(), Lists.mutable.empty(), processingContext));
        LambdaFunction<?> lambdaFunction = (LambdaFunction<?>) iv._values().getFirst();
        FunctionType fType = (FunctionType) lambdaFunction._classifierGenericType()._typeArguments().getFirst()._rawType();

        Variable variable = colSpec.function2.parameters.get(0);
        processingContext.addInferredVariables(variable.name, buildTypedVariable(variable, fType._returnType(), pureModel.getMultiplicity("zeroMany"), pureModel));
    }
}

