package org.finos.legend.engine.plan.execution.stores.relational.connection.ds;

import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

import static org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeAccountType.MultiTenant;
import static org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeAccountType.VPS;

public class SnowflakeDataSourceSpecificationTest extends SnowflakeDataSourceSpecification
{
    public SnowflakeDataSourceSpecificationTest()
    {
        super(new SnowflakeDataSourceSpecificationKey("dummy", "dummy", "dummy", "dummy", "dummy", null),
                new SnowflakeManager(),
                new SnowflakePublicAuthenticationStrategy("dummy", "dummy", "dummy"),
                new Properties(),
                new RelationalExecutorInfo());
    }

    private SnowflakeDataSourceSpecification buildSnowflakeDataSource(String accountName, String region, String warehouse, String database, String cloudType, Boolean quoteIdentifiers)
    {
        return buildSnowflakeDataSource(accountName, region, warehouse, database, cloudType, quoteIdentifiers, null, null, null, null, null,null);
    }

    private SnowflakeDataSourceSpecification buildSnowflakeDataSource(String accountName, String region, String warehouse, String database, String cloudType, Boolean quoteIdentifiers, String proxyHost, String proxyProt, String nonProxyHosts, String accountType, String organisation, String role)
    {
        return new SnowflakeDataSourceSpecification(
                new SnowflakeDataSourceSpecificationKey(accountName, region, warehouse, database, cloudType, quoteIdentifiers, proxyHost, proxyProt, nonProxyHosts, accountType, organisation,role),
                new SnowflakeManager(),
                new SnowflakePublicAuthenticationStrategy("SF_KEY", "SF_PASS", "LEGEND_RO_PIERRE"),
                new RelationalExecutorInfo());
    }

    private String extractURL(SnowflakeDataSourceSpecification snowflakeDataSourceSpecification)
    {
        return snowflakeDataSourceSpecification.getDatabaseManager().buildURL("test.host", 101, "test", snowflakeDataSourceSpecification.extraDatasourceProperties, snowflakeDataSourceSpecification.getAuthenticationStrategy());
    }


    @Test
    public void testSnowflakeDataSourceSpecificationProperties()
    {
        SnowflakeDataSourceSpecification ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", null);


        Properties connectionProperties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", connectionProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", connectionProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", connectionProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, connectionProperties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("SAMPLE_DB", connectionProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("LEGENDRO_WH", connectionProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertNull(connectionProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_PROXY_HOST));
        Assert.assertNull(connectionProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_PROXY_PORT));
        Assert.assertNull(connectionProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_NON_PROXY_HOSTS));
        Assert.assertNotNull(connectionProperties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_USE_PROXY));
        Assert.assertFalse((Boolean)connectionProperties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_USE_PROXY));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithQuoteIdentifiersSetAsFalse()
    {
        SnowflakeDataSourceSpecification ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", false);
        Properties properties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("SAMPLE_DB", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("LEGENDRO_WH", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithQuoteIdentifiersSetAsTrue()
    {
        SnowflakeDataSourceSpecification ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", true);

        Properties properties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(true, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("\"SAMPLE_DB\"", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("\"LEGENDRO_WH\"", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithRole()
    {
        SnowflakeDataSourceSpecification ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", false,null,null,null,null,null,"TEST_ROLE");

        Properties properties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("SAMPLE_DB", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("LEGENDRO_WH", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertEquals("TEST_ROLE", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ROLE));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithRoleWithIdentifier()
    {
        SnowflakeDataSourceSpecification ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", true,null,null,null,null,null,"TEST_ROLE");

        Properties properties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(true, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("\"SAMPLE_DB\"", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("\"LEGENDRO_WH\"", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertEquals("\"TEST_ROLE\"", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ROLE));
    }


    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithProxy()
    {
        SnowflakeDataSourceSpecification ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", false,"testProxy","testPort123","nonHosts",null,null,null);

        Properties properties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("SAMPLE_DB", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("LEGENDRO_WH", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ORGANIZATION_NAME));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_TYPE_NAME));


        Assert.assertEquals("testProxy",properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_PROXY_HOST));
        Assert.assertEquals("testPort123",properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_PROXY_PORT));
        Assert.assertEquals("nonHosts",properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_NON_PROXY_HOSTS));

        Assert.assertNotNull(properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_USE_PROXY));
        Assert.assertTrue((Boolean)properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_USE_PROXY));
    }


    @Test
    public void testSnowflakeDataSourceSpecificationVpsUrl()
    {
        SnowflakeDataSourceSpecification profile = buildSnowflakeDataSource(
                "organisation_division",
                "us-east-1",
                "DEMO_WH",
                "test",
                "aws",
                null,
                null,
                null,
                null,
                "VPS",
                "organisationSample",
                null);

        String url = extractURL(profile);
        Assert.assertEquals("jdbc:snowflake://organisation_division.organisationSample.us-east-1.aws.privatelink.snowflakecomputing.com", url);

        Properties properties = profile.getConnectionProperties();
        Assert.assertEquals("organisation_division", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-1", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("test", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("DEMO_WH", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertEquals("organisationSample", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ORGANIZATION_NAME));
        Assert.assertEquals(VPS.name(), properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_TYPE_NAME));
        Assert.assertFalse(Boolean.parseBoolean(properties.getProperty("useProxy")));

        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_PROXY_HOST));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_PROXY_PORT));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_NON_PROXY_HOSTS));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationNoAccountTypeUrl()
    {
        SnowflakeDataSourceSpecification profile = buildSnowflakeDataSource(
                "organisation_division",
                "us-east-1",
                "DEMO_WH",
                "DEMO_DB",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                 null
        );

        String url = extractURL(profile);
        Assert.assertEquals("jdbc:snowflake://organisation_division.us-east-1.privatelink.snowflakecomputing.com", url);

        Properties properties = profile.getConnectionProperties();
        Assert.assertEquals("organisation_division", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-1", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("privatelink", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("DEMO_DB", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("DEMO_WH", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertNull( properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ORGANIZATION_NAME));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_TYPE_NAME));
        Assert.assertFalse(Boolean.parseBoolean(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_USE_PROXY)));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_PROXY_HOST));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_PROXY_PORT));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_NON_PROXY_HOSTS));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationMultiTenantUrl()
    {
        SnowflakeDataSourceSpecification profile = buildSnowflakeDataSource(
                "organisation_division",
                "us-east-1",
                "DEMO_WH",
                "DEMO_DB",
                null,
                null,

                null,
                null,
                null,
                "MultiTenant",
                null,
                null
        );

        String url = extractURL(profile);
        Assert.assertEquals("jdbc:snowflake://organisation_division.us-east-1.privatelink.snowflakecomputing.com", url);

        Properties properties = profile.getConnectionProperties();
        Assert.assertEquals("organisation_division", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-1", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("privatelink", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("DEMO_DB", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("DEMO_WH", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertNull( properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ORGANIZATION_NAME));
        Assert.assertEquals(MultiTenant.name(), properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_TYPE_NAME));
        Assert.assertFalse(Boolean.parseBoolean(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_USE_PROXY)));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_PROXY_HOST));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_PROXY_PORT));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_NON_PROXY_HOSTS));
    }
}
