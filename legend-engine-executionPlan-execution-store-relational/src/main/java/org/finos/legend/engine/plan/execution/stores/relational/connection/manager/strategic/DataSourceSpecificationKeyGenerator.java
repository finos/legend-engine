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

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.DeltaLakeDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.EmbeddedH2DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.LocalH2DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.StaticDataSourceSpecificationKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.*;

import java.io.File;

public class DataSourceSpecificationKeyGenerator implements DatasourceSpecificationVisitor<DataSourceSpecificationKey>
{
    private final int testDbPort;
    private final RelationalDatabaseConnection connection;

    public DataSourceSpecificationKeyGenerator(int testDbPort, RelationalDatabaseConnection connection)
    {
        this.testDbPort = testDbPort;
        this.connection = connection;
    }

    @Override
    public DataSourceSpecificationKey visit(DatasourceSpecification datasourceSpecification)
    {
        if (datasourceSpecification instanceof EmbeddedH2DatasourceSpecification)
        {
            EmbeddedH2DatasourceSpecification embeddedH2DatasourceSpecification = (EmbeddedH2DatasourceSpecification) datasourceSpecification;
            return new EmbeddedH2DataSourceSpecificationKey(
                    embeddedH2DatasourceSpecification.databaseName,
                    new File(embeddedH2DatasourceSpecification.directory),
                    embeddedH2DatasourceSpecification.autoServerMode);
        }
        else if (datasourceSpecification instanceof LocalH2DatasourceSpecification)
        {
            LocalH2DatasourceSpecification localH2DatasourceSpecification = (LocalH2DatasourceSpecification) datasourceSpecification;
            if (localH2DatasourceSpecification.testDataSetupSqls != null && !localH2DatasourceSpecification.testDataSetupSqls.isEmpty())
            {
                return new LocalH2DataSourceSpecificationKey(localH2DatasourceSpecification.testDataSetupSqls);
            }
            return new StaticDataSourceSpecificationKey("127.0.0.1", testDbPort, "testDB");
        }
        else if (datasourceSpecification instanceof StaticDatasourceSpecification)
        {
            StaticDatasourceSpecification staticDatasourceSpecification = (StaticDatasourceSpecification) datasourceSpecification;
            return new StaticDataSourceSpecificationKey(
                    staticDatasourceSpecification.host,
                    staticDatasourceSpecification.port,
                    staticDatasourceSpecification.databaseName);
        }
        else if (datasourceSpecification instanceof DeltaLakeDatasourceSpecification)
        {
            DeltaLakeDatasourceSpecification staticDatasourceSpecification = (DeltaLakeDatasourceSpecification) datasourceSpecification;
            return new DeltaLakeDataSourceSpecificationKey(
                    staticDatasourceSpecification.shard,
                    staticDatasourceSpecification.httpPath,
                    staticDatasourceSpecification.token);
        }
        else if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
        {
            SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = (SnowflakeDatasourceSpecification) datasourceSpecification;
            return new SnowflakeDataSourceSpecificationKey(
                    snowflakeDatasourceSpecification.accountName,
                    snowflakeDatasourceSpecification.region,
                    snowflakeDatasourceSpecification.warehouseName,
                    snowflakeDatasourceSpecification.databaseName,
                    snowflakeDatasourceSpecification.cloudType);
        }
        return null;
    }
}