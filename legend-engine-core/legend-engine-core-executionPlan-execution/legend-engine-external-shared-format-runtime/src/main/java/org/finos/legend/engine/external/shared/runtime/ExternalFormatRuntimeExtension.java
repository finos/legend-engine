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

package org.finos.legend.engine.external.shared.runtime;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeTDSExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatInternalizeExecutionNode;
import org.finos.legend.engine.shared.core.extension.LegendExtension;
import org.finos.legend.engine.shared.core.extension.LegendExternalFormatExtension;
import org.pac4j.core.profile.CommonProfile;

import java.io.InputStream;
import java.util.List;

public interface ExternalFormatRuntimeExtension extends LegendExternalFormatExtension
{
    @Override
    default String type()
    {
        return "Runtime";
    }

    @Override
    default MutableList<String> typeGroup()
    {
        return Lists.mutable.with("Plan", "Execution");
    }

    List<String> getContentTypes();

    default StreamingObjectResult<?> executeInternalizeExecutionNode(ExternalFormatInternalizeExecutionNode node, InputStream inputStream, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        throw new UnsupportedOperationException("Internalize not supported by format - " + node.contentType);
    }

    default Result executeExternalizeExecutionNode(ExternalFormatExternalizeExecutionNode node, Result result, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        throw new UnsupportedOperationException("Externalize not supported by format - " + node.contentType);
    }

    default Result executeExternalizeTDSExecutionNode(ExternalFormatExternalizeTDSExecutionNode node, Result result, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        throw new UnsupportedOperationException("Externalize TDS not supported by format - " + node.contentType);
    }
}
