// Copyright 2023 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;

public class ClassPathFilters
{
    public static ClassPathFilter fromClasses(Class<?>... classes)
    {
        return fromClasses(ArrayAdapter.adapt(classes));
    }

    public static ClassPathFilter fromClasses(Iterable<? extends Class<?>> classes)
    {
        MutableMap<String, MutableSet<String>> classNamesByPackage = Maps.mutable.empty();
        classes.forEach(c -> classNamesByPackage.getIfAbsentPut(c.getPackage().getName(), () -> Sets.mutable.ofInitialCapacity(1)).add(c.getSimpleName()));
        return new ClassPathFilter()
        {
            @Override
            public boolean isPermittedPackage(String packageName)
            {
                return classNamesByPackage.containsKey(packageName);
            }

            @Override
            public boolean isPermittedClass(String packageName, String className)
            {
                MutableSet<String> classNames = classNamesByPackage.get(packageName);
                return (classNames != null) && classNames.contains(className);
            }
        };
    }

    public static ClassPathFilter any(ClassPathFilter... filters)
    {
        return any(Lists.immutable.with(filters));
    }

    public static ClassPathFilter any(Iterable<? extends ClassPathFilter> filters)
    {
        ListIterable<ClassPathFilter> filterList = Lists.immutable.withAll(filters);
        if (filterList.isEmpty())
        {
            return alwaysFalse();
        }
        if (filterList.size() == 1)
        {
            return filterList.get(0);
        }
        return new ClassPathFilter()
        {
            @Override
            public boolean isPermittedPackage(String packageName)
            {
                return filterList.anySatisfy(f -> f.isPermittedPackage(packageName));
            }

            @Override
            public boolean isPermittedClass(String packageName, String className)
            {
                return filterList.anySatisfy(f -> f.isPermittedClass(packageName, className));
            }
        };
    }

    public static ClassPathFilter all(ClassPathFilter... filters)
    {
        return all(Lists.immutable.with(filters));
    }

    public static ClassPathFilter all(Iterable<? extends ClassPathFilter> filters)
    {
        ListIterable<ClassPathFilter> filterList = Lists.immutable.withAll(filters);
        if (filterList.isEmpty())
        {
            return alwaysTrue();
        }
        if (filterList.size() == 1)
        {
            return filterList.get(0);
        }
        return new ClassPathFilter()
        {
            @Override
            public boolean isPermittedPackage(String packageName)
            {
                return filterList.allSatisfy(f -> f.isPermittedPackage(packageName));
            }

            @Override
            public boolean isPermittedClass(String packageName, String className)
            {
                return filterList.allSatisfy(f -> f.isPermittedClass(packageName, className));
            }
        };
    }

    public static ClassPathFilter alwaysFalse()
    {
        return new ClassPathFilter()
        {
            @Override
            public boolean isPermittedPackage(String packageName)
            {
                return false;
            }

            @Override
            public boolean isPermittedClass(String packageName, String className)
            {
                return false;
            }
        };
    }

    public static ClassPathFilter alwaysTrue()
    {
        return new ClassPathFilter()
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
    }
}
