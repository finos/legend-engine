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

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.ApiTokenAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks.DatabricksManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.DatabricksDataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.DatabricksDataSourceSpecificationKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class DatabricksDataSourceSpecificationRuntimeTest extends DatabricksDataSourceSpecificationRuntime
{
    public DatabricksDataSourceSpecificationRuntimeTest()
    {
        super(new DatabricksDataSourceSpecificationKey(
                        "dummy",
                        "dummy",
                        "dummy",
                        "dummy"
                ),
                new DatabricksManager(),
                new ApiTokenAuthenticationStrategyRuntime("dummy"),
                new Properties());
    }

    private DatabricksDataSourceSpecificationRuntime buildDatabricksDataSource(
            String hostname,
            String port,
            String protocol,
            String httpPath
    )
    {
        return new DatabricksDataSourceSpecificationRuntime(
                new DatabricksDataSourceSpecificationKey(hostname, port, protocol, httpPath),
                new DatabricksManager(),
                new ApiTokenAuthenticationStrategyRuntime("API_TOKEN"));
    }

    private String extractURL(DatabricksDataSourceSpecificationRuntime databricksDataSourceSpecification)
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
        DatabricksDataSourceSpecificationRuntime ds = buildDatabricksDataSource(
                "host.databricks.com", "444", "http", "/path");
        Properties connectionProperties = ds.getConnectionProperties();

        Assert.assertEquals("host.databricks.com",
                connectionProperties.getProperty(DatabricksDataSourceSpecificationRuntime.DATABRICKS_HOSTNAME));
        Assert.assertEquals("444",
                connectionProperties.getProperty(DatabricksDataSourceSpecificationRuntime.DATABRICKS_PORT));
        Assert.assertEquals("http",
                connectionProperties.getProperty(DatabricksDataSourceSpecificationRuntime.DATABRICKS_PROTOCOL));
        Assert.assertEquals("/path",
                connectionProperties.getProperty(DatabricksDataSourceSpecificationRuntime.DATABRICKS_HTTP_PATH));
    }

    @Test
    public void testDatabricksDataSourceSpecificationVpsUrl()
    {
        DatabricksDataSourceSpecificationRuntime profile = buildDatabricksDataSource(
                "hostname",
                "443",
                "https",
                "/httpPath"
        );

        String url = extractURL(profile);
        Assert.assertEquals(
                "jdbc:databricks://hostname:443/default;transportMode=http;ssl=1;AuthMech=3;httpPath=/httpPath;UID=token",
                url
        );

        Properties properties = profile.getConnectionProperties();
        Assert.assertEquals("hostname",
                properties.getProperty(DatabricksDataSourceSpecificationRuntime.DATABRICKS_HOSTNAME));
        Assert.assertEquals("443",
                properties.getProperty(DatabricksDataSourceSpecificationRuntime.DATABRICKS_PORT));
        Assert.assertEquals("https",
                properties.getProperty(DatabricksDataSourceSpecificationRuntime.DATABRICKS_PROTOCOL));
        Assert.assertEquals("/httpPath",
                properties.getProperty(DatabricksDataSourceSpecificationRuntime.DATABRICKS_HTTP_PATH));
    }

}
