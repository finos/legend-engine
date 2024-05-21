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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPWorkloadIdentityFederationAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.flows.DatabaseAuthenticationFlowKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.MemSqlDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification_Impl;

import java.util.List;

public class MemSqlCompilerExtension implements IRelationalCompilerExtension
{
    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((datasourceSpecification, context) ->
        {
            if (datasourceSpecification instanceof MemSqlDatasourceSpecification)
            {
                MemSqlDatasourceSpecification memSqlDatasourceSpecification = (MemSqlDatasourceSpecification) datasourceSpecification;
                Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification _memsql = new Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification_Impl("");
                _memsql._host(memSqlDatasourceSpecification.host);
                _memsql._port(memSqlDatasourceSpecification.port);
                _memsql._databaseName(memSqlDatasourceSpecification.databaseName);
                _memsql._useSsl(memSqlDatasourceSpecification.useSsl);
                return _memsql;
            }
            return null;
        });
    }

    @Override
    public CompilerExtension build()
    {
        return new MemSqlCompilerExtension();
    }

    @Override
    public List<DatabaseAuthenticationFlowKey> getFlowKeys()
    {
        return Lists.mutable.of(DatabaseAuthenticationFlowKey.newKey(DatabaseType.MemSQL, MemSqlDatasourceSpecification.class, GCPApplicationDefaultCredentialsAuthenticationStrategy.class),
                DatabaseAuthenticationFlowKey.newKey(DatabaseType.MemSQL, MemSqlDatasourceSpecification.class, GCPWorkloadIdentityFederationAuthenticationStrategy.class));
    }
}
