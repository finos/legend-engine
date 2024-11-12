//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.protocol.hostedService.deployment;

import java.util.List;
import org.finos.legend.engine.protocol.functionActivator.deployment.DeploymentResult;

public class HostedServiceDeploymentResult extends DeploymentResult
{
    public String error;
    public String deployed;
    public String generationId;
    public List<String> errors;

    public HostedServiceDeploymentResult()
    {
        //jackson
    }

    public HostedServiceDeploymentResult(List<String> errors)
    {
        this.errors = errors;
    }

    public HostedServiceDeploymentResult(String deployed, String generationId)
    {
        this.successful = true;
        this.deployed = deployed;
        this.generationId = generationId;
    }


}
