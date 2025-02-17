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

package org.finos.legend.engine.deployment.model;

import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.extension.LegendGenerationExtension;

import java.util.List;
import java.util.stream.Collectors;


public interface DeploymentExtension extends LegendGenerationExtension
{

    default String type()
    {
        return "Deployment_Configuration";
    }

    /**
     * Gives the key for the deployment extension
     *
     * @return string
     */
    String getKey();

    /**
     * Gives the label for extension for user facing messaging
     * if different from key
     *
     * @return string
     */
    default String getLabel()
    {
        return this.getKey();
    }

    /**
     * Gives supported classifier paths for extension
     * important as used for filtering non-compiled elements
     *
     * @return List of classifier paths
     */
    List<String> getSupportedClassifierPaths();


    /**
     * Determines whether a packageable element can deploy
     *
     * @return boolean flag indicating if the extension can deploy
     */
    boolean isElementDeployable(PackageableElement element);


    /**
     * Deploys a packageable element. Methods assumes element can deploy
     * artifacts
     *
     * @return DeploymentResponse
     * @throws RuntimeException if unable to deploy.
     */
    DeploymentResponse deployElement(PureModelContextData data, PackageableElement element);


    default List<DeploymentResponse> deployAll(PureModelContextData model, List<PackageableElement> filteredElements)
    {
        return filteredElements.stream().filter(this::isElementDeployable).map(el -> this.deployElement(model, el)).collect(Collectors.toList());
    }

    /**
     * Handles deployment on a model level. Will deploy all relevant elements in the model.
     * defaults to filter out and deploy each deployable element
     *
     * @return a list of DeploymentResponse
     * @throws RuntimeException if unable to deploy.
     */
    default List<DeploymentResponse> deployAll(PureModelContextData model)
    {
        return deployAll(model, model.getElements());
    }

    /**
     * Provides guidance of whether an element requires validation for deployment
     *
     * @return boolean
     */
    default boolean requiresValidation()
    {
        return false;
    }

    /**
     * Validates a packageable element. Methods assumes element can deploy
     * artifacts
     *
     * @return DeploymentResponse
     * @throws RuntimeException if unable to deploy.
     */
    default DeploymentResponse validateElement(PureModelContextData data, PackageableElement element)
    {
        return null;
    }

    /**
     * Handles validate on a model level. Will deploy all relevant elements in the model.
     * defaults to filter out and deploy each deployable element
     *
     * @return a list of DeploymentResponse
     * @throws RuntimeException if unable to deploy.
     */
    default List<DeploymentResponse> validateAll(PureModelContextData model)
    {
        return validateAll(model, model.getElements());
    }


    default List<DeploymentResponse> validateAll(PureModelContextData model, List<PackageableElement> filteredElements)
    {
        return filteredElements.stream().filter(this::isElementDeployable).map(el -> this.validateElement(model, el)).collect(Collectors.toList());
    }



}
