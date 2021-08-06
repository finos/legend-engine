// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.flatdata.read.FlatDataReader;
import org.finos.legend.engine.external.format.flatdata.read.IFlatDataDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.flatdata.write.FlatDataWriter;
import org.finos.legend.engine.external.format.flatdata.write.IFlatDataSerializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.shared.runtime.read.ExecutionHelper;
import org.finos.legend.engine.external.shared.runtime.write.ExternalFormatSerializeResult;
import org.finos.legend.engine.external.shared.utils.ExternalFormatRuntime;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtension;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.format.flatdata.FlatDataDeserializeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.format.flatdata.FlatDataSerializeExecutionNode;
import org.pac4j.core.profile.CommonProfile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class FlatDataExecutionExtension implements ExecutionExtension
{
    @Override
    public List<Function3<ExecutionNode, MutableList<CommonProfile>, ExecutionState, Result>> getExtraNodeExecutors()
    {
        return Collections.singletonList((executionNode, pm, executionState) ->
                                         {
                                             if (executionNode instanceof FlatDataSerializeExecutionNode)
                                             {
                                                 return executeSerialize((FlatDataSerializeExecutionNode) executionNode, pm, executionState);
                                             }
                                             else if (executionNode instanceof FlatDataDeserializeExecutionNode)
                                             {
                                                 return executeDeserialize((FlatDataDeserializeExecutionNode) executionNode, pm, executionState);
                                             }
                                             else
                                             {
                                                 return null;
                                             }
                                         });
    }

    private Result executeSerialize(FlatDataSerializeExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            ExecutionNode inputNode = node.executionNodes().getAny();
            Result input = inputNode.accept(new ExecutionNodeExecutor(profiles, executionState));
            Stream inputStream = node.checked
                    ? ExternalFormatRuntime.unwrapCheckedStream(((StreamingObjectResult) input).getObjectStream())
                    : ((StreamingObjectResult) input).getObjectStream();

            String specificsClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
            Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, specificsClassName, executionState, profiles);
            IFlatDataSerializeExecutionNodeSpecifics<?> specifics = (IFlatDataSerializeExecutionNodeSpecifics<?>) specificsClass.getConstructor().newInstance();
            FlatDataContext context = specifics.createContext();
            FlatDataWriter serializer = new FlatDataWriter(context, inputStream);
            return new ExternalFormatSerializeResult(serializer);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private Result executeDeserialize(FlatDataDeserializeExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            String specificsClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
            Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, specificsClassName, executionState, profiles);
            IFlatDataDeserializeExecutionNodeSpecifics<?> specifics = (IFlatDataDeserializeExecutionNodeSpecifics<?>) specificsClass.getConstructor().newInstance();
            FlatDataContext<?> context = specifics.createContext();

            InputStream stream = ExecutionHelper.inputStreamFromConnection(node.connection);
            FlatDataReader<?> deserializer = new FlatDataReader<>(context, stream);
            return new StreamingObjectResult<>(deserializer.startStream());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

}
