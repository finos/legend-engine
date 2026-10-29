// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.integration.store.graphql.compiler;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperExternalFormat;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperMappingBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLClassMapping;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLConnection;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLStore;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphQLStoreCompilerExtension implements IGraphQLStoreCompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("Store", "GraphQL");
    }

    @Override
    public CompilerExtension build()
    {
        return new GraphQLStoreCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        Processor<GraphQLStore> processor = Processor.newProcessor(GraphQLStore.class,
                Collections.emptyList(),
                (GraphQLStore graphQLStore, CompileContext context) ->
                {
                    Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore pureStore = new Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore_Impl(graphQLStore.name)._name(graphQLStore.name);
                    pureStore._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                            ._rawType(context.pureModel.getType("meta::external::store::graphQL::metamodel::pure::GraphQLStore")));
                    if (graphQLStore.schema != null)
                    {
                        pureStore._document(GraphQLStoreCompilerHelper.compileSchemaDocument(graphQLStore, context));
                    }
                    return pureStore;
                },
                (GraphQLStore graphQLStore, CompileContext context) ->
                {
                });

        return Lists.immutable.with(processor);
    }

    @Override
    public List<Function2<Connection, CompileContext, Root_meta_core_runtime_Connection>> getExtraConnectionValueProcessors()
    {
        return Lists.fixedSize.with(
                (connectionValue, context) ->
                {
                    if (connectionValue instanceof GraphQLConnection)
                    {
                        return GraphQLStoreCompilerHelper.buildConnection((GraphQLConnection) connectionValue, context);
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Function3<ClassMapping, Mapping, CompileContext, Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>>>> getExtraClassMappingFirstPassProcessors()
    {
        return Collections.singletonList(
                (cm, parentMapping, context) ->
                {
                    if (cm instanceof GraphQLClassMapping)
                    {
                        GraphQLClassMapping classMapping = (GraphQLClassMapping) cm;

                        Root_meta_external_format_shared_binding_Binding binding = (Root_meta_external_format_shared_binding_Binding) context.pureModel.getPackageableElement(classMapping.bindingPath, cm.sourceInformation);
                        Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore store = (Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore) context.resolveStore(classMapping.storePath, cm.sourceInformation);
                        Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation setImpl = GraphQLStoreCompilerHelper.createRootGraphQLInstanceSetImplementation(classMapping, context, parentMapping, cm, binding, store);

                        ExternalFormatExtension<?> schemaExtension = HelperExternalFormat.getExternalFormatExtension(binding);
                        Root_meta_external_format_shared_binding_validation_BindingDetail bindingDetail = schemaExtension.bindDetails(binding, context);

                        MutableList<EmbeddedSetImplementation> embeddedSetImplementations = Lists.mutable.empty();
                        if (bindingDetail instanceof Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail)
                        {
                            // Seeded with the root's own class -> id so a self-referencing property (e.g.
                            // Employee.manager: Employee) resolves back to the root instead of embedding anew.
                            Map<Class<?>, String> classToSetImplementationId = new HashMap<>();
                            classToSetImplementationId.put(setImpl._class(), setImpl._id());
                            List<PropertyMapping> propertyMappings = GraphQLStoreCompilerHelper.generatePropertyMappings((Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail) bindingDetail, setImpl._class(), setImpl._id(), embeddedSetImplementations, setImpl, classToSetImplementationId, context, parentMapping);
                            setImpl._propertyMappings(Lists.mutable.ofAll(propertyMappings).toImmutable());
                        }
                        else
                        {
                            throw new EngineException("External format : '" + binding._contentType() + "' not yet supported with GraphQL store mapping", classMapping.sourceInformation, EngineErrorType.COMPILATION);
                        }

                        return Tuples.pair(setImpl, embeddedSetImplementations.toImmutable());
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Procedure3<ClassMapping, CompileContext, Set<PackageableElementPointer>>> getExtraClassMappingPrerequisiteElementsPassProcessors()
    {
        return Collections.singletonList(
                (cm, context, prerequisiteElements) ->
                {
                    if (cm instanceof GraphQLClassMapping)
                    {
                        GraphQLClassMapping classMapping = (GraphQLClassMapping) cm;
                        prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.BINDING, classMapping.bindingPath, classMapping.sourceInformation));
                        prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.STORE, classMapping.storePath, classMapping.sourceInformation));
                        prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, classMapping._class, classMapping.classSourceInformation));
                    }
                }
        );
    }

    @Override
    public List<Procedure3<ClassMapping, Mapping, CompileContext>> getExtraClassMappingSecondPassProcessors()
    {
        return Collections.singletonList(
                (cm, parentMapping, context) ->
                {
                    if (cm instanceof GraphQLClassMapping)
                    {
                        GraphQLClassMapping classMapping = (GraphQLClassMapping) cm;
                        GraphQLStoreCompilerHelper.fixUpPolymorphicPropertyMappingsForNativeSubType(parentMapping, context);
                        String id = HelperMappingBuilder.getClassMappingId(classMapping, context);
                        Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation setImpl =
                                (Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation) parentMapping._classMappings().select(c -> c._id().equals(id)).getFirst();
                        if (!classMapping.root)
                        {
                            boolean alsoReachedViaEmbedding = parentMapping._classMappings().anySatisfy(c ->
                                    c instanceof Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation
                                            && ((Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation) c)._class() == setImpl._class());
                            if (alsoReachedViaEmbedding)
                            {
                                throw new EngineException("Class mapping for '" + HelperModelBuilder.getElementFullPath(setImpl._class(), context.pureModel.getExecutionSupport())
                                        + "' is declared independently here (root = false) but is also reached by folding from another property. Remove this standalone declaration - a folded-only type is mapped implicitly from the owning property, not declared separately.",
                                        classMapping.sourceInformation, EngineErrorType.COMPILATION);
                            }
                        }
                        if (classMapping.root)
                        {
                            GraphQLStoreCompilerHelper.validateQualifiedPropertyReturnTypesHaveAClassMapping(classMapping, setImpl._class(), parentMapping, context);
                            GraphQLStoreCompilerHelper.validateThisThreadedQualifiedPropertiesHaveProxyMapping(classMapping, setImpl, setImpl._class(), context);
                        }
                    }
                }
        );
    }

    // Dev version only - a brand-new store with no historical client versions to register.
    @Override
    public List<Procedure<Procedure2<String, List<String>>>> getExtraElementForPathToElementRegisters()
    {
        return Collections.singletonList(registerElementForPathToElement ->
                PureClientVersions.versionsSinceExclusive(PureClientVersions.production).forEach(v ->
                        registerElementForPathToElement.value("meta::protocols::pure::" + v + "::extension::store::graphQL",
                                Lists.mutable.with("getGraphQLStoreExtension_String_1__SerializerExtension_1_"))));
    }
}
