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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.json.read.IJsonDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.json.read.IJsonInternalizeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.json.write.IJsonExternalizeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.json.write.JsonDataWriter;
import org.finos.legend.engine.external.shared.runtime.ExternalFormatRuntimeExtension;
import org.finos.legend.engine.external.shared.runtime.write.ExternalFormatSerializeResult;
import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeSerializerHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.DefaultExecutionNodeContext;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.StoreStreamReadingObjectsIterator;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatInternalizeExecutionNode;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonSchemaRuntimeExtension implements ExternalFormatRuntimeExtension
{
    private static final String CONTENT_TYPE = "application/json";

    @Override
    public List<String> getContentTypes()
    {
        return Collections.singletonList(CONTENT_TYPE);
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("External_Format", "JSON");
    }

    @Override
    public StreamingObjectResult<?> executeInternalizeExecutionNode(ExternalFormatInternalizeExecutionNode node, InputStream inputStream, Identity identity, ExecutionState executionState)
    {
        try
        {
            String specificsClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
            Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, specificsClassName, executionState, identity);

            Stream<?> objectStream;
            if (Arrays.asList(specificsClass.getInterfaces()).contains(IJsonInternalizeExecutionNodeSpecifics.class))
            {
                IJsonInternalizeExecutionNodeSpecifics specifics = (IJsonInternalizeExecutionNodeSpecifics) specificsClass.getConstructor().newInstance();
                objectStream = specifics.createReader(inputStream).startStream();
            }
            else
            {
                // Deprecated Flow to handle old plans
                IJsonDeserializeExecutionNodeSpecifics specifics = (IJsonDeserializeExecutionNodeSpecifics) specificsClass.getConstructor().newInstance();
                // checked made true and enableConstraints made false as these are incorporated in ExternalFormatRuntime centrally
                StoreStreamReadingObjectsIterator<?> storeObjectsIterator = StoreStreamReadingObjectsIterator.newObjectsIterator(specifics.streamReader(inputStream), false, true);
                objectStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(storeObjectsIterator, Spliterator.ORDERED), false);
            }
            return new StreamingObjectResult<>(objectStream);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result executeExternalizeExecutionNode(ExternalFormatExternalizeExecutionNode node, Result result, Identity identity, ExecutionState executionState)
    {
        try
        {
            if (!(node.implementation instanceof JavaPlatformImplementation))
            {
                throw new RuntimeException("Only Java implementations are currently supported, found: " + node.implementation);
            }

            String executionClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
            Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, executionClassName, executionState, identity);

            IExecutionNodeContext context = new DefaultExecutionNodeContext(executionState, result);
            if (Arrays.asList(specificsClass.getInterfaces()).contains(IJsonExternalizeExecutionNodeSpecifics.class))
            {
                IJsonExternalizeExecutionNodeSpecifics nodeSpecifics = (IJsonExternalizeExecutionNodeSpecifics) specificsClass.getConstructor().newInstance();
                JsonDataWriter<?> jsonDataWriter = new JsonDataWriter<>(nodeSpecifics, extractStreamFromResult(result), context);
                return new ExternalFormatSerializeResult(jsonDataWriter, result, CONTENT_TYPE);
            }
            else
            {
                // Deprecated Flow to handle old plans
                org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics nodeSpecifics = (org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics) specificsClass.newInstance();
                return ExecutionNodeSerializerHelper.executeSerialize(nodeSpecifics, null, result, context);
            }
        }
        catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Stream<?> extractStreamFromResult(Result result)
    {
        if (result instanceof ConstantResult)
        {
            Object value = ((ConstantResult) result).getValue();
            if (value instanceof List)
            {
                value = ((List<?>) value).stream();
            }
            if (!(value instanceof Stream))
            {
                value = Stream.of(value);
            }
            return (Stream<?>) value;
        }
        else if (result instanceof StreamingObjectResult)
        {
            return ((StreamingObjectResult<?>) result).getObjectStream();
        }
        else
        {
            throw new IllegalArgumentException("Unexpected result: " + result.getClass().getName());
        }
    }
}
