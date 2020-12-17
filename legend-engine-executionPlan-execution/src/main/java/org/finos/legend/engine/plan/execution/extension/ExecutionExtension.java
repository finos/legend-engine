// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.extension;

import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.pac4j.core.profile.ProfileManager;

import java.util.Collections;
import java.util.List;

public interface ExecutionExtension
{
    default List<Function3<ExecutionNode, ProfileManager<?>, ExecutionState, Result>> getExtraNodeExecutors()
    {
        return Collections.emptyList();
    }

    default List<Function3<ExecutionNode, ProfileManager<?>, ExecutionState, Result>> getExtraSequenceNodeExecutors()
    {
        return Collections.emptyList();
    }
}
