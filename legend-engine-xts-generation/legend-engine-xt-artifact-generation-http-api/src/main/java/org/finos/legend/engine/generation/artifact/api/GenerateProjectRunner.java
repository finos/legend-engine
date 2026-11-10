// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.generation.artifact.api;

import org.finos.legend.engine.language.pure.dsl.generation.project.ProjectGenerationResult;
import org.finos.legend.engine.language.pure.dsl.generation.project.ProjectGenerationRunner;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.identity.Identity;

/**
 * Loads the model of a {@link ProjectGenerationInput} and runs {@link ProjectGenerationRunner}
 * over it. The runner compiles the model itself, the way the build does.
 */
public class GenerateProjectRunner
{
    private final ModelManager modelManager;

    public GenerateProjectRunner(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    /**
     * @throws IllegalArgumentException when the request is not one that can run, such as one
     *                                  without a model or including an artifact extension this
     *                                  server doesn't have; nothing has run then
     */
    public ProjectGenerationResult run(ProjectGenerationInput input, Identity identity)
    {
        if ((input == null) || (input.model == null))
        {
            throw new IllegalArgumentException("A model is required");
        }
        String clientVersion = (input.clientVersion == null) ? PureClientVersions.production : input.clientVersion;
        // rejected before the model is loaded, which can mean fetching it
        ProjectGenerationRunner.validate(input.artifactGenerationConfig);
        PureModelContextData model = this.modelManager.loadData(input.model, clientVersion, identity);
        return ProjectGenerationRunner.newRunner(model)
                .withProjectElementPaths(input.projectElementPaths)
                .withClientVersion(clientVersion)
                .withModelGenerationConfig(input.modelGenerationConfig)
                .withFileGenerationConfig(input.fileGenerationConfig)
                .withArtifactGenerationConfig(input.artifactGenerationConfig)
                .run();
    }
}
