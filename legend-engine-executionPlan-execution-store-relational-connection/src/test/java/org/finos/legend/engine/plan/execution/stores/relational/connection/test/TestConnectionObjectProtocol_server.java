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
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.spanner.SpannerManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SpannerDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SpannerDataSourceSpecificationKey;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Test;

import javax.security.auth.Subject;
import java.io.FileInputStream;
import java.sql.Connection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

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
    public void testSpannerConnection_subject() throws Exception
    {
        testSpannerPublicConnection(c -> c.getConnectionUsingSubject(getSubject()));
    }

    @Test
    public void testSpannerConnection_profile() throws Exception
    {
        testSpannerPublicConnection(c -> c.getConnectionUsingProfiles(null));
    }

    /*
        Note : This test is a very weak test.
        For now, it only asserts that there are no errors in parsing the Jdbc connection properties etc.
        Because of the auth strategy being used, either this test has to execute in a GCP environment or credentials have to be injected via the GOOGLE_APPLICATION_CREDENTIALS env variable
     */
    private void testSpannerPublicConnection(Function<DataSourceSpecification, Connection> toDBConnection) throws Exception
    {
        SpannerDataSourceSpecification ds =
                new SpannerDataSourceSpecification(
                        new SpannerDataSourceSpecificationKey("legend-integration-testing", "instance-1", "database-1"),
                        new SpannerManager(),
                        new GCPApplicationDefaultCredentialsAuthenticationStrategy(),
                        new RelationalExecutorInfo());
        try (Connection connection = toDBConnection.valueOf(ds))
        {
            testConnection(connection, "select * from Employees");
        }
        catch (Exception e)
        {
            String expectedMessage = "Failed to initialize pool: com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: Invalid credentials path specified: There are no credentials set in the connection string, and the default application credentials are not set or are pointing to an invalid or non-existing file.\nPlease check the GOOGLE_APPLICATION_CREDENTIALS environment variable and/or the credentials that have been set using the Google Cloud SDK gcloud auth application-default login command";
            assertEquals(expectedMessage, e.getMessage());
        }
    }

}
