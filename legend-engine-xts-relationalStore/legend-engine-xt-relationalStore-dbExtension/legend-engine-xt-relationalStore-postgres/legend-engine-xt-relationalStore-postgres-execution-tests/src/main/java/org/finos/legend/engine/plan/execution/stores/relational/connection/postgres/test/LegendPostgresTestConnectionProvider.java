// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test;

import org.finos.legend.authentication.intermediationrule.IntermediationRuleProvider;
import org.finos.legend.engine.store.core.LegendStoreTestConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;

public class LegendPostgresTestConnectionProvider implements LegendStoreTestConnectionProvider<Connection>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendPostgresTestConnectionProvider.class);

    private PostgresTestContainerWrapper wrapper;

    @Override
    public void initialize() throws Exception
    {
        this.loadDriver();

        boolean dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        if (!dockerAvailable)
        {
            throw new RuntimeException("Postgres test connection provider cannot be used - Docker not available");
        }
        this.wrapper = PostgresTestContainerWrapper.build();
        wrapper.start();
    }

    @Override
    public void shutdown() throws Exception
    {
        if (this.wrapper == null)
        {
            return;
        }
        try
        {
            this.wrapper.stop();
        }
        catch (Exception e)
        {
            LOGGER.warn("Failed to shutdown postgres container", e);
        }
    }

    @Override
    public Connection getConnection() throws Exception
    {
        return DriverManager.getConnection(
                this.wrapper.getJdbcUrl(),
                this.wrapper.getUser(),
                this.wrapper.getPassword()
        );
    }

    private void loadDriver()
    {
        try
        {
            Class.forName("org.postgresql.Driver");
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configureConnection(Connection connection) throws Exception
    {
        throw new UnsupportedOperationException("unsupported feature");
    }

    @Override
    public Optional<IntermediationRuleProvider> getIntermediationRuleProvider() throws Exception
    {
        return Optional.empty();
    }

    public String getUrl()
    {
        return this.wrapper.getJdbcUrl();
    }

    public String getUser()
    {
        return this.wrapper.getUser();
    }

    public String getPassword()
    {
        return this.wrapper.getPassword();
    }

}
