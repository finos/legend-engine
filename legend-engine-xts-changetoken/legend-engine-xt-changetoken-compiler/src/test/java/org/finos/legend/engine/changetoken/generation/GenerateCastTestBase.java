//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.changetoken.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.classgraph.ClassGraph;
import org.finos.legend.engine.external.language.java.generation.GenerateJavaProject;
import org.finos.legend.pure.generated.Root_meta_pure_changetoken_Versions;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryClassLoader;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryFileManager;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

public abstract class GenerateCastTestBase
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GenerateCastTestBase.class);

    protected static Class<?> compiledClass;
    protected static final ObjectMapper mapper = new ObjectMapper();

    @ClassRule
    public static TemporaryFolder tmpFolder = new TemporaryFolder();

    public static void setupSuite(String versionsFuncName) throws IOException, ClassNotFoundException
    {
        setupSuiteFrom(versionsFuncName, "function");
    }

    public static void setupSuiteFromVersions(Root_meta_pure_changetoken_Versions versions) throws IOException, ClassNotFoundException
    {
        setupSuiteFrom(versions, "versions");
    }

    public static void setupSuiteFromJson(String json) throws IOException, ClassNotFoundException
    {
        setupSuiteFrom(json, "json");
    }

    private static void setupSuiteFrom(Object fromValue, String generatorType) throws IOException, ClassNotFoundException
    {
        Path generatedSourcesDirectory = tmpFolder.newFolder("generated-sources", "java").toPath();
        String classpath = new ClassGraph().getClasspath();

        String baseClassName = "TestCastFunction";
        GenerateJavaProject generateCastProject = null;
        switch (generatorType)
        {
            case "function":
                generateCastProject = new GenerateCast(generatedSourcesDirectory.toString(), (String) fromValue, baseClassName);
                break;
            case "versions":
                generateCastProject = new GenerateCastFromVersions(generatedSourcesDirectory.toString(), (Root_meta_pure_changetoken_Versions) fromValue, baseClassName);
                break;
            case "json":
                generateCastProject = new GenerateCastFromJson(generatedSourcesDirectory.toString(), (String) fromValue, baseClassName);
                break;
            default:
                Assert.fail(generatorType);
        }
        generateCastProject.execute();

        Path fileName = generatedSourcesDirectory.resolve(
                "org/finos/legend/engine/generated/meta/pure/changetoken/cast_generation/" + baseClassName + ".java");
        Assert.assertTrue(Files.exists(fileName));
        String fileContent = new String(Files.readAllBytes(fileName));
        LOGGER.debug("==== Generated Class ====\n" + fileContent);
        String fullClassName = "org.finos.legend.engine.generated.meta.pure.changetoken.cast_generation." + baseClassName;

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        MemoryFileManager fileManager = new MemoryFileManager(compiler);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector,
                Arrays.asList("-classpath", classpath), null,
                Collections.singletonList(new SourceFile(fileName)));
        if (!task.call())
        {
            Assert.fail(diagnosticCollector.getDiagnostics().toString());
        }

        compiledClass = new MemoryClassLoader(fileManager, Thread.currentThread().getContextClassLoader()).loadClass(fullClassName);
    }

    private static class SourceFile extends SimpleJavaFileObject
    {
        private final Path path;

        protected SourceFile(Path path)
        {
            super(path.toUri(), Kind.SOURCE);
            this.path = path;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException
        {
            return new String(Files.readAllBytes(this.path), StandardCharsets.UTF_8);
        }
    }
}
