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

package org.finos.legend.engine.language.sql.grammar.integration.compiled.natives.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.sql.grammar.integration.test.TestSQLEmbedded;
import org.finos.legend.engine.pure.runtime.compiler.test.Tools;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.VoidPureRuntimeStatus;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.BeforeClass;

public class TestSQLEmbeddedCompiled extends TestSQLEmbedded
{
    @BeforeClass
    public static void setUp()
    {
        Pair<FunctionExecution, PureRuntime> res = setUpCompiled();
        functionExecution = res.getOne();
        runtime = res.getTwo();
    }

    public static Pair<FunctionExecution, PureRuntime> setUpCompiled()
    {
        return Tools.initialize(
                codeStorage -> new PureRuntimeBuilder(codeStorage)
                        .withRuntimeStatus(VoidPureRuntimeStatus.VOID_PURE_RUNTIME_STATUS)
                        .withFactoryRegistryOverride(JavaModelFactoryRegistryLoader.loader())
                        .setTransactionalByDefault(true)
                        .build(),
                (runtime, message) ->
                {
                    FunctionExecutionCompiled functionExecution = new FunctionExecutionCompiledBuilder().build();
                    functionExecution.init(runtime, message);
                    return functionExecution;
                });
    }
}
