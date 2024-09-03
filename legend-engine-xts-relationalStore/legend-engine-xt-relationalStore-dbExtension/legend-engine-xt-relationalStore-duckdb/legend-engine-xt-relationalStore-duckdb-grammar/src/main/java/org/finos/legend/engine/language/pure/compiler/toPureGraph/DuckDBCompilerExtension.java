// Copyright 2024 Goldman Sachs
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
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DuckDBDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.IcebergDuckDBPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.authentication.DuckDBS3AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_PostProcessor;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_DuckDBS3AuthenticationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_postprocessor_IcebergDuckDBPostProcessor;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_postprocessor_IcebergDuckDBPostProcessor_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DuckDBDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DuckDBDatasourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_runtime_PostProcessorWithParameter;
import org.finos.legend.pure.generated.core_relational_duckdb_relational_sqlQueryToString_duckdbExtension;

public class DuckDBCompilerExtension implements IRelationalCompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "DuckDB");
    }

    @Override
    public List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy>> getExtraAuthenticationStrategyProcessors()
    {
        return Lists.mutable.with((authenticationStrategy, context) ->
        {
            if (authenticationStrategy instanceof DuckDBS3AuthenticationStrategy)
            {
                DuckDBS3AuthenticationStrategy s3AuthenticationStrategy = (DuckDBS3AuthenticationStrategy) authenticationStrategy;
                return new Root_meta_pure_alloy_connections_alloy_authentication_DuckDBS3AuthenticationStrategy_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::authentication::DuckDBS3AuthenticationStrategy"))
                        ._region(s3AuthenticationStrategy.region)
                        ._accessKeyId(s3AuthenticationStrategy.accessKeyId)
                        ._secretAccessKeyVaultReference(s3AuthenticationStrategy.secretAccessKeyVaultReference)
                        ._endpoint(s3AuthenticationStrategy.endpoint);
            }

            return null;
        });
    }

    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((datasourceSpecification, context) ->
        {
            if (datasourceSpecification instanceof DuckDBDatasourceSpecification)
            {
                DuckDBDatasourceSpecification staticDatasourceSpecification = (DuckDBDatasourceSpecification) datasourceSpecification;
                Root_meta_pure_alloy_connections_alloy_specification_DuckDBDatasourceSpecification _static = new Root_meta_pure_alloy_connections_alloy_specification_DuckDBDatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::specification::DuckDBDatasourceSpecification"));
                _static._path(staticDatasourceSpecification.path);
                return _static;
            }
            return null;
        });
    }

    @Override
    public List<Function3<Connection, PostProcessor, CompileContext, Pair<Root_meta_pure_alloy_connections_PostProcessor, Root_meta_relational_runtime_PostProcessorWithParameter>>> getExtraConnectionPostProcessor()
    {
        return Lists.mutable.with((connection, processor, context) ->
        {
            if (processor instanceof IcebergDuckDBPostProcessor)
            {
                IcebergDuckDBPostProcessor icebergDuckDBPostProcessor = (IcebergDuckDBPostProcessor) processor;
                Root_meta_pure_alloy_connections_alloy_postprocessor_IcebergDuckDBPostProcessor p = new Root_meta_pure_alloy_connections_alloy_postprocessor_IcebergDuckDBPostProcessor_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::postprocessor::IcebergDuckDBPostProcessor"));
                p._rootPath(icebergDuckDBPostProcessor.rootPath);
                p._allowMovedPath(icebergDuckDBPostProcessor.allowMovedPath);

                Root_meta_relational_runtime_PostProcessorWithParameter f =
                        core_relational_duckdb_relational_sqlQueryToString_duckdbExtension.Root_meta_relational_functions_sqlQueryToString_duckDB_postprocessor_icebergDuckDBPostProcessor_IcebergDuckDBPostProcessor_1__PostProcessorWithParameter_1_(p, context.pureModel.getExecutionSupport());

                return Tuples.pair(p, f);
            }
            return null;
        });
    }

    @Override
    public CompilerExtension build()
    {
        return new DuckDBCompilerExtension();
    }
}
