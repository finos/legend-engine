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

package org.finos.legend.engine.deployment.extension;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.deployment.model.DeploymentExtension;
import org.finos.legend.engine.deployment.model.DeploymentResponse;
import org.finos.legend.engine.deployment.model.DeploymentStatus;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.SDLC;
import org.slf4j.Logger;

import java.util.List;

public class ClassDeploymentExtension implements DeploymentExtension
{

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ClassDeploymentExtension.class);

    @Override
    public String getKey()
    {
        return "classDeploymentTest";
    }

    @Override
    public boolean requiresValidation()
    {
        return true;
    }


    @Override
    public List<String> getSupportedClassifierPaths()
    {
        return Lists.mutable.with("meta::pure::metamodel::type::Class");
    }

    @Override
    public boolean isElementDeployable(org.finos.legend.engine.protocol.pure.m3.PackageableElement element)
    {
        return element instanceof org.finos.legend.engine.protocol.pure.m3.type.Class;
    }

    @Override
    public DeploymentResponse validateElement(PureModelContextData data, org.finos.legend.engine.protocol.pure.m3.PackageableElement element)
    {
        return new DeploymentResponse(this.getKey(), element.getPath(), DeploymentStatus.SUCCESS, "Validate passed");
    }

    @Override
    public DeploymentResponse deployElement(PureModelContextData data, org.finos.legend.engine.protocol.pure.m3.PackageableElement element)
    {
        String elementPath = element.getPath();
        LOGGER.info("lets start testing on this element" + elementPath);
        DeploymentResponse response = new DeploymentResponse(this.getKey());
        response.status = DeploymentStatus.SUCCESS;
        PureModelContextPointer pointer = data.getOrigin();
        if (pointer == null)
        {
            return new DeploymentResponse(this.getKey(), elementPath, DeploymentStatus.ERROR, "Pure Model Context Data pointer required");
        }
        SDLC sdlc = pointer.sdlcInfo;
        if (sdlc == null)
        {
            return new DeploymentResponse(this.getKey(), elementPath, DeploymentStatus.ERROR, "Pure Model Context Data pointer sdlc required");
        }
        if (!(sdlc instanceof AlloySDLC))
        {
            return new DeploymentResponse(this.getKey(), elementPath, DeploymentStatus.ERROR, "Pure Model Context Data pointer alloy sdlc required");
        }
        response.message = "successfully deployed";
        return response;
    }


}