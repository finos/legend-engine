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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.EmbeddedH2DataSourceSpecificationKey;
import org.eclipse.collections.api.list.MutableList;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class EmbeddedH2DataSourceSpecification extends DataSourceSpecification
{
    public static final String H2_DATA_DIRECTORY_PATH = "h2_data_directory_path";
    public static final String H2_AUTO_SERVER_MODE = "h2_auto_server_mode";

    private EmbeddedH2DataSourceSpecificationKey key;
    private DataSource dataSource;

    public EmbeddedH2DataSourceSpecification(EmbeddedH2DataSourceSpecificationKey key, AuthenticationStrategy authenticationStrategy, RelationalExecutorInfo relationalExecutorInfo)
    {
        super(key, authenticationStrategy, new Properties(), relationalExecutorInfo);
        this.databaseManager = new H2Manager();
        this.extraDatasourceProperties.put(H2_DATA_DIRECTORY_PATH, key.getDirectory().getAbsolutePath());
        this.extraDatasourceProperties.put(H2_AUTO_SERVER_MODE, String.valueOf(key.isAutoServerMode()).toUpperCase());
        this.key = key;
        this.dataSource = this.buildDataSource(null);
    }

    @Override
    protected DataSource buildDataSource(MutableList<CommonProfile> profiles)
    {
        return this.buildDataSource(null, -1, this.key.getDatabaseName(), profiles);
    }

    @Override
    public Connection getConnectionUsingSubject(Subject subject)
    {
        try
        {
            return this.dataSource.getConnection();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
