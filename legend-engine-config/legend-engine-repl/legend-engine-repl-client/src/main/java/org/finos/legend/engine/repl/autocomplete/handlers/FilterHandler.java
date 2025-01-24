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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.repl.autocomplete.FunctionHandler;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;

public class FilterHandler extends FunctionHandler
{
    @Override
    public String functionName()
    {
        return "filter";
    }

    @Override
    public void handleFunctionAppliedParameters(AppliedFunction currentFunc, GenericType leftType, ProcessingContext processingContext, PureModel pureModel)
    {
        if (currentFunc.parameters.size() == 2 && currentFunc.parameters.get(1) instanceof Lambda)
        {
            // Specific inference for filter
            Lambda lambda = (Lambda) currentFunc.parameters.get(1);
            Variable variable = lambda.parameters.get(0);

            // Filter for 'Relation' has a different type propagation to parameter
            GenericType propertyType = leftType;
            if (org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(leftType._rawType(), pureModel.getType(M3Paths.Relation), pureModel.getExecutionSupport().getProcessorSupport()))
            {
                propertyType = leftType._typeArguments().getFirst();
            }

            processingContext.addInferredVariables(variable.name, buildTypedVariable(variable, propertyType, pureModel.getMultiplicity("one"), pureModel));
        }
    }
}
