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

package org.finos.legend.connection;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanism;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A StoreSupport describes the capabilities supported by a Store.
 * For now, it describes the authentication mechanisms.
 */
public class StoreSupport
{
    private final String identifier;
    private final Map<AuthenticationMechanism, AuthenticationMechanismConfiguration> authenticationMechanismConfigurationIndex;

    protected StoreSupport(String identifier, List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations)
    {
        this.identifier = Objects.requireNonNull(identifier, "Identifier is missing");

        Map<AuthenticationMechanism, AuthenticationMechanismConfiguration> authenticationMechanismConfigurationIndex = new LinkedHashMap<>();
        Map<Class<? extends AuthenticationConfiguration>, AuthenticationMechanism> authenticationConfigurationTypeIndex = new LinkedHashMap<>();
        for (AuthenticationMechanismConfiguration authenticationMechanismConfiguration : authenticationMechanismConfigurations)
        {
            AuthenticationMechanism authenticationMechanism = authenticationMechanismConfiguration.getAuthenticationMechanism();
            if (authenticationMechanismConfigurationIndex.containsKey(authenticationMechanism))
            {
                throw new RuntimeException(String.format("Found multiple configurations for authentication mechanism '%s'", authenticationMechanism.getLabel()));
            }
            authenticationMechanismConfigurationIndex.put(authenticationMechanism, authenticationMechanismConfiguration);
            authenticationMechanismConfiguration.getAuthenticationConfigurationTypes().forEach(authenticationConfigurationType ->
            {
                if (authenticationConfigurationTypeIndex.containsKey(authenticationConfigurationType))
                {
                    throw new RuntimeException(String.format("Authentication configuration type '%s' is associated with multiple authentication mechanisms", authenticationConfigurationType.getSimpleName()));
                }
                authenticationConfigurationTypeIndex.put(authenticationConfigurationType, authenticationMechanism);
            });
        }

        this.authenticationMechanismConfigurationIndex = authenticationMechanismConfigurationIndex;
        this.authenticationMechanismConfigurationIndex.forEach((authenticationMechanism, authenticationMechanismConfiguration) ->
        {
            if (authenticationMechanismConfiguration.getAuthenticationConfigurationTypes().isEmpty())
            {
                throw new RuntimeException(String.format("No authentication configuration type is associated with authentication mechanism '%s'", authenticationMechanism.getLabel()));
            }
        });
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public AuthenticationMechanismConfiguration getAuthenticationMechanismConfiguration(AuthenticationMechanism authenticationMechanism)
    {
        return authenticationMechanismConfigurationIndex.get(authenticationMechanism);
    }

    public List<AuthenticationMechanism> getAuthenticationMechanisms()
    {
        return new ArrayList<>(this.authenticationMechanismConfigurationIndex.keySet());
    }

    public static class Builder
    {
        private String identifier;
        private final List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations = Lists.mutable.empty();

        public Builder withIdentifier(String identifier)
        {
            this.identifier = identifier;
            return this;
        }

        public Builder withAuthenticationMechanismConfiguration(AuthenticationMechanismConfiguration authenticationMechanismConfiguration)
        {
            this.authenticationMechanismConfigurations.add(authenticationMechanismConfiguration);
            return this;
        }

        public Builder withAuthenticationMechanismConfigurations(List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations)
        {
            this.authenticationMechanismConfigurations.addAll(authenticationMechanismConfigurations);
            return this;
        }

        public Builder withAuthenticationMechanismConfigurations(AuthenticationMechanismConfiguration... authenticationMechanismConfigurations)
        {
            this.authenticationMechanismConfigurations.addAll(Lists.mutable.of(authenticationMechanismConfigurations));
            return this;
        }

        public StoreSupport build()
        {
            return new StoreSupport(
                    this.identifier,
                    this.authenticationMechanismConfigurations
            );
        }
    }
}
