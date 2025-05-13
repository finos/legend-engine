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

public abstract class AbstractTestNewAssociation extends AbstractPureTestWithCoreCompiled
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
                "    let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);\n" +
                "    let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);\n" +
                "    let newAssociation = 'meta::pure::functions::meta::A_B'->newAssociation($propertyA, $propertyB);\n" +
                "    assertEquals('A_B', $newAssociation.name);\n" +
                "    assertEquals('meta', $newAssociation.package.name);\n" +
                "    assertEquals('meta::pure::functions::meta::A_B', $newAssociation->elementToPath());\n" +
                "    assertEquals('a', $newAssociation.properties->at(0).name);\n" +
                "    assertEquals('b', $newAssociation.properties->at(1).name);\n" +
                "}\n";

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void callWithEmptyPackage()
    {
        String source = "function go():Any[*]\n" +
                "{\n" +
                "    let classA = 'A'->newClass();\n" +
                "    let classB = 'B'->newClass();\n" +
                "    let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);\n" +
                "    let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);\n" +
                "    let newAssociation = 'A_B'->newAssociation($propertyA, $propertyB);\n" +
                "    assertEquals('A_B', $newAssociation.name);\n" +
                "    assertEquals('A_B', $newAssociation->elementToPath());\n" +
                "}";

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void callWithEmpty()
    {
        String source = "function go():Any[*]\n" +
                "{\n" +
                "    let classA = 'A'->newClass();\n" +
                "    let classB = 'B'->newClass();\n" +
                "    let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);\n" +
                "    let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);\n" +
                "    let newAssociation = ''->newAssociation($propertyA, $propertyB);\n" +
                "    assertEquals('', $newAssociation.name);\n" +
                "    assertEquals('', $newAssociation->elementToPath());\n" +
                "}";

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void newPackage()
    {
        String source = "function go():Any[*]\n" +
                "{\n" +
                "    let classA = 'A'->newClass();\n" +
                "    let classB = 'B'->newClass();\n" +
                "    let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);\n" +
                "    let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);\n" +
                "    let newAssociation = 'foo::bar::A_B'->newAssociation($propertyA, $propertyB);\n" +
                "    assertEquals('foo::bar::A_B', $newAssociation->elementToPath());\n" +
                "}";

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }
}
