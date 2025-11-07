// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.variant.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.extension.external.variant.compiled.natives.FromJson;
import org.finos.legend.pure.runtime.java.extension.external.variant.compiled.natives.To;
import org.finos.legend.pure.runtime.java.extension.external.variant.compiled.natives.ToJson;
import org.finos.legend.pure.runtime.java.extension.external.variant.compiled.natives.ToMany;
import org.finos.legend.pure.runtime.java.extension.external.variant.compiled.natives.ToVariant;

import java.util.List;

public class VariantExtensionCompiled implements CompiledExtension
{
    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(
                new To(),
                new ToJson(),
                new ToMany(),
                new FromJson(),
                new ToVariant()
        );
    }

    @Override
    public String getRelatedRepository()
    {
        return "core_functions_variant";
    }
}
