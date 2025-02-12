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

package org.finos.legend.engine.deployment.manager;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.deployment.model.DeploymentExtension;
import org.finos.legend.engine.deployment.model.DeploymentExtensionLoader;
import org.finos.legend.engine.deployment.model.DeploymentResponse;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DeploymentManager
{

    private final PureModelContextData pureModelContextData;
    private final List<PackageableElement> elements;
    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentManager.class);
    public List<DeploymentExtension> extensions;

    public DeploymentManager(PureModelContextData pureModelContextData, List<PackageableElement> elements, List<DeploymentExtension> extensions)
    {
        this.pureModelContextData = pureModelContextData;
        this.elements = elements;
        this.extensions = extensions;
    }

    public DeploymentManager(PureModelContextData pureModelContextData, List<PackageableElement> elements)
    {
        this(pureModelContextData, elements, DeploymentExtensionLoader.extensions());
    }

    public DeploymentResponse deployElement(String elementPath)
    {
        PackageableElement element = this.findElement(pureModelContextData, elementPath);
        if (element == null)
        {
            return null;
        }
        for (DeploymentExtension extension : extensions)
        {
            if (extension.isElementDeployable(element))
            {
                try
                {
                    LOGGER.info("Start deploying '{}' for element '{}'", extension.getKey(), element.getPath());
                    DeploymentResponse response = extension.deployElement(this.pureModelContextData, element);
                    LOGGER.info("Start deploying '{}' for element '{}'", extension.getKey(), element.getPath());
                    return  response;
                }
                catch (Exception exception)
                {
                    LOGGER.error("Error generating deploying for extension '{}' for element '{}':", extension.getClass(), element.getPath(), exception);
                    throw exception;
                }
            }
        }
        return null;
    }

    public DeploymentResponse validateElement(String elementPath)
    {
        PackageableElement element = this.findElement(pureModelContextData, elementPath);
        if (element == null)
        {
            return null;
        }
        for (DeploymentExtension extension : extensions)
        {
            if (extension.isElementDeployable(element))
            {
                try
                {
                    LOGGER.info("Start validating '{}' for element '{}'", extension.getKey(), element.getPath());
                    DeploymentResponse response = extension.validateElement(this.pureModelContextData, element);
                    LOGGER.info("Start validating '{}' for element '{}'", extension.getKey(), element.getPath());
                    return  response;
                }
                catch (Exception exception)
                {
                    LOGGER.error("Error generating validating for extension '{}' for element '{}':", extension.getClass(), element.getPath(), exception);
                    throw exception;
                }
            }
        }
        return null;
    }

    public List<DeploymentResponse> deploy()
    {
        List<DeploymentResponse> responses = Lists.mutable.empty();
        if (extensions.isEmpty() || this.elements.isEmpty())
        {
            return responses;
        }
        for (DeploymentExtension extension : extensions)
        {
            try
            {
                LOGGER.info("Start deploying all elements for extension '{}'", extension.getKey());
                List<DeploymentResponse> response = extension.deployAll(this.pureModelContextData, this.elements);
                if (response != null)
                {
                    responses.addAll(response);
                }
                LOGGER.info("Stop deploying all elements for extension '{}'", extension.getKey());
            }
            catch (Exception exception)
            {
                LOGGER.info("Error deploying all elements for extension '{}'", extension.getKey());
                throw exception;
            }

        }
        return responses;
    }

    public List<DeploymentResponse> validate()
    {
        List<DeploymentResponse> responses = Lists.mutable.empty();
        if (extensions.isEmpty() || this.elements.isEmpty())
        {
            return responses;
        }
        for (DeploymentExtension extension : extensions)
        {
            try
            {
                LOGGER.info("Start validating all elements for extension '{}'", extension.getKey());
                List<DeploymentResponse> response = extension.validateAll(this.pureModelContextData, this.elements);
                if (response != null)
                {
                    responses.addAll(response);
                }
                LOGGER.info("Stop validating all elements for extension '{}'", extension.getKey());
            }
            catch (Exception exception)
            {
                LOGGER.info("Error validating all elements for extension '{}'", extension.getKey());
                throw exception;
            }

        }
        return responses;
    }

    private PackageableElement findElement(PureModelContextData modelContextData, String elementPath)
    {
        return modelContextData.getElements().stream().filter(e -> e.getPath().equals(elementPath)).findFirst().orElse(null);
    }


}
