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
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.*;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

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

    public static Pair<FunctionExecution, PureRuntime> initialize(Function<CompositeCodeStorage, PureRuntime> runtimeBuilder, Function2<PureRuntime, Message, FunctionExecution> exec)
    {
        RichIterable<CodeRepository> repositories = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(true)).build().getRepositories().select(p -> !p.getName().startsWith("other_") && !p.getName().startsWith("test_"));
        System.out.println(repositories.collect(CodeRepository::getName).makeString(", "));
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories));

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


    public static void test(String code, String otherParserCode, FunctionExecution functionExecution, PureRuntime runtime)
    {
        System.out.println("Starting new test");
        try
        {
            runtime.createInMemoryAndCompile(
                    Tuples.pair("testSource.pure",
                            otherParserCode +
                            "###Pure\n" +
                            "function test():Any[*] \n"
                                    + "{\n"
                                    +    code
                                    + "}\n"));
            CoreInstance func = runtime.getFunction("test():Any[*]");
            functionExecution.start(func, Lists.immutable.empty());
        }
        finally
        {
            runtime.delete("testSource.pure");
        }
    }

    public static void loadPureFile(PureRuntime runtime, String resourcePath)
    {
        String pureCode ;
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource("tests.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
            pureCode = reader.lines().collect(Collectors.joining("\n"));
        }
        catch(Exception e)
        {
            throw new RuntimeException("Error loading code repository definition from resource " + resourcePath  );
        }

        runtime.createInMemoryAndCompile(Tuples.pair("testSource.pure", pureCode));
    }

    public static void runTest(String testName, FunctionExecution functionExecution, PureRuntime runtime)
    {
        CoreInstance func = runtime.getFunction(testName);
        functionExecution.start(func, Lists.immutable.empty());
    }
}
