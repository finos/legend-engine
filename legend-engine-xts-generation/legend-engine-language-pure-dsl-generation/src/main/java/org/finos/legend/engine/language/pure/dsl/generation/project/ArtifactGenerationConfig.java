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

import java.util.Set;

/**
 * How {@link ProjectGenerationRunner} runs artifact generation. The build runs every artifact
 * extension it has, which is what leaving everything here unset does.
 */
public class ArtifactGenerationConfig
{
    /**
     * Skips artifact generation.
     */
    public boolean disabled;

    /**
     * Only these extensions run. Unset or empty means every extension. A key no extension on
     * this server has is rejected before anything runs.
     */
    public Set<String> includedExtensionKeys;

    /**
     * These extensions don't run, after {@link #includedExtensionKeys} has narrowed the set, so a
     * key in both is excluded. A key no extension has is ignored.
     */
    public Set<String> excludedExtensionKeys;
}
