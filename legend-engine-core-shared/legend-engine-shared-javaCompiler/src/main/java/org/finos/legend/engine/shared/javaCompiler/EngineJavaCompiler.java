// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.shared.javaCompiler;

import io.github.classgraph.ClassGraph;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import javax.lang.model.SourceVersion;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class EngineJavaCompiler
{
    private static final Map<ClassLoader, String> CLASSPATH_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private final JavaCompiler compiler;
    private final MemoryFileManager memoryFileManager;
    private final EngineJavaCompiler parent;
    private final JavaVersion javaVersion;
    private final FilterControl filterControl;
    private MemoryClassLoader memoryClassLoader;

    public EngineJavaCompiler(JavaVersion javaVersion, EngineJavaCompiler parent, ClassPathFilter filter)
    {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.parent = parent;
        this.javaVersion = (javaVersion != null) ? javaVersion : ((parent != null) ? parent.javaVersion : JavaVersion.JAVA_7);
        this.filterControl = new FilterControl(filter);
        this.memoryFileManager = new MemoryFileManager((parent == null) ? this.compiler.getStandardFileManager(null, null, null) : parent.memoryFileManager, this.filterControl);
        this.memoryClassLoader = newClassLoader();
    }

    public EngineJavaCompiler(JavaVersion javaVersion, ClassPathFilter filter)
    {
        this(javaVersion, null, filter);
    }

    public EngineJavaCompiler(EngineJavaCompiler parent)
    {
        this(null, parent, null);
    }

    public EngineJavaCompiler(JavaVersion javaVersion)
    {
        this(javaVersion, null, null);
    }

    public EngineJavaCompiler()
    {
        this(null, null, null);
    }

    public EngineJavaCompiler compile(Iterable<? extends StringJavaSource> javaSources) throws JavaCompileException
    {
        MetricsHandler.observeCount("Java compilation");
        MetricsHandler.incrementJavaCompilationCount();
        compile(this.compiler, javaSources, this.memoryFileManager, this.javaVersion, getClassPath());
        this.memoryClassLoader = newClassLoader();
        return this;
    }

    public MutableMap<String, String> save()
    {
        return this.memoryFileManager.getEncodedClassSources();
    }

    public EngineJavaCompiler load(MapIterable<String, String> save)
    {
        save.forEachKeyValue(this::load);
        return this;
    }

    public EngineJavaCompiler load(String className, String encodedBytecode)
    {
        // ---- To remove -----
        String message = encodedBytecode;
        if (encodedBytecode.startsWith("\""))
        {
            message = encodedBytecode.substring(1, encodedBytecode.length() - 1);
        }
        //---------------------

        ClassJavaSource cl;
        try
        {
            cl = (ClassJavaSource) this.memoryFileManager.getJavaFileForOutput(StandardLocation.CLASS_PATH, className, JavaFileObject.Kind.CLASS, null);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        cl.setEncodedBytes(message);
        return this;
    }

    public ClassLoader getClassLoader()
    {
        return this.memoryClassLoader;
    }

    public void setFilteringEnabled(boolean enabled)
    {
        this.filterControl.enabled = enabled;
    }

    private MemoryClassLoader newClassLoader()
    {
        return new MemoryClassLoader(this.memoryFileManager, (this.parent == null) ? Thread.currentThread().getContextClassLoader() : this.parent.memoryClassLoader);
    }

    private String getClassPath()
    {
        return CLASSPATH_CACHE.computeIfAbsent(Thread.currentThread().getContextClassLoader(), cl -> new ClassGraph().getClasspath());
    }

    private static void compile(JavaCompiler compiler, Iterable<? extends StringJavaSource> javaSources, JavaFileManager fileManager, JavaVersion javaVersion, String classPath) throws JavaCompileException
    {
        MutableList<String> options = buildCompileOptions(javaVersion, classPath);
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector, options, null, javaSources);
        if (!task.call())
        {
            throw new JavaCompileException(diagnosticCollector, options);
        }
    }

    private static MutableList<String> buildCompileOptions(JavaVersion javaVersion, String classPath)
    {
        MutableList<String> options = Lists.mutable.empty();

        // classpath
        if (classPath != null)
        {
            options.with("-classpath").with(classPath);
        }

        // source/target/release version
        if (javaVersion == JavaVersion.JAVA_7)
        {
            options.with("-source").with("7");
        }
        // When JDK 9+ is allowed, use this code instead:
        // else if (Runtime.version().version().get(0) <= 8)
        else if (SourceVersion.latest().ordinal() <= 8)
        {
            // if this JVM is version 8 or older, we use -source and -target options
            options.with("-source").with("8")
                    .with("-target").with("8");
        }
        else
        {
            // if this JVM is version 9 or newer, we use the --release option
            options.with("--release").with("8");
        }

        return options;
    }

    private static class FilterControl implements ClassPathFilter
    {
        private final ClassPathFilter delegate;
        private boolean enabled = true;

        FilterControl(ClassPathFilter delegate)
        {
            this.delegate = (delegate == null) ? ClassPathFilters.alwaysTrue() : delegate;
        }

        @Override
        public boolean isPermittedPackage(String packageName)
        {
            return !this.enabled || this.delegate.isPermittedPackage(packageName);
        }

        @Override
        public boolean isPermittedClass(String packageName, String className)
        {
            return !this.enabled || this.delegate.isPermittedClass(packageName, className);
        }
    }
}

