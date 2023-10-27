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
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.AuthenticationConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AuthenticationMechanism
{
    private final AuthenticationMechanismType authenticationMechanismType;
    private final ImmutableList<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes;

    private AuthenticationMechanism(AuthenticationMechanismType authenticationMechanismType, List<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes)
    {
        this.authenticationMechanismType = Objects.requireNonNull(authenticationMechanismType, "Authentication mechanism is missing");
        this.authenticationConfigurationTypes = Lists.immutable.withAll(authenticationConfigurationTypes);
    }

    public AuthenticationMechanismType getAuthenticationMechanismType()
    {
        return authenticationMechanismType;
    }

    public ImmutableList<Class<? extends AuthenticationConfiguration>> getAuthenticationConfigurationTypes()
    {
        return authenticationConfigurationTypes;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private AuthenticationMechanismType authenticationMechanismType;
        private final Set<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes = new LinkedHashSet<>();

        private Builder()
        {
        }

        public Builder type(AuthenticationMechanismType authenticationMechanismType)
        {
            this.authenticationMechanismType = authenticationMechanismType;
            return this;
        }

        public Builder authenticationConfigurationType(Class<? extends AuthenticationConfiguration> authenticationConfigurationType)
        {
            this.authenticationConfigurationTypes.add(authenticationConfigurationType);
            return this;
        }

        public Builder authenticationConfigurationTypes(List<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes)
        {
            this.authenticationConfigurationTypes.addAll(authenticationConfigurationTypes);
            return this;
        }

        @SafeVarargs
        public final Builder authenticationConfigurationTypes(Class<? extends AuthenticationConfiguration>... authenticationConfigurationTypes)
        {
            this.authenticationConfigurationTypes.addAll(Lists.mutable.of(authenticationConfigurationTypes));
            return this;
        }

        public AuthenticationMechanism build()
        {
            return new AuthenticationMechanism(
                    this.authenticationMechanismType,
                    new ArrayList<>(this.authenticationConfigurationTypes)
            );
        }
    }
}
