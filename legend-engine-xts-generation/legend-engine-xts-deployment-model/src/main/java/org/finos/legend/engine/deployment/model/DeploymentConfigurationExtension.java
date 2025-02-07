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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.extension.LegendGenerationExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;


public interface DeploymentConfigurationExtension  extends LegendGenerationExtension
{

    default String type()
    {
        return "Deployment_Configuration";
    }

    /**
     * Gives the key for the extension
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
     * @return boolean flag indicating if the extension can generate artifacts
     */
    boolean canDeploy(PackageableElement element);


    /**
     * Deploys a packageable element. Methods assumes element can deploy
     * artifacts
     *
     * @return a list of GenerationOutput
     * @throws RuntimeException if unable to deply.
     */
    DeploymentResponse deploy(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion);


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
     * vALIDATES a packageable element. Methods assumes element can deploy
     * artifacts
     *
     * @return a list of GenerationOutput
     * @throws RuntimeException if unable to deply.
     */
    default DeploymentResponse validate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        return null;
    }

}
