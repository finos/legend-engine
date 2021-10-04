// Copyright 2021 Goldman Sachs
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

import org.eclipse.collections.impl.utility.ListIterate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ClassListFilter implements ClassPathFilter
{
    private final List<Class<?>> classes;

    public ClassListFilter(Class<?>... classes)
    {
        this(Arrays.asList(classes));
    }

    public ClassListFilter(Collection<Class<?>> classes)
    {
        this.classes = new ArrayList<>(classes);
    }

    @Override
    public boolean isPermittedPackage(String packageName)
    {
        return ListIterate.anySatisfy(classes, c -> packageName.equals(c.getPackage().getName()));
    }

    @Override
    public boolean isPermittedClass(String packageName, String className)
    {
        return ListIterate.anySatisfy(classes, c -> packageName.equals(c.getPackage().getName()) && className.equals(c.getSimpleName()));
    }
}
