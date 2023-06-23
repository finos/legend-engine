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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.finos.legend.pure.generated.platform_dsl_mapping_functions_Mapping.Root_meta_pure_mapping__allClassMappingsRecursive_Mapping_1__SetImplementation_MANY_;

public class HelperRuntimeBuilder
{
    public static Store getConnectionStore(Root_meta_pure_runtime_Connection connection)
    {
        Object store = connection._element();
        if (store instanceof Store)
        {
            return (Store) store;
        }
        throw new IllegalStateException("Connection does not have a proper store");
    }

    private static Set<String> getAllMapStorePathsFromMappings(List<Mapping> mappings, CompileContext context)
    {
        Set<String> mappedStores = new HashSet<>();
        mappings.forEach(mapping ->
                ListIterate.forEach(Root_meta_pure_mapping__allClassMappingsRecursive_Mapping_1__SetImplementation_MANY_(mapping, context.pureModel.getExecutionSupport()).toList(), setImplementation ->
                {
                    if (setImplementation instanceof PureInstanceSetImplementation && ((PureInstanceSetImplementation) setImplementation)._srcClass() != null)
                    {
                        mappedStores.add("ModelStore");
                    }
                    context.getCompilerExtensions().getExtraSetImplementationSourceScanners().forEach(scanner -> scanner.value(setImplementation, mappedStores, context));
                }));
        return mappedStores;
    }

    public static void checkRuntimeMappingCoverage(Root_meta_pure_runtime_Runtime pureRuntime, List<Mapping> mappings, CompileContext context, SourceInformation sourceInformation)
    {
        Set<String> mappedStores = getAllMapStorePathsFromMappings(mappings, context);
        ListIterate.forEach(pureRuntime._connections().toList(), connection ->
        {
            Store store = getConnectionStore(connection);
            // TODO Identify binding store as ModelStore better than this
            if (store instanceof Root_meta_external_shared_format_binding_Binding)
            {
                mappedStores.remove("ModelStore");
            }
            else
            {
                String storePath = HelperModelBuilder.getElementFullPath(store, context.pureModel.getExecutionSupport());
                mappedStores.remove(storePath);
            }
        });
        // NOTE: this check for relational will be breaking as we start migrating to EngineRuntime, this is a good check, but we should assess its impact
        // hence, maybe we should relax this to a warning
        // TODO?: drill down for model store to detect when a class has not been mapped?
        if (!mappedStores.isEmpty())
        {
            context.pureModel.addWarnings(Lists.mutable.with(new Warning(sourceInformation, "Runtime does not cover store(s) '"
                    + StringUtils.join(new ArrayList<>(mappedStores), "', '")
                    + "' in mapping(s) '" + StringUtils.join(mappings.stream().map(mapping -> HelperModelBuilder.getElementFullPath(mapping, context.pureModel.getExecutionSupport())).collect(Collectors.toList())))));
        }
    }

    public static Root_meta_pure_runtime_Runtime buildEngineRuntime(EngineRuntime engineRuntime, CompileContext context)
    {
        // convert EngineRuntime with connection as a map indexes by store to Pure runtime which only contains an array of connections
        Root_meta_pure_runtime_Runtime pureRuntime = new Root_meta_pure_runtime_Runtime_Impl("Root::meta::pure::runtime::Runtime");
        buildEngineRuntime(engineRuntime, pureRuntime, context);
        return pureRuntime;
    }

    public static void buildEngineRuntime(EngineRuntime engineRuntime, Root_meta_pure_runtime_Runtime pureRuntime, CompileContext context)
    {
        if (engineRuntime.mappings.isEmpty())
        {
            context.pureModel.addWarnings(Lists.mutable.with(new Warning(engineRuntime.sourceInformation, "Runtime must cover at least one mapping")));
        }
        // verify if each mapping associated with the PackageableRuntime exists
        List<Mapping> mappings = engineRuntime.mappings.isEmpty() ? Lists.mutable.empty() : engineRuntime.mappings.stream().map(mappingPointer -> context.resolveMapping(mappingPointer.path, mappingPointer.sourceInformation)).collect(Collectors.toList());
        // build connections
        List<Connection> connections = new ArrayList<>();
        List<CoreInstance> visitedSourceClasses = new ArrayList<>();
        List<CoreInstance> visitedConnectionTypes = new ArrayList<>();
        List<String> visitedStores = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        ListIterate.forEach(engineRuntime.connections.stream().filter(c -> c.store.path.equals("ModelStore") || !(context.resolvePackageableElement(c.store.path, c.store.sourceInformation) instanceof Root_meta_external_shared_format_binding_Binding)).collect(Collectors.toList()), storeConnections ->
        {
            if (!storeConnections.store.path.equals("ModelStore"))
            {
                // verify stores used for indexing exist
                context.resolveStore(storeConnections.store.path, storeConnections.store.sourceInformation);
            }
            ListIterate.forEach(storeConnections.storeConnections, identifiedConnection ->
            {
                // ID must be unique across all connections of the runtime
                if (ids.contains(identifiedConnection.id))
                {
                    throw new EngineException("Runtime connection with ID '" + identifiedConnection.id + "' has already been specified", identifiedConnection.sourceInformation, EngineErrorType.COMPILATION);
                }
                else
                {
                    ids.add(identifiedConnection.id);
                }
                if (identifiedConnection.connection instanceof ConnectionPointer)
                {
                    ConnectionPointer pointer = (ConnectionPointer) identifiedConnection.connection;
                    Store connectionStore = HelperRuntimeBuilder.getConnectionStore(context.resolveConnection(pointer.connection, pointer.sourceInformation));
                    String connectionStorePath = HelperModelBuilder.getElementFullPath(connectionStore, context.pureModel.getExecutionSupport());
                    if (!storeConnections.store.path.equals(connectionStorePath))
                    {
                        throw new EngineException("Connection pointer for store '" + connectionStorePath + "' should not be indexed to store '" + storeConnections.store.path + "'", pointer.sourceInformation, EngineErrorType.COMPILATION);
                    }
                    connections.add(identifiedConnection.connection);
                }
                else if (storeConnections.store.path.equals(identifiedConnection.connection.element))
                {
                    connections.add(identifiedConnection.connection);
                }
                else if (identifiedConnection.connection.element == null)
                {
                    identifiedConnection.connection.element = storeConnections.store.path;
                    identifiedConnection.connection.elementSourceInformation = identifiedConnection.connection.sourceInformation;
                    connections.add(identifiedConnection.connection);
                }
                else
                {
                    throw new EngineException("Connection for store '" + identifiedConnection.connection.element + "' should not be indexed to store '" + storeConnections.store.path + "'", identifiedConnection.connection.sourceInformation, EngineErrorType.COMPILATION);
                }
            });
        });

        ListIterate.forEach(connections, connection ->
        {
            final Root_meta_pure_runtime_Connection pureConnection = connection.accept(new ConnectionFirstPassBuilder(context));
            connection.accept(new ConnectionSecondPassBuilder(context, pureConnection));

            if (pureConnection instanceof Root_meta_pure_mapping_modelToModel_JsonModelConnection || pureConnection instanceof Root_meta_pure_mapping_modelToModel_XmlModelConnection)
            {
                if (visitedSourceClasses.contains(pureConnection.getValueForMetaPropertyToOne("class")) && visitedStores.contains(HelperModelBuilder.getElementFullPath((PackageableElement) pureConnection._element(),context.pureModel.getExecutionSupport())))
                {
                    context.pureModel.addWarnings(Lists.mutable.with(new Warning(connection.sourceInformation, "Multiple Connections available for Source Class - " + pureConnection.getValueForMetaPropertyToOne("class"))));
                }
                else
                {
                    visitedSourceClasses.add(pureConnection.getValueForMetaPropertyToOne("class"));
                }
            }
            else
            {
                if (visitedConnectionTypes.contains(pureConnection.getClassifier()) && visitedStores.contains(HelperModelBuilder.getElementFullPath((PackageableElement) pureConnection._element(),context.pureModel.getExecutionSupport())))
                {
                    if (pureConnection instanceof Root_meta_pure_mapping_modelToModel_ModelChainConnection)
                    {
                        context.pureModel.addWarnings(Lists.mutable.with(new Warning(connection.sourceInformation, "Multiple " + pureConnection.getClassifier() + "s are Not Supported for the same Runtime.")));
                    }
                    else
                    {
                        context.pureModel.addWarnings(Lists.mutable.with(new Warning(connection.sourceInformation, "Multiple " + pureConnection.getClassifier() + "s are Not Supported for the same Store - " + HelperModelBuilder.getElementFullPath((PackageableElement) pureConnection._element(),context.pureModel.getExecutionSupport()))));
                    }
                }
                else
                {
                    visitedConnectionTypes.add(pureConnection.getClassifier());
                }
            }

            visitedStores.add(HelperModelBuilder.getElementFullPath((PackageableElement) pureConnection._element(),context.pureModel.getExecutionSupport()));
            pureRuntime._connectionsAdd(pureConnection);
        });
        // verify runtime mapping coverage
        checkRuntimeMappingCoverage(pureRuntime, mappings, context, engineRuntime.sourceInformation);
    }

    public static Root_meta_pure_runtime_Runtime buildPureRuntime(Runtime runtime, CompileContext context)
    {
        if (runtime == null)
        {
            return null;
        }
        if (runtime instanceof LegacyRuntime)
        {
            LegacyRuntime legacyRuntime = (LegacyRuntime) runtime;
            Root_meta_pure_runtime_Runtime pureRuntime = new Root_meta_pure_runtime_Runtime_Impl("Root::meta::pure::runtime::Runtime");
            ListIterate.forEach(legacyRuntime.connections, connection ->
            {
                final Root_meta_pure_runtime_Connection pureConnection = connection.accept(new ConnectionFirstPassBuilder(context));
                connection.accept(new ConnectionSecondPassBuilder(context, pureConnection));
                pureRuntime._connectionsAdd(pureConnection);
            });
            return pureRuntime;
        }
        if (runtime instanceof EngineRuntime)
        {
            return buildEngineRuntime(((EngineRuntime) runtime), context);
        }
        else if (runtime instanceof RuntimePointer)
        {
            return context.resolveRuntime(((RuntimePointer) runtime).runtime, ((RuntimePointer) runtime).sourceInformation);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * NOTE: this compatibility check is fairly simple and only do static analysis on the runtimes
     * However, we could be more advanced and do some router analysis, etc.
     */
    public static List<Root_meta_pure_runtime_PackageableRuntime> getMappingCompatibleRuntimes(
            Mapping mappingToCheck, List<PackageableRuntime> runtimes, PureModel pureModel)
    {
        return ListIterate.collect(runtimes, runtime -> pureModel.getPackageableRuntime(runtime.getPath(), null)).distinct()
                .select(runtime -> isRuntimeCompatibleWithMapping(runtime, mappingToCheck));
    }

    public static boolean isRuntimeCompatibleWithMapping(Root_meta_pure_runtime_PackageableRuntime runtime, Mapping mappingToCheck)
    {
        return ListIterate.collect(runtime._runtimeValue()._mappings().toList(), mapping ->
        {
            Set<Mapping> mappings = new HashSet<>();
            mappings.add(mapping);
            mappings.addAll(HelperMappingBuilder.getAllIncludedMappings(mapping).toSet());
            return mappings;
        }).anySatisfy(mappings -> mappings.contains(mappingToCheck));
    }
}
