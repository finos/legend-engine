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
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class ArtifactGenerationExtensionLoader
{
    public static final String EXTENSION_KEY_REGEX = "^[a-zA-Z_\\-]+$";

    public static List<ArtifactGenerationExtension> extensions()
    {
        List<ArtifactGenerationExtension> extensions = Lists.mutable.withAll(ServiceLoader.load(ArtifactGenerationExtension.class));
        Set<String> extensionKeys = Sets.mutable.empty();
        for (ArtifactGenerationExtension extension : extensions)
        {
            if (!extensionKeys.add(extension.getKey()))
            {
                String extensionsWithSameKey = ListIterate.collect(extensions.stream().filter(e -> e.getKey().equals(extension.getKey())).collect(Collectors.toList()), e -> e.getClass().getName())
                    .makeString(",");
                throw new EngineException("Artifact extension keys must be unique. Found duplicate key: '" + extension.getKey() + "' on extensions: " + extensionsWithSameKey);
            }
            if (!extension.getKey().matches(EXTENSION_KEY_REGEX))
            {
                throw new EngineException("Artifact extension keys can't have spaces or special characters. Found invalid key: '" + extension.getKey() + "'.");
            }
        }
        return extensions;
    }

}
