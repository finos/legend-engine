// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.PureClientVersions;

import java.util.List;
import java.util.Map;

public class ExternalFormatCompilerExtension implements CompilerExtension
{
    private final Map<String, ExternalFormatExtension<?>> externalFormatExtensions;
    final SchemaSetCompiler schemaSetCompiler;
    final BindingCompiler bindingCompiler;

    public ExternalFormatCompilerExtension()
    {
        externalFormatExtensions = ExternalFormatExtensionLoader.extensions();
        schemaSetCompiler = new SchemaSetCompiler(externalFormatExtensions);
        bindingCompiler = new BindingCompiler(externalFormatExtensions);
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.with(schemaSetCompiler.getProcessor(), bindingCompiler.getProcessor());
    }

    @Override
    public List<Procedure<Procedure2<String, List<String>>>> getExtraElementForPathToElementRegisters()
    {
        ImmutableList<String> versions = PureClientVersions.versionsSince("v1_21_0");
        List<String> elements = versions.collect(v -> "meta::protocols::pure::" + v + "::external::shared::format::serializerExtension_String_1__SerializerExtension_1_").toList();
        return ListIterate.collect(elements, this::registerElement);
    }

    private Procedure<Procedure2<String, List<String>>> registerElement(String element)
    {
        int pos = element.lastIndexOf("::");
        String pkg = element.substring(0, pos);
        String name = element.substring(pos + 2);
        return (Procedure2<String, List<String>> registerElementForPathToElement) ->
        {
            registerElementForPathToElement.value(pkg, Lists.mutable.with(name));
        };
    }
}
