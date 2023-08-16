// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.pure.runtime.extensions.tests.all.shared;

import org.finos.legend.engine.pure.runtime.extensions.tests.all.Tools;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.junit.Test;

public abstract class TestGetExtensions
{
    protected static PureRuntime runtime;
    protected static FunctionExecution functionExecution;

    @Test
    public void testExtensionsFound()
    {
        test("let vals = meta::pure::extension::runtime::getExtensions().type;" +
                "assertContains($vals, 'testExtension');" +
                "assertContains($vals, 'testExtensionOne');" +
                "assertContains($vals, 'testExtensionTwo');");
    }

    private void test(String code)
    {
        Tools.test(code, functionExecution, runtime);
    }
}

