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
    protected static Class<?> compiledClass;
    protected static final ObjectMapper mapper = new ObjectMapper();

    @ClassRule
    public static TemporaryFolder tmpFolder = new TemporaryFolder();

    public static void setupSuite(String versionsFuncName) throws IOException, ClassNotFoundException
    {
        Path generatedSourcesDirectory = tmpFolder.newFolder("generated-sources", "java").toPath();
        String CLASSPATH = new ClassGraph().getClasspath();

        String baseClassName = "TestCastFunction";
        GenerateCast.main(generatedSourcesDirectory.toString(), versionsFuncName, baseClassName);

        Path fileName = generatedSourcesDirectory.resolve(
                "org/finos/legend/engine/generated/meta/pure/changetoken/cast_generation/" + baseClassName + ".java");
        Assert.assertTrue(Files.exists(fileName));
        System.out.println("==== Generated Class ====\n" + new String(Files.readAllBytes(fileName)));
        String fullClassName = "org.finos.legend.engine.generated.meta.pure.changetoken.cast_generation." + baseClassName;

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        MemoryFileManager fileManager = new MemoryFileManager(compiler);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector,
                Arrays.asList("-classpath", CLASSPATH), null,
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
