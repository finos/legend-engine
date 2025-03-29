// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.protocol.functionJar.deployment;

import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentContent;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;

public class FunctionJarContent extends FunctionActivatorDeploymentContent
{
    public PureModelContext functionJarData;
    public ExecutionPlan executionPlan;

    public FunctionJarContent()
    {
        //jackson
    }

    public FunctionJarContent(String ownership)
    {
        this.ownership = ownership;
    }

    public FunctionJarContent(PureModelContext functionJarData, String ownership)
    {
        this(ownership);
        this.functionJarData = functionJarData;
    }

    public FunctionJarContent(PureModelContext functionJarData, String ownership, ExecutionPlan executionPlan)
    {
        this(ownership);
        this.functionJarData = functionJarData;
        this.executionPlan = executionPlan;
    }
}
