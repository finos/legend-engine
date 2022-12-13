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

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks.DatabricksManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.redshift.RedshiftManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.DatabricksDataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.LocalH2DataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.RedshiftDataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.StaticDataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.DatabricksDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.RedshiftDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.StaticDataSourceSpecificationKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatabricksDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.EmbeddedH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.RedshiftDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;

public class DataSourceSpecificationRuntimeGenerator implements DatasourceSpecificationVisitor<DataSourceSpecificationRuntime>
{

    private final DataSourceSpecificationKey key;
    private final RelationalDatabaseConnection connection;
    private final AuthenticationStrategyRuntime authenticationStrategyRuntime;

    public DataSourceSpecificationRuntimeGenerator(DataSourceSpecificationKey key, AuthenticationStrategyRuntime authenticationStrategyRuntime, RelationalDatabaseConnection connection)
    {
        this.key = key;
        this.authenticationStrategyRuntime = authenticationStrategyRuntime;
        this.connection = connection;
    }

    @Override
    public DataSourceSpecificationRuntime visit(DatasourceSpecification datasourceSpecification)
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
                return new LocalH2DataSourceSpecificationRuntime(
                        localH2DatasourceSpecification.testDataSetupSqls,
                        new H2Manager(),
                        new TestDatabaseAuthenticationStrategyRuntime()
                );
            }
            else
            {
                return new StaticDataSourceSpecificationRuntime(
                        (StaticDataSourceSpecificationKey) key,
                        new H2Manager(),
                        this.authenticationStrategyRuntime
                );
            }
        }
        else if (datasourceSpecification instanceof StaticDatasourceSpecification)
        {
            return new StaticDataSourceSpecificationRuntime(
                    (StaticDataSourceSpecificationKey) key,
                    DatabaseManager.fromString(connection.type.name()),
                    authenticationStrategyRuntime
            );
        }
        else if (datasourceSpecification instanceof DatabricksDatasourceSpecification)
        {
            return new DatabricksDataSourceSpecificationRuntime(
                    (DatabricksDataSourceSpecificationKey) key,
                    new DatabricksManager(),
                    authenticationStrategyRuntime
            );
        }
        else if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
        {
            return new SnowflakeDataSourceSpecificationRuntime(
                    (SnowflakeDataSourceSpecificationKey) key,
                    new SnowflakeManager(),
                    authenticationStrategyRuntime);
        }
        else if (datasourceSpecification instanceof RedshiftDatasourceSpecification)
        {
            return new RedshiftDataSourceSpecificationRuntime(
                    (RedshiftDataSourceSpecificationKey) key,
                    new RedshiftManager(),
                    authenticationStrategyRuntime
            );
        }

        return null;
    }
}
