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

import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager>
{
    private static Pattern CLASS_NAME = Pattern.compile("(?<outerClass>[\\p{L}_][\\p{L}\\p{N}_]*)([$][\\p{L}\\p{N}_$]*)?\\.class", Pattern.UNICODE_CHARACTER_CLASS);

    private final MutableMap<String, ClassJavaSource> codeByName = UnifiedMap.newMap();
    private final MutableListMultimap<String, ClassJavaSource> codeByPackage = FastListMultimap.newMultimap();
    private final ClassPathFilter filter;

    MemoryFileManager(JavaFileManager fileManager, ClassPathFilter filter)
    {
        super(fileManager);
        this.filter = filter;
    }

    synchronized ClassJavaSource getClassJavaSourceByName(String name)
    {
        return this.codeByName.get(name);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException
    {
        MutableList<JavaFileObject> result = FastList.newList(this.codeByName.size());
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

    public synchronized MutableMap<String, ClassJavaSource> getCodeByName()
    {
        return this.codeByName;
    }

    private void collectFiles(final MutableCollection<JavaFileObject> target, Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException
    {
        if ((location == StandardLocation.CLASS_PATH) && kinds.contains(JavaFileObject.Kind.CLASS) && !packageName.startsWith("java"))
        {
            if (filter.isPermittedPackage(packageName))
            {
                for (JavaFileObject fileObject : super.list(location, packageName, kinds, recurse))
                {
                    if (fileObject.getKind() == JavaFileObject.Kind.CLASS)
                    {
                        Matcher m = CLASS_NAME.matcher(fileObject.toUri().toString());
                        if (m.find())
                        {
                            // Inner classes are allowed if their containing class is
                            if (filter.isPermittedClass(packageName, m.group("outerClass")))
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
            synchronized (this)
            {
                target.addAll(this.codeByPackage.get(packageName));
                if (recurse)
                {
                    final String packagePrefix = packageName + '.';
                    this.codeByPackage.forEachKeyMultiValues(new Procedure2<String, Iterable<ClassJavaSource>>()
                    {
                        @Override
                        public void value(String pkg, Iterable<ClassJavaSource> files)
                        {
                            if (pkg.startsWith(packagePrefix))
                            {
                                target.addAllIterable(files);
                            }
                        }
                    });
                }
            }
        }
    }

    private synchronized ClassJavaSource getClassJavaSourceForOutput(String className)
    {
        ClassJavaSource source = this.codeByName.get(className);
        if (source == null)
        {
            source = ClassJavaSource.fromClassName(className);
            this.codeByName.put(className, source);
            String pkg = getPackageFromClassName(className);
            this.codeByPackage.put(pkg, source);
        }
        return source;
    }

    private static String getPackageFromClassName(String className)
    {
        int lastDotIndex = className.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : className.substring(0, lastDotIndex);
    }
}
