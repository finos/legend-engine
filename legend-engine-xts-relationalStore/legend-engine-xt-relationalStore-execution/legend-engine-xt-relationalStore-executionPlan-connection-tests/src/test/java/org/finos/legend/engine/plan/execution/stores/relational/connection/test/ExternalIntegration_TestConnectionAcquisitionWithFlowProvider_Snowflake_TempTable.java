// Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.security.auth.Subject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_Snowflake_TempTable extends DbSpecificTests
{
    private final Boolean quotedIdentifiersIgnoreCaseFlagForSnowflake;
    private final Boolean quotedIdentifiersIgnoreCaseFlagInConnection;
    private final Boolean isExecutionSuccess;
    private final String errorMessage;
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

    public ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_Snowflake_TempTable(Boolean quotedIdentifiersIgnoreCaseFlagForSnowflake, Boolean quotedIdentifiersIgnoreCaseFlagInConnection, Boolean isExecutionSuccess, String errorMessage) throws Exception
    {
       this.quotedIdentifiersIgnoreCaseFlagForSnowflake = quotedIdentifiersIgnoreCaseFlagForSnowflake;
       this.quotedIdentifiersIgnoreCaseFlagInConnection = quotedIdentifiersIgnoreCaseFlagInConnection;
       this.isExecutionSuccess = isExecutionSuccess;
       this.errorMessage = errorMessage;
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

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][]{
                {true,  true,  true,  null},
                {true,  false, false, "java.lang.RuntimeException: java.lang.RuntimeException: java.sql.SQLException: Column not found: parent_key_gen_0" },
                {false, true,  false, "java.lang.RuntimeException: java.lang.RuntimeException: java.sql.SQLException: Column not found: PARENT_KEY_GEN_0" },
                {false, false, true,  null},
        });
    }

    @Test
    public void TestCasesForTempTables() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.snowflakeWithKeyPairSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
        testConnection(connection, String.format("ALTER USER SET QUOTED_IDENTIFIERS_IGNORE_CASE = %s",this.quotedIdentifiersIgnoreCaseFlagForSnowflake));

        String planJSON = new String(Files.readAllBytes(Paths.get(ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_Snowflake_TempTable.class.getResource("/snowflake-graph-fetch-plan.json").toURI())));
        planJSON = planJSON.replace("QUOTED_IDENTIFIERS_IGNORE_CASE_PLACEHOLDER",quotedIdentifiersIgnoreCaseFlagInConnection.toString());

        String result = execute(planJSON);
        String expected = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"defects\":[],\"value\":{\"legalName\":\"firm1\",\"employees\":[{\"firstName\":\"pf1\",\"lastName\":\"pl1\"},{\"firstName\":\"pf2\",\"lastName\":\"pl2\"},{\"firstName\":\"pf3\",\"lastName\":\"pl3\"}]}},{\"defects\":[],\"value\":{\"legalName\":\"firm2\",\"employees\":[{\"firstName\":\"pf4\",\"lastName\":\"pl4\"}]}},{\"defects\":[],\"value\":{\"legalName\":\"firm3\",\"employees\":[{\"firstName\":\"pf5\",\"lastName\":\"pl5\"}]}},{\"defects\":[],\"value\":{\"legalName\":\"firm4\",\"employees\":[{\"firstName\":\"pf6\",\"lastName\":\"pl6\"}]}},{\"defects\":[],\"value\":{\"legalName\":\"firm5\",\"employees\":[]}}]}";
        if (isExecutionSuccess)
        {
            assertEquals(expected, result);
        }
        else
        {
            assertEquals(errorMessage,result);
        }

        testConnection(connection, "ALTER USER UNSET QUOTED_IDENTIFIERS_IGNORE_CASE");
    }

    public String execute(String planJSON)
    {
        try
        {
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
            return outputStream.toString();
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }
}