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

package org.finos.legend.engine.language.pure.dsl.generation.project;

/**
 * A failure of the file or artifact generation step, with what it failed on: the file
 * generation, or the element and the artifact extension.
 */
final class ProjectGenerationStepException extends RuntimeException
{
    final RuntimeException failure;
    final String fileGeneration;
    final String element;
    final String extension;

    ProjectGenerationStepException(RuntimeException failure, String fileGeneration)
    {
        this(failure, fileGeneration, null, null);
    }

    ProjectGenerationStepException(RuntimeException failure, String element, String extension)
    {
        this(failure, null, element, extension);
    }

    private ProjectGenerationStepException(RuntimeException failure, String fileGeneration, String element, String extension)
    {
        super(failure.getMessage(), failure);
        this.failure = failure;
        this.fileGeneration = fileGeneration;
        this.element = element;
        this.extension = extension;
    }
}
