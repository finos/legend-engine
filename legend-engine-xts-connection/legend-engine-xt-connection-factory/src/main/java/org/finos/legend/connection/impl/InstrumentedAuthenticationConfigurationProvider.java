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

package org.finos.legend.connection.impl;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.connection.AuthenticationConfigurationProvider;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.StoreInstanceProvider;
import org.finos.legend.engine.protocol.pure.v1.connection.AuthenticationConfiguration;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Map;
import java.util.Objects;

/**
 * This is the instrumented version of {@link AuthenticationConfigurationProvider} which is used for testing.
 */
public class InstrumentedAuthenticationConfigurationProvider extends AuthenticationConfigurationProvider
{
    private final Map<String, AuthenticationConfiguration> authenticationConfigurationIndex = Maps.mutable.empty();

    public InstrumentedAuthenticationConfigurationProvider(StoreInstanceProvider storeInstanceProvider, LegendEnvironment environment)
    {
        super(storeInstanceProvider, environment);
    }

    public void injectAuthenticationConfiguration(String storeInstanceIdentifier, AuthenticationConfiguration authenticationConfiguration)
    {
        StoreInstance storeInstance = this.storeInstanceProvider.lookup(storeInstanceIdentifier);
        if (!storeInstance.getAuthenticationConfigurationTypes().contains(authenticationConfiguration.getClass()))
        {
            throw new RuntimeException(String.format("Authentication configuration of type '%s' is not supported by store '%s'", authenticationConfiguration.getClass().getSimpleName(), storeInstance.getIdentifier()));
        }
        this.authenticationConfigurationIndex.put(storeInstanceIdentifier, authenticationConfiguration);
    }

    @Override
    public AuthenticationConfiguration lookup(Identity identity, StoreInstance storeInstance)
    {
        return Objects.requireNonNull(this.authenticationConfigurationIndex.get(storeInstance.getIdentifier()), String.format("Can't find authentication configuration for store '%s'", storeInstance.getIdentifier()));
    }
}
