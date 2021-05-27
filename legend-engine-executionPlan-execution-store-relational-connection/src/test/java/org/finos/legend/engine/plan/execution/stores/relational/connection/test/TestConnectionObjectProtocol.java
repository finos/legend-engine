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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.EmbeddedH2DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.LocalH2DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.EmbeddedH2DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.LocalH2DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.h2.tools.Server;
import org.junit.Test;

import javax.security.auth.Subject;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestConnectionObjectProtocol extends org.finos.legend.engine.plan.execution.stores.relational.connection.test.DbSpecificTests
{
    @Override
    protected Subject getSubject()
    {
        return null;//SubjectTools.getLocalSubject();
    }

    @Test
    public void testLocalTestConnection() throws Exception
    {
        Server server = AlloyH2Server.startServer(1234);
        LocalH2DataSourceSpecification ds =
                new LocalH2DataSourceSpecification(
                        new LocalH2DataSourceSpecificationKey(
                                1234,
                                "testDB"),
                        new org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager(),
                        new TestDatabaseAuthenticationStrategy(),
                        server,
                        new RelationalExecutorInfo());
        testConnection(ds::getConnectionUsingSubject, "SELECT * FROM INFORMATION_SCHEMA.TABLES");
    }

    @Test
    public void testEmbeddedH2Connection() throws Exception
    {
        EmbeddedH2DataSourceSpecification ds =
                new EmbeddedH2DataSourceSpecification(
                        new EmbeddedH2DataSourceSpecificationKey(
                                "testDB", tempFolder.newFolder()),
                        new org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager(),
                        new TestDatabaseAuthenticationStrategy(),
                        new RelationalExecutorInfo());
        testConnection(ds::getConnectionUsingSubject, "SELECT * FROM INFORMATION_SCHEMA.TABLES");
    }

    @Test
    public void testSnowflakePublicConnection() throws Exception
    {
        Properties properties = new Properties();
        properties.load(new FileInputStream("../legend-engine-server/src/test/resources/org/finos/legend/engine/server/test/snowflake.properties"));
        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(properties));

        SnowflakeDataSourceSpecification ds =
                new SnowflakeDataSourceSpecification(
                        new SnowflakeDataSourceSpecificationKey("ki79827", "us-east-2", "LEGENDRO_WH", "KNOEMA_RENEWABLES_DATA_ATLAS"),
                        new SnowflakeManager(),
                        new SnowflakePublicAuthenticationStrategy("SF_KEY", "SF_PASS", "LEGEND_RO_PIERRE"),
                        new RelationalExecutorInfo());
        testConnection(ds::getConnectionUsingSubject, "select * from KNOEMA_RENEWABLES_DATA_ATLAS.RENEWABLES.DATASETS");
    }

    protected void testConnection(Function<Subject, Connection> toDBConnection, String sqlExpression) throws Exception
    {
        // Kerberos
        Subject subject = getSubject();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        MutableList<Future<Boolean>> result = FastList.newList();
        for (int i = 0; i < 10; i++)
        {
            result.add(executor.submit(() -> {
                try (Connection connection = toDBConnection.valueOf(subject);
                     Statement st = connection.createStatement();
                     ResultSet resultSet = st.executeQuery(sqlExpression))
                {
                    while (resultSet.next())
                    {
                        for (int i1 = 1; i1 < resultSet.getMetaData().getColumnCount() + 1; i1++)
                        {
                            System.out.println(resultSet.getMetaData().getColumnLabel(i1) + " = " + resultSet.getObject(i1));
                        }
                    }
                    return true;
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }));
        }
        executor.shutdown();
        executor.awaitTermination(100000, TimeUnit.MINUTES);

        boolean res = true;
        for (Future<Boolean> val : result)
        {
            res = res && val.get();
        }
        assert (res);
    }
}
