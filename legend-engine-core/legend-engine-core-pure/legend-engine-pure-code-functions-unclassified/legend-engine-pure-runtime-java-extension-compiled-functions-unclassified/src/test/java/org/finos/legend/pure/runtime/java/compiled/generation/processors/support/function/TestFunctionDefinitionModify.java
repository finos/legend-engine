// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.function.AbstractTestFunctionDefinitionModify;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestFunctionDefinitionModify extends AbstractTestFunctionDefinitionModify
{
    @BeforeClass
    public static void setUp()
    {
        AbstractPureTestWithCoreCompiled.setUpRuntime(getFunctionExecution(), AbstractTestFunctionDefinitionModify.getCodeStorage(), JavaModelFactoryRegistryLoader.loader());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Test
    @Ignore(value = "Further fixes needed for clone as source information is copied over currently")
    @Override
    public void testConcreteFunctionDefinitionModifyWithCopyConstructor()
    {
        super.testConcreteFunctionDefinitionModifyWithCopyConstructor();
    }

}
