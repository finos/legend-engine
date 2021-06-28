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

package org.finos.legend.engine.plan.execution.stores.relational.connection.test;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.DefaultH2AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.EmbeddedH2DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.LocalH2DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.EmbeddedH2DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.LocalH2DataSourceSpecificationKey;
import org.junit.Assert;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class TestConnectionObjectProtocol_local extends DbSpecificTests
{
    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @Test
    public void testLocalTestConnection_subject() throws Exception
    {
        testLocalTestConnection(c -> c.getConnectionUsingSubject(getSubject()));
    }


    @Test
    public void testSequentialCallsLocalTestConnection_subject() throws Exception
    {

        DataSourceSpecification dsTwo = buildLocalDataSourceSpecification(new OtherTestDatabaseAuthenticationStrategy("mockuser", "mockPass"));
        Connection connectionTwo = dsTwo.getConnectionUsingSubject(getSubject());
        Assert.assertNotNull(connectionTwo);


        DataSourceSpecification dsOne = buildLocalDataSourceSpecification(new TestDatabaseAuthenticationStrategy());
        Connection connectionOne = dsOne.getConnectionUsingSubject(getSubject());
        Assert.assertNotNull(connectionOne);
        Assert.assertEquals(connectionOne.getMetaData().getURL(), connectionTwo.getMetaData().getURL());
        Assert.assertNotEquals(connectionOne.getMetaData().getUserName(), connectionTwo.getMetaData().getUserName());


        DataSourceSpecification dsThree = buildLocalDataSourceSpecification(new OtherTestDatabaseAuthenticationStrategy("mockuser2", "mockPass2"));
        Connection connectionThree= dsThree.getConnectionUsingSubject(getSubject());
        Assert.assertNotEquals(connectionThree.getMetaData().getUserName(),connectionOne.getMetaData().getUserName());
        Assert.assertEquals(connectionThree.getMetaData().getUserName(),connectionTwo.getMetaData().getUserName());


    }


    private static class OtherTestDatabaseAuthenticationStrategy extends AuthenticationStrategy
    {
        private String login;
        private String token;

        public OtherTestDatabaseAuthenticationStrategy(String login, String token)
        {
            this.login = login;
            this.token = token;
            this.password =token;
        }

        @Override
        protected Connection getConnectionImpl(DataSourceWithStatistics ds, Subject subject, MutableList<CommonProfile> profiles) throws ConnectionException
        {
            try
            {
                return ds.getDataSource().getConnection(login,token);
            }
            catch (SQLException e)
            {
                throw new ConnectionException(e);
            }
        }

        @Override
        public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
        {
            properties.put("pro1",this.login);
            properties.put("token",this.token);

            OtherTestDatabaseAuthenticationStrategy newStrategy = new OtherTestDatabaseAuthenticationStrategy(login, token);
            properties.putAll(databaseManager.getExtraDataSourceProperties(newStrategy));
            return Tuples.pair(url, properties);
        }

        @Override
        public AuthenticationStrategyKey getKey()
        {
            return new OtherH2AuthenticationStrategyKey(login, password);
        }

        @Override
        public String getLogin()
        {
            return this.login;
        }

        @Override
        public String getPassword()
        {
            return this.token;
        }
    }

    private static class OtherH2AuthenticationStrategyKey implements AuthenticationStrategyKey
    {
        private final String login;
        private final String password;

        private OtherH2AuthenticationStrategyKey(String login, String password)
        {
            this.login = login;
            this.password = password;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OtherH2AuthenticationStrategyKey that = (OtherH2AuthenticationStrategyKey)o;
            return Objects.equals(login, that.login) &&
                    Objects.equals(password, that.password);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(login, password);
        }

        @Override
        public String shortId()
        {
            return this.login + "-" + this.password;
        }

        @Override
        public String type()
        {
            return "OtherH2";
        }
    }

    @Test
    public void testLocalTestConnection_profile() throws Exception
    {
        testLocalTestConnection(c -> c.getConnectionUsingProfiles(null));
    }

    private void testLocalTestConnection(Function<DataSourceSpecification, Connection> toDBConnection, AuthenticationStrategy authenticationStrategy) throws Exception
    {
        LocalH2DataSourceSpecification ds = buildLocalDataSourceSpecification(authenticationStrategy);
        try (Connection connection = toDBConnection.valueOf(ds))
        {
            testConnection(connection, "SELECT * FROM PersonTable");
        }
    }

    private LocalH2DataSourceSpecification buildLocalDataSourceSpecification(AuthenticationStrategy authenticationStrategy)
    {
        return new LocalH2DataSourceSpecification(
                new LocalH2DataSourceSpecificationKey(Lists.mutable.with("drop table if exists PersonTable;",
                        "create table PersonTable(id INT, firmId INT, firstName VARCHAR(200), lastName VARCHAR(200));",
                        "insert into PersonTable (id, firmId, firstName, lastName) values (1, 1, 'pierre', 'de belen');",
                        "drop table if exists FirmTable;",
                        "create table FirmTable(id INT, legalName VARCHAR(200));",
                        "insert into FirmTable (id, legalName) values (1, 'firm')")),
                new org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager(),
                authenticationStrategy,
                new RelationalExecutorInfo());
    }

    private void testLocalTestConnection(Function<DataSourceSpecification, Connection> toDBConnection) throws Exception
    {
        testLocalTestConnection(toDBConnection, new TestDatabaseAuthenticationStrategy());
    }

    @Test
    public void testEmbeddedH2Connection_subject() throws Exception
    {
        testEmbeddedH2Connection(c -> c.getConnectionUsingSubject(null));
    }

    @Test
    public void testEmbeddedH2Connection_profile() throws Exception
    {
        testEmbeddedH2Connection(c -> c.getConnectionUsingProfiles(null));
    }

    private void testEmbeddedH2Connection(Function<DataSourceSpecification, Connection> toDBConnection) throws Exception
    {
        EmbeddedH2DataSourceSpecification ds =
                new EmbeddedH2DataSourceSpecification(
                        new EmbeddedH2DataSourceSpecificationKey(
                                "testDB", tempFolder.newFolder()),
                        new org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager(),
                        new TestDatabaseAuthenticationStrategy(),
                        new RelationalExecutorInfo());
        try (Connection connection = toDBConnection.valueOf(ds))
        {
            testConnection(connection, "SELECT * FROM INFORMATION_SCHEMA.TABLES");
        }
    }
}
