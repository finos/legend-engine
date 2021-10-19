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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import javax.sql.DataSource;

import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.EmbeddedH2DataSourceSpecificationKey;
import org.finos.legend.engine.shared.core.identity.Identity;

public class EmbeddedH2DataSourceSpecification extends DataSourceSpecification
{
    public static final String H2_DATA_DIRECTORY_PATH = "h2_data_directory_path";
    public static final String H2_AUTO_SERVER_MODE = "h2_auto_server_mode";

    private EmbeddedH2DataSourceSpecificationKey key;
    private DataSource dataSource;

    public EmbeddedH2DataSourceSpecification(EmbeddedH2DataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, RelationalExecutorInfo relationalExecutorInfo)
    {
        super(key, databaseManager, authenticationStrategy, new Properties(), relationalExecutorInfo);
        this.extraDatasourceProperties.put(H2_DATA_DIRECTORY_PATH, key.getDirectory().getAbsolutePath());
        this.extraDatasourceProperties.put(H2_AUTO_SERVER_MODE, String.valueOf(key.isAutoServerMode()).toUpperCase());
        this.key = key;
    }

    @Override
    public Connection getConnectionUsingIdentity(Identity identity, Optional<CredentialSupplier> databaseCredentialSupplier)
    {
        try
        {
            super.cacheConnectionState(identity, databaseCredentialSupplier);
            this.dataSource = this.buildDataSource(identity);
            return this.dataSource.getConnection();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
