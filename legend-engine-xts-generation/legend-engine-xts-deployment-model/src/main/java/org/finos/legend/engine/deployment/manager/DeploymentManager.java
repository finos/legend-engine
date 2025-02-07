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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.deployment.model.DeploymentConfigurationExtension;
import org.finos.legend.engine.deployment.model.DeploymentResponse;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class DeploymentManager
{

    private final PureModel pureModel;
    private final PureModelContextData pureModelContextData;
    private final List<PackageableElement> elements;
    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentManager.class);
    public List<DeploymentConfigurationExtension> extensions;


    public DeploymentManager(PureModelContextData pureModelContextData, PureModel pureModel, List<PackageableElement> elements, List<DeploymentConfigurationExtension> extensions)
    {
        this.pureModel = pureModel;
        this.pureModelContextData = pureModelContextData;
        this.elements = elements;
        this.extensions = extensions;
    }

    public DeploymentManager(PureModelContextData pureModelContextData, PureModel pureModel, List<PackageableElement> elements)
    {
        this(pureModelContextData, pureModel, elements, extensions());
    }

    public static List<DeploymentConfigurationExtension> extensions()
    {
        List<DeploymentConfigurationExtension> extensions = Lists.mutable.withAll(ServiceLoader.load(DeploymentConfigurationExtension.class));
        Set<String> extensionKeys = Sets.mutable.empty();
        for (DeploymentConfigurationExtension extension : extensions)
        {
            if (!extensionKeys.add(extension.getKey()))
            {
                String extensionsWithSameKey = ListIterate.collect(extensions.stream().filter(e -> e.getKey().equals(extension.getKey())).collect(Collectors.toList()), e -> e.getClass().getName())
                        .makeString(",");
                throw new EngineException("Deployment extension keys must be unique. Found duplicate key: '" + extension.getKey() + "' on extensions: " + extensionsWithSameKey);
            }
        }
        return extensions;
    }

    public List<DeploymentResponse> deploy()
    {
        List<DeploymentResponse> responses = Lists.mutable.empty();
        if (extensions.isEmpty() || this.elements.isEmpty())
        {
            return responses;
        }
        for (PackageableElement element : this.elements)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = this.findPackageableElement(pureModel, element);
            if (packageableElement != null)
            {
                for (DeploymentConfigurationExtension extension : extensions)
                {
                    if (extension.canDeploy(packageableElement))
                    {
                        try
                        {
                            LOGGER.info("Start deploying '{}' for element '{}'", extension.getKey(), element.getPath());
                            DeploymentResponse response = extension.deploy(packageableElement, this.pureModel, this.pureModelContextData, PureClientVersions.production);
                            if (response != null)
                            {
                                responses.add(response);
                            }
                            LOGGER.info("Start deploying '{}' for element '{}'", extension.getKey(), element.getPath());
                        }
                        catch (Exception exception)
                        {
                            LOGGER.error("Error generating deploying for extension '{}' for element '{}':", extension.getClass(), element.getPath(), exception);
                            throw exception;
                        }

                    }
                }
            }
        }
        return responses;
    }

    public List<DeploymentResponse> validate()
    {
        List<DeploymentResponse> responses = Lists.mutable.empty();
        List<DeploymentConfigurationExtension> validateExtensions = extensions.stream().filter(DeploymentConfigurationExtension::requiresValidation).collect(Collectors.toList());
        if (validateExtensions.isEmpty() || this.elements.isEmpty())
        {
            return responses;
        }
        for (PackageableElement element : this.elements)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = this.findPackageableElement(pureModel, element);
            if (packageableElement != null)
            {
                for (DeploymentConfigurationExtension extension : validateExtensions)
                {
                    if (extension.canDeploy(packageableElement))
                    {
                        try
                        {
                            LOGGER.info("Start validate '{}' for element '{}'", extension.getKey(), element.getPath());
                            DeploymentResponse validateResponse = extension.validate(packageableElement, this.pureModel, this.pureModelContextData, PureClientVersions.production);
                            if (validateResponse != null)
                            {
                                responses.add(validateResponse);
                            }
                            LOGGER.info("Start validate '{}' for element '{}'", extension.getKey(), element.getPath());
                        }
                        catch (Exception exception)
                        {
                            LOGGER.error("Error validate validating for extension '{}' for element '{}':", extension.getClass(), element.getPath(), exception);
                            throw exception;
                        }

                    }
                }
            }
        }
        return responses;
    }

    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement findPackageableElement(PureModel pureModel, PackageableElement packageableElement)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element = pureModel.getPackageableElement_safe(packageableElement.getPath());
        if (element != null)
        {
            return element;
        }
        if (packageableElement instanceof Function)
        {
            Function elementFunc = (Function) packageableElement;
            String fullPath = pureModel.buildPackageString(packageableElement._package, HelperModelBuilder.getSignature(elementFunc));
            element = pureModel.getPackageableElement_safe(fullPath);
        }
        if (element == null)
        {
            LOGGER.debug("Unable to find element '{}' in Pure Model", packageableElement.getPath());
        }
        return element;
    }

}
