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

import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager>
{
    private static final Pattern CLASS_NAME = Pattern.compile("(?<outerClass>[\\p{L}_][\\p{L}\\p{N}_]*)([$][\\p{L}\\p{N}_$]*)?\\.class", Pattern.UNICODE_CHARACTER_CLASS);

    private final MutableMap<String, ClassJavaSource> codeByName = Maps.mutable.empty();
    private final MutableMap<String, MutableList<ClassJavaSource>> codeByPackage = Maps.mutable.empty();
    private final ClassPathFilter filter;

    MemoryFileManager(JavaFileManager fileManager, ClassPathFilter filter)
    {
        super(fileManager);
        this.filter = filter;
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException
    {
        MutableList<JavaFileObject> result = Lists.mutable.empty();
        collectFiles(result, location, packageName, kinds, recurse);
        return result;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file)
    {
        if (file instanceof ClassJavaSource)
        {
            return ((ClassJavaSource) file).inferBinaryName();
        }
        return super.inferBinaryName(location, file);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException
    {
        if (kind == JavaFileObject.Kind.CLASS)
        {
            return getClassJavaSourceForOutput(className);
        }
        return super.getJavaFileForOutput(location, className, kind, sibling);
    }

    ClassJavaSource getClassJavaSourceByName(String name)
    {
        synchronized (this.codeByName)
        {
            return this.codeByName.get(name);
        }
    }

    MutableMap<String, String> getEncodedClassSources()
    {
        synchronized (this.codeByName)
        {
            MutableMap<String, String> result = Maps.mutable.ofInitialCapacity(this.codeByName.size());
            this.codeByName.forEachKeyValue((name, source) -> result.put(name, source.getEncodedBytes()));
            return result;
        }
    }

    private void collectFiles(MutableCollection<JavaFileObject> target, Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException
    {
        if ((location == StandardLocation.CLASS_PATH) && kinds.contains(JavaFileObject.Kind.CLASS) && !packageName.startsWith("java"))
        {
            if (this.filter.isPermittedPackage(packageName))
            {
                for (JavaFileObject fileObject : super.list(location, packageName, kinds, recurse))
                {
                    if (fileObject.getKind() == JavaFileObject.Kind.CLASS)
                    {
                        Matcher m = CLASS_NAME.matcher(fileObject.toUri().toString());
                        if (m.find())
                        {
                            // Inner classes are allowed if their containing class is
                            if (this.filter.isPermittedClass(packageName, m.group("outerClass")))
                            {
                                target.add(fileObject);
                            }
                        }
                    }
                }
            }
        }
        else
        {
            target.addAllIterable(super.list(location, packageName, kinds, recurse));
        }

        if ((location == StandardLocation.CLASS_PATH) && kinds.contains(JavaFileObject.Kind.CLASS))
        {
            synchronized (this.codeByName)
            {
                MutableList<ClassJavaSource> packageSources = this.codeByPackage.get(packageName);
                if (packageSources != null)
                {
                    target.addAll(packageSources);
                }
                if (recurse)
                {
                    String packagePrefix = packageName + '.';
                    this.codeByPackage.forEachKeyValue((pkg, files) ->
                    {
                        if (pkg.startsWith(packagePrefix))
                        {
                            target.addAll(files);
                        }
                    });
                }
            }
        }
    }

    private ClassJavaSource getClassJavaSourceForOutput(String className)
    {
        synchronized (this.codeByName)
        {
            ClassJavaSource source = this.codeByName.get(className);
            if (source == null)
            {
                source = ClassJavaSource.fromClassName(className);
                this.codeByName.put(className, source);
                this.codeByPackage.getIfAbsentPut(getPackageFromClassName(className), Lists.mutable::empty).add(source);
            }
            return source;
        }
    }

    private static String getPackageFromClassName(String className)
    {
        int lastDotIndex = className.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : className.substring(0, lastDotIndex);
    }
}
