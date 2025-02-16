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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.ClassInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.repl.autocomplete.Completer;
import org.finos.legend.engine.repl.autocomplete.CompletionItem;
import org.finos.legend.engine.repl.autocomplete.FunctionHandler;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;

import static org.finos.legend.engine.repl.autocomplete.Completer.proposeColumnNamesForEditColSpec;
import static org.finos.legend.engine.repl.autocomplete.handlers.GroupByHandler.updateColSpecSecondFunction;

public class ExtendHandler extends FunctionHandler
{
    @Override
    public String functionName()
    {
        return "extend";
    }

    @Override
    public MutableList<CompletionItem> proposedParameters(AppliedFunction currentFunc, GenericType leftType, PureModel pureModel, Completer completer, ProcessingContext processingContext, ValueSpecification currentVS)
    {
        if (currentFunc.parameters.size() == 2 && currentFunc.parameters.get(1) instanceof AppliedFunction && "over".equals(((AppliedFunction) currentFunc.parameters.get(1)).function))
        {
            AppliedFunction over = (AppliedFunction) currentFunc.parameters.get(1);
            if (!over.parameters.isEmpty())
            {
                Object parameter = over.parameters.get(over.parameters.size() - 1);
                if (parameter instanceof ClassInstance)
                {
                    return proposeColumnNamesForEditColSpec(((ClassInstance) parameter).value, leftType);
                }
                else if (parameter instanceof Collection)
                {
                    Object lastValue = ((Collection) parameter).values.get(((Collection) parameter).values.size() - 1);
                    if (lastValue instanceof ClassInstance)
                    {
                        return proposeColumnNamesForEditColSpec(((ClassInstance) lastValue).value, leftType);
                    }
                }
            }
        }
        return Lists.mutable.empty();
    }

    @Override
    public void handleFunctionAppliedParameters(AppliedFunction currentFunc, GenericType leftType, ProcessingContext processingContext, PureModel pureModel)
    {
        if (currentFunc.parameters.size() > 1 && currentFunc.parameters.get(currentFunc.parameters.size() - 1) instanceof ClassInstance)
        {
            Object val = ((ClassInstance) currentFunc.parameters.get(currentFunc.parameters.size() - 1)).value;
            updateColSpecFirstFunction(val, leftType, processingContext, pureModel);
            updateColSpecSecondFunction(processingContext, pureModel, val);
        }
    }

    public static void updateColSpecFirstFunction(Object o, GenericType leftType, ProcessingContext processingContext, PureModel pureModel)
    {
        GenericType propertyType = leftType._typeArguments().getFirst();

        if (o instanceof ColSpec)
        {
            processColSpecFirstFunction((ColSpec) o, propertyType, processingContext, pureModel);
        }
        if (o instanceof ColSpecArray)
        {
            ListIterate.forEach(((ColSpecArray) o).colSpecs, x -> processColSpecFirstFunction(x, propertyType, processingContext, pureModel));
        }
    }

    private static void processColSpecFirstFunction(ColSpec o, GenericType propertyType, ProcessingContext processingContext, PureModel pureModel)
    {
        LambdaFunction lambda = o.function1;
        if (lambda != null)
        {
            if (lambda.parameters.size() > 1)
            {
                Variable p = lambda.parameters.get(0);
                processingContext.addInferredVariables(p.name, buildTypedVariable(p, pureModel.getGenericType(M3Paths.Relation), pureModel.getMultiplicity("one"), pureModel));
                Variable f = lambda.parameters.get(1);
                processingContext.addInferredVariables(f.name, buildTypedVariable(f, pureModel.getGenericType("meta::pure::functions::relation::Frame"), pureModel.getMultiplicity("one"), pureModel));
            }
            Variable variable = lambda.parameters.get(lambda.parameters.size() - 1);
            processingContext.addInferredVariables(variable.name, buildTypedVariable(variable, propertyType, pureModel.getMultiplicity("one"), pureModel));
        }
    }
}