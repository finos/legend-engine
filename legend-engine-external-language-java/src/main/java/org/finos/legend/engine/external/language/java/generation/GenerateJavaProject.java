// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.language.java.generation;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_Class;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_ProjectDirectory;
import org.finos.legend.pure.generated.Root_meta_external_language_java_serialization_Stringifier;
import org.finos.legend.pure.generated.core_external_language_java_metamodel_factories;
import org.finos.legend.pure.generated.core_external_language_java_metamodel_serialization;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.SVNCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.VersionControlledClassLoaderCodeStorage;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class GenerateJavaProject
{
    private final String outputDirectory;
    private final CompiledExecutionSupport executionSupport;

    protected GenerateJavaProject(String outputDirectory)
    {
        this.outputDirectory = outputDirectory;

        ClassLoader classLoader = Pure.class.getClassLoader();
        this.executionSupport = new CompiledExecutionSupport(
                new JavaCompilerState(null, classLoader),
                new CompiledProcessorSupport(classLoader, MetadataLazy.fromClassLoader(classLoader, CodeRepositoryProviderHelper.findCodeRepositories().collect(CodeRepository::getName)), Sets.mutable.empty()),
                null,
                new PureCodeStorage(null, new VersionControlledClassLoaderCodeStorage(classLoader, Lists.mutable.of(
                        CodeRepository.newPlatformCodeRepository(),
                        SVNCodeRepository.newSystemCodeRepository()
                ), null)),
                null,
                null,
                new ConsoleCompiled(),
                new FunctionCache(),
                new ClassCache(),
                null,
                Sets.mutable.empty()
        );
    }

    public void execute()
    {
        Root_meta_external_language_java_metamodel_project_Project project = doExecute(executionSupport);
        Root_meta_external_language_java_serialization_Stringifier stringifier = core_external_language_java_metamodel_serialization.Root_meta_external_language_java_serialization_newStringifier_Project_1__Stringifier_1_(project, executionSupport);

        Root_meta_external_language_java_metamodel_project_ProjectDirectory javaDir = project._root()
                ._subdirectories().detect(sd -> "src".equals(sd._name()))
                ._subdirectories().detect(sd -> "main".equals(sd._name()))
                ._subdirectories().detect(sd -> "java".equals(sd._name()));
        javaDir._subdirectories().forEach(dir -> processDir(dir, Paths.get(outputDirectory), stringifier));
    }

    protected abstract Root_meta_external_language_java_metamodel_project_Project doExecute(CompiledExecutionSupport executionSupport);

    private void processDir(Root_meta_external_language_java_metamodel_project_ProjectDirectory directory, Path dirPath, Root_meta_external_language_java_serialization_Stringifier stringifier)
    {
        try
        {
            Path newDirPath = dirPath.resolve(directory._name());
            Files.createDirectories(newDirPath);

            directory._classes().forEach(cls -> writeClass(cls, newDirPath, stringifier));

            directory._subdirectories().forEach(dir -> processDir(dir, newDirPath, stringifier));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void writeClass(Root_meta_external_language_java_metamodel_Class cls, Path dirPath, Root_meta_external_language_java_serialization_Stringifier stringifier)
    {
        try
        {
            Path javaFilePath = dirPath.resolve(cls._simpleName() + ".java");
            String code = core_external_language_java_metamodel_serialization.Root_meta_external_language_java_serialization_ofClass_Stringifier_1__Class_1__String_1_(stringifier, core_external_language_java_metamodel_factories.Root_meta_external_language_java_factory_inferImports_Class_1__Class_1_(cls, executionSupport), executionSupport);
            Files.write(javaFilePath, code.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
