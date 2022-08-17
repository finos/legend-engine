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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;

import java.util.List;

public class BigQueryCompilerExtension implements IRelationalCompilerExtension
{
    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((datasourceSpecification, context) ->
        {
            if (datasourceSpecification instanceof BigQueryDatasourceSpecification)
            {
                BigQueryDatasourceSpecification bigQueryDatasourceSpecification = (BigQueryDatasourceSpecification) datasourceSpecification;
                Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification _bigquery = new Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification_Impl("");
                _bigquery._projectId(bigQueryDatasourceSpecification.projectId);
                _bigquery._defaultDataset(bigQueryDatasourceSpecification.defaultDataset);
                _bigquery._proxyHost(bigQueryDatasourceSpecification.proxyHost);
                _bigquery._proxyPort(bigQueryDatasourceSpecification.proxyPort);
                return _bigquery;
            }
            return null;
        });
    }
}
