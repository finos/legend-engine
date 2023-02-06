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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.ServiceStoreEmbeddedDataCompiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.RootServiceStoreClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ServiceStore;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ServiceStore;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_ServiceStore_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_runtime_ServiceStoreConnection;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_runtime_ServiceStoreConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;

import java.util.Collections;
import java.util.List;

public class ServiceStoreCompilerExtension implements IServiceStoreCompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.with(
                Processor.newProcessor(
                        ServiceStore.class,
                        Collections.singletonList(Binding.class),
                        // First pass - add serviceStores to compile context
                        (ServiceStore serviceStore, CompileContext context) ->
                        {
                            Root_meta_external_store_service_metamodel_ServiceStore pureServiceStore = new Root_meta_external_store_service_metamodel_ServiceStore_Impl(serviceStore.name)._name(serviceStore.name);

                            pureServiceStore._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                                    ._rawType(context.pureModel.getType("meta::external::store::service::metamodel::ServiceStore")));

                            context.pureModel.storesIndex.put(context.pureModel.buildPackageString(serviceStore._package, serviceStore.name), pureServiceStore);
                            HelperServiceStoreBuilder.compileAndAddSecuritySchemesToServiceStore(pureServiceStore, serviceStore.securitySchemes, context);
                            return pureServiceStore;
                        },
                        // Second pass - resolve binding and model elements
                        (ServiceStore serviceStore, CompileContext context) ->
                        {
                            Root_meta_external_store_service_metamodel_ServiceStore pureServiceStore = HelperServiceStoreBuilder.getServiceStore(context.pureModel.buildPackageString(serviceStore._package, serviceStore.name), serviceStore.sourceInformation, context);
                            HelperServiceStoreBuilder.compileAndAddElementsToServiceStore(pureServiceStore, serviceStore, context);
                        }));
    }

    @Override
    public List<Function3<ClassMapping, Mapping, CompileContext, Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>>>> getExtraClassMappingFirstPassProcessors()
    {
        return Collections.singletonList(
                (cm, parentMapping, context) ->
                {
                    if (cm instanceof RootServiceStoreClassMapping)
                    {
                        RootServiceStoreClassMapping classMapping = (RootServiceStoreClassMapping) cm;
                        return HelperServiceStoreClassMappingBuilder.compileRootServiceStoreClassMapping(classMapping, parentMapping, context);
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Function2<Connection, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection>> getExtraConnectionValueProcessors()
    {
        return Lists.mutable.with(
                (connectionValue, context) ->
                {
                    if (connectionValue instanceof ServiceStoreConnection)
                    {
                        ServiceStoreConnection serviceStoreConnection = (ServiceStoreConnection) connectionValue;

                        Root_meta_external_store_service_metamodel_runtime_ServiceStoreConnection pureServiceStoreConnection = new Root_meta_external_store_service_metamodel_runtime_ServiceStoreConnection_Impl("", null, context.pureModel.getClass("meta::external::store::service::metamodel::runtime::ServiceStoreConnection"));
                        pureServiceStoreConnection._element(HelperServiceStoreBuilder.getServiceStore(serviceStoreConnection.element, serviceStoreConnection.elementSourceInformation, context));
                        pureServiceStoreConnection._baseUrl(serviceStoreConnection.baseUrl);
                        HelperServiceStoreBuilder.compileAndAddAuthenticationSpecifications(pureServiceStoreConnection,serviceStoreConnection.authenticationSpecifications,context);
                        return pureServiceStoreConnection;
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Procedure<Procedure2<String, List<String>>>> getExtraElementForPathToElementRegisters()
    {
        return Collections.singletonList(registerElementForPathToElement ->
        {
            registerElementForPathToElement.value("meta::external::store::service::contract", Lists.mutable.with(
                    "supports_FunctionExpression_1__Boolean_1_",
                    "planExecution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__Extension_MANY__DebugContext_1__ExecutionNode_1_"
            ));

            ImmutableList<String> versions = PureClientVersions.versionsSince("v1_21_0");
            versions.forEach(v -> registerElementForPathToElement.value("meta::protocols::pure::" + v + "::extension::store::service", Lists.mutable.with("getServiceStoreExtension_String_1__SerializerExtension_1_")));
        });
    }

    @Override
    public List<Function3<EmbeddedData, CompileContext, ProcessingContext, Root_meta_pure_data_EmbeddedData>> getExtraEmbeddedDataProcessors()
    {
        return Collections.singletonList(ServiceStoreEmbeddedDataCompiler::compileServiceStoreEmbeddedDataCompiler);
    }
}
