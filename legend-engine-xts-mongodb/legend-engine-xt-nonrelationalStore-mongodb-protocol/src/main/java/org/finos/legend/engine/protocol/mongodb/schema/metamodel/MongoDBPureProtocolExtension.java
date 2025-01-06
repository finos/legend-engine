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

package org.finos.legend.engine.protocol.mongodb.schema.metamodel;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBDocumentInternalizeExecutionNode;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBExecutionNode;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.RootMongoDBClassMapping;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;

import java.util.List;
import java.util.Map;

public class MongoDBPureProtocolExtension implements PureProtocolExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Mongo");
    }

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.fixedSize.with(
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(MongoDatabase.class, "MongoDatabase")
                        .build(),
                // Connection
                ProtocolSubTypeInfo.newBuilder(Connection.class)
                        .withSubtype(MongoDBConnection.class, "MongoDBConnection")
                        .build(),
                // Execution Nodes
                ProtocolSubTypeInfo.newBuilder(ExecutionNode.class)
                        .withSubtype(MongoDBExecutionNode.class, "MongoDBExecutionNode")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(ExecutionNode.class)
                        .withSubtype(MongoDBDocumentInternalizeExecutionNode.class, "MongoDBDocumentInternalizeExecutionNode")
                        .build(),
                // Class mapping
                ProtocolSubTypeInfo.newBuilder(ClassMapping.class)
                        .withSubtype(RootMongoDBClassMapping.class, "MongoDB")
                        .build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(MongoDatabase.class, "meta::external::store::mongodb::metamodel::pure::MongoDatabase");
    }
}


