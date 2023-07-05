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

package org.finos.legend.authentication.vault.impl;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.authentication.vault.CredentialVaultProvider;

import java.util.Properties;

public class CredentialVaultProviderForTest extends CredentialVaultProvider
{
    private BuilderForTest builder;

    public static BuilderForTest buildForTest()
    {
        return new BuilderForTest();
    }

    public static class BuilderForTest
    {
        private Properties properties = new Properties();
        private MutableMap<String, String> systemProperties = Maps.mutable.empty();

        public BuilderForTest withProperties(Pair<String, String> keyValue)
        {
            this.properties.put(keyValue.getOne(), keyValue.getTwo());
            return this;
        }

        public BuilderForTest withProperties(String key, String value)
        {
            this.properties.put(key, value);
            return this;
        }

        public BuilderForTest withSystemProperties(String key, String value)
        {
            this.systemProperties.put(key, value);
            return this;
        }


        public BuilderForTest withProperties(Properties properties)
        {
            this.properties.putAll(properties);
            return this;
        }

        public CredentialVaultProvider build()
        {
            CredentialVaultProvider credentialVaultProvider = new CredentialVaultProvider();
            credentialVaultProvider.register(new PropertiesFileCredentialVault(this.properties));

            this.systemProperties.forEachKeyValue((key, value) -> System.setProperty(key, value));
            credentialVaultProvider.register(new SystemPropertiesCredentialVault());
            credentialVaultProvider.register(new EnvironmentCredentialVault());

            return credentialVaultProvider;
        }
    }

    private CredentialVaultProviderForTest(BuilderForTest builder)
    {
        this.builder = builder;
    }
}
