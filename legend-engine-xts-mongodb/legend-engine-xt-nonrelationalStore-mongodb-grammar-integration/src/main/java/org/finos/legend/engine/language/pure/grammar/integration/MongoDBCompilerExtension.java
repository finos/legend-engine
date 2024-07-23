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
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperExternalFormat;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.grammar.integration.extensions.IMongoDBStoreCompilerExtension;
import org.finos.legend.engine.language.pure.grammar.integration.util.MongoDBCompilerHelper;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.RootMongoDBClassMapping;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MongoDBCompilerExtension implements IMongoDBStoreCompilerExtension
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MongoDBCompilerExtension.class);

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Mongo");
    }

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
    public List<Function2<Connection, CompileContext, Root_meta_core_runtime_Connection>> getExtraConnectionValueProcessors()
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
                        Set<Class<?>> processedClasses = new HashSet<>();

                        Root_meta_external_format_shared_binding_Binding binding = ((Root_meta_external_format_shared_binding_Binding)context.pureModel.getPackageableElement(((RootMongoDBClassMapping) cm).bindingPath, cm.sourceInformation));
                        MutableList<EmbeddedSetImplementation> embeddedSetImplementations = org.eclipse.collections.impl.factory.Lists.mutable.empty();
                        Root_meta_external_store_mongodb_metamodel_pure_MongoDBSetImplementation mongoDBSetImplementation = MongoDBCompilerHelper.createMongoDBSetImplementation(classMapping, context, parentMapping, cm, binding);

                        ExternalFormatExtension schemaExtension = HelperExternalFormat.getExternalFormatExtension(binding, context);
                        Root_meta_external_format_shared_binding_validation_BindingDetail bindingDetail = schemaExtension.bindDetails(binding, context);

                        if (bindingDetail instanceof Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail)
                        {
                            List<PropertyMapping> propertyMappings = MongoDBCompilerHelper.generatePropertyMappings((Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail) bindingDetail, mongoDBSetImplementation._class(), mongoDBSetImplementation._id(), embeddedSetImplementations, mongoDBSetImplementation, classMapping.sourceInformation, processedClasses, context, parentMapping);
                            mongoDBSetImplementation._propertyMappings(Lists.mutable.ofAll(propertyMappings).toImmutable());
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

    @Override
    public List<Procedure<Procedure2<String, List<String>>>> getExtraElementForPathToElementRegisters()
    {
        return Collections.singletonList(registerElementForPathToElement ->
        {
            ImmutableList<String> versions = PureClientVersions.versionsSinceExclusive("v1_31_0");
            versions.forEach(v -> registerElementForPathToElement.value(
                            "meta::protocols::pure::" + v + "::extension::store::mongodb",
                            Collections.singletonList("getMongoDBStoreExtension_String_1__SerializerExtension_1_")
                    )
            );
        });
    }
}
