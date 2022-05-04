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

package org.finos.legend.engine.authentication.provider;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
/*
    The selector is used to load a flow provider from the classpath.
 */
public class DatabaseAuthenticationFlowProviderSelector
{
    public static Optional<DatabaseAuthenticationFlowProvider> INSTANCE = null;

    private DatabaseAuthenticationFlowProviderSelector()
    {
        // singleton
    }

    public static synchronized Optional<DatabaseAuthenticationFlowProvider> getProvider(String flowProviderClass)
    {
        Supplier<MutableList<DatabaseAuthenticationFlowProvider>> locator = () -> Iterate.addAllTo(ServiceLoader.load(DatabaseAuthenticationFlowProvider.class), Lists.mutable.empty());
        return getProviderImpl(locator, flowProviderClass);
    }

    private static synchronized Optional<DatabaseAuthenticationFlowProvider> getProviderImpl(Supplier<MutableList<DatabaseAuthenticationFlowProvider>> locator, String databaseAuthFlowProviderImplClass)
    {
        if (INSTANCE == null)
        {
            INSTANCE = initialize(locator, databaseAuthFlowProviderImplClass);
        }
        return INSTANCE;
    }

    private static Optional<DatabaseAuthenticationFlowProvider> initialize(Supplier<MutableList<DatabaseAuthenticationFlowProvider>> locator, String databaseAuthFlowProviderImplClass)
    {
        MutableList<DatabaseAuthenticationFlowProvider> providers = locator.get();
        Optional<DatabaseAuthenticationFlowProvider> providerHolder = providers.select(provider -> provider.getClass().getCanonicalName().equals(databaseAuthFlowProviderImplClass)).getFirstOptional();
        if (!providerHolder.isPresent())
        {
            throw new RuntimeException(String.format("Failed to locate database flow provider '%s' in the classpath", databaseAuthFlowProviderImplClass));
        }
        return providerHolder;
    }
}