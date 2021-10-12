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

package org.finos.legend.engine.external.shared.runtime.read;

import org.finos.legend.engine.plan.execution.result.InputStreamResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared.UrlStreamExecutionNode;

import java.io.InputStream;

public class ExecutionHelper
{
    public static InputStream inputStreamFromResult(Result result)
    {
        if (result instanceof InputStreamResult)
        {
            return ((InputStreamResult) result).getInputStream();
        }
        else
        {
            throw new IllegalStateException("Unsupported result type for external formats: " + result.getClass().getSimpleName());
        }
    }

    public static String locationFromSourceNode(ExecutionNode executionNode)
    {
        if (executionNode instanceof UrlStreamExecutionNode)
        {
            return ((UrlStreamExecutionNode) executionNode).url;
        }
        else
        {
            throw new IllegalStateException("Unsupported child node type for external formats: " + executionNode.getClass().getSimpleName());
        }
    }
}
