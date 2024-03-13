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

package org.finos.legend.engine.protocol.functionActivator.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionActivatorArtifact
{
    public FunctionActivatorDeploymentContent content;
    public FunctionActivatorDeploymentConfiguration deploymentConfiguration;

    public String version;

    public String getVersionInfo(AlloySDLC sdlc)
    {
        if (this.version != null)
        {
            return sdlc.groupId + ":" + sdlc.artifactId + ":" + sdlc.version;
        }
        return "";
    }
}
