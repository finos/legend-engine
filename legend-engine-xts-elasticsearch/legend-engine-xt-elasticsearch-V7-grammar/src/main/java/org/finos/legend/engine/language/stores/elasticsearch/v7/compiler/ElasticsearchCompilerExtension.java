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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.compiler;

import java.util.Collections;
import java.util.List;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.context.Elasticsearch7ExecutionContext;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreConnection;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7Store;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext;

public class ElasticsearchCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Elastic");
    }

    @Override
    public CompilerExtension build()
    {
        return this;
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.with(Processor.newProcessor(Elasticsearch7Store.class, HelperElasticsearchBuilder::buildStoreFirstPass));
    }

    @Override
    public List<Function2<Connection, CompileContext, Root_meta_core_runtime_Connection>> getExtraConnectionValueProcessors()
    {
        return Lists.fixedSize.with(
                (connectionValue, context) ->
                {
                    if (connectionValue instanceof Elasticsearch7StoreConnection)
                    {
                        return HelperElasticsearchBuilder.buildConnection((Elasticsearch7StoreConnection) connectionValue, context);
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Function<Handlers, List<FunctionHandlerRegistrationInfo>>> getExtraFunctionHandlerRegistrationInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                Lists.fixedSize.with(
                        new FunctionHandlerRegistrationInfo(null,
                                handlers.h("meta::external::store::elasticsearch::v7::tds::indexToTDS_Elasticsearch7Store_1__String_1__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"))
                        )
                ));
    }

    @Override
    public List<Function2<ExecutionContext, CompileContext, Root_meta_pure_runtime_ExecutionContext>> getExtraExecutionContextProcessors()
    {
        return Collections.singletonList((executionContext, context) ->
        {
            if (executionContext instanceof Elasticsearch7ExecutionContext)
            {
                return HelperElasticsearchBuilder.buildExecutionContext((Elasticsearch7ExecutionContext) executionContext, context);
            }
            return null;
        });
    }

    @Override
    public List<Procedure<Procedure2<String, List<String>>>> getExtraElementForPathToElementRegisters()
    {
        return Collections.singletonList(registerElementForPathToElement ->
        {
            // we will support protocol after 1_30_0
            ImmutableList<String> versions = PureClientVersions.versionsSinceExclusive("v1_30_0");
            versions.forEach(v -> registerElementForPathToElement.value(
                            "meta::external::store::elasticsearch::v7::protocol::" + v,
                            Collections.singletonList("elasticsearchV7StoreExtension_String_1__SerializerExtension_1_")
                    )
            );
        });
    }
}
