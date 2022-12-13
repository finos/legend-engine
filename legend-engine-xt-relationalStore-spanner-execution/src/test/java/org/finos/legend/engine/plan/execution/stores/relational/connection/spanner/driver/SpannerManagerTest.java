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

package org.finos.legend.engine.plan.execution.stores.relational.connection.spanner.driver;

import java.util.Properties;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPApplicationDefaultCredentialsAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategyRuntime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpannerManagerTest
{
    private final SpannerManager spannerManager = new SpannerManager();

    @Test
    public void buildUrlWithHostOnly()
    {
        Properties properties = new Properties();
        properties.put("spanner_projectId", "test-project");
        properties.put("spanner_instanceId", "test-instance");
        properties.put("spanner_databaseId", "test-database");
        properties.put("spanner_proxyHost", "test-host");

        assertThat(buildUrl(spannerManager, properties, new GCPApplicationDefaultCredentialsAuthenticationStrategyRuntime()),
                is("jdbc:cloudspanner://test-host/projects/test-project/instances/test-instance/databases/test-database?lenient=true;"));
    }

    @Test
    public void buildUrlWithHostAndPort()
    {
        Properties properties = new Properties();
        properties.put("spanner_projectId", "test-project");
        properties.put("spanner_instanceId", "test-instance");
        properties.put("spanner_databaseId", "test-database");
        properties.put("spanner_proxyHost", "test-host");
        properties.put("spanner_proxyPort", "test-port");

        assertThat(buildUrl(spannerManager, properties, new GCPApplicationDefaultCredentialsAuthenticationStrategyRuntime()),
                is("jdbc:cloudspanner://test-host:test-port/projects/test-project/instances/test-instance/databases/test-database?lenient=true;"));
    }

    @Test
    public void buildUrlWithoutHostAndPort()
    {
        Properties properties = new Properties();
        properties.put("spanner_projectId", "test-project");
        properties.put("spanner_instanceId", "test-instance");
        properties.put("spanner_databaseId", "test-database");

        assertThat(buildUrl(spannerManager, properties, new GCPApplicationDefaultCredentialsAuthenticationStrategyRuntime()),
                is("jdbc:cloudspanner:/projects/test-project/instances/test-instance/databases/test-database?lenient=true;"));
    }

    @Test
    public void buildUrlWithDisabledAuthIfStrategyIsBlank()
    {
        Properties properties = new Properties();
        properties.put("spanner_projectId", "test-project");
        properties.put("spanner_instanceId", "test-instance");
        properties.put("spanner_databaseId", "test-database");
        properties.put("spanner_proxyHost", "test-host");
        properties.put("spanner_proxyPort", "test-port");

        assertThat(buildUrl(spannerManager, properties, new TestDatabaseAuthenticationStrategyRuntime()),
                is("jdbc:cloudspanner://test-host:test-port/projects/test-project/instances/test-instance/databases/test-database?usePlainText=true;lenient=true;"));
        assertThat(buildUrl(spannerManager, properties, null),
                is("jdbc:cloudspanner://test-host:test-port/projects/test-project/instances/test-instance/databases/test-database?usePlainText=true;lenient=true;"));
    }

    private String buildUrl(SpannerManager spannerManager, Properties properties, AuthenticationStrategyRuntime authenticationStrategyRuntime)
    {
        return spannerManager.buildURL(null, -1, null, properties, authenticationStrategyRuntime);
    }
}
