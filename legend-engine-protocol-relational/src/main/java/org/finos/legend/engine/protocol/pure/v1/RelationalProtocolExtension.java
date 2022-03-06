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

package org.finos.legend.engine.protocol.pure.v1;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.RelationResultType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.*;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.*;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.ResultType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.MapperPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessSnapshotMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.ProcessingMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.DatabaseInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.executionContext.RelationalExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;

import java.util.List;
import java.util.Map;

public class RelationalProtocolExtension implements PureProtocolExtension
{
    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.mutable.with(() -> Lists.mutable.with(
                // Packageable element
                ProtocolSubTypeInfo.Builder
                        .newInstance(PackageableElement.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(Database.class, "relational")
                        )).build(),
                // Value specification
                ProtocolSubTypeInfo.Builder
                        .newInstance(ValueSpecification.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(DatabaseInstance.class, "databaseInstance")
                        )).build(),
                // Class mapping
                ProtocolSubTypeInfo.Builder
                        .newInstance(ClassMapping.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(RootRelationalClassMapping.class, "relational"),
                                Tuples.pair(RelationalClassMapping.class, "embedded")
                        )).build(),
                // Mapping Test InputData
                ProtocolSubTypeInfo.Builder
                        .newInstance(InputData.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(RelationalInputData.class, "relational")
                        )).build(),
                // Association mapping
                ProtocolSubTypeInfo.Builder
                        .newInstance(AssociationMapping.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(RelationalAssociationMapping.class, "relational")
                        )).build(),
                // Property mapping
                ProtocolSubTypeInfo.Builder
                        .newInstance(PropertyMapping.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(RelationalPropertyMapping.class, "relationalPropertyMapping"),
                                Tuples.pair(EmbeddedRelationalPropertyMapping.class, "embeddedPropertyMapping"),
                                Tuples.pair(InlineEmbeddedPropertyMapping.class, "inlineEmbeddedPropertyMapping"),
                                Tuples.pair(OtherwiseEmbeddedRelationalPropertyMapping.class, "otherwiseEmbeddedPropertyMapping")
                        )).build(),
                // Connection
                ProtocolSubTypeInfo.Builder
                        .newInstance(Connection.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(RelationalDatabaseConnection.class, "RelationalDatabaseConnection")
                        )).build(),
                // Execution context
                ProtocolSubTypeInfo.Builder
                        .newInstance(ExecutionContext.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(RelationalExecutionContext.class, "RelationalExecutionContext")
                        )).build(),
                // Execution plan result type
                ProtocolSubTypeInfo.Builder
                        .newInstance(ResultType.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(RelationResultType.class, "relation")
                        )).build(),
                // Execution plan node
                ProtocolSubTypeInfo.Builder
                        .newInstance(ExecutionNode.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(RelationalExecutionNode.class, "relational"),
                                Tuples.pair(RelationalTdsInstantiationExecutionNode.class, "relationalTdsInstantiation"),
                                Tuples.pair(RelationalClassInstantiationExecutionNode.class, "relationalClassInstantiation"),
                                Tuples.pair(RelationalRelationDataInstantiationExecutionNode.class, "relationalRelationDataInstantiation"),
                                Tuples.pair(RelationalDataTypeInstantiationExecutionNode.class, "relationalDataTypeInstantiation"),
                                Tuples.pair(RelationalRootGraphFetchExecutionNode.class, "relationalRootGraphFetchExecutionNode"),
                                Tuples.pair(RelationalCrossRootGraphFetchExecutionNode.class, "relationalCrossRootGraphFetchExecutionNode"),
                                Tuples.pair(RelationalTempTableGraphFetchExecutionNode.class, "relationalTempTableGraphFetchExecutionNode"),
                                Tuples.pair(RelationalGraphFetchExecutionNode.class, "relationalGraphFetchExecutionNode"),
                                Tuples.pair(RelationalBlockExecutionNode.class, "relationalBlock"),
                                Tuples.pair(CreateAndPopulateTempTableExecutionNode.class, "createAndPopulateTempTable"),
                                Tuples.pair(SQLExecutionNode.class, "sql"),
                                Tuples.pair(RelationalPrimitiveQueryGraphFetchExecutionNode.class, "relationalPrimitiveQueryGraphFetch"),
                                Tuples.pair(RelationalClassQueryTempTableGraphFetchExecutionNode.class, "relationalClassQueryTempTableGraphFetch"),
                                Tuples.pair(RelationalRootQueryTempTableGraphFetchExecutionNode.class, "relationalRootQueryTempTableGraphFetch"),
                                Tuples.pair(RelationalCrossRootQueryTempTableGraphFetchExecutionNode.class, "relationalCrossRootQueryTempTableGraphFetch")
                        )).build(),

                //DatasourceSpecification
                ProtocolSubTypeInfo.Builder
                        .newInstance(DatasourceSpecification.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(LocalH2DatasourceSpecification.class, "h2Local"),
                                Tuples.pair(StaticDatasourceSpecification.class, "static"),
                                Tuples.pair(EmbeddedH2DatasourceSpecification.class, "h2Embedded"),
                                Tuples.pair(SnowflakeDatasourceSpecification.class, "snowflake"),
                                Tuples.pair(BigQueryDatasourceSpecification.class, "bigQuery"),
                                Tuples.pair(DatabricksDatasourceSpecification.class, "databricks")
                        )).build(),

                // AuthenticationStrategy
                ProtocolSubTypeInfo.Builder
                        .newInstance(AuthenticationStrategy.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(DefaultH2AuthenticationStrategy.class, "h2Default"),
                                Tuples.pair(TestDatabaseAuthenticationStrategy.class, "test"),
                                Tuples.pair(DelegatedKerberosAuthenticationStrategy.class, "delegatedKerberos"),
                                Tuples.pair(UserNamePasswordAuthenticationStrategy.class, "userNamePassword"),
                                Tuples.pair(SnowflakePublicAuthenticationStrategy.class, "snowflakePublic"),
                                Tuples.pair(GCPApplicationDefaultCredentialsAuthenticationStrategy.class, "gcpApplicationDefaultCredentials"),
                                Tuples.pair(ApiTokenAuthenticationStrategy.class, "apiToken")
                                )).build(),

                //Post Processor
                ProtocolSubTypeInfo.Builder
                        .newInstance(PostProcessor.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(MapperPostProcessor.class, "mapper")
                        )).build(),

                //Post Processor Parameter
                ProtocolSubTypeInfo.Builder
                        .newInstance(Milestoning.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(BusinessMilestoning.class, "businessMilestoning"),
                                Tuples.pair(BusinessSnapshotMilestoning.class, "businessSnapshotMilestoning"),
                                Tuples.pair(ProcessingMilestoning.class, "processingMilestoning")
                        )).build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(Database.class, "meta::relational::metamodel::Database");
    }

}
