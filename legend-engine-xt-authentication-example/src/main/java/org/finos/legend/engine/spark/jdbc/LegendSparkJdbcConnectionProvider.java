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

package org.finos.legend.engine.spark.jdbc;

import org.apache.spark.sql.jdbc.JdbcConnectionProvider;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.connection.ConnectionSpecificationProvider;
import org.finos.legend.engine.connection.jdbc.LegendJdbcConnectionProvider;
import scala.collection.immutable.Map;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Optional;

public class LegendSparkJdbcConnectionProvider extends JdbcConnectionProvider
{
    public static Builder builder()
    {
        return new Builder();
    }

    private ImmutableList<LegendJdbcConnectionProvider> jdbcConnectionProviders;

    public LegendSparkJdbcConnectionProvider(ImmutableList<LegendJdbcConnectionProvider> jdbcConnectionProviders)
    {
        this.jdbcConnectionProviders = jdbcConnectionProviders;
    }

    @Override
    public String name()
    {
        return "legend-spark-jdbc";
    }

    @Override
    public boolean canHandle(Driver driver, Map<String, String> options)
    {
        return this.findProvider(driver, options).isPresent();
    }

    private Optional<LegendJdbcConnectionProvider> findProvider(Driver driver, Map<String, String> options)
    {
        ImmutableList<LegendJdbcConnectionProvider> matches = this.jdbcConnectionProviders.select(p -> p.canHandle(driver, options));
        if (matches.isEmpty())
        {
            return Optional.empty();
        }
        if (matches.size() > 1)
        {
            ImmutableList<String> providerNames = matches.collect(p -> p.getClass().getCanonicalName());
            throw new RuntimeException(String.format("Too many connection providers found. Driver='%s'. Matching providers=%s", driver.getClass().getCanonicalName(), providerNames));
        }
        return Optional.of(matches.get(0));
    }

    @Override
    public Connection getConnection(Driver driver, Map<String, String> options)
    {
        try
        {
            Optional<LegendJdbcConnectionProvider> holder = this.findProvider(driver, options);
            if (!holder.isPresent())
            {
                throw new RuntimeException(String.format("Connection provider not found. Driver='%s'", driver.getClass().getCanonicalName()));
            }
            return holder.get().getConnection(driver, options);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean modifiesSecurityContext(Driver driver, Map<String, String> options)
    {
        return false;
    }

    public static class Builder
    {
        private CredentialProviderProvider credentialProviderProvider;
        private ConnectionSpecificationProvider connectionSpecificationProvider;
        private MutableList<LegendJdbcConnectionProvider> connectionProviders = Lists.mutable.empty();

        public Builder with(CredentialProviderProvider credentialProviderProvider)
        {
            this.credentialProviderProvider = credentialProviderProvider;
            return this;
        }

        public Builder with(ConnectionSpecificationProvider connectionSpecificationProvider)
        {
            this.connectionSpecificationProvider = connectionSpecificationProvider;
            return this;
        }

        public Builder with(LegendJdbcConnectionProvider jdbcConnectionProvider)
        {
            this.connectionProviders.add(jdbcConnectionProvider);
            return this;
        }

        public LegendSparkJdbcConnectionProvider build()
        {
            return new LegendSparkJdbcConnectionProvider(this.connectionProviders.toImmutable());
        }
    }
}
