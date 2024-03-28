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

package org.finos.legend.engine.generation.artifact;

import java.util.List;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

public class ArtifactGenerationResult
{

    private final PackageableElement element;
    private final List<GenerationOutput> results;
    private final ArtifactGenerationExtension generator;

    public ArtifactGenerationResult(PackageableElement element, List<GenerationOutput> results, ArtifactGenerationExtension generator)
    {
        this.element = element;
        this.results = results;
        this.generator = generator;
    }

    public ArtifactGenerationExtension getGenerator()
    {
        return this.generator;
    }

    public List<GenerationOutput> getResults()
    {
        return this.results;
    }

    public PackageableElement getElement()
    {
        return this.element;
    }
}
