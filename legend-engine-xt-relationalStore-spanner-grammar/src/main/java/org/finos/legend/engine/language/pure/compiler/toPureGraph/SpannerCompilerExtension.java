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

import java.util.List;
import java.util.Optional;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SpannerDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SpannerDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SpannerDatasourceSpecification_Impl;

public class SpannerCompilerExtension implements IRelationalCompilerExtension
{
    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((datasourceSpecification, context) ->
        {
            if (datasourceSpecification instanceof SpannerDatasourceSpecification)
            {
                SpannerDatasourceSpecification spannerDatasourceSpecification = (SpannerDatasourceSpecification) datasourceSpecification;
                Root_meta_pure_alloy_connections_alloy_specification_SpannerDatasourceSpecification spannerSpec =
                        new Root_meta_pure_alloy_connections_alloy_specification_SpannerDatasourceSpecification_Impl("");
                spannerSpec._projectId(spannerDatasourceSpecification.projectId);
                spannerSpec._instanceId(spannerDatasourceSpecification.instanceId);
                spannerSpec._proxyHost(spannerDatasourceSpecification.proxyHost);
                spannerSpec._proxyPort(Optional.ofNullable(spannerDatasourceSpecification.proxyPort).map(Integer::longValue).orElse(null));
                spannerSpec._databaseId(spannerDatasourceSpecification.databaseId);
                return spannerSpec;
            }
            return null;
        });
    }
}
