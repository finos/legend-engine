//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

import static org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeAccountType.MultiTenant;
import static org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeAccountType.VPS;

public class SnowflakeDataSourceSpecificationRuntimeTest extends SnowflakeDataSourceSpecificationRuntime
{
    public SnowflakeDataSourceSpecificationRuntimeTest()
    {
        super(new SnowflakeDataSourceSpecificationKey("dummy", "dummy", "dummy", "dummy", "dummy", null),
                new SnowflakeManager(),
                new SnowflakePublicAuthenticationStrategyRuntime("dummy", "dummy", "dummy"),
                new Properties());
    }

    private SnowflakeDataSourceSpecificationRuntime buildSnowflakeDataSource(String accountName, String region, String warehouse, String database, String cloudType, Boolean quoteIdentifiers)
    {
        return buildSnowflakeDataSource(accountName, region, warehouse, database, cloudType, quoteIdentifiers, null, null, null, null, null, null);
    }

    private SnowflakeDataSourceSpecificationRuntime buildSnowflakeDataSource(String accountName, String region, String warehouse, String database, String cloudType, Boolean quoteIdentifiers, String proxyHost, String proxyProt, String nonProxyHosts, String accountType, String organisation, String role)
    {
        return new SnowflakeDataSourceSpecificationRuntime(
                new SnowflakeDataSourceSpecificationKey(accountName, region, warehouse, database, cloudType, quoteIdentifiers, proxyHost, proxyProt, nonProxyHosts, accountType, organisation, role),
                new SnowflakeManager(),
                new SnowflakePublicAuthenticationStrategyRuntime("SF_KEY", "SF_PASS", "LEGEND_RO_PIERRE"));
    }

    private String extractURL(SnowflakeDataSourceSpecificationRuntime snowflakeDataSourceSpecification)
    {
        return snowflakeDataSourceSpecification.getDatabaseManager().buildURL("test.host", 101, "test", snowflakeDataSourceSpecification.extraDatasourceProperties, snowflakeDataSourceSpecification.getAuthenticationStrategy());
    }


    @Test
    public void testSnowflakeDataSourceSpecificationProperties()
    {
        SnowflakeDataSourceSpecificationRuntime ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", null);


        Properties connectionProperties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", connectionProperties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", connectionProperties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", connectionProperties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, connectionProperties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("SAMPLE_DB", connectionProperties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("LEGENDRO_WH", connectionProperties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertNull(connectionProperties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_HOST));
        Assert.assertNull(connectionProperties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_PORT));
        Assert.assertNull(connectionProperties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_NON_PROXY_HOSTS));
        Assert.assertNotNull(connectionProperties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_USE_PROXY));
        Assert.assertFalse((Boolean) connectionProperties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_USE_PROXY));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithQuoteIdentifiersSetAsFalse()
    {
        SnowflakeDataSourceSpecificationRuntime ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", false);
        Properties properties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("SAMPLE_DB", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("LEGENDRO_WH", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_WAREHOUSE_NAME));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithQuoteIdentifiersSetAsTrue()
    {
        SnowflakeDataSourceSpecificationRuntime ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", true);

        Properties properties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(true, properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("\"SAMPLE_DB\"", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("\"LEGENDRO_WH\"", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_WAREHOUSE_NAME));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithRole()
    {
        SnowflakeDataSourceSpecificationRuntime ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", false, null, null, null, null, null, "TEST_ROLE");

        Properties properties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("SAMPLE_DB", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("LEGENDRO_WH", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertEquals("TEST_ROLE", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ROLE));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithRoleWithIdentifier()
    {
        SnowflakeDataSourceSpecificationRuntime ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", true, null, null, null, null, null, "TEST_ROLE");

        Properties properties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(true, properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("\"SAMPLE_DB\"", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("\"LEGENDRO_WH\"", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertEquals("\"TEST_ROLE\"", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ROLE));
    }


    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithProxy()
    {
        SnowflakeDataSourceSpecificationRuntime ds = buildSnowflakeDataSource("sampleAccount", "us-east-2", "LEGENDRO_WH", "SAMPLE_DB", "aws", false, "testProxy", "testPort123", "nonHosts", null, null, null);

        Properties properties = ds.getConnectionProperties();

        Assert.assertEquals("sampleAccount", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("SAMPLE_DB", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("LEGENDRO_WH", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ORGANIZATION_NAME));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_TYPE_NAME));


        Assert.assertEquals("testProxy", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_HOST));
        Assert.assertEquals("testPort123", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_PORT));
        Assert.assertEquals("nonHosts", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_NON_PROXY_HOSTS));

        Assert.assertNotNull(properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_USE_PROXY));
        Assert.assertTrue((Boolean) properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_USE_PROXY));
    }


    @Test
    public void testSnowflakeDataSourceSpecificationVpsUrl()
    {
        SnowflakeDataSourceSpecificationRuntime profile = buildSnowflakeDataSource(
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
        Assert.assertEquals("organisation_division", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-1", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("test", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("DEMO_WH", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertEquals("organisationSample", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ORGANIZATION_NAME));
        Assert.assertEquals(VPS.name(), properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_TYPE_NAME));
        Assert.assertFalse(Boolean.parseBoolean(properties.getProperty("useProxy")));

        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_HOST));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_PORT));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_NON_PROXY_HOSTS));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationNoAccountTypeUrl()
    {
        SnowflakeDataSourceSpecificationRuntime profile = buildSnowflakeDataSource(
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
        Assert.assertEquals("organisation_division", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-1", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_REGION));
        Assert.assertEquals("privatelink", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("DEMO_DB", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("DEMO_WH", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ORGANIZATION_NAME));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_TYPE_NAME));
        Assert.assertFalse(Boolean.parseBoolean(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_USE_PROXY)));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_HOST));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_PORT));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_NON_PROXY_HOSTS));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationMultiTenantUrl()
    {
        SnowflakeDataSourceSpecificationRuntime profile = buildSnowflakeDataSource(
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
        Assert.assertEquals("organisation_division", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-1", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_REGION));
        Assert.assertEquals("privatelink", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("DEMO_DB", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("DEMO_WH", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ORGANIZATION_NAME));
        Assert.assertEquals(MultiTenant.name(), properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_ACCOUNT_TYPE_NAME));
        Assert.assertFalse(Boolean.parseBoolean(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_USE_PROXY)));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_HOST));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_PORT));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_NON_PROXY_HOSTS));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationWithoutPrivateLink()
    {
        SnowflakeDataSourceSpecificationRuntime profile = buildSnowflakeDataSource(
                "account1",
                "us-east-1",
                "DEMO_WH",
                "test",
                "aws",
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        String url = extractURL(profile);
        Assert.assertEquals("jdbc:snowflake://account1.us-east-1.aws.snowflakecomputing.com", url);

        Properties properties = profile.getConnectionProperties();
        Assert.assertEquals("us-east-1", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("test", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("DEMO_WH", properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_WAREHOUSE_NAME));
        Assert.assertFalse(Boolean.parseBoolean(properties.getProperty("useProxy")));

        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_HOST));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_PROXY_PORT));
        Assert.assertNull(properties.getProperty(SnowflakeDataSourceSpecificationRuntime.SNOWFLAKE_NON_PROXY_HOSTS));
    }
}
