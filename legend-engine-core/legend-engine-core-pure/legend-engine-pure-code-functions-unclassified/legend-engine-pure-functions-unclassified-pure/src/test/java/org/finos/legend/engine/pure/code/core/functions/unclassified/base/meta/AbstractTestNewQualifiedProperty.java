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
import org.junit.After;
import org.junit.Test;

public abstract class AbstractTestNewQualifiedProperty extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("StandardCall.pure");
        runtime.compile();
    }

    @Test
    public void standardCall()
    {
        String source = "function go():Any[*]\n" +
                "{\n" +
                "    let classA = 'meta::pure::functions::meta::A'->newClass();\n" +
                "    let classB = 'meta::pure::functions::meta::B'->newClass();\n" +
                "    let qualifiedProperty = newQualifiedProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne, [^VariableExpression(name = 'newName', multiplicity = ZeroOne, genericType = ^GenericType(rawType = Any))]);\n" +
                "    assertEquals('a', $qualifiedProperty.name);\n" +
                "    assertEquals('a', $qualifiedProperty.functionName);\n" +
                "    assertEquals('B', $qualifiedProperty.owner.name->toOne());\n" +
                "    assertEquals(PureOne, $qualifiedProperty.multiplicity);\n" +
                "    assertEquals('A', $qualifiedProperty.genericType.rawType->toOne().name);\n" +
                "    assertEquals('QualifiedProperty', $qualifiedProperty.classifierGenericType.rawType->toOne().name);\n" +
                "    let typeArguments = $qualifiedProperty.classifierGenericType.typeArguments;\n" +
                "    assertEquals(1, $typeArguments->size());\n" +
                "    assert($typeArguments->toOne().rawType->toOne()->instanceOf(FunctionType), |'Expected qualified property type argument to be instance of FunctionType');\n" +
                "    assertEquals('A', $typeArguments->toOne().rawType->toOne()->cast(@FunctionType).returnType.rawType->toOne().name);\n" +
                "    assertEquals(PureOne, $typeArguments->toOne().rawType->toOne()->cast(@FunctionType).returnMultiplicity);\n" +
                "    let params = $typeArguments->toOne().rawType->toOne()->cast(@FunctionType).parameters->evaluateAndDeactivate();\n" +
                "    assertEquals(1, $params->size());\n" +
                "    assert($params->toOne()->instanceOf(Any), |'Expected function type to have one parameter');\n" +
                "    assertEquals(ZeroOne, $params->toOne().multiplicity);\n" +
                "    assertEquals('newName', $params.name);\n" +
                "}";

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }
}
