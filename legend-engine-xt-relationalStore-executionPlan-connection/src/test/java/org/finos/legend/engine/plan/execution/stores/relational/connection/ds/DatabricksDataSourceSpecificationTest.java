// Copyright 2022 Databricks
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.ApiTokenAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks.DatabricksManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.DatabricksDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.DatabricksDataSourceSpecificationKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class DatabricksDataSourceSpecificationTest extends DatabricksDataSourceSpecification
{
    public DatabricksDataSourceSpecificationTest()
    {
        super(new DatabricksDataSourceSpecificationKey(
                        "dummy",
                        "dummy",
                        "dummy",
                        "dummy"
                ),
                new DatabricksManager(),
                new ApiTokenAuthenticationStrategy("dummy"),
                new Properties());
    }

    private DatabricksDataSourceSpecification buildDatabricksDataSource(
            String hostname,
            String port,
            String protocol,
            String httpPath
    )
    {
        return new DatabricksDataSourceSpecification(
                new DatabricksDataSourceSpecificationKey(hostname, port, protocol, httpPath),
                new DatabricksManager(),
                new ApiTokenAuthenticationStrategy("API_TOKEN"));
    }

    private String extractURL(DatabricksDataSourceSpecification databricksDataSourceSpecification)
    {
        return databricksDataSourceSpecification.getDatabaseManager().buildURL(
                "hostname",
                443,
                "dummy",
                databricksDataSourceSpecification.extraDatasourceProperties,
                databricksDataSourceSpecification.getAuthenticationStrategy()
        );
    }


    @Test
    public void testDatabricksDataSourceSpecificationProperties()
    {
        DatabricksDataSourceSpecification ds = buildDatabricksDataSource(
                "host.databricks.com", "444", "http", "/path");
        Properties connectionProperties = ds.getConnectionProperties();

        Assert.assertEquals("host.databricks.com",
                connectionProperties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_HOSTNAME));
        Assert.assertEquals("444",
                connectionProperties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_PORT));
        Assert.assertEquals("http",
                connectionProperties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_PROTOCOL));
        Assert.assertEquals("/path",
                connectionProperties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_HTTP_PATH));
    }

    @Test
    public void testDatabricksDataSourceSpecificationVpsUrl()
    {
        DatabricksDataSourceSpecification profile = buildDatabricksDataSource(
                "hostname",
                "443",
                "https",
                "/httpPath"
        );

        String url = extractURL(profile);
        Assert.assertEquals(
                "jdbc:spark://hostname:443/default;transportMode=http;ssl=1;AuthMech=3;httpPath=/httpPath;",
                url
        );

        Properties properties = profile.getConnectionProperties();
        Assert.assertEquals("hostname",
                properties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_HOSTNAME));
        Assert.assertEquals("443",
                properties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_PORT));
        Assert.assertEquals("https",
                properties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_PROTOCOL));
        Assert.assertEquals("/httpPath",
                properties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_HTTP_PATH));
    }

}
