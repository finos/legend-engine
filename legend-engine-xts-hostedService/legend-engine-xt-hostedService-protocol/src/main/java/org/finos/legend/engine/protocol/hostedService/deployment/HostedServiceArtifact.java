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

//import org.finos.legend.engine.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.hostedService.deployment.model.GenerationInfo;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;

public class HostedServiceArtifact extends FunctionActivatorArtifact
{

    public HostedServiceArtifact()
    {

    }

    public HostedServiceArtifact(GenerationInfo info)
    {
        this.content = new HostedServiceContent(info);
    }

    public HostedServiceArtifact(GenerationInfo info, PureModelContextData serviceData)
    {
        this.content = new HostedServiceContent(info, serviceData);
    }


}
