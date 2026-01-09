// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core;

import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.generated.platform_pure_essential_meta_graph_pathToElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.VersionControlledClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryClassLoader;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class MatchGenerator
{
    public static void main(String[] args) throws Exception
    {
        ExecutionSupport executionSupport = getExecutionSupport();

        String pkg = args[0];
        String className = args[1];
        String inputFunctionsFile = args[2];
        Path tagetDirectory = Paths.get(args[3]);

        RichIterable<? extends Function<?>> functions = Lists.mutable.withAll(readFunctionFile(inputFunctionsFile))
                .collect(x -> (Function<?>) platform_pure_essential_meta_graph_pathToElement.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_(x, executionSupport));

        Class<?> matchGenClass = Class.forName("org.finos.legend.pure.generated.core_legend_compiler_matchGenerator");
        Method genMethod = matchGenClass.getMethod("Root_meta_legend_compiler_match_generate_String_1__String_1__Function_MANY__String_1_", String.class, String.class, RichIterable.class, ExecutionSupport.class);

        String code = (String) genMethod.invoke(null, pkg, className, functions, executionSupport);

        Path output = tagetDirectory.resolve(Paths.get(pkg.replaceAll("\\.", "/") + "/" + className + ".java"));
        Files.createDirectories(output.getParent());
        Files.write(output, code.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static ExecutionSupport getExecutionSupport()
    {
        PureJavaCompiler compiler = new PureJavaCompiler(new Message("MatchGenerator"));
        MemoryClassLoader classLoader = compiler.getClassLoader();
        return new CompiledExecutionSupport(new JavaCompilerState(null, classLoader), new CompiledProcessorSupport(classLoader, MetadataLazy.fromClassLoader(classLoader, CodeRepositoryProviderHelper.findCodeRepositories().collect(CodeRepository::getName)), Sets.mutable.empty()), null, new CompositeCodeStorage(new VersionControlledClassLoaderCodeStorage(classLoader, Lists.mutable.of(CodeRepositoryProviderHelper.findPlatformCodeRepository()), null)), null, null, new ConsoleCompiled(), null, null, null, Sets.mutable.empty(), CompiledExtensionLoader.extensions());
    }

    private static List<String> readFunctionFile(String file) throws Exception
    {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file))
        {
            assert inputStream != null;
            return IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
        }
    }
}
