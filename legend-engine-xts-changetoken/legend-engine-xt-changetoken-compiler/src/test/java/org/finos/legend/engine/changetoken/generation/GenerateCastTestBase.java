//  Copyright 2024 Goldman Sachs
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.classgraph.ClassGraph;
import org.finos.legend.pure.generated.Root_meta_pure_changetoken_Versions;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryClassLoader;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryFileManager;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TemporaryFolder;

import javax.tools.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertThrows;

public abstract class GenerateCastTestBase
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GenerateCastTestBase.class);

    protected static Class<?> compiledClass;
    private static final ObjectMapper mapper = new ObjectMapper();

    @ClassRule
    public static TemporaryFolder tmpFolder = new TemporaryFolder();

    public static void setupSuiteFromVersions(Root_meta_pure_changetoken_Versions versions) throws IOException, ClassNotFoundException
    {
        setupSuiteFrom(versions, "versions", true, false, true, "@type", "version");
    }

    public static void setupSuiteFromJson(String json) throws IOException, ClassNotFoundException
    {
        setupSuiteFromJson(json, true, false, true, "@type", "version");
    }

    public static void setupSuiteFromJson(String json, String typeKeyName, String versionKeyName) throws IOException, ClassNotFoundException
    {
        setupSuiteFromJson(json, true, false, true, typeKeyName, versionKeyName);
    }

    public static void setupSuiteFromJson(String json, boolean alwaysStampAtRootVersion, boolean optionalStampAllVersions, boolean obsoleteJsonAsString) throws IOException, ClassNotFoundException
    {
        setupSuiteFrom(json, "json", alwaysStampAtRootVersion, optionalStampAllVersions, obsoleteJsonAsString, "@type", "version");
    }

    private static void setupSuiteFromJson(String json, boolean alwaysStampAtRootVersion, boolean optionalStampAllVersions, boolean obsoleteJsonAsString, String typeKeyName, String versionKeyName) throws IOException, ClassNotFoundException
    {
        setupSuiteFrom(json, "json", alwaysStampAtRootVersion, optionalStampAllVersions, obsoleteJsonAsString, typeKeyName, versionKeyName);
    }

    private static void setupSuiteFrom(Object fromValue, String generatorType, boolean alwaysStampAtRootVersion, boolean optionalStampAllVersions, boolean obsoleteJsonAsString, String typeKeyName, String versionKeyName) throws IOException, ClassNotFoundException
    {
        Path generatedSourcesDirectory = tmpFolder.newFolder("generated-sources", "java").toPath();
        String classpath = new ClassGraph().getClasspath();

        String baseClassName = "TestCastFunction";
        GenerateCastFromVersions generateCastProject = null;
        switch (generatorType)
        {
            case "versions":
                generateCastProject = new GenerateCastFromVersions(generatedSourcesDirectory.toString(), (Root_meta_pure_changetoken_Versions) fromValue, baseClassName);
                break;
            case "json":
                generateCastProject = new GenerateCastFromJson(generatedSourcesDirectory.toString(), (String) fromValue, baseClassName);
                break;
            default:
                Assert.fail(generatorType);
        }
        generateCastProject.setAlwaysStampAtRootVersion(alwaysStampAtRootVersion);
        generateCastProject.setOptionalStampAllVersions(optionalStampAllVersions);
        generateCastProject.setObsoleteJsonAsString(obsoleteJsonAsString);
        generateCastProject.setTypeKeyName(typeKeyName);
        generateCastProject.setVersionKeyName(versionKeyName);
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

    private Map<String, Object> upcast(Map<String, Object> objectNode) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        return (Map<String, Object>) compiledClass.getMethod("upcast", Map.class).invoke(null, objectNode);
    }

    private Map<String, Object> upcast(Map<String, Object> objectNode, String currentVersion) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        return (Map<String, Object>) compiledClass.getMethod("upcast", Map.class, String.class).invoke(null, objectNode, currentVersion);
    }

    private Map<String, Object> downcast(Map<String, Object> objectNode, String targetVersion) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        return (Map<String, Object>) compiledClass.getMethod("downcast", Map.class, String.class).invoke(null, objectNode, targetVersion);
    }

    private Map<String, Object> downcast(Map<String, Object> objectNode, String targetVersion, String currentVersion) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        return (Map<String, Object>) compiledClass.getMethod("downcast", Map.class, String.class, String.class).invoke(null, objectNode, targetVersion, currentVersion);
    }

    protected Map<String, Object> parse(String value) throws JsonProcessingException
    {
        return mapper.readValue(value, Map.class);
    }

    public Map<String, Object> upcast(String input) throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Map<String, Object> jsonNode = parse(input);
        try
        {
            return upcast(jsonNode);
        }
        finally
        {
            Assert.assertEquals(parse(input), jsonNode);
        }
    }

    public Map<String, Object> upcast(String input, String currentVersion) throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Map<String, Object> jsonNode = parse(input);
        try
        {
            return upcast(jsonNode, currentVersion);
        }
        finally
        {
            Assert.assertEquals(parse(input), jsonNode);
        }
    }

    public Map<String, Object> downcast(String input, String targetVersion) throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Map<String, Object> jsonNode = parse(input);
        try
        {
            return downcast(jsonNode, targetVersion);
        }
        finally
        {
            Assert.assertEquals(parse(input), jsonNode);
        }
    }

    public Map<String, Object> downcast(String input, String targetVersion, String currentVersion) throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Map<String, Object> jsonNode = parse(input);
        try
        {
            return downcast(jsonNode, targetVersion, currentVersion);
        }
        finally
        {
            Assert.assertEquals(parse(input), jsonNode);
        }
    }

    public void expect(Map<String, Object> actual, String expected) throws JsonProcessingException
    {
        Assert.assertEquals(parse(expected), actual);
    }

    public void exception(ThrowingRunnable runnable, String expected)
    {
        InvocationTargetException re = assertThrows("non-default", InvocationTargetException.class, runnable);
        Assert.assertEquals(expected, re.getCause().getMessage());
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
