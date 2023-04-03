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
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperMappingBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.grammar.integration.extensions.IMongoDBStoreCompilerExtension;
import org.finos.legend.engine.language.pure.grammar.integration.util.MongoDBCompilerHelper;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Collection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.mapping.MongoDBPropertyMapping;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.RootMongoDBClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
                        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass mappingClass = generateMappingClass(pureClass, id, classMapping, parentMapping, context);
                        mongoDBSetImplementation._id(id);
                        mongoDBSetImplementation._root(classMapping.root);
                        mongoDBSetImplementation._class(pureClass);
                        mongoDBSetImplementation._parent(parentMapping);
                        mongoDBSetImplementation._mappingClass(mappingClass);
                        Store mongoDatabase = context.pureModel.getStore(((RootMongoDBClassMapping) cm).storePath, cm.sourceInformation);
                        mongoDBSetImplementation._storesAdd(mongoDatabase);
                        mongoDBSetImplementation._mainCollection(((Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase_Impl) mongoDatabase)._collections());
                        mongoDBSetImplementation._binding(((Root_meta_external_shared_format_binding_Binding)context.pureModel.getStore(((RootMongoDBClassMapping) cm).bindingPath, cm.sourceInformation)));

//                        RichIterable<? extends Root_meta_external_store_mongodb_metamodel_PropertyType> properties = ((Root_meta_external_store_mongodb_metamodel_aggregation_JsonSchemaExpression_Impl)((Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase_Impl)mongoDatabase)._collections().getFirst()._validator()._validatorExpression())._schemaExpression()._properties();

//                        for (Root_meta_external_store_mongodb_metamodel_PropertyType prop : properties)
//                        {
//                            if (!prop._value().getClassifier().toString().equals("ObjectType"))
//                            {
//                                Root_meta_external_store_mongodb_metamodel_mapping_MongoDBPropertyMapping currentPropertyMapping = new Root_meta_external_store_mongodb_metamodel_mapping_MongoDBPropertyMapping_Impl("", null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::mapping::MongoDBPropertyMapping"))
//                                        ._property(getPropertyFromDomainClass(prop._key(), pureClass._properties()))
//                                        ._sourceSetImplementationId(pureClass._name())
//                                        ._targetSetImplementationId(pureClass._name());
//                                mongoDBSetImplementation._propertyMappingsAdd(currentPropertyMapping);
//                            }
//                        }

                        MutableList<EmbeddedSetImplementation> embeddedSetImplementations = org.eclipse.collections.impl.factory.Lists.mutable.empty();

                        return Tuples.pair(mongoDBSetImplementation, embeddedSetImplementations);
                    }
                    return null;
                }
        );
    }

//    private static Root_meta_pure_metamodel_function_property_Property_Impl getPropertyFromDomainClass(String propertyName, RichIterable<? extends Root_meta_pure_metamodel_function_property_Property_Impl> properties)
//    {
//        for (Root_meta_pure_metamodel_function_property_Property_Impl prop : properties)
//        {
//            if (prop._name().toString().equals(propertyName))
//            {
//                return prop;
//            }
//        }
//        return null;
//    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass generateMappingClass(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass, String id, RootMongoDBClassMapping mongoDBClassMapping, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping parent, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass mappingClass = new Root_meta_pure_mapping_MappingClass_Impl<>("");

        GenericType gType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                ._rawType(context.pureModel.getType("meta::pure::mapping::MappingClass"))
                ._typeArguments(org.eclipse.collections.impl.factory.Lists.mutable.with(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(mappingClass)));
        Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))
                ._specific(mappingClass)
                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(pureClass));

        mappingClass._name(pureClass._name() + "_" + parent._name() + "_" + id);
        mappingClass._classifierGenericType(gType);
        mappingClass._generalizations(org.eclipse.collections.impl.factory.Lists.mutable.with(g));

        return mappingClass;
    }
}
