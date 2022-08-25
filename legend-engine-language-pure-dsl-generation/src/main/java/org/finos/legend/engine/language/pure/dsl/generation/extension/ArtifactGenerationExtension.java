// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.generation.extension;

import java.util.List;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

/**
 * This interface provides the specification for generating artifacts driven by a
 * packageable element.
 */
public interface ArtifactGenerationExtension
{


    /**
     * Determines whether the extension can generate artifacts based on the element.
     *
     * @return boolean flag indicating if the extension can generate artifacts
     */
    boolean canGenerate(PackageableElement element);


    /**
     * Gets root path where all artifacts will be stored
     *
     * @return root path
     */

    String getArtifactsRootPath();


    /**
     * Generates artifacts given a packageable element. Methods assumes element can generate
     * artifacts
     *
     * @return a list of GenerationOutput
     * @throws RuntimeException if unable to generate artifacts.
     */
    List<Artifact> generate(PackageableElement element, PureModel pureModel,
        PureModelContextData data, String clientVersion);

}
