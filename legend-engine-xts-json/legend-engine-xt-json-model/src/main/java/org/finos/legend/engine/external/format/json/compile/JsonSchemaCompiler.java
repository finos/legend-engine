//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.format.json.compile;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalSchemaCompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionExpressionBuilderRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JsonSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JsonSchema_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_schema_fromSchema_SchemaInput;
import org.finos.legend.pure.generated.Root_meta_external_format_json_schema_fromSchema_SchemaInput_Impl;
import org.finos.legend.pure.generated.core_external_format_json_transformation_toBeRefactored_fromJSONSchema;

import java.util.Collections;
import java.util.List;

public class JsonSchemaCompiler implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("External_Format", "JSON");
    }

    public Root_meta_external_format_json_metamodel_JsonSchema compile(ExternalSchemaCompileContext context)
    {
        String content = context.getContent();
        String location = context.getLocation();

        // validation step
        Root_meta_external_format_json_schema_fromSchema_SchemaInput schemaInput =
                new Root_meta_external_format_json_schema_fromSchema_SchemaInput_Impl("", null, context.getPureModel().getClass("meta::external::format::json::schema::fromSchema::SchemaInput"))
                        ._fileName(location)
                        ._schema(content);
        core_external_format_json_transformation_toBeRefactored_fromJSONSchema.Root_meta_external_format_json_schema_fromSchema_JSONSchemaToPure_SchemaInput_MANY__PackageableElement_MANY_(Lists.mutable.with(schemaInput), context.getPureModel().getExecutionSupport());

        return new Root_meta_external_format_json_metamodel_JsonSchema_Impl("", null, context.getPureModel().getClass("meta::external::format::json::metamodel::JsonSchema"))
                ._content(content);
    }

    @Override
    public List<Function<Handlers, List<FunctionExpressionBuilderRegistrationInfo>>> getExtraFunctionExpressionBuilderRegistrationInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                Lists.mutable.with(
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(handlers.h("meta::json::schema::mapSchema_String_1__Type_1__DiscriminatorMapping_1_", false, ps -> handlers.res("meta::external::format::json::schema::DiscriminatorMapping", "one"), ps -> ps.size() == 2))
                        ),
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(handlers.h("meta::json::schema::discriminateOneOf_Any_1__Any_1__Type_MANY__DiscriminatorMapping_MANY__Boolean_1_", false, ps -> handlers.res("Boolean", "one"), ps -> ps.size() == 4))
                        ),
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(handlers.h("meta::external::format::json::functions::toJson_T_MANY__RootGraphFetchTree_1__String_1_", false, ps -> handlers.res("String", "one"), ps -> ps.size() == 2))
                        ),
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(
                                        handlers.h("meta::external::format::json::functions::fromJson_Class_1__String_1__T_MANY_", false, ps -> handlers.res(ps.get(0)._genericType()._typeArguments().getFirst(), "zeroMany"), ps -> Lists.mutable.with(ps.get(0)._genericType()._typeArguments().getFirst()), ps -> ps.size() == 2 && "String".equals(ps.get(1)._genericType()._rawType()._name())),
                                        handlers.h("meta::external::format::json::functions::fromJson_Class_1__Byte_MANY__T_MANY_", false, ps -> handlers.res(ps.get(0)._genericType()._typeArguments().getFirst(), "zeroMany"), ps -> Lists.mutable.with(ps.get(0)._genericType()._typeArguments().getFirst()), ps -> ps.size() == 2 && "Byte".equals(ps.get(1)._genericType()._rawType()._name()))
                                )
                        )
                ));
    }

    @Override
    public CompilerExtension build()
    {
        return new JsonSchemaCompiler();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.mutable.empty();
    }
}
