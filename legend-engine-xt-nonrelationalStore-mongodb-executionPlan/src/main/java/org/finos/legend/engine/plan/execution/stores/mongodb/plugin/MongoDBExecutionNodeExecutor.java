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

package org.finos.legend.engine.plan.execution.stores.mongodb.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.authentication.credentialprovider.CredentialBuilder;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.external.shared.utils.ExternalFormatRuntime;
import org.finos.legend.engine.language.pure.grammar.to.MongoDBQueryJsonComposer;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.Constrained;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.EnforcementLevel;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.StoreStreamReadingObjectsIterator;
import org.finos.legend.engine.plan.execution.stores.mongodb.MongoDBExecutor;
import org.finos.legend.engine.plan.execution.stores.mongodb.result.MongoDBResult;
import org.finos.legend.engine.plan.execution.stores.mongodb.specifics.IMongoDocumentDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBDocumentInternalizeExecutionNode;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.pac4j.core.profile.CommonProfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MongoDBExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    private final MutableList<CommonProfile> profiles;
    private ExecutionState executionState;

    public MongoDBExecutionNodeExecutor(MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        this.profiles = profiles;
        this.executionState = executionState;
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        if (executionNode instanceof MongoDBExecutionNode)
        {
            try (Scope scope = GlobalTracer.get().buildSpan("MongoDB Store Execution").startActive(true))
            {
                return executeMongoDBExecutionNode((MongoDBExecutionNode) executionNode);
            }
        }
        if (executionNode instanceof MongoDBDocumentInternalizeExecutionNode)
        {
            try (Scope scope = GlobalTracer.get().buildSpan("MongoDB Document Internalize Execution").startActive(true))
            {
                return executeDocumentInternalizeExecutionNode((MongoDBDocumentInternalizeExecutionNode) executionNode, this.profiles, this.executionState);
            }
        }
        else
        {
            return null;
        }
    }

    public Result executeMongoDBExecutionNode(MongoDBExecutionNode mongoDBExecutionNode)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("MongoDB Store Execution").startActive(true))
        {
            scope.span().setTag("databaseCommand", (mongoDBExecutionNode).databaseCommand.toString());
            MongoDBConnection mongoDBConnection = mongoDBExecutionNode.connection;
            String databaseCommand = mongoDBExecutionNode.databaseCommand;

            ObjectMapper objectMapper = new ObjectMapper();
            DatabaseCommand dbCommand = objectMapper.readValue(databaseCommand, DatabaseCommand.class);
            MongoDBQueryJsonComposer mongoDBQueryJsonComposer = new MongoDBQueryJsonComposer(false);
            String composedDbCommand = mongoDBQueryJsonComposer.parseDatabaseCommand(dbCommand);

            CredentialProviderProvider credentialProviderProvider = this.executionState.getCredentialProviderProvider();
            Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);

            return new MongoDBExecutor(credentialProviderProvider).executeMongoDBQuery(composedDbCommand, mongoDBConnection, identity);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to parse databaseCommand from Mongo executionNode", e);
        }
    }

    private Result executeDocumentInternalizeExecutionNode(MongoDBDocumentInternalizeExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {

        MongoDBResult resultCursor = getResultCursor(node.executionNodes().getFirst().accept(executionState.getStoreExecutionState(StoreType.NonRelational_MongoDB).getVisitor(profiles, executionState)));
        StreamingObjectResult<?> streamingObjectResult = executeInternalizeExecutionNode(node, resultCursor, profiles, executionState);
        return applyConstraints(streamingObjectResult, node.checked, node.enableConstraints);
    }

    private MongoDBResult getResultCursor(Result mongoResult)
    {
        if (mongoResult instanceof MongoDBResult)
        {
            return (MongoDBResult) mongoResult;
        }
        throw new EngineException(
                String.format("MongoDBExecutionNode should return MongoDBResult, but instead got:%s", mongoResult.getClass().getName()),
                ExceptionCategory.INTERNAL_SERVER_ERROR);
    }


    private StreamingObjectResult<?> executeInternalizeExecutionNode(MongoDBDocumentInternalizeExecutionNode node, MongoDBResult mongoDBResult, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            String specificsClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
            //We don't need specifics class for reader - as we know the object.
            Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, specificsClassName, executionState, profiles);
            IMongoDocumentDeserializeExecutionNodeSpecifics specifics = (IMongoDocumentDeserializeExecutionNodeSpecifics) specificsClass.getConstructor().newInstance();

            // checked made true and enableConstraints made false as these are incorporated in ExternalFormatRuntime centrally
            StoreStreamReadingObjectsIterator<?> storeObjectsIterator = StoreStreamReadingObjectsIterator.newObjectsIterator(specifics.streamReader(mongoDBResult), false, true);

            Stream<?> objectStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(storeObjectsIterator, Spliterator.ORDERED), false);
            return new StreamingObjectResult<>(objectStream, mongoDBResult.getResultBuilder(), mongoDBResult);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private Result applyConstraints(StreamingObjectResult<?> streamingObjectResult, boolean checked, boolean enableConstraints)
    {
        Stream<IChecked<?>> checkedStream = (Stream<IChecked<?>>) streamingObjectResult.getObjectStream();
        Stream<IChecked<?>> withConstraints = enableConstraints
                ? checkedStream.map(this::applyConstraints)
                : checkedStream;
        if (checked)
        {
            return new StreamingObjectResult<>(withConstraints, streamingObjectResult.getResultBuilder(), streamingObjectResult);
        }
        else
        {
            Stream<?> objectStream = ExternalFormatRuntime.unwrapCheckedStream(withConstraints);
            return new StreamingObjectResult<>(objectStream, streamingObjectResult.getResultBuilder(), streamingObjectResult);
        }
    }

    private IChecked<?> applyConstraints(IChecked<?> checked)
    {
        Object value = checked.getValue();
        List<IDefect> constraintFailures = Collections.emptyList();
        if (value instanceof Constrained)
        {
            constraintFailures = ((Constrained) value).allConstraints();
        }
        if (constraintFailures.isEmpty())
        {
            return checked;
        }
        else
        {
            List<IDefect> allDefects = new ArrayList(checked.getDefects());
            allDefects.addAll(constraintFailures);
            return allDefects.stream().anyMatch(d -> d.getEnforcementLevel() == EnforcementLevel.Critical)
                    ? BasicChecked.newChecked(null, checked.getSource(), allDefects)
                    : BasicChecked.newChecked(checked.getValue(), checked.getSource(), allDefects);
        }
    }


}
