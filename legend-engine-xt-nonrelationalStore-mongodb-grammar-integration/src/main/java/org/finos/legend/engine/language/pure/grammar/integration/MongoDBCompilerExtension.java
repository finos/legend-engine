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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.grammar.integration.extensions.IMongoDBStoreCompilerExtension;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Connection;

import java.util.Collections;
import java.util.List;

public class MongoDBCompilerExtension implements IMongoDBStoreCompilerExtension
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        Processor<MongoDatabase> processor = Processor.newProcessor(MongoDatabase.class,
                Collections.singletonList(Binding.class),
                (MongoDatabase mongoDBStore, CompileContext context) ->
                {
                    Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase pureMongoDatabase = new Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase_Impl("id");
                    return pureMongoDatabase;
                },
                // Second pass - resolve binding and model elements
                (MongoDatabase mongoDBStore, CompileContext context) ->
                {
                    Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase pureMongoDatabase = new Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase_Impl(mongoDBStore.name);
                    LOGGER.info("processing Second pass");
                });
        return Lists.immutable.with(processor);
    }

    @Override
    public List<Function2<Connection, CompileContext, Root_meta_pure_runtime_Connection>> getExtraConnectionValueProcessors()
    {
        return Lists.mutable.empty();
    }
}
