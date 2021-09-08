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

package org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic;

import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.bigquery.BigQueryManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.spanner.SpannerManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.*;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.*;

public class DataSourceSpecificationTransformer implements DatasourceSpecificationVisitor<DataSourceSpecification>
{
    private final RelationalExecutorInfo relationalExecutorInfo;
    private final DataSourceSpecificationKey key;
    private final RelationalDatabaseConnection connection;
    private final AuthenticationStrategy authenticationStrategy;

    public DataSourceSpecificationTransformer(RelationalExecutorInfo relationalExecutorInfo, DataSourceSpecificationKey key, AuthenticationStrategy authenticationStrategy, RelationalDatabaseConnection connection)
    {
        this.relationalExecutorInfo = relationalExecutorInfo;
        this.key = key;
        this.authenticationStrategy = authenticationStrategy;
        this.connection = connection;
    }

    @Override
    public DataSourceSpecification visit(DatasourceSpecification datasourceSpecification)
    {
        if (datasourceSpecification instanceof EmbeddedH2DatasourceSpecification)
        {
            throw new UnsupportedOperationException("Embedded H2 currently not supported");
        }
        else if (datasourceSpecification instanceof LocalH2DatasourceSpecification)
        {
            LocalH2DatasourceSpecification localH2DatasourceSpecification = (LocalH2DatasourceSpecification) datasourceSpecification;
            if (localH2DatasourceSpecification.testDataSetupSqls != null && !localH2DatasourceSpecification.testDataSetupSqls.isEmpty())
            {
                return new LocalH2DataSourceSpecification(
                        (LocalH2DataSourceSpecificationKey) key,
                        new H2Manager(),
                        new TestDatabaseAuthenticationStrategy(),
                        relationalExecutorInfo
                );
            }
            else
            {
                return new StaticDataSourceSpecification(
                        (StaticDataSourceSpecificationKey) key,
                        new H2Manager(),
                        new TestDatabaseAuthenticationStrategy(),
                        relationalExecutorInfo
                );
            }
        }
        else if (datasourceSpecification instanceof StaticDatasourceSpecification)
        {
            return new StaticDataSourceSpecification(
                    (StaticDataSourceSpecificationKey) key,
                    DatabaseManager.fromString(connection.type.name()),
                    authenticationStrategy,
                    relationalExecutorInfo
            );
        }
        else if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
        {
            return new SnowflakeDataSourceSpecification(
                    (SnowflakeDataSourceSpecificationKey) key,
                    new SnowflakeManager(),
                    authenticationStrategy,
                    relationalExecutorInfo
                    );
        }
        else if (datasourceSpecification instanceof BigQueryDatasourceSpecification)
        {
            return new BigQueryDataSourceSpecification(
                    (BigQueryDataSourceSpecificationKey) key,
                    new BigQueryManager(),
                    authenticationStrategy,
                    relationalExecutorInfo
            );
        }
        else if (datasourceSpecification instanceof SpannerDatasourceSpecification)
        {
            return new SpannerDataSourceSpecification(
                    (SpannerDataSourceSpecificationKey) key,
                    new SpannerManager(),
                    authenticationStrategy,
                    relationalExecutorInfo
            );
        }
        return null;
    }
}
