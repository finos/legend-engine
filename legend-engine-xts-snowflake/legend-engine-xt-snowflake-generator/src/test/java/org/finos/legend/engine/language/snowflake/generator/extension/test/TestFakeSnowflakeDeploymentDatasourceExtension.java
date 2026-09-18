// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.snowflake.generator.extension.test;

import org.finos.legend.engine.language.snowflake.generator.extension.SnowflakeDeploymentDatasourceExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;

public class TestFakeSnowflakeDeploymentDatasourceExtension implements SnowflakeDeploymentDatasourceExtension
{
    @Override
    public DatasourceSpecificationVisitor<SnowflakeDatasourceSpecification> getExtraSnowflakeDatasourceSpecificationVisitor(Identity identity)
    {
        return datasourceSpecification -> visit(datasourceSpecification, identity);
    }

    private SnowflakeDatasourceSpecification visit(DatasourceSpecification datasourceSpecification, Identity identity)
    {
        if (datasourceSpecification instanceof FakeThrowingLakehouseDatasourceSpecification)
        {
            throw new RuntimeException("Simulated failure resolving a live/authenticated datasource specification.");
        }
        if (!(datasourceSpecification instanceof FakeLakehouseDatasourceSpecification))
        {
            return null;
        }
        FakeLakehouseDatasourceSpecification lakehouseSpec = (FakeLakehouseDatasourceSpecification) datasourceSpecification;
        SnowflakeDatasourceSpecification resolved = new SnowflakeDatasourceSpecification();
        resolved.region = lakehouseSpec.environment;
        resolved.accountName = lakehouseSpec.warehouse + ":" + identity.getName();
        resolved.databaseName = lakehouseSpec.database;
        return resolved;
    }
}
