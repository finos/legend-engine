//  Copyright 2026 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.testable.service.extension;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.DataResolverHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.RelationAccessorTestConnectionFactory;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.TestConnectionBuildParameters;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataResolver;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElementsData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.ModelStore;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

// mirror function testing
public final class ServiceTestDataResolverSetup
{
    private static final MutableList<RelationAccessorTestConnectionFactory> RELATION_BUILDERS =
            Lists.mutable.withAll(ServiceLoader.load(RelationAccessorTestConnectionFactory.class));

    private static final MutableList<ConnectionFactoryExtension> CONNECTION_BUILDERS =
            Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));

    private ServiceTestDataResolverSetup()
    {
    }

    public static final class Result
    {
        // re-written function; null when the store path applies
        public FunctionDefinition<?> modifiedFunction;

        // closeables the session must release at teardown
        public final List<Closeable> closeables = Lists.mutable.empty();
    }

    public static Result build(List<DataResolver> resolvers,
                               Root_meta_core_runtime_Runtime compiledRuntime,
                               FunctionDefinition<?> function,
                               ConnectionVisitor<Root_meta_core_runtime_Connection> connectionVisitor,
                               PureModel pureModel,
                               PureModelContextData pmcd,
                               TestConnectionBuildParameters hints,
                               String suiteId)
    {
        MutableMap<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement, EmbeddedData> resolved =
                new DataResolverHelper().resolveDataFromDataResolversToProtocol(resolvers, pureModel.getContext(), pmcd);

        MutableMap<org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store, EmbeddedData> storeBucket = Maps.mutable.empty();
        MutableMap<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement, RelationElementsData> nonStoreRelationBucket = Maps.mutable.empty();
        resolved.forEach((element, data) ->
        {
            if (element instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store)
            {
                storeBucket.put((org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store) element, data);
            }
            else if (data instanceof RelationElementsData)
            {
                nonStoreRelationBucket.put(element, (RelationElementsData) data);
            }
            else
            {
                throw new IllegalStateException("Error in service testSuite " + suiteId + ". Resolved test data for '"
                        + HelperModelBuilder.getElementFullPath(element, pureModel.getExecutionSupport())
                        + "' is not attachable: expected a Store target or RelationElementsData payload, got "
                        + (data == null ? "null" : data.getClass().getSimpleName()));
            }
        });

        if (!storeBucket.isEmpty() && !nonStoreRelationBucket.isEmpty())
        {
            throw new IllegalStateException("Error in service testSuite " + suiteId + ". The combination of store and non-store relation test data is not supported");
        }

        Result result = new Result();
        if (!storeBucket.isEmpty())
        {
            if (compiledRuntime != null)
            {
                mutateCompiledRuntime(compiledRuntime, storeBucket, connectionVisitor, pureModel, pmcd, hints, suiteId, result);
            }
        }
        else if (!nonStoreRelationBucket.isEmpty())
        {
            if (function == null)
            {
                throw new UnsupportedOperationException("Non-store relation serviceTestData is not yet supported for PureMultiExecution in service testSuite " + suiteId + "; use PureSingleExecution or connectionsTestData.");
            }
            result.modifiedFunction = rewriteFunction(nonStoreRelationBucket, function, pureModel, suiteId);
        }
        return result;
    }

    private static void mutateCompiledRuntime(Root_meta_core_runtime_Runtime compiledRuntime,
                                              Map<org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store, EmbeddedData> storeBucket,
                                              ConnectionVisitor<Root_meta_core_runtime_Connection> connectionVisitor,
                                              PureModel pureModel,
                                              PureModelContextData pmcd,
                                              TestConnectionBuildParameters hints,
                                              String suiteId,
                                              Result result)
    {
        Map<String, EmbeddedData> dataByPath = Maps.mutable.empty();
        storeBucket.forEach((store, data) -> dataByPath.put(HelperModelBuilder.getElementFullPath(store, pureModel.getExecutionSupport()), data));
        Map<String, DataElement> dataElementIndex = buildDataElementIndex(pmcd);

        // Group ConnectionStore instances by the compiled Connection they carry.
        MutableMap<Root_meta_core_runtime_Connection, MutableList<Root_meta_core_runtime_ConnectionStore>> byConnection = Maps.mutable.empty();
        compiledRuntime._connectionStores().forEach(cs -> byConnection.getIfAbsentPut(cs._connection(), Lists.mutable::empty).add(cs));

        // Prepend a restore-closeable so partial mutation is undone even if a later group fails.
        List<Pair<Root_meta_core_runtime_ConnectionStore, Root_meta_core_runtime_Connection>> savedOriginals = Lists.mutable.empty();
        result.closeables.add(() -> savedOriginals.forEach(p -> p.getOne()._connection(p.getTwo())));

        byConnection.forEachKeyValue((originalConnection, connectionStores) ->
        {
            MutableMap<Store, EmbeddedData> storeMap = Maps.mutable.empty();
            connectionStores.forEach(cs ->
            {
                Object element = cs._element();
                if (element instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store)
                {
                    String path = HelperModelBuilder.getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store) element, pureModel.getExecutionSupport());
                    EmbeddedData data = dataByPath.get(path);
                    if (data != null)
                    {
                        storeMap.put(resolveProtocolStore(pmcd, path), data);
                    }
                }
            });
            if (storeMap.isEmpty())
            {
                return;
            }
            Pair<Connection, List<Closeable>> built = buildMockedConnection(originalConnection, storeMap, dataElementIndex, hints, suiteId);
            Root_meta_core_runtime_Connection mockedCompiled = built.getOne().accept(connectionVisitor);
            connectionStores.forEach(cs ->
            {
                savedOriginals.add(Tuples.pair(cs, cs._connection()));
                cs._connection(mockedCompiled);
            });
            result.closeables.addAll(built.getTwo());
        });
    }

    private static Store resolveProtocolStore(PureModelContextData pmcd, String path)
    {
        if ("ModelStore".equals(path))
        {
            return new ModelStore();
        }
        return ListIterate.detect(pmcd.getElementsOfType(Store.class), s -> path.equals(s.getPath()));
    }

    private static Pair<Connection, List<Closeable>> buildMockedConnection(Root_meta_core_runtime_Connection originalConnection,
                                                                          Map<Store, EmbeddedData> storeMap,
                                                                          Map<String, DataElement> dataElementIndex,
                                                                          TestConnectionBuildParameters hints,
                                                                          String suiteId)
    {
        for (ConnectionFactoryExtension factory : CONNECTION_BUILDERS)
        {
            Optional<Pair<Connection, List<Closeable>>> optional = factory.tryBuildConnectionForStoreData(dataElementIndex, storeMap, hints);
            if (optional != null && optional.isPresent())
            {
                return optional.get();
            }
        }
        throw new UnsupportedOperationException("Unsupported test data for service testSuite " + suiteId + " (connection: " + originalConnection.getClass().getSimpleName() + ")");
    }

    private static Map<String, DataElement> buildDataElementIndex(PureModelContextData pmcd)
    {
        Map<String, DataElement> index = Maps.mutable.empty();
        pmcd.getElementsOfType(DataElement.class).forEach(d -> index.put(d.getPath(), d));
        return index;
    }

    private static FunctionDefinition<?> rewriteFunction(Map<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement, RelationElementsData> relationData,
                                                         FunctionDefinition<?> function,
                                                         PureModel pureModel,
                                                         String suiteId)
    {
        MutableList<FunctionDefinition<?>> modifiedFunctions = RELATION_BUILDERS
                .collect(f -> f.rewriteFunctionForTestDataExecution(function, relationData, pureModel));
        modifiedFunctions.removeIf(java.util.Objects::isNull);
        if (modifiedFunctions.size() > 1)
        {
            throw new IllegalStateException("Error in service testSuite " + suiteId + ". The combination of accessors used is not supported");
        }
        if (modifiedFunctions.isEmpty())
        {
            throw new IllegalStateException("Error in service testSuite " + suiteId + ". Unsupported accessors type");
        }
        return modifiedFunctions.get(0);
    }
}
