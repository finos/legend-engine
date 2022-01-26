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

import java.sql.Connection;

import javax.security.auth.Subject;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.EmbeddedH2DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.LocalH2DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.EmbeddedH2DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.LocalH2DataSourceSpecificationKey;
import org.junit.Test;

public class TestConnectionObjectProtocol_H2 extends DbSpecificTests
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
                Lists.mutable.with("drop table if exists PersonTable;",
                        "create table PersonTable(id INT, firmId INT, firstName VARCHAR(200), lastName VARCHAR(200));",
                        "insert into PersonTable (id, firmId, firstName, lastName) values (1, 1, 'pierre', 'de belen');",
                        "drop table if exists FirmTable;",
                        "create table FirmTable(id INT, legalName VARCHAR(200));",
                        "insert into FirmTable (id, legalName) values (1, 'firm')"),
                new org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager(),
                authenticationStrategy);
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
                        new TestDatabaseAuthenticationStrategy());
        try (Connection connection = toDBConnection.valueOf(ds))
        {
            testConnection(connection, "SELECT * FROM INFORMATION_SCHEMA.TABLES");
        }
    }
}
