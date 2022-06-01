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

package org.finos.legend.engine.plan.compilation;

import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExecutionPlanDependenciesFilter implements ClassPathFilter
{
    private static final Collection<Class<?>> DEPENDENCY_CLASSES =
            GeneratePureConfig.MAIN_DEPENDENCIES.values().stream()
                    .map(ExecutionPlanDependenciesFilter::expandClass)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
    private static final Set<String> DEPENDENCY_PACKAGES = DEPENDENCY_CLASSES.stream()
            .map(Class::getPackage)
            .map(Package::getName)
            .collect(Collectors.toSet());

    private static final List<String> LIBRARY_PACKAGE_ROOTS = Arrays.asList(
            "com.fasterxml",
            "org.eclipse.collections",
            "org.openjdk.jol",
            "org.apache.commons",
            "org.finos.legend.pure.m4.coreinstance.primitive.date"
    );

    /*
     * This method exists to ensure we have compile-time dependencies on libraries that
     * are required when compiling generated code (these should be included in those listed
     * above).  That is to say that generated code should only depend on classes from
     * libraries that are guaranteed to be available at compile time through a reference
     * to them here.
     */
    public static void main(String[] args)
    {
        System.out.println(com.fasterxml.jackson.core.JsonFactory.class.getSimpleName());
        System.out.println(com.fasterxml.jackson.dataformat.xml.XmlFactory.class.getSimpleName());
        System.out.println(org.openjdk.jol.info.ClassLayout.class.getSimpleName());
    }

    private static List<Class<?>> expandClass(Class<?> c)
    {
        List<Class<?>> result = new ArrayList<>();
        result.add(c);
        if (c.getSuperclass() != null && !c.getSuperclass().equals(Object.class))
        {
            result.addAll(expandClass(c.getSuperclass()));
        }
        for (Class<?> _interface : c.getInterfaces())
        {
            result.addAll(expandClass(_interface));
        }
        return result;
    }

    @Override
    public boolean isPermittedPackage(String packageName)
    {
        return isLibraryPackage(packageName) || DEPENDENCY_PACKAGES.contains(packageName);
    }

    @Override
    public boolean isPermittedClass(String packageName, String className)
    {
        return isLibraryPackage(packageName) || DEPENDENCY_CLASSES.stream()
                .filter(c -> c.getPackage()
                        .getName()
                        .equals(packageName))
                .anyMatch(c -> c.getSimpleName().equals(className));
    }

    private boolean isLibraryPackage(String packageName)
    {
        return LIBRARY_PACKAGE_ROOTS.stream().anyMatch(packageName::startsWith);
    }
}
