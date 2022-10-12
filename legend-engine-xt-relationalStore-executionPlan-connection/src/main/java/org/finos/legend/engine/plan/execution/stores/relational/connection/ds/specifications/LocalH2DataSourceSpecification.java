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

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.LocalH2DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;


public class LocalH2DataSourceSpecification extends StaticDataSourceSpecification
{

    private static final int MAX_POOL_SIZE = 100;
    private static final int MIN_POOL_SIZE = 0;

    public LocalH2DataSourceSpecification(List<String> setupSQLs, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy)
    {
        super(new LocalH2DataSourceSpecificationKey(setupSQLs), databaseManager, authenticationStrategy, new Properties(), MAX_POOL_SIZE, MIN_POOL_SIZE);
    }

    public LocalH2DataSourceSpecification(List<String> setupSQLs, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties properties)
    {
        super(new LocalH2DataSourceSpecificationKey(setupSQLs), databaseManager, authenticationStrategy, properties, MAX_POOL_SIZE, MIN_POOL_SIZE);
    }

    @Override
    protected Connection getConnection(IdentityState identityState, Supplier<DataSource> dataSourceBuilder)
    {
        Connection connection = super.getConnection(identityState, dataSourceBuilder);
        LocalH2DataSourceSpecificationKey _key = (LocalH2DataSourceSpecificationKey) this.datasourceKey;
        if (_key.getTestDataSetupSqls() != null && !_key.getTestDataSetupSqls().isEmpty())
        {
            try
            {
                for (String sql : _key.getTestDataSetupSqls())
                {
                    try (Statement statement = connection.createStatement())
                    {
                        statement.executeUpdate(sql);
                    }
                }
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        return connection;
    }
}
