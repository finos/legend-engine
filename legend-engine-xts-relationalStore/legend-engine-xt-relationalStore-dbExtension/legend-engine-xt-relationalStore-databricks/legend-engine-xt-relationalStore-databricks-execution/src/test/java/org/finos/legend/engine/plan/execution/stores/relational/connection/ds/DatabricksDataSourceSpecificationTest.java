// Copyright 2022 Databricks
// ©2026 JP Morgan Chase & Co. All rights reserved.
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

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.ApiTokenAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks.DatabricksManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.DatabricksDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.DatabricksDataSourceSpecificationKey;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
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
                connectionProperties.getProperty(DATABRICKS_HOSTNAME));
        Assert.assertEquals("444",
                connectionProperties.getProperty(DATABRICKS_PORT));
        Assert.assertEquals("http",
                connectionProperties.getProperty(DATABRICKS_PROTOCOL));
        Assert.assertEquals("/path",
                connectionProperties.getProperty(DATABRICKS_HTTP_PATH));
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
                "jdbc:databricks://hostname:443/default;ansi_mode=true;EnableComplexDatatypeSupport=1;transportMode=http;ssl=1;EnableArrow=1;EnableTelemetry=0;httpPath=/httpPath",
                url
        );

        Properties properties = profile.getConnectionProperties();
        Assert.assertEquals("hostname",
                properties.getProperty(DATABRICKS_HOSTNAME));
        Assert.assertEquals("443",
                properties.getProperty(DATABRICKS_PORT));
        Assert.assertEquals("https",
                properties.getProperty(DATABRICKS_PROTOCOL));
        Assert.assertEquals("/httpPath",
                properties.getProperty(DATABRICKS_HTTP_PATH));
    }

    private DatabricksDataSourceSpecification buildDatabricksDataSourceWithProxy(String proxyHost, String proxyPort)
    {
        Properties extraUserProperties = new Properties();
        if (proxyHost != null)
        {
            extraUserProperties.put(DATABRICKS_PROXY_HOST, proxyHost);
        }
        if (proxyPort != null)
        {
            extraUserProperties.put(DATABRICKS_PROXY_PORT, proxyPort);
        }
        return new DatabricksDataSourceSpecification(
                new DatabricksDataSourceSpecificationKey("hostname", "443", "https", "/httpPath"),
                new DatabricksManager(),
                new ApiTokenAuthenticationStrategy("API_TOKEN"),
                extraUserProperties);
    }

    @Test
    public void testDatabricksUrlWithProxy()
    {
        String url = extractURL(buildDatabricksDataSourceWithProxy("proxy.internal", "8080"));
        Assert.assertEquals(
                "jdbc:databricks://hostname:443/default;ansi_mode=true;EnableComplexDatatypeSupport=1;transportMode=http;ssl=1;EnableArrow=1;EnableTelemetry=0;httpPath=/httpPath;UseProxy=1;ProxyHost=proxy.internal;ProxyPort=8080",
                url
        );
    }

    @Test
    public void testDatabricksUrlProxyPropertiesAreTrimmed()
    {
        String url = extractURL(buildDatabricksDataSourceWithProxy("  proxy.internal  ", " 8080 "));
        Assert.assertTrue(url.endsWith(";UseProxy=1;ProxyHost=proxy.internal;ProxyPort=8080"));
    }

    @Test
    public void testDatabricksUrlWithBlankProxyIsUnchanged()
    {
        String url = extractURL(buildDatabricksDataSourceWithProxy("   ", ""));
        Assert.assertEquals(
                "jdbc:databricks://hostname:443/default;ansi_mode=true;EnableComplexDatatypeSupport=1;transportMode=http;ssl=1;EnableArrow=1;EnableTelemetry=0;httpPath=/httpPath",
                url
        );
    }

    @Test
    public void testDatabricksUrlWithProxyHostButNoPortFails()
    {
        EngineException e = Assert.assertThrows(EngineException.class,
                () -> extractURL(buildDatabricksDataSourceWithProxy("proxy.internal", null)));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("Databricks proxy configuration is incomplete"));
    }

    @Test
    public void testDatabricksUrlWithProxyPortButNoHostFails()
    {
        Assert.assertThrows(EngineException.class,
                () -> extractURL(buildDatabricksDataSourceWithProxy(null, "8080")));
    }

    @Test
    public void testDatabricksOAuthUrl()
    {
        DatabricksDataSourceSpecification profile = buildDatabricksDataSource(
                "hostname",
                "443",
                "https",
                "/httpPath"
        );

        AuthenticationStrategy oauthStrategy = new TestDatabaseAuthenticationStrategy();
        String url = profile.getDatabaseManager().buildURL(
                "hostname",
                443,
                "dummy",
                profile.extraDatasourceProperties,
                oauthStrategy
        );
        Assert.assertEquals(
                "jdbc:databricks://hostname:443/default;ansi_mode=true;EnableComplexDatatypeSupport=1;transportMode=http;ssl=1;EnableArrow=1;EnableTelemetry=0;httpPath=/httpPath",
                url
        );
    }

}
