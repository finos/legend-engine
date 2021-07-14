package org.finos.legend.engine.plan.execution.stores.relational.connection.ds;

import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

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

    @Test
    public void testSnowflakeDataSourceSpecificationProperties()
    {
        SnowflakeDataSourceSpecification ds =
                new SnowflakeDataSourceSpecification(
                        new SnowflakeDataSourceSpecificationKey("ki79827", "us-east-2", "LEGENDRO_WH", "KNOEMA_RENEWABLES_DATA_ATLAS", "aws", null),
                        new SnowflakeManager(),
                        new SnowflakePublicAuthenticationStrategy("SF_KEY", "SF_PASS", "LEGEND_RO_PIERRE"),
                        new RelationalExecutorInfo());

        Properties properties = ds.extraDatasourceProperties;

        Assert.assertEquals("ki79827", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("KNOEMA_RENEWABLES_DATA_ATLAS", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("LEGENDRO_WH", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithQuoteIdentifiersSetAsFalse()
    {
        SnowflakeDataSourceSpecification ds =
                new SnowflakeDataSourceSpecification(
                        new SnowflakeDataSourceSpecificationKey("ki79827", "us-east-2", "LEGENDRO_WH", "KNOEMA_RENEWABLES_DATA_ATLAS", "aws", false),
                        new SnowflakeManager(),
                        new SnowflakePublicAuthenticationStrategy("SF_KEY", "SF_PASS", "LEGEND_RO_PIERRE"),
                        new RelationalExecutorInfo());

        Properties properties = ds.extraDatasourceProperties;

        Assert.assertEquals("ki79827", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(false, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("KNOEMA_RENEWABLES_DATA_ATLAS", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("LEGENDRO_WH", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
    }

    @Test
    public void testSnowflakeDataSourceSpecificationPropertiesWithQuoteIdentifiersSetAsTrue()
    {
        SnowflakeDataSourceSpecification ds =
                new SnowflakeDataSourceSpecification(
                        new SnowflakeDataSourceSpecificationKey("ki79827", "us-east-2", "LEGENDRO_WH", "KNOEMA_RENEWABLES_DATA_ATLAS", "aws", true),
                        new SnowflakeManager(),
                        new SnowflakePublicAuthenticationStrategy("SF_KEY", "SF_PASS", "LEGEND_RO_PIERRE"),
                        new RelationalExecutorInfo());

        Properties properties = ds.extraDatasourceProperties;

        Assert.assertEquals("ki79827", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME));
        Assert.assertEquals("us-east-2", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION));
        Assert.assertEquals("aws", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE));
        Assert.assertEquals(true, properties.get(SnowflakeDataSourceSpecification.SNOWFLAKE_QUOTE_IDENTIFIERS));
        Assert.assertEquals("\"KNOEMA_RENEWABLES_DATA_ATLAS\"", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_DATABASE_NAME));
        Assert.assertEquals("\"LEGENDRO_WH\"", properties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME));
    }
}
