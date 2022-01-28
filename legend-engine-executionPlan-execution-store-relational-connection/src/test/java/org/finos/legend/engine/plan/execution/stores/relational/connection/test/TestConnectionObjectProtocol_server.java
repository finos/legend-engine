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
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.DefaultH2AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.StaticDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.StaticDataSourceSpecificationKey;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.h2.tools.Server;
import org.junit.Test;

import javax.security.auth.Subject;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

public class TestConnectionObjectProtocol_server extends org.finos.legend.engine.plan.execution.stores.relational.connection.test.DbSpecificTests
{
    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @Test
    public void testSnowflakePublicConnection_subject() throws Exception
    {
        testSnowflakePublicConnection(c -> c.getConnectionUsingSubject(getSubject()));
    }

    @Test
    public void testSnowflakePublicConnection_profile() throws Exception
    {
        testSnowflakePublicConnection(c -> c.getConnectionUsingProfiles(null));
    }

    private void testSnowflakePublicConnection(Function<DataSourceSpecification, Connection> toDBConnection) throws Exception
    {
        Properties properties = new Properties();
        properties.load(new FileInputStream("../legend-engine-server/src/test/resources/org/finos/legend/engine/server/test/snowflake.properties"));
        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(properties));

        SnowflakeDataSourceSpecification ds =
                new SnowflakeDataSourceSpecification(
                        new SnowflakeDataSourceSpecificationKey("ki79827", "us-east-2", "LEGENDRO_WH", "KNOEMA_RENEWABLES_DATA_ATLAS", "aws", null),
                        new SnowflakeManager(),
                        new SnowflakePublicAuthenticationStrategy("SF_KEY", "SF_PASS", "LEGEND_RO_PIERRE"));
        try (Connection connection = toDBConnection.valueOf(ds))
        {
            testConnection(connection, "select * from KNOEMA_RENEWABLES_DATA_ATLAS.RENEWABLES.DATASETS");
        }

    }

    @Test
    public void testStaticH2Connection() throws Exception
    {
        String staticDBName = tempFolder.newFolder().getAbsolutePath() + "/staticTestDB";

        int port1 = DynamicPortGenerator.generatePort();
        StaticDataSourceSpecification ds1 = new StaticDataSourceSpecification(new StaticDataSourceSpecificationKey("0", port1, staticDBName), new H2Manager(), new DefaultH2AuthenticationStrategy());
        Server server1 = AlloyH2Server.startServer(port1);
        try (Connection connection = ds1.getConnectionUsingSubject(null))
        {
            try (Statement stmt = connection.createStatement())
            {
                stmt.execute("CREATE TABLE personTable (ID INT, NAME VARCHAR(100))");
                stmt.execute("INSERT INTO personTable VALUES (1,'Peter'), (2,'John')");
            }
        }
        finally
        {
            server1.shutdown();
            server1.stop();
        }

        int port2 = DynamicPortGenerator.generatePort();
        StaticDataSourceSpecification ds2 = new StaticDataSourceSpecification(new StaticDataSourceSpecificationKey("0", port2, staticDBName), new H2Manager(), new DefaultH2AuthenticationStrategy());
        Server server2 = AlloyH2Server.startServer(port2);
        try (Connection connection = ds2.getConnectionUsingSubject(null))
        {
            testConnection(connection, "SELECT * FROM personTable");
        }
        finally
        {
            server2.shutdown();
            server2.stop();
        }
    }
}
