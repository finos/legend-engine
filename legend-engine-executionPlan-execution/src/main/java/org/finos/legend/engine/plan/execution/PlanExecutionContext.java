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

package org.finos.legend.engine.plan.execution;

import org.finos.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.engine.shared.javaCompiler.JavaCompileException;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCache;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;

import java.util.Arrays;
import java.util.List;

public class PlanExecutionContext
{
    private final EngineJavaCompiler externalJavaCompiler;
    private final List<GraphFetchCache> graphFetchCaches;

    public PlanExecutionContext(SingleExecutionPlan singleExecutionPlan, GraphFetchCache... graphFetchCaches) throws JavaCompileException
    {
        this(singleExecutionPlan, Arrays.asList(graphFetchCaches));
    }

    public PlanExecutionContext(SingleExecutionPlan singleExecutionPlan, List<GraphFetchCache> graphFetchCaches) throws JavaCompileException
    {
        this.externalJavaCompiler = JavaHelper.compilePlan(singleExecutionPlan, null);
        this.graphFetchCaches = graphFetchCaches;
    }

    EngineJavaCompiler getExternalJavaCompiler()
    {
        return this.externalJavaCompiler;
    }

    List<GraphFetchCache> getGraphFetchCaches()
    {
        return this.graphFetchCaches;
    }
}
