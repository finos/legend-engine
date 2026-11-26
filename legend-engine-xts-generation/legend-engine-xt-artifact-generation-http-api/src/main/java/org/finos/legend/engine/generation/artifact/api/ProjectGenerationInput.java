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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.language.pure.dsl.generation.project.ArtifactGenerationConfig;
import org.finos.legend.engine.language.pure.dsl.generation.project.FileGenerationConfig;
import org.finos.legend.engine.language.pure.dsl.generation.project.ModelGenerationConfig;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;

import java.util.Set;

/**
 * A request to generate what a project's build generates. Leaving out everything but the model
 * runs what the build runs.
 */
public class ProjectGenerationInput
{
    /**
     * The project's elements and its dependencies'.
     */
    @JsonProperty(required = true)
    public PureModelContext model;

    /**
     * The paths of the project's own elements: only they can hold the generation specification
     * and get artifacts. Unset or empty means every element of the model.
     */
    public Set<String> projectElementPaths;

    /**
     * Unset means the production client version, as the build uses.
     */
    public String clientVersion;

    public ModelGenerationConfig modelGenerationConfig;
    public FileGenerationConfig fileGenerationConfig;
    public ArtifactGenerationConfig artifactGenerationConfig;
}
