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

package org.finos.legend.pure.code.core.test;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.code.core.CoreRelationalSparkSQLCodeRepositoryProvider;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider;
import org.junit.Assert;
import org.junit.Test;

import java.util.ServiceLoader;

public class TestSparkSQLCodeRepositoryProviderAvailable
{
    @Test
    public void testCodeRepositoryProviderAvailable()
    {
        MutableList<Class<?>> codeRepositoryProviders =
                Lists.mutable.withAll(ServiceLoader.load(CodeRepositoryProvider.class))
                        .collect(Object::getClass);
        Assert.assertTrue(codeRepositoryProviders.contains(CoreRelationalSparkSQLCodeRepositoryProvider.class));
    }
}
