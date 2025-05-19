// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment;

import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.postDeployment.ActionContent;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;

import java.util.List;

public class SnowflakeM2MUdfArtifact extends FunctionActivatorArtifact
{
    public String deployedLocation;

    public SnowflakeM2MUdfArtifact()
    {
        //empty artifact
    }

    public SnowflakeM2MUdfArtifact(SnowflakeM2MUdfContent content, AlloySDLC sdlc)
    {
        this.content = content;
        if (sdlc != null)
        {
            this.version = getVersionInfo(sdlc);
        }
    }

    public SnowflakeM2MUdfArtifact(SnowflakeM2MUdfContent content, List<ActionContent> actions, AlloySDLC sdlc)
    {
        this.content = content;
        this.actions = actions;
        if (sdlc != null)
        {
            this.version = getVersionInfo(sdlc);
        }
    }

    public SnowflakeM2MUdfArtifact(SnowflakeM2MUdfContent content, SnowflakeM2MUdfDeploymentConfiguration config, String deployedLocation, AlloySDLC sdlc)
    {
        this(content, sdlc);
        this.deploymentConfiguration = config;
        this.deployedLocation = deployedLocation;
    }

    public SnowflakeM2MUdfArtifact(SnowflakeM2MUdfContent content, SnowflakeM2MUdfDeploymentConfiguration config, String deployedLocation, List<ActionContent> actions, AlloySDLC sdlc)
    {
        this(content, actions, sdlc);
        this.deploymentConfiguration = config;
        this.deployedLocation = deployedLocation;
    }

}
