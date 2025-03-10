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

package org.finos.legend.engine.protocol.hostedService.deployment;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentDetails;
import org.finos.legend.engine.protocol.functionActivator.postDeployment.ActionContent;
import org.finos.legend.engine.protocol.hostedService.deployment.model.GenerationInfoData;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;

import java.util.List;

public class HostedServiceDeploymentDetails extends FunctionActivatorDeploymentDetails
{
    public String pattern;
    public String ownership;
    public AlloySDLC versionInfo;
    public String generationId;
    public String linkedSpecification;
    public GenerationInfoData generation;
    public List<ActionContent> actions = Lists.mutable.empty();
    
    public HostedServiceDeploymentDetails()
    {
        
    }
    
    public HostedServiceDeploymentDetails(String pattern, String ownership, AlloySDLC versionInfo, String generationId, String linkedSpecification, GenerationInfoData generation, List<ActionContent> actions)
    {
        this.pattern = pattern;
        this.ownership = ownership;
        this.versionInfo = versionInfo;
        this.generationId = generationId;
        this.linkedSpecification = linkedSpecification;
        this.generation = generation;
        this.actions = actions;
    }
}
