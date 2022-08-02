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

package org.finos.legend.engine.external.shared.runtime;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.shared.utils.ExternalFormatRuntime;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.Constrained;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.EnforcementLevel;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtension;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.InputStreamResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.DataQualityExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.UrlStreamExecutionNode;
import org.finos.legend.engine.shared.core.url.UrlFactory;
import org.pac4j.core.profile.CommonProfile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExternalFormatExecutionExtension implements ExecutionExtension
{
    @Override
    public List<Function3<ExecutionNode, MutableList<CommonProfile>, ExecutionState, Result>> getExtraNodeExecutors()
    {
        return Collections.singletonList((executionNode, pm, executionState) ->
        {
            if (executionNode instanceof DataQualityExecutionNode)
            {
                return executeDataQuality((DataQualityExecutionNode) executionNode, pm, executionState);
            }
            else if (executionNode instanceof UrlStreamExecutionNode)
            {
                return executeUrlStream((UrlStreamExecutionNode) executionNode, pm, executionState);
            }
            else
            {
                return null;
            }
        });
    }

    private Result executeUrlStream(UrlStreamExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            InputStream inputStream = UrlFactory.create(node.url).openStream();
            return new InputStreamResult(inputStream);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Result executeDataQuality(DataQualityExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        ExecutionNode inputNode = node.executionNodes().getAny();
        Result input = inputNode.accept(new ExecutionNodeExecutor(profiles, executionState));
        StreamingObjectResult<?> streamingObjectResult = (StreamingObjectResult) input;

        Stream<IChecked<?>> checkedStream = (Stream<IChecked<?>>) streamingObjectResult.getObjectStream();
        Stream<IChecked<?>> withConstraints = node.enableConstraints
                ? checkedStream.map(this::applyConstraints)
                : checkedStream;
        if (node.checked)
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

    private Object extractValue(IChecked<?> checked)
    {
        if (checked.getDefects().stream().anyMatch(d -> d.getEnforcementLevel() != EnforcementLevel.Warn))
        {
            throw new IllegalStateException(checked.getDefects().stream().map(IDefect::getMessage).filter(Objects::nonNull).collect(Collectors.joining("\n")));
        }
        else if (checked.getValue() == null)
        {
            throw new IllegalStateException("Unexpected error: no object and no explanatory defects");
        }
        return checked.getValue();
    }
}
