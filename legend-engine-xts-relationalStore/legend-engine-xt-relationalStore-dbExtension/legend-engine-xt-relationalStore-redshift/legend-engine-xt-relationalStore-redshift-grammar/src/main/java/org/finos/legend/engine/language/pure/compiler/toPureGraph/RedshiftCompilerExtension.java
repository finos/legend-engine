// Copyright 2023 Goldman Sachs
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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.flows.DatabaseAuthenticationFlowKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.RedshiftDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_legend_connections_legend_specification_RedshiftDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_legend_connections_legend_specification_RedshiftDatasourceSpecification_Impl;

import java.util.List;

public class RedshiftCompilerExtension implements IRelationalCompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Redshift");
    }

    @Override
    public List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy>> getExtraAuthenticationStrategyProcessors()
    {
        return Lists.mutable.with((authenticationStrategy, context) -> null);
    }

    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((datasourceSpecification, context) ->
        {
            if (datasourceSpecification instanceof RedshiftDatasourceSpecification)
            {
                RedshiftDatasourceSpecification redshiftDatasourceSpecification = (RedshiftDatasourceSpecification) datasourceSpecification;
                Root_meta_pure_legend_connections_legend_specification_RedshiftDatasourceSpecification redshiftSpec = new Root_meta_pure_legend_connections_legend_specification_RedshiftDatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::legend::connections::legend::specification::RedshiftDatasourceSpecification"));
                redshiftSpec._clusterID(redshiftDatasourceSpecification.clusterID);
                redshiftSpec._databaseName(redshiftDatasourceSpecification.databaseName);
                redshiftSpec._endpointURL(redshiftDatasourceSpecification.endpointURL);
                redshiftSpec._host(redshiftDatasourceSpecification.host);
                redshiftSpec._port(redshiftDatasourceSpecification.port);
                redshiftSpec._region(redshiftDatasourceSpecification.region);
                return redshiftSpec;
            }
            return null;
        });
    }

    @Override
    public CompilerExtension build()
    {
        return new RedshiftCompilerExtension();
    }

    @Override
    public List<DatabaseAuthenticationFlowKey> getFlowKeys()
    {
        return Lists.mutable.of(DatabaseAuthenticationFlowKey.newKey(DatabaseType.Redshift, RedshiftDatasourceSpecification.class, UserNamePasswordAuthenticationStrategy.class));
    }
}
