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

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanism;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AuthenticationMechanismConfiguration
{
    private final AuthenticationMechanism authenticationMechanism;
    private final ImmutableList<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes;
    private final Function0<AuthenticationConfiguration> defaultAuthenticationConfigurationGenerator;

    private AuthenticationMechanismConfiguration(AuthenticationMechanism authenticationMechanism, List<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes, Function0<AuthenticationConfiguration> defaultAuthenticationConfigurationGenerator)
    {
        this.authenticationMechanism = Objects.requireNonNull(authenticationMechanism, "Authentication mechanism is missing");
        this.authenticationConfigurationTypes = Lists.immutable.withAll(authenticationConfigurationTypes);
        this.defaultAuthenticationConfigurationGenerator = defaultAuthenticationConfigurationGenerator;
    }

    public AuthenticationMechanism getAuthenticationMechanism()
    {
        return authenticationMechanism;
    }

    public ImmutableList<Class<? extends AuthenticationConfiguration>> getAuthenticationConfigurationTypes()
    {
        return authenticationConfigurationTypes;
    }

    public Function0<AuthenticationConfiguration> getDefaultAuthenticationConfigurationGenerator()
    {
        return defaultAuthenticationConfigurationGenerator;
    }

    public static class Builder
    {
        private final AuthenticationMechanism authenticationMechanism;
        private final Set<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes = new LinkedHashSet<>();
        private Function0<AuthenticationConfiguration> defaultAuthenticationConfigurationGenerator;

        public Builder(AuthenticationMechanism authenticationMechanism)
        {
            this.authenticationMechanism = authenticationMechanism;
        }

        public Builder withAuthenticationConfigurationType(Class<? extends AuthenticationConfiguration> authenticationConfigurationType)
        {
            this.authenticationConfigurationTypes.add(authenticationConfigurationType);
            return this;
        }

        public Builder withAuthenticationConfigurationTypes(List<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes)
        {
            this.authenticationConfigurationTypes.addAll(authenticationConfigurationTypes);
            return this;
        }

        @SafeVarargs
        public final Builder withAuthenticationConfigurationTypes(Class<? extends AuthenticationConfiguration>... authenticationConfigurationTypes)
        {
            this.authenticationConfigurationTypes.addAll(Lists.mutable.of(authenticationConfigurationTypes));
            return this;
        }

        public Builder withDefaultAuthenticationConfigurationGenerator(Function0<AuthenticationConfiguration> defaultAuthenticationConfigurationGenerator)
        {
            this.defaultAuthenticationConfigurationGenerator = defaultAuthenticationConfigurationGenerator;
            return this;
        }

        public AuthenticationMechanismConfiguration build()
        {
            return new AuthenticationMechanismConfiguration(
                    this.authenticationMechanism,
                    new ArrayList<>(this.authenticationConfigurationTypes),
                    this.defaultAuthenticationConfigurationGenerator
            );
        }
    }
}
