//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.json.read.IJsonDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.shared.runtime.read.ExecutionHelper;
import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtension;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeSerializerHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.DefaultExecutionNodeContext;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.StoreStreamReadingObjectsIterator;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.format.json.JsonDeserializeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.format.json.JsonSerializeExecutionNode;
import org.pac4j.core.profile.CommonProfile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Deprecated
public class JsonExecutionExtension implements ExecutionExtension
{
    @Override
    public List<Function3<ExecutionNode, MutableList<CommonProfile>, ExecutionState, Result>> getExtraNodeExecutors()
    {
        return Collections.singletonList((executionNode, pm, executionState) ->
        {
            if (executionNode instanceof JsonSerializeExecutionNode)
            {
                return executeSerialize((JsonSerializeExecutionNode) executionNode, pm, executionState);
            }
            else if (executionNode instanceof JsonDeserializeExecutionNode)
            {
                return executeDeserialize((JsonDeserializeExecutionNode) executionNode, pm, executionState);
            }
            else
            {
                return null;
            }
        });
    }

    private Result executeSerialize(JsonSerializeExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            if (!(node.implementation instanceof JavaPlatformImplementation))
            {
                throw new RuntimeException("Only Java implementations are currently supported, found: " + node.implementation);
            }

            JavaPlatformImplementation javaPlatformImpl = (JavaPlatformImplementation) node.implementation;
            String executionClassName = JavaHelper.getExecutionClassFullName(javaPlatformImpl);
            Class<?> clazz = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, executionClassName, executionState, profiles);

            org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics nodeSpecifics = (org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics) clazz.newInstance();
            Result childResult = node.executionNodes().getFirst().accept(new ExecutionNodeExecutor(profiles, executionState));
            IExecutionNodeContext context = new DefaultExecutionNodeContext(executionState, childResult);

            return ExecutionNodeSerializerHelper.executeSerialize(nodeSpecifics, null, childResult, context);
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Result executeDeserialize(JsonDeserializeExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            String specificsClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
            Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, specificsClassName, executionState, profiles);
            IJsonDeserializeExecutionNodeSpecifics specifics = (IJsonDeserializeExecutionNodeSpecifics) specificsClass.getConstructor().newInstance();

            InputStream stream = ExecutionHelper.inputStreamFromResult(node.executionNodes().getFirst().accept(new ExecutionNodeExecutor(profiles, new ExecutionState(executionState))));
            StoreStreamReadingObjectsIterator<?> storeObjectsIterator = StoreStreamReadingObjectsIterator.newObjectsIterator(specifics.streamReader(stream), false, true);

            Stream<?> objectStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(storeObjectsIterator, Spliterator.ORDERED), false);
            return new StreamingObjectResult<>(objectStream);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

}
