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

package org.finos.legend.engine.shared.core.vault;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

public class DefaultVaultExtension implements VaultExtension
{
    @Override
    public List<Pair<String, Class<? extends VaultConfiguration>>> getExtraVaultConfigurationSubTypes()
    {
        return Lists.mutable.with(Tuples.pair("property", PropertyVaultConfiguration.class));
    }

    @Override
    public List<Function<VaultConfiguration, VaultImplementation>> getExtraVaultImplementationGenerators()
    {
        return Lists.mutable.with(
                v ->
                {
                    if (v instanceof PropertyVaultConfiguration)
                    {
                        try
                        {
                            Properties properties = new Properties();
                            properties.load(new FileInputStream(((PropertyVaultConfiguration) v).location));
                            return new PropertiesVaultImplementation(properties);
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                    return null;
                }
        );
    }
}
