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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.EmbeddedH2DataSourceSpecification;

import java.util.Properties;

public class H2Manager extends DatabaseManager
{
    public static final String DATABASE_TO_UPPER = "DATABASE_TO_UPPER";

    public static int getMajorVersion()
    {
        try
        {
            return DriverManager.getDriver("jdbc:h2:").getMajorVersion();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("cannot identify H2 driver major version", e);
        }
    }

    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("H2");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        if (isEmbeddedMode(extraUserDataSourceProperties))
        {
            String dataDirectoryPath = extraUserDataSourceProperties.getProperty(EmbeddedH2DataSourceSpecification.H2_DATA_DIRECTORY_PATH).replace("\\", "/");
            String autoServerMode = extraUserDataSourceProperties.getProperty(EmbeddedH2DataSourceSpecification.H2_AUTO_SERVER_MODE);
            return "jdbc:h2:file:" + dataDirectoryPath + "/" + databaseName + ";AUTO_SERVER=" + autoServerMode;
        }
        if (extraUserDataSourceProperties.contains(DATABASE_TO_UPPER))
        {
            databaseName += String.format(";%s=%s", DATABASE_TO_UPPER, extraUserDataSourceProperties.getProperty(DATABASE_TO_UPPER));
        }

        String defaultH2Properties;

        if (getMajorVersion() == 2)
        {
            defaultH2Properties = System.getProperty("legend.test.h2.properties",
                    ";NON_KEYWORDS=ANY,ASYMMETRIC,AUTHORIZATION,CAST,CURRENT_PATH,CURRENT_ROLE,DAY,DEFAULT,ELSE,END,HOUR,KEY,MINUTE,MONTH,SECOND,SESSION_USER,SET,SOME,SYMMETRIC,SYSTEM_USER,TO,UESCAPE,USER,VALUE,WHEN,YEAR,OVER;MODE=LEGACY");
        }
        else
        {
            defaultH2Properties = "";
        }

        return "jdbc:h2:tcp://" + host + ":" + port + "/mem:" + databaseName + defaultH2Properties;
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Driver";
    }

    @Override
    public H2Commands relationalDatabaseSupport()
    {
        return new H2Commands();
    }

    private boolean isEmbeddedMode(Properties properties)
    {
        return properties.containsKey(EmbeddedH2DataSourceSpecification.H2_DATA_DIRECTORY_PATH);
    }

    @Override
    public boolean publishMetrics()
    {
        return false;
    }
}
