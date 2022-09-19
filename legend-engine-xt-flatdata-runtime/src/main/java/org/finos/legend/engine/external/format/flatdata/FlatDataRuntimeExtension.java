// Copyright 2022 Goldman Sachs
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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.flatdata.read.FlatDataReader;
import org.finos.legend.engine.external.format.flatdata.read.IFlatDataDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.flatdata.write.FlatDataWriter;
import org.finos.legend.engine.external.format.flatdata.write.IFlatDataSerializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.shared.runtime.ExternalFormatRuntimeExtension;
import org.finos.legend.engine.external.shared.runtime.write.ExternalFormatSerializeResult;
import org.finos.legend.engine.external.shared.utils.ExternalFormatRuntime;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatInternalizeExecutionNode;
import org.pac4j.core.profile.CommonProfile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class FlatDataRuntimeExtension implements ExternalFormatRuntimeExtension
{
    private static long DEFAULT_MAX_SCHEMA_OBJECT_SIZE = 50 * 1024 * 1024;
    private static final String CONTENT_TYPE = "application/x.flatdata";

    @Override
    public List<String> getContentTypes()
    {
        return Collections.singletonList(CONTENT_TYPE);
    }

    @Override
    public StreamingObjectResult<?> executeInternalizeExecutionNode(ExternalFormatInternalizeExecutionNode node, InputStream inputStream, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            String specificsClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
            Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, specificsClassName, executionState, profiles);
            IFlatDataDeserializeExecutionNodeSpecifics<?> specifics = (IFlatDataDeserializeExecutionNodeSpecifics<?>) specificsClass.getConstructor().newInstance();
            // TODO Allow size to vary when run from jar
            specifics.setMaximumSchemaObjectSize(DEFAULT_MAX_SCHEMA_OBJECT_SIZE);

            FlatDataContext<?> context = specifics.createContext();
            FlatDataReader<?> deserializer = new FlatDataReader<>(context, inputStream);
            return new StreamingObjectResult<>(deserializer.startStream());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result executeExternalizeExecutionNode(ExternalFormatExternalizeExecutionNode node, Result result, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            Stream inputStream = node.checked
                    ? ExternalFormatRuntime.unwrapCheckedStream(((StreamingObjectResult) result).getObjectStream())
                    : ((StreamingObjectResult) result).getObjectStream();

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
}
