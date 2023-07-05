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
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_PostProcessor;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_relational_runtime_PostProcessorWithParameter;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;

import java.util.List;
import java.util.Objects;

public interface IRelationalCompilerExtension extends CompilerExtension
{
    static List<IRelationalCompilerExtension> getExtensions(CompileContext context)
    {
        return ListIterate.selectInstancesOf(context.getCompilerExtensions().getExtensions(), IRelationalCompilerExtension.class);
    }

    static Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification process(DatasourceSpecification datasourceSpecification, List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> processors, CompileContext context)
    {
        return process(datasourceSpecification, processors, context, "Data Source Specification", datasourceSpecification.sourceInformation);
    }

    static Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy process(AuthenticationStrategy authenticationStrategy, List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy>> processors, CompileContext context)
    {
        return process(authenticationStrategy, processors, context, "Authentication Strategy", authenticationStrategy.sourceInformation);
    }

    static Pair<Root_meta_pure_alloy_connections_PostProcessor, Root_meta_relational_runtime_PostProcessorWithParameter> process(Connection connection, PostProcessor postProcessor, List<Function3<Connection, PostProcessor, CompileContext, Pair<Root_meta_pure_alloy_connections_PostProcessor, Root_meta_relational_runtime_PostProcessorWithParameter>>> processors, CompileContext context)
    {
        SourceInformation srcInfo = postProcessor.sourceInformation;
        return ListIterate
                .collect(processors, processor -> processor.value(connection, postProcessor, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported Post Processor type '" + postProcessor.getClass() + "'", srcInfo, EngineErrorType.COMPILATION));
    }

    static Root_meta_relational_runtime_PostProcessorWithParameter process(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.legacy.PostProcessorWithParameter postProcessorWithParameter, List<Function2<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.legacy.PostProcessorWithParameter, CompileContext, Root_meta_relational_runtime_PostProcessorWithParameter>> processors, CompileContext context)
    {
        return process(postProcessorWithParameter, processors, context, "Post Processor With Parameter", null);
    }

    static org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Milestoning process(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning milestoning, List<Function3<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning, CompileContext, Multimap<String, Column>, org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Milestoning>> processors, Multimap<String, Column> columnMap, CompileContext context)
    {
        return process(milestoning, processors, context, columnMap, "Milestoning", milestoning.sourceInformation);
    }

    static <T, U> U process(T item, List<Function2<T, CompileContext, U>> processors, CompileContext context, String type, SourceInformation srcInfo)
    {
        return ListIterate
                .collect(processors, processor -> processor.value(item, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", srcInfo, EngineErrorType.COMPILATION));
    }

    static <T, U, V> U process(T item, List<Function3<T, CompileContext, V, U>> processors, CompileContext context, V parameter, String type, SourceInformation srcInfo)
    {
        return ListIterate
                .collect(processors, processor -> processor.value(item, context, parameter))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", srcInfo, EngineErrorType.COMPILATION));
    }

    default Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.with();
    }

    default List<Function3<Connection, PostProcessor, CompileContext, Pair<Root_meta_pure_alloy_connections_PostProcessor, Root_meta_relational_runtime_PostProcessorWithParameter>>> getExtraConnectionPostProcessor()
    {
        return FastList.newList();
    }

    default List<Function2<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.legacy.PostProcessorWithParameter, CompileContext, Root_meta_relational_runtime_PostProcessorWithParameter>> getExtraLegacyPostProcessors()
    {
        return FastList.newList();
    }

    default List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy>> getExtraAuthenticationStrategyProcessors()
    {
        return FastList.newList();
    }

    default List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return FastList.newList();
    }

    default List<Function3<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning, CompileContext, Multimap<String, Column>, org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Milestoning>> getExtraMilestoningProcessors()
    {
        return FastList.newList();
    }

}
