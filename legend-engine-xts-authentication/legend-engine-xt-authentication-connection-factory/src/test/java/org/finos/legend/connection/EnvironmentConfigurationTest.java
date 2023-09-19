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
import org.junit.Assert;
import org.junit.Test;

public class EnvironmentConfigurationTest
{
    @Test
    public void testValidateBuilder()
    {
        // success
        new EnvironmentConfiguration.Builder()
                .withAuthenticationMechanisms(Lists.mutable.of(
                        TestAuthenticationMechanismType.X,
                        TestAuthenticationMechanismType.Y
                )).build();

        Exception exception;

        // failure: found invalid mechanism
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            new EnvironmentConfiguration.Builder()
                    .withAuthenticationMechanisms(Lists.mutable.of(
                            TestAuthenticationMechanismType.X,
                            TestAuthenticationMechanismType.Y,
                            TestAuthenticationMechanismType.Z
                    )).build();
        });
        Assert.assertEquals("Can't build environment configuration: authentication mechanism 'Z' is misconfigured, its associated configuration type is 'AuthenticationConfiguration_Z' and its generated configuration type is 'AuthenticationConfiguration_X'", exception.getMessage());

        // failure: found conflicting mechanisms
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            new EnvironmentConfiguration.Builder()
                    .withAuthenticationMechanisms(Lists.mutable.of(
                            TestAuthenticationMechanismType.X,
                            TestAuthenticationMechanismType.Y,
                            TestAuthenticationMechanismType.T
                    )).build();
        });
        Assert.assertEquals("Can't build environment configuration: found multiple authentication mechanisms (Y, T) associated with the same configuration type 'AuthenticationConfiguration_Y'", exception.getMessage());
    }

    private static class AuthenticationConfiguration_X extends AuthenticationConfiguration
    {
    }

    private static class AuthenticationConfiguration_Y extends AuthenticationConfiguration
    {
    }

    private static class AuthenticationConfiguration_Z extends AuthenticationConfiguration
    {
    }

    private enum TestAuthenticationMechanismType implements AuthenticationMechanism
    {
        X
                {
                    @Override
                    public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                    {
                        return AuthenticationConfiguration_X.class;
                    }
                },
        Y
                {
                    @Override
                    public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                    {
                        return AuthenticationConfiguration_Y.class;
                    }

                    @Override
                    public AuthenticationConfiguration generateConfiguration()
                    {
                        return new AuthenticationConfiguration_Y();
                    }
                },
        Z
                {
                    @Override
                    public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                    {
                        return AuthenticationConfiguration_Z.class;
                    }

                    @Override
                    public AuthenticationConfiguration generateConfiguration()
                    {
                        return new AuthenticationConfiguration_X();
                    }
                },
        T
                {
                    @Override
                    public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                    {
                        return AuthenticationConfiguration_Y.class;
                    }
                };

        @Override
        public String getLabel()
        {
            return this.toString();
        }
    }
}
