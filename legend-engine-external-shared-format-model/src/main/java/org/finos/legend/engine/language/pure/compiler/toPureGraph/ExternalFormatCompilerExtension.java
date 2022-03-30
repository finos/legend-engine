
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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionExpressionBuilderRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.protocol.pure.PureClientVersions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExternalFormatCompilerExtension implements CompilerExtension
{
    private final Map<String, ExternalFormatExtension> externalFormatExtensions;
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
        List<String> elements = versions.collect(v -> "meta::protocols::pure::"  + v + "::external::shared::format::serializerExtension_String_1__SerializerExtension_1_").toList();
        externalFormatExtensions.values().forEach(x -> elements.addAll(x.getRegisterablePackageableElementNames()));
        return ListIterate.collect(elements, this::registerElement);
    }

    private Procedure<Procedure2<String, List<String>>> registerElement(String element)
    {
        int pos = element.lastIndexOf("::");
        String pkg = element.substring(0, pos);
        String name = element.substring(pos+2);
        return (Procedure2<String, List<String>> registerElementForPathToElement) ->
        {
            registerElementForPathToElement.value(pkg, Lists.mutable.with(name));
        };
    }

    @Override
    public List<Function<Handlers, List<FunctionExpressionBuilderRegistrationInfo>>> getExtraFunctionExpressionBuilderRegistrationInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                Lists.mutable.with(
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(
                                        handlers.m(handlers.h("meta::external::shared::format::executionPlan::externalize_Checked_MANY__Binding_1__String_1_", false, ps -> handlers.res("String", "one"), ps -> ps.size() == 2 && "Checked".equals(ps.get(0)._genericType()._rawType()._name()))),
                                        handlers.m(handlers.h("meta::external::shared::format::executionPlan::externalize_T_MANY__Binding_1__String_1_", false, ps -> handlers.res("String", "one"), ps -> ps.size() == 2))
                                )
                        ),
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(
                                        handlers.m(handlers.h("meta::external::shared::format::executionPlan::internalize_Binding_1__Class_1__String_1__T_MANY_", false, ps -> handlers.res(ps.get(1)._genericType()._typeArguments().getFirst(), "zeroMany"), ps -> ps.size() == 3 && "String".equals(ps.get(2)._genericType()._rawType()._name()))),
                                        handlers.m(handlers.h("meta::external::shared::format::executionPlan::internalize_Binding_1__Class_1__ByteStream_1__T_MANY_", false, ps -> handlers.res(ps.get(1)._genericType()._typeArguments().getFirst(), "zeroMany"), ps -> ps.size() == 3 && "ByteStream".equals(ps.get(2)._genericType()._rawType()._name()))),
                                        handlers.m(handlers.h("meta::external::shared::format::executionPlan::internalize_Binding_1__Class_1__Url_1__T_MANY_", false, ps -> handlers.res(ps.get(1)._genericType()._typeArguments().getFirst(), "zeroMany"), ps -> ps.size() == 3 && ps.get(2)._genericType()._rawType()._name().endsWith("Url")))
                                )
                        )
                ));
    }
}
