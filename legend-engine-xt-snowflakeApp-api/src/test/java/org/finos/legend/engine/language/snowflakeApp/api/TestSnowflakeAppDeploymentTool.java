package org.finos.legend.engine.language.snowflakeApp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification_Impl;
import org.junit.Before;
import org.junit.Ignore;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class TestSnowflakeAppDeploymentTool
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private ConnectionManagerSelector connectionManager;

    @Before
    public void setup()
    {
        TemporaryTestDbConfiguration conf = new TemporaryTestDbConfiguration();
        conf.port = Integer.parseInt(System.getProperty("h2ServerPort", "1234"));
        this.connectionManager = new ConnectionManagerSelector(conf, FastList.newList());
    }

    // Deliberately ignored - This test requires cred. Search for XXXXX
    @Ignore
    public void testDeployment() throws Exception
    {
        SnowflakeAppDeploymentTool snowflakeAppDeploymentTool = new SnowflakeAppDeploymentTool(connectionManager);

        Object[] testObjects = this.buildSnowflakeLocalConnection();
        Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification ds = (Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification) testObjects[0];
        Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy as = (Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy) testObjects[1];

        // cleanup
        Connection jdbcConnection = snowflakeAppDeploymentTool.getJdbcConnection(ds, as);
        String deploymentTableName = snowflakeAppDeploymentTool.getDeploymentTableName(jdbcConnection);
        this.cleanUpOldDeployments(jdbcConnection, deploymentTableName);

        // now deploy twice
        snowflakeAppDeploymentTool.deploy(ds, as, "App1");
        snowflakeAppDeploymentTool.deploy(ds, as, "App2");

        // now verify deployments
        ImmutableList<SnowflakeAppDeploymentTool.DeploymentInfo> deployments = snowflakeAppDeploymentTool.getDeployed(ds, as);
        String s = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deployments);
        assertEquals(2, deployments.size());
    }

    public void cleanUpOldDeployments(Connection jdbcConnection, String deploymentTableName) throws SQLException
    {
        Statement statement = jdbcConnection.createStatement();
        statement.executeQuery("delete from " + deploymentTableName);
    }

    private Object[] buildSnowflakeLocalConnection()
    {
        Properties snowflakeLocalDataSourceSpecFileProperties = new Properties();
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-accountName", "XXXXX");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-region", "prod3.us-west-2");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-warehouseName", "demo_wh1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-databaseName", "demo_db1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-cloudType", "aws");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-role", "demo_role1");

        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-privateKeyVaultReference", "XXXX");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-passphraseVaultReference", "XXXX");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-publicuserName", "demo_user1");

        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(snowflakeLocalDataSourceSpecFileProperties));

        Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification_Impl("");
        snowflakeDatasourceSpecification._accountName("legend-local-snowflake-accountName");
        snowflakeDatasourceSpecification._databaseName("legend-local-snowflake-databaseName");
        snowflakeDatasourceSpecification._role("legend-local-snowflake-role");
        snowflakeDatasourceSpecification._warehouseName("legend-local-snowflake-warehouseName");
        snowflakeDatasourceSpecification._region("legend-local-snowflake-region");
        snowflakeDatasourceSpecification._cloudType("legend-local-snowflake-cloudType");

        Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy authenticationStrategy = new Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy_Impl("");
        authenticationStrategy._privateKeyVaultReference("legend-local-snowflake-privateKeyVaultReference");
        authenticationStrategy._passPhraseVaultReference("legend-local-snowflake-passphraseVaultReference");
        authenticationStrategy._publicUserName("legend-local-snowflake-publicuserName");

        return new Object[]{snowflakeDatasourceSpecification, authenticationStrategy};
    }
}