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

package org.finos.legend.connection.protocol;

import org.finos.legend.connection.impl.ApiKeyAuthenticationConfiguration;
import org.finos.legend.connection.impl.EncryptedPrivateKeyPairAuthenticationConfiguration;
import org.finos.legend.connection.impl.KerberosAuthenticationConfiguration;
import org.finos.legend.connection.impl.UserPasswordAuthenticationConfiguration;

public enum AuthenticationMechanismType implements AuthenticationMechanism
{
    USER_PASSWORD
            {
                @Override
                public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                {
                    return UserPasswordAuthenticationConfiguration.class;
                }
            },

    API_KEY
            {
                @Override
                public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                {
                    return ApiKeyAuthenticationConfiguration.class;
                }
            },

    KEY_PAIR
            {
                @Override
                public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                {
                    return EncryptedPrivateKeyPairAuthenticationConfiguration.class;
                }
            },

    KERBEROS
            {
                @Override
                public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                {
                    return KerberosAuthenticationConfiguration.class;
                }

                @Override
                public AuthenticationConfiguration generateConfiguration()
                {
                    return new KerberosAuthenticationConfiguration();
                }
            };

    @Override
    public String getLabel()
    {
        return this.toString();
    }
}
