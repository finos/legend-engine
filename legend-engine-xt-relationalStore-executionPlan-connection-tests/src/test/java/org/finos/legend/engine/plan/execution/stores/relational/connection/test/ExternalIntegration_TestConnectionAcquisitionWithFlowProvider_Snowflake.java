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

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToJsonDefaultSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.EnvironmentVaultImplementation;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_Snowflake extends org.finos.legend.engine.plan.execution.stores.relational.connection.test.DbSpecificTests
{
    private ConnectionManagerSelector connectionManagerSelector;

    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @BeforeClass
    public static void setupTest() throws IOException
    {
        Vault.INSTANCE.registerImplementation(new EnvironmentVaultImplementation());
    }

    @Before
    public void setup()
    {
        LegendDefaultDatabaseAuthenticationFlowProvider flowProvider = new LegendDefaultDatabaseAuthenticationFlowProvider();
        flowProvider.configure(new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration());
        assertSnowflakeKeyPairFlowIsAvailable(flowProvider);
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), Optional.of(flowProvider));
    }

    public void assertSnowflakeKeyPairFlowIsAvailable(LegendDefaultDatabaseAuthenticationFlowProvider flowProvider)
    {
        SnowflakeDatasourceSpecification datasourceSpecification = new SnowflakeDatasourceSpecification();
        SnowflakePublicAuthenticationStrategy authenticationStrategy = new SnowflakePublicAuthenticationStrategy();
        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection(datasourceSpecification, authenticationStrategy, DatabaseType.Snowflake);
        relationalDatabaseConnection.type = DatabaseType.Snowflake;

        Optional<DatabaseAuthenticationFlow> flow = flowProvider.lookupFlow(relationalDatabaseConnection);
        assertTrue("snowflake keypair flow does not exist ", flow.isPresent());
    }

    @Test
    public void testSnowflakePublicConnection_subject() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.snowflakeWithKeyPairSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
        testConnection(connection, "select * from INTEGRATION_DB1.INTEGRATION_SCHEMA1.test");
    }

    @Test
    public void testSnowflakePublicConnection_profile() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.snowflakeWithKeyPairSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((MutableList<CommonProfile>) null, systemUnderTest);
        testConnection(connection, "select * from INTEGRATION_DB1.INTEGRATION_SCHEMA1.test");
    }

    private RelationalDatabaseConnection snowflakeWithKeyPairSpec() throws Exception
    {
        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        snowflakeDatasourceSpecification.accountName = "ki79827";
        snowflakeDatasourceSpecification.region = "us-east-2";
        snowflakeDatasourceSpecification.warehouseName = "INTEGRATION_WH1";
        snowflakeDatasourceSpecification.databaseName = "INTEGRATION_DB1";
        snowflakeDatasourceSpecification.role = "INTEGRATION_ROLE1";
        snowflakeDatasourceSpecification.cloudType = "aws";
        SnowflakePublicAuthenticationStrategy authSpec = new SnowflakePublicAuthenticationStrategy();
        authSpec.publicUserName = "INTEGRATION_USER1";
        authSpec.privateKeyVaultReference = "SNOWFLAKE_INTEGRATION_USER1_PRIVATEKEY";
        authSpec.passPhraseVaultReference = "SNOWFLAKE_INTEGRATION_USER1_PASSWORD";
        return new RelationalDatabaseConnection(snowflakeDatasourceSpecification, authSpec, DatabaseType.Snowflake);
    }

    // TODO - This test is deliberately ignored. The Snowflake user/key used to run this test requires additional privileges on the Snowflake account
    @Test
    public void executePlan() throws Exception
    {
        Properties properties = new Properties();
        properties.put("PK_VAULT_REFERENCE", "invalid");
        properties.put("PASSPHRASE_VAULT_REFERENCE", "invalid");
        PropertiesVaultImplementation propertiesVaultImplementation = new PropertiesVaultImplementation(properties);
        Vault.INSTANCE.registerImplementation(propertiesVaultImplementation);

        String planJSON = new String(Files.readAllBytes(Paths.get(ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_Snowflake.class.getResource("/snowflake-graph-fetch-plan.json").toURI())));

        RelationalExecutionConfiguration relationalExecutionConfiguration = RelationalExecutionConfiguration.newInstance()
                .withTemporaryTestDbConfiguration(new TemporaryTestDbConfiguration(9078))
                .withDatabaseAuthenticationFlowProvider(LegendDefaultDatabaseAuthenticationFlowProvider.class, new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration())
                .build();
        StoreExecutor storeExecutor = Relational.build(relationalExecutionConfiguration);

        PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(storeExecutor);

        Result result = planExecutor.execute(planJSON);
        JsonStreamToJsonDefaultSerializer jsonStreamToJsonDefaultSerializer = new JsonStreamToJsonDefaultSerializer(((JsonStreamingResult) result));
        OutputStream outputStream = new ByteArrayOutputStream();
        jsonStreamToJsonDefaultSerializer.stream(outputStream);

        String expected = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"defects\":[],\"value\":{\"legalName\":\"firm1\",\"employees\":[{\"firstName\":\"pf1\",\"lastName\":\"pl1\"},{\"firstName\":\"pf2\",\"lastName\":\"pl2\"},{\"firstName\":\"pf3\",\"lastName\":\"pl3\"}]}},{\"defects\":[],\"value\":{\"legalName\":\"firm2\",\"employees\":[{\"firstName\":\"pf4\",\"lastName\":\"pl4\"}]}},{\"defects\":[],\"value\":{\"legalName\":\"firm3\",\"employees\":[{\"firstName\":\"pf5\",\"lastName\":\"pl5\"}]}},{\"defects\":[],\"value\":{\"legalName\":\"firm4\",\"employees\":[{\"firstName\":\"pf6\",\"lastName\":\"pl6\"}]}},{\"defects\":[],\"value\":{\"legalName\":\"firm5\",\"employees\":[]}}]}";
        assertEquals(expected, outputStream.toString());
    }

    @Ignore
    public void testTempTableHandlingWithRawJDBC() throws Exception
    {
        Class.forName("net.snowflake.client.jdbc.SnowflakeDriver");

        Properties properties = new Properties();
        properties.put("ocspFailOpen", "true");
        properties.put("account", "ki79827");
        properties.put("warehouse", "LEGENDRO_WH");
        properties.put("db", "LEGEND_TEMP_DB");
        properties.put("schema", "LEGEND_TEMP_SCHEMA");
        properties.put("role", "LEGEND_INTEGRATION_ROLE1");
        properties.put("user", "GITHUB_EPSSTAN");
        properties.put("password", "xxxxx!");
        //properties.put("privateKey", "xxxxx");
        String url = "jdbc:snowflake://ki79827.us-east-2.aws.snowflakecomputing.com";
        Connection connection = DriverManager.getConnection(url, properties);

        String data = Lists.immutable.of("1", "2", "3").makeString("\n");
        Path tempPath = Files.createTempFile("temp", "temp").toAbsolutePath();
        Files.write(tempPath, data.getBytes(), StandardOpenOption.APPEND);

        ImmutableList<String> sqls = Lists.immutable.of(
                "CREATE TEMPORARY TABLE temp_1 (a VARCHAR(100))",
                "CREATE OR REPLACE TEMPORARY STAGE LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.LEGEND_TEMP_STAGE",
                "PUT file://" + tempPath.toAbsolutePath().toString() + " @LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.LEGEND_TEMP_STAGE//tmp/temp.csv PARALLEL = 16 AUTO_COMPRESS = TRUE",
                "COPY INTO temp_1 FROM @LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.LEGEND_TEMP_STAGE file_format = (type = CSV field_optionally_enclosed_by= '\"')",
                "DROP STAGE LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.LEGEND_TEMP_STAGE"
        );

        Statement statement = connection.createStatement();
        for (String sql : sqls)
        {
            System.out.println("Executing " + sql);
            statement.execute(sql);
        }
    }
}