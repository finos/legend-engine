// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.json.compiled;

import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonExtraTypeProcessor;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializationContext;

public class CompiledJsonExtraTypeProcessor implements JsonExtraTypeProcessor
{
    @Override
    public Object process(Object pureObject, JsonSerializationContext jsonSerializationContext)
    {
        if (pureObject instanceof PureMap)
        {
            return PureMapSerializer.toJson((PureMap) pureObject, jsonSerializationContext);
        }
        return null;
    }
}
