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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;
import org.finos.legend.engine.repl.autocomplete.Completer;
import org.finos.legend.engine.repl.autocomplete.CompletionItem;
import org.finos.legend.engine.repl.autocomplete.FunctionHandler;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;

public class AsOfJoinHandler extends FunctionHandler
{
    @Override
    public String functionName()
    {
        return "asOfJoin";
    }

    @Override
    public MutableList<CompletionItem> proposedParameters(AppliedFunction currentFunc, GenericType leftType, PureModel pureModel, Completer completer, ProcessingContext processingContext, ValueSpecification currentVS)
    {
        if (currentFunc.parameters.size() == 2)
        {
            return completer.processValueSpecification(currentFunc.parameters.get(1), currentVS, pureModel, processingContext).getCompletion();
        }
        return Lists.mutable.empty();
    }

    public void handleFunctionAppliedParameters(AppliedFunction currentFunc, GenericType leftType, ProcessingContext processingContext, PureModel pureModel)
    {
        if (currentFunc.parameters.size() == 3)
        {
            processLambda(currentFunc, currentFunc.parameters.get(2), leftType, processingContext, pureModel);
        }
        if (currentFunc.parameters.size() == 4)
        {
            processLambda(currentFunc, currentFunc.parameters.get(3), leftType, processingContext, pureModel);
        }
    }

    private static void processLambda(AppliedFunction currentFunc, ValueSpecification value, GenericType leftType, ProcessingContext processingContext, PureModel pureModel)
    {
        GenericType genericType = currentFunc.parameters.get(1).accept(new ValueSpecificationBuilder(new CompileContext.Builder(pureModel).build(), Lists.mutable.empty(), processingContext))._genericType()._typeArguments().getFirst();
        if (value instanceof Lambda)
        {
            processLambda((Lambda)value, leftType._typeArguments().getFirst(), genericType, processingContext, pureModel);
        }
    }


    private static void processLambda(Lambda lambda, GenericType first, GenericType second, ProcessingContext processingContext, PureModel pureModel)
    {
        if (lambda != null)
        {
            Variable f_variable = lambda.parameters.get(0);
            processingContext.addInferredVariables(f_variable.name, buildTypedVariable(f_variable, first, pureModel.getMultiplicity("one"), pureModel));
            Variable s_variable = lambda.parameters.get(1);
            processingContext.addInferredVariables(s_variable.name, buildTypedVariable(s_variable, second, pureModel.getMultiplicity("one"), pureModel));
        }
    }

}
