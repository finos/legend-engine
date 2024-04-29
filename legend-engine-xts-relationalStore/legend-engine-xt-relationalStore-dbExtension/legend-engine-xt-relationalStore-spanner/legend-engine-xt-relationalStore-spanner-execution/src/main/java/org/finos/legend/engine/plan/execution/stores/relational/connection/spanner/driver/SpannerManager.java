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

import java.util.Optional;
import java.util.Properties;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.spanner.ds.specifications.SpannerDataSourceSpecification;

public class SpannerManager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("Spanner");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties,
                           AuthenticationStrategy authenticationStrategy)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("jdbc:cloudspanner:");

        String proxyHost = extraUserDataSourceProperties.getProperty(SpannerDataSourceSpecification.SPANNER_PROXY_HOST);
        String proxyPort = extraUserDataSourceProperties.getProperty(SpannerDataSourceSpecification.SPANNER_PROXY_PORT);

        // constructing "proxyHost:proxyPort;" url routine
        if (!StringIterate.isEmptyOrWhitespace(proxyHost))
        {
            stringBuilder.append("//").append(proxyHost);
            if (!StringIterate.isEmptyOrWhitespace(proxyPort))
            {
                stringBuilder.append(":").append(proxyPort);
            }
        }
        stringBuilder.append("/");

        Optional.ofNullable(extraUserDataSourceProperties.getProperty(SpannerDataSourceSpecification.SPANNER_PROJECT_ID))
                .ifPresent(projectId -> stringBuilder.append("projects/").append(projectId).append("/"));
        Optional.ofNullable(extraUserDataSourceProperties.getProperty(SpannerDataSourceSpecification.SPANNER_INSTANCE_ID))
                .ifPresent(instanceId -> stringBuilder.append("instances/").append(instanceId).append("/"));
        Optional.ofNullable(extraUserDataSourceProperties.getProperty(SpannerDataSourceSpecification.SPANNER_DATABASE_ID))
                .ifPresent(database -> stringBuilder.append("databases/").append(database).append("?"));

        if (authenticationStrategy == null || authenticationStrategy instanceof TestDatabaseAuthenticationStrategy)
        {
            stringBuilder.append("usePlainText=true;");
        }
        stringBuilder.append("lenient=true;");

        return stringBuilder.toString();
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.spanner.driver.SpannerDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new SpannerCommands();
    }
}
