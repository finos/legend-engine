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

package org.finos.legend.engine.plan.execution.stores.mongodb;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBDocumentInternalizeExecutionNode;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.pac4j.core.profile.CommonProfile;

import java.util.Collections;
import java.util.List;

public class MongoDBStoreExecutionExtension implements IMongoDBStoreExecutionExtension
{
    public List<Function3<ExecutionNode, MutableList<CommonProfile>, ExecutionState, Result>> getExtraNodeExecutors()
    {
        return Collections.singletonList(((executionNode, profiles, executionState) ->
        {
            if (executionNode instanceof MongoDBExecutionNode)
            {
                return executionNode.accept(executionState.getStoreExecutionState(StoreType.NonRelational_MongoDB).getVisitor(profiles, executionState));
            }
            else if (executionNode instanceof MongoDBDocumentInternalizeExecutionNode)
            {
                return executionNode.accept(executionState.getStoreExecutionState(StoreType.NonRelational_MongoDB).getVisitor(profiles, executionState));
                //return executeDocumentInternalizeExecutionNode((MongoDBDocumentInternalizeExecutionNode) executionNode, profiles, executionState);
            }
            return null;
        }));
    }

    /*
    private Result executeDocumentInternalizeExecutionNode(MongoDBDocumentInternalizeExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        InputStream stream = ExecutionHelper.inputStreamFromResult(node.executionNodes().getFirst().accept(new ExecutionNodeExecutor(profiles, new ExecutionState(executionState))));
        StreamingObjectResult<?> streamingObjectResult = executeInternalizeExecutionNode(node, stream, profiles, executionState);
        return applyConstraints(streamingObjectResult, node.checked, node.enableConstraints);
    }


    private StreamingObjectResult<?> executeInternalizeExecutionNode(MongoDBDocumentInternalizeExecutionNode node, InputStream inputStream, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            String specificsClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
            //We don't need specifics class for reader - as we know the object.
            Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, specificsClassName, executionState, profiles);
            IJsonDeserializeExecutionNodeSpecifics specifics = (IJsonDeserializeExecutionNodeSpecifics) specificsClass.getConstructor().newInstance();

            // checked made true and enableConstraints made false as these are incorporated in ExternalFormatRuntime centrally
            StoreStreamReadingObjectsIterator<?> storeObjectsIterator = StoreStreamReadingObjectsIterator.newObjectsIterator(specifics.streamReader(inputStream), false, true);

            Stream<?> objectStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(storeObjectsIterator, Spliterator.ORDERED), false);
            return new StreamingObjectResult<>(objectStream);
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

     */
}
