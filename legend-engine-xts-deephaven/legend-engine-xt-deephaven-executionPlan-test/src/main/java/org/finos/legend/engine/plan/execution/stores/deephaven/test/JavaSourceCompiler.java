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

package org.finos.legend.engine.plan.execution.stores.deephaven.test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JavaSourceCompiler
{
    public static Path compileToJar(String javaSource, String className, String packageName, Path outputDir) throws Exception
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
        {
            throw new IllegalStateException("No Java compiler available. Ensure you are running with a JDK, not a JRE.");
        }

        Path classOutputDir = Files.createTempDirectory("deephaven-compile");

        String qualifiedName = packageName.isEmpty() ? className : packageName + "." + className;
        JavaFileObject sourceFile = new InMemoryJavaFileObject(qualifiedName, javaSource);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8))
        {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(classOutputDir.toFile()));

            String classpath = System.getProperty("java.class.path");

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    Arrays.asList("-classpath", classpath),
                    null,
                    Collections.singletonList(sourceFile)
            );

            boolean success = task.call();
            if (!success)
            {
                StringBuilder sb = new StringBuilder("Java compilation failed:\n");
                diagnostics.getDiagnostics().forEach(d -> sb.append(d.toString()).append("\n"));
                throw new RuntimeException(sb.toString());
            }
        }

        Path jarPath = outputDir.resolve(className.toLowerCase() + ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath.toFile().toPath())))
        {
            addFilesToJar(classOutputDir.toFile(), classOutputDir.toFile(), jos);
        }

        return jarPath;
    }

    private static void addFilesToJar(File root, File source, JarOutputStream jos) throws Exception
    {
        File[] files = source.listFiles();
        if (files == null)
        {
            return;
        }
        for (File file : files)
        {
            if (file.isDirectory())
            {
                addFilesToJar(root, file, jos);
            }
            else
            {
                String entryName = root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
                jos.putNextEntry(new JarEntry(entryName));
                Files.copy(file.toPath(), jos);
                jos.closeEntry();
            }
        }
    }

    private static class InMemoryJavaFileObject extends SimpleJavaFileObject
    {
        private final String code;

        InMemoryJavaFileObject(String qualifiedName, String code)
        {
            super(URI.create("string:///" + qualifiedName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors)
        {
            return code;
        }
    }
}

