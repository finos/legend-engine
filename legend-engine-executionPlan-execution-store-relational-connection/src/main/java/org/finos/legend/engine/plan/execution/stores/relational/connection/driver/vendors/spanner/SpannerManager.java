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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.spanner;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.GCPApplicationDefaultCredentialsAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SpannerDataSourceSpecification;

import java.util.Properties;

public class SpannerManager extends DatabaseManager {
    @Override
    public MutableList<String> getIds() {
        return Lists.mutable.with("Spanner");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy) {
        switch (authenticationStrategy.getKey().type()) {
            case GCPApplicationDefaultCredentialsAuthenticationStrategyKey.TYPE:
                return buildUrlWithApplicationDefaultCredentials(extraUserDataSourceProperties, (GCPApplicationDefaultCredentialsAuthenticationStrategy) authenticationStrategy);
        }
        throw new UnsupportedOperationException("Unsupported auth strategy :" + authenticationStrategy.getKey().type());
    }

    private String buildUrlWithApplicationDefaultCredentials(Properties extraUserDataSourceProperties, GCPApplicationDefaultCredentialsAuthenticationStrategy authenticationStrategy) {
        GCPApplicationDefaultCredentialsAuthenticationStrategy GCPApplicationDefaultCredentialsAuthenticationStrategy = authenticationStrategy;
        String url =
                String.format(
                        "jdbc:cloudspanner://spanner.googleapis.com/projects/%s/instances/%s/databases/%s",
                        extraUserDataSourceProperties.getProperty(SpannerDataSourceSpecification.SPANNER_PROJECT_ID),
                        extraUserDataSourceProperties.getProperty(SpannerDataSourceSpecification.SPANNER_INSTANCE_ID),
                        extraUserDataSourceProperties.getProperty(SpannerDataSourceSpecification.SPANNER_PROJECT_ID)
                );
        return url;
    }

    @Override
    public Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy) {
        return new Properties();
    }

    @Override
    public String getDriver() {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.spanner.SpannerDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport() {
        return new SpannerCommands();
    }

    private boolean isEmbeddedMode(Properties properties) {
        return false;
    }

    @Override
    public boolean publishMetrics() {
        return false;
    }
}
