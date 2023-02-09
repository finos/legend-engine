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

package org.finos.legend.engine.pure.runtime.compiler;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.pure.runtime.compiler.interpreted.natives.LegendCompileMixedProcessorSupport;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.GraphLoader;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.VoidPureRuntimeStatus;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

public class Tools
{
    public static Pair<FunctionExecution, PureRuntime> setUpCompiled()
    {
        return initialize(
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

    public static Pair<FunctionExecution, PureRuntime> setUpInterpreted()
    {
        return initialize(
                codeStorage -> new PureRuntimeBuilder(codeStorage)
                        .withRuntimeStatus(VoidPureRuntimeStatus.VOID_PURE_RUNTIME_STATUS)
                        .setTransactionalByDefault(true)
                        .build(),
                (runtime, message) ->
                {
                    FunctionExecutionInterpreted functionExecution = new FunctionExecutionInterpreted();
                    functionExecution.init(runtime, message);
                    functionExecution.setProcessorSupport(new LegendCompileMixedProcessorSupport(functionExecution.getRuntime().getContext(), functionExecution.getRuntime().getModelRepository(), functionExecution.getProcessorSupport()));
                    return functionExecution;
                });
    }

    public static Pair<FunctionExecution, PureRuntime> initialize(Function<PureCodeStorage, PureRuntime> runtimeBuilder, Function2<PureRuntime, Message, FunctionExecution> exec)
    {
        RichIterable<CodeRepository> repositories = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(true)).build().getRepositories().select(p -> !p.getName().startsWith("other_") && !p.getName().startsWith("test_"));
        System.out.println(repositories.collect(CodeRepository::getName).makeString(", "));
        PureCodeStorage codeStorage = new PureCodeStorage(null, new ClassLoaderCodeStorage(repositories));

        PureRuntime runtime = runtimeBuilder.apply(codeStorage);

        Message message = new Message("")
        {
            public void setMessage(String message)
            {
                System.out.println(message);
            }
        };

        FunctionExecution functionExecution = exec.value(runtime, message);
        functionExecution.getConsole().disable();

        PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(GraphLoader.findJars(Lists.mutable.withAll(repositories.collect(CodeRepository::getName)), Thread.currentThread().getContextClassLoader(), message));
        GraphLoader loader = new GraphLoader(runtime.getModelRepository(), runtime.getContext(), runtime.getIncrementalCompiler().getParserLibrary(), runtime.getIncrementalCompiler().getDslLibrary(), runtime.getSourceRegistry(), runtime.getURLPatternLibrary(), jarLibrary);
        loader.loadAll(message);

        runtime.loadAndCompileSystem();

        return Tuples.pair(functionExecution, runtime);
    }


    public static void test(String code, FunctionExecution functionExecution, PureRuntime runtime)
    {
        try
        {
            runtime.createInMemoryAndCompile(
                    Tuples.pair("testSource.pure",
                            "function test():Any[*] \n"
                                    + "{"
                                    + code
                                    + "}\n"));
            CoreInstance func = runtime.getFunction("test():Any[*]");
            functionExecution.start(func, Lists.immutable.empty());
        }
        finally
        {
            runtime.delete("testSource.pure");
        }
    }
}
