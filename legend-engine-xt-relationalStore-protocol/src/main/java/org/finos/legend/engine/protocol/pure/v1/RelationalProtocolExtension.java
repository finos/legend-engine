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

import java.util.List;
import java.util.Map;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.RelationResultType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.CreateAndPopulateTempTableExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalBlockExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalClassInstantiationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalDataTypeInstantiationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalRelationDataInstantiationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalTdsInstantiationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalClassQueryTempTableGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalCrossRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalCrossRootQueryTempTableGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalPrimitiveQueryGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalRootQueryTempTableGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalTempTableGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.ResultType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.ApiTokenAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DefaultH2AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPWorkloadIdentityFederationAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierUserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.MapperPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatabricksDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.EmbeddedH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.RedshiftDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.EmbeddedRelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.InlineEmbeddedPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.OtherwiseEmbeddedRelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RootRelationalClassMapping;
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

public class RelationalProtocolExtension implements PureProtocolExtension
{
    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.fixedSize.with(
                // Packageable element
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(Database.class, "relational")
                        .build(),
                // Value specification
                ProtocolSubTypeInfo.newBuilder(ValueSpecification.class)
                        .withSubtype(DatabaseInstance.class, "databaseInstance")
                        .build(),
                // Class mapping
                ProtocolSubTypeInfo.newBuilder(ClassMapping.class)
                        .withSubtype(RootRelationalClassMapping.class, "relational")
                        .withSubtype(RelationalClassMapping.class, "embedded")
                        .build(),
                // Mapping Test InputData
                ProtocolSubTypeInfo.newBuilder(InputData.class)
                        .withSubtype(RelationalInputData.class, "relational")
                        .build(),
                // Association mapping
                ProtocolSubTypeInfo.newBuilder(AssociationMapping.class)
                        .withSubtype(RelationalAssociationMapping.class, "relational")
                        .build(),
                // Property mapping
                ProtocolSubTypeInfo.newBuilder(PropertyMapping.class)
                        .withSubtype(RelationalPropertyMapping.class, "relationalPropertyMapping")
                        .withSubtype(EmbeddedRelationalPropertyMapping.class, "embeddedPropertyMapping")
                        .withSubtype(InlineEmbeddedPropertyMapping.class, "inlineEmbeddedPropertyMapping")
                        .withSubtype(OtherwiseEmbeddedRelationalPropertyMapping.class, "otherwiseEmbeddedPropertyMapping")
                        .build(),
                // Connection
                ProtocolSubTypeInfo.newBuilder(Connection.class)
                        .withSubtype(RelationalDatabaseConnection.class, "RelationalDatabaseConnection")
                        .build(),
                // Execution context
                ProtocolSubTypeInfo.newBuilder(ExecutionContext.class)
                        .withSubtype(RelationalExecutionContext.class, "RelationalExecutionContext")
                        .build(),
                // Execution plan result type
                ProtocolSubTypeInfo.newBuilder(ResultType.class)
                        .withSubtype(RelationResultType.class, "relation")
                        .build(),
            // Embedded Data
            ProtocolSubTypeInfo.newBuilder(EmbeddedData.class)
                .withSubtype(RelationalCSVData.class, "relationalCSVData")
                .build(),
                // Execution plan node
                ProtocolSubTypeInfo.newBuilder(ExecutionNode.class)
                        .withSubtype(RelationalExecutionNode.class, "relational")
                        .withSubtype(RelationalTdsInstantiationExecutionNode.class, "relationalTdsInstantiation")
                        .withSubtype(RelationalClassInstantiationExecutionNode.class, "relationalClassInstantiation")
                        .withSubtype(RelationalRelationDataInstantiationExecutionNode.class, "relationalRelationDataInstantiation")
                        .withSubtype(RelationalDataTypeInstantiationExecutionNode.class, "relationalDataTypeInstantiation")
                        .withSubtype(RelationalRootGraphFetchExecutionNode.class, "relationalRootGraphFetchExecutionNode")
                        .withSubtype(RelationalCrossRootGraphFetchExecutionNode.class, "relationalCrossRootGraphFetchExecutionNode")
                        .withSubtype(RelationalTempTableGraphFetchExecutionNode.class, "relationalTempTableGraphFetchExecutionNode")
                        .withSubtype(RelationalGraphFetchExecutionNode.class, "relationalGraphFetchExecutionNode")
                        .withSubtype(RelationalBlockExecutionNode.class, "relationalBlock")
                        .withSubtype(CreateAndPopulateTempTableExecutionNode.class, "createAndPopulateTempTable")
                        .withSubtype(SQLExecutionNode.class, "sql")
                        .withSubtype(RelationalPrimitiveQueryGraphFetchExecutionNode.class, "relationalPrimitiveQueryGraphFetch")
                        .withSubtype(RelationalClassQueryTempTableGraphFetchExecutionNode.class, "relationalClassQueryTempTableGraphFetch")
                        .withSubtype(RelationalRootQueryTempTableGraphFetchExecutionNode.class, "relationalRootQueryTempTableGraphFetch")
                        .withSubtype(RelationalCrossRootQueryTempTableGraphFetchExecutionNode.class, "relationalCrossRootQueryTempTableGraphFetch")
                        .build(),

                //DatasourceSpecification
                ProtocolSubTypeInfo.newBuilder(DatasourceSpecification.class)
                        .withSubtype(LocalH2DatasourceSpecification.class, "h2Local")
                        .withSubtype(StaticDatasourceSpecification.class, "static")
                        .withSubtype(EmbeddedH2DatasourceSpecification.class, "h2Embedded")
                        .withSubtype(SnowflakeDatasourceSpecification.class, "snowflake")
                        .withSubtype(DatabricksDatasourceSpecification.class, "databricks")
                        .withSubtype(RedshiftDatasourceSpecification.class, "redshift")
                        .build(),

                // AuthenticationStrategy
                ProtocolSubTypeInfo.newBuilder(AuthenticationStrategy.class)
                        .withSubtype(DefaultH2AuthenticationStrategy.class, "h2Default")
                        .withSubtype(TestDatabaseAuthenticationStrategy.class, "test")
                        .withSubtype(DelegatedKerberosAuthenticationStrategy.class, "delegatedKerberos")
                        .withSubtype(UserNamePasswordAuthenticationStrategy.class, "userNamePassword")
                        .withSubtype(SnowflakePublicAuthenticationStrategy.class, "snowflakePublic")
                        .withSubtype(GCPApplicationDefaultCredentialsAuthenticationStrategy.class, "gcpApplicationDefaultCredentials")
                        .withSubtype(ApiTokenAuthenticationStrategy.class, "apiToken")
                        .withSubtype(GCPWorkloadIdentityFederationAuthenticationStrategy.class, "gcpWorkloadIdentityFederation")
                        .withSubtype(MiddleTierUserNamePasswordAuthenticationStrategy.class, "middleTierUserNamePassword")
                        .build(),

                //Post Processor
                ProtocolSubTypeInfo.newBuilder(PostProcessor.class)
                        .withSubtype(MapperPostProcessor.class, "mapper")
                        .build(),

                //Post Processor Parameter
                ProtocolSubTypeInfo.newBuilder(Milestoning.class)
                        .withSubtype(BusinessMilestoning.class, "businessMilestoning")
                        .withSubtype(BusinessSnapshotMilestoning.class, "businessSnapshotMilestoning")
                        .withSubtype(ProcessingMilestoning.class, "processingMilestoning")
                        .build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(Database.class, "meta::relational::metamodel::Database");
    }
}
