// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.Dispatch;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.ReturnInference;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.TypeAndMultiplicity;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;

/**
 * Makes a function of a baseline Pure graph callable from a model compiled as a layer on it, the way the compiler treats
 * the model's own functions: the declared return type, and a call matches when every argument is compatible with its parameter.
 */
public class BaselineFunctionHandler extends FunctionHandler
{
    public BaselineFunctionHandler(PureModel pureModel, String functionName, Function<?> func)
    {
        super(pureModel, null, PackageableElement.getUserPathForPackageableElement(func), functionName, func instanceof NativeFunction, declaredReturn(func), null, parametersMatch(pureModel, func));
        initialize(func);
    }

    private static FunctionType functionType(Function<?> func)
    {
        return (FunctionType) func._classifierGenericType()._typeArguments().getFirst()._rawType();
    }

    private static ReturnInference declaredReturn(Function<?> func)
    {
        FunctionType functionType = functionType(func);
        return parameters -> new TypeAndMultiplicity(functionType._returnType(), functionType._returnMultiplicity());
    }

    private static Dispatch parametersMatch(PureModel pureModel, Function<?> func)
    {
        ListIterable<? extends VariableExpression> signature = functionType(func)._parameters().toList();
        String errorStub = "Error in function '" + PackageableElement.getUserPathForPackageableElement(func) + "'";
        return parameters ->
        {
            if (parameters.size() != signature.size())
            {
                return false;
            }
            CompileContext context = new CompileContext.Builder(pureModel).build();
            try
            {
                for (int i = 0; i < parameters.size(); i++)
                {
                    HelperModelBuilder.checkCompatibility(context, parameters.get(i)._genericType(), parameters.get(i)._multiplicity(), signature.get(i)._genericType(), signature.get(i)._multiplicity(), errorStub, SourceInformation.getUnknownSourceInformation());
                }
            }
            catch (Exception e)
            {
                return false;
            }
            return true;
        };
    }
}
