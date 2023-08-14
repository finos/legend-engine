//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.language.java.runtime.compiler.shared;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.SynchronizedMutableMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Note that this implementation is EXTREMELY UNSAFE, as it allows runtime compilation and execution of arbitrary user
 * provided Java code. It is only suitable for use in situations where there are no concerns about users running unsafe
 * code (such as a user running a local Pure IDE Light).
 */
public class CompileAndExecuteJava
{
    public static CompilationResult compile(Collection<? extends JavaFileObject> javaSources, Iterable<String> options)
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        MemoryFileManager fileManager = new MemoryFileManager(compiler);
        if (javaSources.isEmpty())
        {
            return CompilationResult.success(new MemoryClassLoader(fileManager));
        }

        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector, options, null, javaSources);
        return task.call() ?
                CompilationResult.success(new MemoryClassLoader(fileManager)) :
                CompilationResult.error(diagnosticCollector);
    }

    public static ExecutionResult execute(CompilationResult compilationResult, String className, String methodName)
    {
        if (!compilationResult.isSuccess())
        {
            throw new IllegalArgumentException("Cannot execute: compilation was not successful");
        }
        try
        {
            Class<?> cls = compilationResult.getClassLoader().loadClass(className);
            Method method = cls.getMethod(methodName);
            return ExecutionResult.success(method.invoke(null));
        }
        catch (Exception e)
        {
            return ExecutionResult.error(e);
        }
    }

    private static class MemoryClassLoader extends ClassLoader
    {
        private final MemoryFileManager manager;
        private final MutableSet<String> names = Sets.mutable.empty();

        public MemoryClassLoader(MemoryFileManager manager)
        {
            super(null);
            this.manager = manager;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException
        {
            synchronized (this.names)
            {
                if (!this.names.contains(name))
                {
                    MemoryJavaClassFileObject mc = this.manager.getClassJavaFile(name);
                    if (mc != null)
                    {
                        this.names.add(name);
                        byte[] array = mc.getBytes();
                        return defineClass(name, array, 0, array.length);
                    }
                }
            }
            return super.findClass(name);
        }
    }

    private static class MemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>
    {
        private final SynchronizedMutableMap<String, MemoryJavaClassFileObject> codeByName = SynchronizedMutableMap.of(Maps.mutable.empty());

        private MemoryFileManager(StandardJavaFileManager fileManager)
        {
            super(fileManager);
        }

        private MemoryFileManager(JavaCompiler compiler)
        {
            this(compiler.getStandardFileManager(null, null, null));
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException
        {
            if ((location != StandardLocation.CLASS_PATH) || !kinds.contains(JavaFileObject.Kind.CLASS))
            {
                return super.list(location, packageName, kinds, recurse);
            }

            MutableList<JavaFileObject> result = Lists.mutable.withAll(super.list(location, packageName, kinds, recurse));
            String packagePrefix = packageName + '.';
            this.codeByName.forEachKeyValue((className, classFile) ->
            {
                if (className.startsWith(packagePrefix))
                {
                    result.add(classFile);
                }
            });
            return result;
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file)
        {
            if (file instanceof MemoryJavaClassFileObject)
            {
                return ((MemoryJavaClassFileObject) file).inferBinaryName();
            }
            return super.inferBinaryName(location, file);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException
        {
            if (kind == JavaFileObject.Kind.CLASS)
            {
                return this.codeByName.getIfAbsentPutWithKey(className, MemoryJavaClassFileObject::new);
            }
            return super.getJavaFileForOutput(location, className, kind, sibling);
        }

        private MemoryJavaClassFileObject getClassJavaFile(String name)
        {
            return this.codeByName.get(name);
        }
    }

    private static class MemoryJavaClassFileObject extends SimpleJavaFileObject
    {
        private byte[] bytes = new byte[0];

        private MemoryJavaClassFileObject(String className)
        {
            super(URI.create("memo:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }

        @Override
        public InputStream openInputStream()
        {
            return new ByteArrayInputStream(getBytes());
        }

        @Override
        public OutputStream openOutputStream()
        {
            return new ByteArrayOutputStream(1024)
            {
                @Override
                public synchronized void close()
                {
                    setBytes(toByteArray());
                }
            };
        }

        private synchronized byte[] getBytes()
        {
            return this.bytes;
        }

        private synchronized void setBytes(byte[] bytes)
        {
            this.bytes = bytes;
        }

        private String inferBinaryName()
        {
            String fileName = getName();
            return fileName.substring(0, fileName.lastIndexOf('.')).replace('/','.');
        }
    }
}
