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

package org.finos.legend.engine.plan.execution.stores;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.pac4j.core.profile.CommonProfile;
import java.util.Map;

public interface StoreExecutionState
{
    StoreState getStoreState();

    ExecutionNodeVisitor<Result> getVisitor(MutableList<CommonProfile> profiles, ExecutionState executionState);

    StoreExecutionState copy();

    RuntimeContext getRuntimeContext();

    void setRuntimeContext(RuntimeContext runtimeContext);

    class RuntimeContext
    {
        final MutableMap<String, String> contextParams = Maps.mutable.empty();

        public static RuntimeContext newWith(Map<String, String> contextParams)
        {
            return new RuntimeContext(contextParams);
        }

        public static RuntimeContext empty()
        {
            return new RuntimeContext();
        }

        private RuntimeContext(Map<String, String> contextParams)
        {
            this.contextParams.putAll(contextParams);
        }

        private RuntimeContext()
        {
        }

        public ImmutableMap<String, String> getContextParams()
        {
            return contextParams.toImmutable();
        }
    }
}
