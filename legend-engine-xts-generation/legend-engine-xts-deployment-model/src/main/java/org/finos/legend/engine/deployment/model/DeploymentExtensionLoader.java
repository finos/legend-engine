//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.deployment.model;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class DeploymentExtensionLoader
{

    public static List<DeploymentExtension> extensions()
    {
        List<DeploymentExtension> extensions = Lists.mutable.withAll(ServiceLoader.load(DeploymentExtension.class));
        Set<String> extensionKeys = Sets.mutable.empty();
        for (DeploymentExtension extension : extensions)
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


    public static List<DeploymentExtensionMetadata> getExtensionsMetadata()
    {
        return extensions().stream().map(e -> new DeploymentExtensionMetadata(e.getKey(), e.getSupportedClassifierPaths())).collect(Collectors.toList());
    }

    public static ImmutableSet<String> getAllSupportedClassifierPaths()
    {
        return Sets.immutable.withAll(extensions().stream().map(DeploymentExtension::getSupportedClassifierPaths).flatMap(List::stream).collect(Collectors.toList()));
    }
}
