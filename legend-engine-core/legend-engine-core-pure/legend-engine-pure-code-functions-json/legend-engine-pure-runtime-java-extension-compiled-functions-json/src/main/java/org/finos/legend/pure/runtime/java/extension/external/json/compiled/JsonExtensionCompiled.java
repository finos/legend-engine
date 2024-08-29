// Copyright 2020 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.AbstractCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.EqualJsonStrings;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.Escape;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.FromJson;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.FromJsonDeprecated;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.ParseJSON;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.ToJsonBeta;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonParser;

import java.util.List;

public class JsonExtensionCompiled extends AbstractCompiledExtension
{
    static
    {
        JsonParser.processor = new CompiledJsonExtraTypeProcessor();
    }

    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.with(loadExtraJavaSource("org.finos.legend.pure.generated", "JsonGen", "org/finos/legend/pure/runtime/java/extension/external/json/compiled/JsonGen.java"));
    }

    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(
                new EqualJsonStrings(),
                new Escape(),
                new FromJson(),
                new FromJsonDeprecated(),
                new ParseJSON(),
                new ToJsonBeta()
        );
    }

    @Override
    public String getRelatedRepository()
    {
        return "core_functions_json";
    }

    public static CompiledExtension extension()
    {
        return new JsonExtensionCompiled();
    }
}
