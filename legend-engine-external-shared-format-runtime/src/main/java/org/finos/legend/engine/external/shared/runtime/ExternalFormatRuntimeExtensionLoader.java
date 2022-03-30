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

package org.finos.legend.engine.external.shared.runtime;

import org.eclipse.collections.api.factory.Maps;

import java.util.Map;
import java.util.ServiceLoader;

public class ExternalFormatRuntimeExtensionLoader
{
    public static Map<String, ExternalFormatRuntimeExtension> extensions()
    {
        Map<String, ExternalFormatRuntimeExtension> result = Maps.mutable.empty();
        for (ExternalFormatRuntimeExtension extension : ServiceLoader.load(ExternalFormatRuntimeExtension.class))
        {
            for (String contentType: extension.getContentTypes())
            {
                if (result.put(contentType, extension) != null)
                {
                    throw new IllegalArgumentException("Conflicting runtime extension for external format content type: " + contentType);
                }
            }
        }
        return result;
    }
}
