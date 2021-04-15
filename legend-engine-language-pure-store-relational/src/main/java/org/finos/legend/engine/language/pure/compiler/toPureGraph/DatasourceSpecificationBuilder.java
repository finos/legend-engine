// Copyright 2020 Goldman Sachs
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

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.*;
import org.finos.legend.pure.generated.*;

public class DatasourceSpecificationBuilder implements DatasourceSpecificationVisitor<Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>
{
    private final CompileContext context;

    public DatasourceSpecificationBuilder(CompileContext context)
    {
        this.context = context;
    }

    @Override
    public Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification visit(DatasourceSpecification datasourceSpecification)
    {
        if (datasourceSpecification instanceof LocalH2DatasourceSpecification)
        {
            LocalH2DatasourceSpecification localH2DatasourceSpecification = (LocalH2DatasourceSpecification) datasourceSpecification;
            Root_meta_pure_alloy_connections_alloy_specification_LocalH2DatasourceSpecification local = new Root_meta_pure_alloy_connections_alloy_specification_LocalH2DatasourceSpecification_Impl("");
            local._testDataSetupCsv(localH2DatasourceSpecification.testDataSetupCsv);
            local._testDataSetupSqls(localH2DatasourceSpecification.testDataSetupSqls == null ? FastList.newList() : FastList.newList(localH2DatasourceSpecification.testDataSetupSqls));
            return local;
        }
        else if(datasourceSpecification instanceof EmbeddedH2DatasourceSpecification)
        {
            EmbeddedH2DatasourceSpecification embeddedH2DatasourceSpecification = (EmbeddedH2DatasourceSpecification) datasourceSpecification;
            Root_meta_pure_alloy_connections_alloy_specification_EmbeddedH2DatasourceSpecification embedded = new Root_meta_pure_alloy_connections_alloy_specification_EmbeddedH2DatasourceSpecification_Impl("");
            embedded._autoServerMode(embeddedH2DatasourceSpecification.autoServerMode);
            embedded._databaseName(embeddedH2DatasourceSpecification.databaseName);
            embedded._directory(embeddedH2DatasourceSpecification.directory);
            return embedded;
        }
        else if(datasourceSpecification instanceof StaticDatasourceSpecification)
        {
            StaticDatasourceSpecification staticDatasourceSpecification = (StaticDatasourceSpecification) datasourceSpecification;
            Root_meta_pure_alloy_connections_alloy_specification_StaticDatasourceSpecification _static = new Root_meta_pure_alloy_connections_alloy_specification_StaticDatasourceSpecification_Impl("");
            _static._host(staticDatasourceSpecification.host);
            _static._port(staticDatasourceSpecification.port);
            _static._databaseName(staticDatasourceSpecification.databaseName);
            return _static;
        }
        return null;
    }
}