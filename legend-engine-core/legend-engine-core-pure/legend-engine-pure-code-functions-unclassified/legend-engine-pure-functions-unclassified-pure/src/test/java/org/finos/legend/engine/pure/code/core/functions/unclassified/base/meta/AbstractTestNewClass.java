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

public abstract class AbstractTestNewClass extends AbstractPureTestWithCoreCompiled
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
                "    let newClass = 'meta::pure::functions::meta::newClass'->newClass();\n" +
                "    assertEquals('newClass', $newClass.name);\n" +
                "    assertEquals('meta', $newClass.package.name);\n" +
                "    assertEquals('meta::pure::functions::meta::newClass', $newClass->elementToPath());\n" +
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
                "    let newClass = 'MyNewClass'->newClass();\n" +
                "    assertEquals('MyNewClass', $newClass.name);\n" +
                "    assertEquals('MyNewClass', $newClass->elementToPath());\n" +
                "}\n";

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void callWithEmpty()
    {
        String source = "function go():Any[*]\n" +
                "{\n" +
                "    let newClass = ''->newClass();\n" +
                "    assertEquals('', $newClass.name);\n" +
                "    assertEquals('', $newClass->elementToPath());\n" +
                "}\n";

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void newPackage()
    {
        String source = "function go():Any[*]\n" +
                "{\n" +
                "    let newClass = 'foo::bar::newClass'->newClass();\n" +
                "    assertEquals('foo::bar::newClass', $newClass->elementToPath());\n" +
                "}\n";

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }
}
