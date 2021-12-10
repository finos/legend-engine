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
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProvider;

/*
    The selector is used to select a flow provider. The following selection rules are applied :
    1/ A provider is loaded via a ServiceLoader
    2/ However, the implementation to use has to be explicitly indicated via a System property. This not only acts as a feature flag to enable/disable the flows but also provides deterministic resolution of the flow provider.
 */
public class DatabaseAuthenticationFlowProviderSelector
{
    public static final String DATABASE_AUTH_FLOW_PROVIDER_IMPL_CLASS = "org.finos.legend.engine.execution.databaseAuthFlowProviderImplClass";

    private static Optional<DatabaseAuthenticationFlowProvider> INSTANCE = Optional.empty();

    private DatabaseAuthenticationFlowProviderSelector()
    {
        // singleton
    }

    public static void disableFlowProvider()
    {
        System.clearProperty(DatabaseAuthenticationFlowProviderSelector.DATABASE_AUTH_FLOW_PROVIDER_IMPL_CLASS);
    }

    public static void enableLegendDefaultFlowProvider()
    {
        System.setProperty(DatabaseAuthenticationFlowProviderSelector.DATABASE_AUTH_FLOW_PROVIDER_IMPL_CLASS, LegendDefaultDatabaseAuthenticationFlowProvider.class.getCanonicalName());
    }

    public static void enableFlowProvider(Class clazz)
    {
        System.setProperty(DatabaseAuthenticationFlowProviderSelector.DATABASE_AUTH_FLOW_PROVIDER_IMPL_CLASS, clazz.getCanonicalName());
    }

    public static synchronized Optional<DatabaseAuthenticationFlowProvider> getProvider()
    {
        Supplier<MutableList<DatabaseAuthenticationFlowProvider>> locator = () -> Iterate.addAllTo(ServiceLoader.load(DatabaseAuthenticationFlowProvider.class), Lists.mutable.empty());
        return getProviderImpl(locator);
    }

    public static synchronized Optional<DatabaseAuthenticationFlowProvider> getProviderForTests(Supplier<MutableList<DatabaseAuthenticationFlowProvider>> locator)
    {
        return getProviderImpl(locator);
    }

    private static synchronized Optional<DatabaseAuthenticationFlowProvider> getProviderImpl(Supplier<MutableList<DatabaseAuthenticationFlowProvider>> locator)
    {
        String databaseAuthFlowProviderImplClass = System.getProperty(DATABASE_AUTH_FLOW_PROVIDER_IMPL_CLASS);
        if (databaseAuthFlowProviderImplClass == null)
        {
            return Optional.empty();
        }
        if (!INSTANCE.isPresent())
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
            throw new RuntimeException(String.format("Failed to locate database flow provider '%s'", databaseAuthFlowProviderImplClass));
        }
        return providerHolder;
    }
}