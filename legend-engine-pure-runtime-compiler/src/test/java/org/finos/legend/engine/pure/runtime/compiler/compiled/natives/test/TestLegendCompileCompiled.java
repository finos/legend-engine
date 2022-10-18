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

package org.finos.legend.engine.pure.runtime.compiler.compiled.natives.test;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.pure.runtime.compiler.test.LegendCompileTest;
import org.finos.legend.pure.configuration.PureRepositoriesExternal;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.VoidPureRuntimeStatus;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.BeforeClass;

public class TestLegendCompileCompiled extends LegendCompileTest
{
    @BeforeClass
    public static void setUp()
    {
        RichIterable<CodeRepository> repositories = PureRepositoriesExternal.repositories().select(p -> !p.getName().startsWith("other_") && !p.getName().startsWith("test_"));
        System.out.println(repositories.collect(CodeRepository::getName).makeString(", "));
        PureCodeStorage codeStorage = new PureCodeStorage(null, new ClassLoaderCodeStorage(repositories));
        functionExecution = new FunctionExecutionCompiledBuilder().build();
        functionExecution.getConsole().disable();
        runtime = new PureRuntimeBuilder(codeStorage)
                .withRuntimeStatus(VoidPureRuntimeStatus.VOID_PURE_RUNTIME_STATUS)
                .setTransactionalByDefault(true)
                .build();
        functionExecution.init(runtime, new Message(""));
        runtime.loadAndCompileCore();
        runtime.loadAndCompileSystem();
    }
}
