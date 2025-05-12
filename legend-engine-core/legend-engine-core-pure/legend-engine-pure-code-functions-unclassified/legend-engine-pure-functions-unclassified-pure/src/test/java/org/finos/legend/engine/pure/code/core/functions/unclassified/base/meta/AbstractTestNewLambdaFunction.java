// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.meta;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Test;

public abstract class AbstractTestNewLambdaFunction extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void standardCall()
    {
        String source = "function a(func:LambdaFunction<Any>[1]):String[1]\n" +
                "{\n" +
                "   let funcType = $func->genericType().typeArguments->at(0).rawType->toOne()->cast(@FunctionType);\n" +
                "   $funcType.parameters->evaluateAndDeactivate()->map(v | $v.name)->joinStrings(', ');\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   let ftype = ^FunctionType(parameters=^VariableExpression(name='ok', genericType=^GenericType(rawType=String), multiplicity=PureOne), returnType=^GenericType(rawType=String), returnMultiplicity=PureOne);\n" +
                "   let newLambda = meta::pure::functions::meta::newLambdaFunction($ftype);\n" +
                "   assertEquals('ok', $newLambda->a());\n" +
                "}";

        runtime.createInMemorySource("StandardCall.pure", source);
        runtime.compile();
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }
}
