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

package org.finos.engine.shared.javaCompiler;

import io.github.classgraph.ClassGraph;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.util.Base64;
import java.util.Objects;
import java.util.function.BiConsumer;

public class EngineJavaCompiler
{
    private static final ClassPathFilter NULL_FILTER = new ClassPathFilter()
    {
        @Override
        public boolean isPermittedPackage(String packageName)
        {
            return true;
        }

        @Override
        public boolean isPermittedClass(String packageName, String className)
        {
            return true;
        }
    };

    private final javax.tools.JavaCompiler compiler;
    private final MemoryFileManager memoryFileManager;
    private final EngineJavaCompiler parent;
    private final JavaVersion javaVersion;
    private final FilterControl filterControl;
    private MemoryClassLoader memoryClassLoader;


    public EngineJavaCompiler()
    {
        this(JavaVersion.JAVA_7, null, NULL_FILTER);
    }

    public EngineJavaCompiler(JavaVersion javaVersion)
    {
        this(javaVersion, null, NULL_FILTER);
    }

    public EngineJavaCompiler(JavaVersion javaVersion, ClassPathFilter filter)
    {
        this(javaVersion, null, filter);
    }

    public EngineJavaCompiler(EngineJavaCompiler parent)
    {
        this(parent == null ? JavaVersion.JAVA_7 : parent.javaVersion, parent, NULL_FILTER);
    }

    public EngineJavaCompiler(JavaVersion javaVersion, EngineJavaCompiler parent, ClassPathFilter filter)
    {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        JavaFileManager delegate = parent == null ? compiler.getStandardFileManager(null, null, null) : parent.memoryFileManager;
        this.filterControl = new FilterControl(Objects.requireNonNull(filter));
        this.memoryFileManager = new MemoryFileManager(delegate, this.filterControl);
        this.memoryClassLoader = parent == null ?
                new MemoryClassLoader(this.memoryFileManager, Thread.currentThread().getContextClassLoader()) :
                new MemoryClassLoader(this.memoryFileManager, parent.memoryClassLoader);
        this.parent = parent;
        this.javaVersion = javaVersion;
    }

    public EngineJavaCompiler compile(Iterable<? extends StringJavaSource> javaSources) throws JavaCompileException
    {
        compile(this.compiler, this.javaVersion, javaSources, this.memoryFileManager);
        this.memoryClassLoader = parent == null ?
                new MemoryClassLoader(this.memoryFileManager, Thread.currentThread().getContextClassLoader()) :
                new MemoryClassLoader(this.memoryFileManager, parent.memoryClassLoader);
        return this;
    }

    public MutableMap<String, String> save()
    {
        MutableMap<String, String> res = Maps.mutable.empty();
        this.memoryFileManager.getCodeByName().keyValuesView().forEach((Procedure<Pair<String, ClassJavaSource>>) p -> res.put(p.getOne(), Base64.getEncoder().encodeToString(p.getTwo().getBytes())));
        return res;
    }

    public EngineJavaCompiler load(MutableMap<String, String> save)
    {
        save.forEach((BiConsumer<String, String>) this::load);
        return this;
    }

    public EngineJavaCompiler load(String className, String encodedBytecode)
    {
        try
        {
            ClassJavaSource cl = (ClassJavaSource) this.memoryFileManager.getJavaFileForOutput(StandardLocation.CLASS_PATH, className, JavaFileObject.Kind.CLASS, null);
            // ---- To remove -----
            String message = encodedBytecode;
            if (encodedBytecode.startsWith("\""))
            {
                message = encodedBytecode.substring(1, encodedBytecode.length() - 1);
            }
            //---------------------
            cl.setBytes(Base64.getDecoder().decode(message));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return this;
    }

    public ClassLoader getClassLoader()
    {
        return this.memoryClassLoader;
    }

    private static void compile(javax.tools.JavaCompiler compiler, JavaVersion javaVersion, Iterable<? extends StringJavaSource> javaSources, JavaFileManager fileManager) throws JavaCompileException
    {
        MutableList<String> options = FastList.newList();
        options.add("-source");
        options.add(javaVersion == JavaVersion.JAVA_7 ? "7" : "8");
        options.add("-classpath");
        options.add(new ClassGraph().getClasspath());

        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        javax.tools.JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector, options, null, javaSources);
        if (!task.call())
        {
            throw new JavaCompileException(diagnosticCollector);
        }
    }

    public void setFilteringEnabled(boolean enabled)
    {
        this.filterControl.enabled = enabled;
    }

    private static class FilterControl implements ClassPathFilter
    {
        private final ClassPathFilter delegate;
        private boolean enabled = true;

        FilterControl(ClassPathFilter delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public boolean isPermittedPackage(String packageName)
        {
            return !enabled || delegate.isPermittedPackage(packageName);
        }

        @Override
        public boolean isPermittedClass(String packageName, String className)
        {
            return !enabled || delegate.isPermittedClass(packageName, className);
        }
    }
}

