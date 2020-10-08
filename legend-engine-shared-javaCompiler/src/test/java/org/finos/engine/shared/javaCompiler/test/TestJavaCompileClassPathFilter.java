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

package org.finos.engine.shared.javaCompiler.test;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.engine.shared.javaCompiler.ClassPathFilter;
import org.finos.engine.shared.javaCompiler.JavaCompileException;
import org.finos.engine.shared.javaCompiler.JavaVersion;
import org.finos.engine.shared.javaCompiler.StringJavaSource;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class TestJavaCompileClassPathFilter
{
    private final String code = "package engine.generated;" +
            "public class Example" +
            "{" +
            "    public static String execute()\n" +
            "    {\n" +
            "       return org.finos.engine.shared.javaCompiler.JavaVersion.JAVA_8.toString();" +
            "    }\n" +
            "}";

    @Test
    public void compilesWithoutFilters() throws Exception
    {
        EngineJavaCompiler c = new EngineJavaCompiler();
        c.compile(Lists.mutable.with(StringJavaSource.newStringJavaSource("engine.generated", "Example", code)));
        Assert.assertEquals("JAVA_8", execute(c));
    }

    @Test
    public void compilesWhenPackageIsFilteredIn() throws Exception
    {
        ClassPathFilter filter = new ClassPathFilter()
        {
            @Override
            public boolean isPermittedPackage(String packageName)
            {
                return packageName.equals(JavaVersion.class.getPackage().getName());
            }

            @Override
            public boolean isPermittedClass(String packageName, String className)
            {
                return isPermittedPackage(packageName);
            }
        };

        EngineJavaCompiler c = new EngineJavaCompiler(JavaVersion.JAVA_8, filter);
        c.compile(Lists.mutable.with(StringJavaSource.newStringJavaSource("engine.generated", "Example", code)));
        Assert.assertEquals("JAVA_8", execute(c));
    }

    @Test(expected = JavaCompileException.class)
    public void failsWhenPackageIsFilteredOut() throws Exception
    {
        ClassPathFilter filter = new ClassPathFilter()
        {
            @Override
            public boolean isPermittedPackage(String packageName)
            {
                return !packageName.equals(JavaVersion.class.getPackage().getName());
            }

            @Override
            public boolean isPermittedClass(String packageName, String className)
            {
                return isPermittedPackage(packageName);
            }
        };

        EngineJavaCompiler c = new EngineJavaCompiler(JavaVersion.JAVA_8, filter);
        c.compile(Lists.mutable.with(StringJavaSource.newStringJavaSource("engine.generated", "Example", code)));
    }

    @Test
    public void succeedsWhenPackageIsFilteredOutButFilteringSwitchedOff() throws Exception
    {
        ClassPathFilter filter = new ClassPathFilter()
        {
            @Override
            public boolean isPermittedPackage(String packageName)
            {
                return !packageName.equals(JavaVersion.class.getPackage().getName());
            }

            @Override
            public boolean isPermittedClass(String packageName, String className)
            {
                return isPermittedPackage(packageName);
            }
        };

        EngineJavaCompiler c = new EngineJavaCompiler(JavaVersion.JAVA_8, filter);
        c.setFilteringEnabled(false);
        c.compile(Lists.mutable.with(StringJavaSource.newStringJavaSource("engine.generated", "Example", code)));
        Assert.assertEquals("JAVA_8", execute(c));
    }

    @Test
    public void compilesWhenClassIsFilteredIn() throws Exception
    {
        ClassPathFilter filter = new ClassPathFilter()
        {
            @Override
            public boolean isPermittedPackage(String packageName)
            {
                return packageName.equals(JavaVersion.class.getPackage().getName());
            }

            @Override
            public boolean isPermittedClass(String packageName, String className)
            {
                return isPermittedPackage(packageName) && className.equals(JavaVersion.class.getSimpleName());
            }
        };

        EngineJavaCompiler c = new EngineJavaCompiler(JavaVersion.JAVA_8, filter);
        c.compile(Lists.mutable.with(StringJavaSource.newStringJavaSource("engine.generated", "Example", code)));
        Assert.assertEquals("JAVA_8", execute(c));
    }

    @Test(expected = JavaCompileException.class)
    public void failsWhenClassIsFilteredOut() throws Exception
    {
        ClassPathFilter filter = new ClassPathFilter()
        {
            @Override
            public boolean isPermittedPackage(String packageName)
            {
                return packageName.equals(JavaVersion.class.getPackage().getName());
            }

            @Override
            public boolean isPermittedClass(String packageName, String className)
            {
                return isPermittedPackage(packageName) && !className.equals(JavaVersion.class.getSimpleName());
            }
        };

        EngineJavaCompiler c = new EngineJavaCompiler(JavaVersion.JAVA_8, filter);
        c.compile(Lists.mutable.with(StringJavaSource.newStringJavaSource("engine.generated", "Example", code)));
    }

    private String execute(EngineJavaCompiler c) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        Class cl = c.getClassLoader().loadClass("engine.generated.Example");
        return (String) cl.getMethod("execute").invoke(null);
    }
}
