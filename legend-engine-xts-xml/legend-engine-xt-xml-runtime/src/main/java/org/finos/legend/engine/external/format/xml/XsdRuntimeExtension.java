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

package org.finos.legend.engine.external.format.xml;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.xml.read.IXmlDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.xml.read.XmlReader;
import org.finos.legend.engine.external.shared.runtime.ExternalFormatRuntimeExtension;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatInternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.UrlStreamExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.VariableResolutionExecutionNode;
import org.pac4j.core.profile.CommonProfile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class XsdRuntimeExtension implements ExternalFormatRuntimeExtension
{
    private static final String CONTENT_TYPE = "application/xml";

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
            IXmlDeserializeExecutionNodeSpecifics specifics = (IXmlDeserializeExecutionNodeSpecifics) specificsClass.getConstructor().newInstance();

            String location = idFromSourceNode(node.executionNodes().getFirst());
            XmlReader<?> deserializer = new XmlReader<>(specifics, inputStream, location);
            return new StreamingObjectResult<>(deserializer.startStream());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private String idFromSourceNode(ExecutionNode executionNode)
    {
        if (executionNode instanceof UrlStreamExecutionNode)
        {
            return ((UrlStreamExecutionNode) executionNode).url;
        }
        else if (executionNode instanceof VariableResolutionExecutionNode)
        {
            return ((VariableResolutionExecutionNode) executionNode).varName;
        }
        else
        {
            return "unknown";
        }
    }
}
