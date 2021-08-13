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
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.UserPasswordAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.RedshiftDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.RedshiftDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Test;

import javax.security.auth.Subject;
import java.io.FileInputStream;
import java.sql.Connection;
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
                        new SnowflakePublicAuthenticationStrategy("SF_KEY", "SF_PASS", "LEGEND_RO_PIERRE"),
                        new RelationalExecutorInfo());
        try (Connection connection = toDBConnection.valueOf(ds))
        {
            testConnection(connection, "select * from KNOEMA_RENEWABLES_DATA_ATLAS.RENEWABLES.DATASETS");
        }

    }

    @Test
    public void testRedshiftConnection_profile() throws Exception
    {
        testRedshiftConnection(c -> c.getConnectionUsingProfiles(null));
//        testRedshiftConnection(RedshiftDatasourceSpecification(databaseName = 'dev', port = 5439,
//                endpoint = 'lab.cqzp3tj1qpzo.us-east-2.redshift.amazonaws.com'));
//
//       b, datasourceSpecification = ^meta::pure::alloy::connections::alloy::specification:
//        :RedshiftDatasourceSpecification(databaseName = 'dev', port = 5439,
//            endpoint = 'lab.cqzp3tj1qpzo.us-east-2.redshift.amazonaws.com'),
//            authenticationStrategy = ^meta::pure::alloy::connections::alloy::authentication::UserPasswordAuthenticationStrategy(passwordVaultReference = 'rEdShiFt528491!', userName = 'awsuser'),
//            type = $dbType -> cast(@DatabaseType));,

    }

    private void testRedshiftConnection(Function<DataSourceSpecification, Connection> toDBConnection) throws Exception
    {
        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream("redshift.properties"));
        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(properties));

        RedshiftDataSourceSpecification ds =
                new RedshiftDataSourceSpecification(
                        new RedshiftDataSourceSpecificationKey(
                                "dev", "lab.cqzp3tj1qpzo.us-east-2.redshift.amazonaws.com", 5439),
                        new org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.redshift.RedshiftManager(),
                        new UserPasswordAuthenticationStrategy("awsuser",System.getenv("redshiftPassword")),
                        new RelationalExecutorInfo());
        ;
        try (Connection connection = toDBConnection.valueOf(ds))
        {
            testConnection(connection, "SELECT * FROM INFORMATION_SCHEMA.TABLES");
        }
    }

}
