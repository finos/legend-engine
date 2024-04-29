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

package org.finos.legend.engine.authentication.test;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.authentication.DatabricksTestDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.junit.Assert;
import org.junit.Test;

import java.util.ServiceLoader;

public class TestDatabricksAuthFlowExtensionAvailable
{
    @Test
    public void testDatabaseAuthenticationFlowProviderExtensionsAvailable()
    {
        MutableList<Class<?>> databaseAuthenticationFlowProviderExtensions =
                Lists.mutable.withAll(ServiceLoader.load(DatabaseAuthenticationFlowProvider.class))
                        .collect(Object::getClass);
        Assert.assertTrue(databaseAuthenticationFlowProviderExtensions.contains(DatabricksTestDatabaseAuthenticationFlowProvider.class));
    }
}
