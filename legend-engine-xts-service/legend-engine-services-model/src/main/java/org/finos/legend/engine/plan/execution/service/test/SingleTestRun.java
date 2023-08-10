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

package org.finos.legend.engine.plan.execution.service.test;

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;

import java.util.Collection;
import java.util.Map;

public class SingleTestRun extends TestRun
{
    public ExecutionPlan executionPlan;
    public JavaCode asserts;
    public Map<String, Boolean> results;

    public SingleTestRun()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public SingleTestRun(ExecutionPlan executionPlan, JavaCode asserts, Map<String, Boolean> results)
    {
        this.executionPlan = executionPlan;
        this.asserts = asserts;
        this.results = results;
    }

    @Override
    public Collection<Boolean> getResults(boolean dummy)
    {
        return this.results.values();
    }
}
