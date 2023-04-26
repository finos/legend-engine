// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.integration;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperExternalFormat;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperMappingBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.grammar.integration.extensions.IMongoDBStoreCompilerExtension;
import org.finos.legend.engine.language.pure.grammar.integration.util.AssociationPropertyToClassName;
import org.finos.legend.engine.language.pure.grammar.integration.util.MongoDBCompilerHelper;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.RootMongoDBClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MongoDBCompilerExtension implements IMongoDBStoreCompilerExtension
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    @Override
    public CompilerExtension build()
    {
        return new MongoDBCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        Processor<MongoDatabase> processor = Processor.newProcessor(MongoDatabase.class,
                Collections.singletonList(Binding.class),
                (MongoDatabase mongoDBStore, CompileContext context) ->
                {
                    Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase pureMongoDatabase = new Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase_Impl(mongoDBStore.name)._name(mongoDBStore.name);
                    pureMongoDatabase._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                            ._rawType(context.pureModel.getType("meta::external::store::mongodb::metamodel::pure::MongoDatabase")));

                    return pureMongoDatabase;
                },
                // Second pass - resolve binding and model elements
                (MongoDatabase mongoDBStore, CompileContext context) ->
                {
                    Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase pureMongoDatabase = MongoDBCompilerHelper.getMongoDatabase(context.pureModel.buildPackageString(mongoDBStore._package, mongoDBStore.name), mongoDBStore.sourceInformation, context);
                    MongoDBCompilerHelper.compileAndAddCollectionstoMongoDatabase(pureMongoDatabase, mongoDBStore, context);
                });
        return Lists.immutable.with(processor);
    }

    @Override
    public List<Function2<Connection, CompileContext, Root_meta_pure_runtime_Connection>> getExtraConnectionValueProcessors()
    {
        return Lists.fixedSize.with(
                (connectionValue, context) ->
                {
                    if (connectionValue instanceof MongoDBConnection)
                    {
                        return MongoDBCompilerHelper.buildConnection((MongoDBConnection) connectionValue, context);
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
                    if (cm instanceof RootMongoDBClassMapping)
                    {
                        RootMongoDBClassMapping classMapping = (RootMongoDBClassMapping) cm;
                        String id = HelperMappingBuilder.getClassMappingId(classMapping, context);
                        Root_meta_external_store_mongodb_metamodel_pure_MongoDBSetImplementation mongoDBSetImplementation = new Root_meta_external_store_mongodb_metamodel_pure_MongoDBSetImplementation_Impl(id, null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::pure::MongoDBSetImplementation"));
                        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass = context.resolveClass(classMapping._class, classMapping.classSourceInformation);
                        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass mappingClass = MongoDBCompilerHelper.generateMappingClass(pureClass, id, classMapping, parentMapping, context);
                        mongoDBSetImplementation._id(id);
                        mongoDBSetImplementation._root(classMapping.root);
                        mongoDBSetImplementation._class(pureClass);
                        mongoDBSetImplementation._parent(parentMapping);
                        mongoDBSetImplementation._mappingClass(mappingClass);
                        Store mongoDatabase = context.pureModel.getStore(((RootMongoDBClassMapping) cm).storePath, cm.sourceInformation);
                        mongoDBSetImplementation._storesAdd(mongoDatabase);
                        mongoDBSetImplementation._mainCollection(((Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase_Impl) mongoDatabase)._collections());

                        String topDomainClassFullPath = HelperModelBuilder.getElementFullPath(pureClass, context.pureModel.getExecutionSupport());
                        List<AssociationPropertyToClassName> classesInTheAssociationsOfTheTopDomainClass = context.pureModel.getClass(topDomainClassFullPath)._propertiesFromAssociations().toList().stream().map(c ->
                                new AssociationPropertyToClassName(c.getName(), HelperModelBuilder.getElementFullPath((PackageableElement) c._genericType()._rawType(), context.pureModel.getExecutionSupport()), (PropertyInstance) c)
                        ).collect(Collectors.toList());

                        MutableList<EmbeddedSetImplementation> embeddedSetImplementations = org.eclipse.collections.impl.factory.Lists.mutable.empty();

                        Root_meta_external_shared_format_binding_Binding binding = ((Root_meta_external_shared_format_binding_Binding)context.pureModel.getStore(((RootMongoDBClassMapping) cm).bindingPath, cm.sourceInformation));
                        mongoDBSetImplementation._binding(binding);

                        Set<Class<?>> processedClasses = new HashSet<>();
                        Set<Class<?>> processedClassesAssociations = new HashSet<>();

                        ExternalFormatExtension schemaExtension = HelperExternalFormat.getExternalFormatExtension(binding);
                        Root_meta_external_shared_format_binding_validation_BindingDetail bindingDetail = schemaExtension.bindDetails(binding, context);

                        if (bindingDetail instanceof Root_meta_external_shared_format_binding_validation_SuccessfulBindingDetail)
                        {
                            List<PropertyMapping> propertyMappings = MongoDBCompilerHelper.generatePropertyMappings((Root_meta_external_shared_format_binding_validation_SuccessfulBindingDetail) bindingDetail, mongoDBSetImplementation._class(), mongoDBSetImplementation._id(), embeddedSetImplementations, mongoDBSetImplementation, classMapping.sourceInformation, processedClasses, context);
                            mongoDBSetImplementation._propertyMappings(FastList.newList(propertyMappings).toImmutable());
                            for (AssociationPropertyToClassName currentClass : classesInTheAssociationsOfTheTopDomainClass)
                            {
                                Root_meta_external_store_mongodb_metamodel_pure_EmbeddedMongoDBSetImplementation embeddedMongoDBSetImplementation = new Root_meta_external_store_mongodb_metamodel_pure_EmbeddedMongoDBSetImplementation_Impl("", null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::pure::EmbeddedMongoDBSetImplementation"));
                                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class associationClass = context.resolveClass(currentClass.getClassFullPath());

                                String embeddedPropertyId = pureClass._name() + "_" + currentClass.getPropertyName();
                                embeddedMongoDBSetImplementation._class(associationClass);
                                embeddedMongoDBSetImplementation._id(embeddedPropertyId);
                                embeddedMongoDBSetImplementation._root(false);
                                embeddedMongoDBSetImplementation._property(currentClass.getPropertyInstance());
                                embeddedMongoDBSetImplementation._owner(mongoDBSetImplementation);
                                embeddedMongoDBSetImplementation._sourceSetImplementationId(mongoDBSetImplementation._id());
                                embeddedMongoDBSetImplementation._targetSetImplementationId(embeddedPropertyId);
                                embeddedMongoDBSetImplementation._parent(parentMapping);

                                List<PropertyMapping> associationPropertyMappings = MongoDBCompilerHelper.generatePropertyMappingFromAssociation(associationClass, processedClassesAssociations, embeddedMongoDBSetImplementation._id(), context, (Root_meta_external_shared_format_binding_validation_SuccessfulBindingDetail) bindingDetail, embeddedMongoDBSetImplementation, embeddedSetImplementations, classMapping.sourceInformation);
                                embeddedMongoDBSetImplementation._propertyMappings(FastList.newList(associationPropertyMappings).toImmutable());
                                mongoDBSetImplementation._propertyMappingsAdd(embeddedMongoDBSetImplementation);
                                embeddedSetImplementations.add(embeddedMongoDBSetImplementation);
                            }
                        }
                        else
                        {
                            throw new EngineException("External format : '" + binding._contentType() + "' not yet supported with mongodb store mapping", classMapping.sourceInformation, EngineErrorType.COMPILATION);
                        }

                        return Tuples.pair(mongoDBSetImplementation, embeddedSetImplementations);
                    }
                    return null;
                }
        );
    }
}
